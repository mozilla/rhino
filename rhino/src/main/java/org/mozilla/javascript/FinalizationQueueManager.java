/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages finalization cleanup tasks for all FinalizationRegistry instances. This class provides a
 * Cleaner-like infrastructure using PhantomReferences to properly handle JavaScript finalization
 * callbacks.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Single shared ReferenceQueue for all registries (efficiency)
 *   <li>PhantomReference-based for correct GC semantics
 *   <li>Integration with Context for JavaScript execution
 *   <li>Thread-safe operation
 * </ul>
 */
final class FinalizationQueueManager {

    // Configuration constants
    private static final int QUEUE_TIMEOUT_MS = 1000;
    private static final int ERROR_THRESHOLD = 10;
    private static final int ERROR_PAUSE_MS = 100;
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_PENDING_CLEANUPS = 10000; // Prevent unbounded growth
    private static final String BATCH_SIZE_PROPERTY = "rhino.finalization.batchSize";
    private static final String MAX_PENDING_PROPERTY = "rhino.finalization.maxPendingCleanups";
    private static final String THREAD_NAME = "Rhino-FinalizationQueue";

    // Singleton holder for lazy initialization
    private static class Holder {
        static final FinalizationQueueManager INSTANCE = new FinalizationQueueManager();
    }

    // Get the singleton instance
    static FinalizationQueueManager getInstance() {
        return Holder.INSTANCE;
    }

    // Shared queue for all phantom references
    private final ReferenceQueue<Scriptable> sharedQueue = new ReferenceQueue<>();

    // Maps phantom references to their cleanup tasks
    private final ConcurrentHashMap<PhantomReference<?>, CleanupTask> tasks =
            new ConcurrentHashMap<>();

    // Index for O(1) token-based unregistration
    private final ConcurrentHashMap<TokenKey, Set<PhantomReference<?>>> tokenIndex =
            new ConcurrentHashMap<>();

    // Pending cleanups when no Context is available
    private final ConcurrentLinkedQueue<CleanupTask> pendingCleanups =
            new ConcurrentLinkedQueue<>();

    // Background thread for processing phantom references
    private volatile Thread processorThread;

    // Flag to stop the processor thread
    private volatile boolean shutdown = false;

    // Statistics for monitoring (useful for debugging)
    private final AtomicLong totalRegistrations = new AtomicLong();
    private final AtomicLong totalCleanups = new AtomicLong();
    private final AtomicLong totalUnregistrations = new AtomicLong();

    /**
     * Data structure holding cleanup information. Stores all data needed since
     * PhantomReference.get() always returns null.
     */
    static class CleanupTask {
        // Use WeakReference to allow registry to be GC'd if no longer referenced
        final WeakReference<NativeFinalizationRegistry> registryRef;
        final Object heldValue;
        final Object unregisterToken;

        CleanupTask(NativeFinalizationRegistry registry, Object heldValue, Object unregisterToken) {
            this.registryRef = new WeakReference<>(registry);
            this.heldValue = heldValue;
            this.unregisterToken = unregisterToken;
        }
    }

    /**
     * Custom PhantomReference that stores cleanup data. We can't get the referent from a phantom
     * reference, so we store everything we need in the reference itself.
     */
    static class FinalizationPhantomReference extends PhantomReference<Scriptable> {
        private final CleanupTask task;

        FinalizationPhantomReference(
                Scriptable target, ReferenceQueue<? super Scriptable> queue, CleanupTask task) {
            super(target, queue);
            this.task = task;
        }

        CleanupTask getTask() {
            return task;
        }
    }

    /**
     * Key for the token index to enable O(1) unregistration lookups. The token is held strongly as
     * per ECMAScript spec - unregister tokens must remain valid until explicitly unregistered. The
     * registry uses WeakReference to allow GC when the registry itself is no longer reachable.
     */
    static class TokenKey {
        final WeakReference<NativeFinalizationRegistry> registryRef;
        final Object token; // Strong reference required by spec
        private final int hashCode;

        TokenKey(NativeFinalizationRegistry registry, Object token) {
            this.registryRef = new WeakReference<>(registry);
            this.token = token;
            // Pre-compute hash code for performance
            this.hashCode = System.identityHashCode(registry) * 31 + System.identityHashCode(token);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TokenKey)) return false;
            TokenKey other = (TokenKey) obj;
            NativeFinalizationRegistry thisRegistry = registryRef.get();
            NativeFinalizationRegistry otherRegistry = other.registryRef.get();
            // null-safe comparison
            if (thisRegistry == null || otherRegistry == null) {
                return thisRegistry == otherRegistry && token == other.token;
            }
            return thisRegistry == otherRegistry && token == other.token;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        boolean isRegistryAlive() {
            return registryRef.get() != null;
        }
    }

    // Private constructor for singleton
    private FinalizationQueueManager() {
        // Thread will be created lazily when first registration happens
    }

    /**
     * Register a target for finalization cleanup.
     *
     * @param target The JavaScript object to monitor
     * @param registry The FinalizationRegistry that registered this target
     * @param heldValue The value to pass to the cleanup callback
     * @param unregisterToken Optional token for unregistration
     * @return PhantomReference that can be used for unregistration
     */
    PhantomReference<Scriptable> register(
            Scriptable target,
            NativeFinalizationRegistry registry,
            Object heldValue,
            Object unregisterToken) {
        ensureThreadRunning();

        CleanupTask task = new CleanupTask(registry, heldValue, unregisterToken);
        FinalizationPhantomReference ref =
                new FinalizationPhantomReference(target, sharedQueue, task);
        tasks.put(ref, task);
        totalRegistrations.incrementAndGet();

        // Add to token index for O(1) unregistration
        if (unregisterToken != null && unregisterToken != Undefined.instance) {
            TokenKey key = new TokenKey(registry, unregisterToken);
            tokenIndex
                    .computeIfAbsent(key, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                    .add(ref);
        }

        return ref;
    }

    /**
     * Unregister a previously registered target.
     *
     * @param ref The PhantomReference returned from register()
     * @return true if successfully unregistered, false if not found
     */
    boolean unregister(PhantomReference<?> ref) {
        CleanupTask removed = tasks.remove(ref);
        if (removed != null) {
            // Remove from token index if present
            if (removed.unregisterToken != null && removed.unregisterToken != Undefined.instance) {
                NativeFinalizationRegistry registry = removed.registryRef.get();
                if (registry != null) {
                    TokenKey key = new TokenKey(registry, removed.unregisterToken);
                    var refs = tokenIndex.get(key);
                    if (refs != null) {
                        refs.remove(ref);
                        if (refs.isEmpty()) {
                            tokenIndex.remove(key);
                        }
                    }
                }
            }
            ref.clear(); // Clear the reference to allow GC
            totalUnregistrations.incrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Find and unregister all references with the given token.
     *
     * @param registry The registry to search in
     * @param unregisterToken The token to match
     * @return Number of references unregistered
     */
    int unregisterByToken(NativeFinalizationRegistry registry, Object unregisterToken) {
        if (unregisterToken == null || unregisterToken == Undefined.instance) {
            return 0;
        }

        int count = 0;
        // Use token index for O(1) lookup
        TokenKey key = new TokenKey(registry, unregisterToken);
        var refs = tokenIndex.remove(key);
        if (refs != null) {
            for (PhantomReference<?> ref : refs) {
                CleanupTask task = tasks.remove(ref);
                if (task != null) {
                    ref.clear();
                    count++;
                    totalUnregistrations.incrementAndGet();
                }
            }
        }
        return count;
    }

    // Ensure the processor thread is running
    private synchronized void ensureThreadRunning() {
        if (processorThread == null || !processorThread.isAlive()) {
            if (shutdown) {
                shutdown = false; // Only reset if explicitly shut down
            }
            processorThread = new Thread(this::processQueue, THREAD_NAME);
            processorThread.setDaemon(true);
            processorThread.start();
        }
    }

    // Background thread that processes phantom references
    private void processQueue() {
        int consecutiveErrors = 0;
        while (!shutdown && !Thread.interrupted()) {
            try {
                // Wait for a reference to be enqueued (blocks with timeout)
                Reference<? extends Scriptable> ref = sharedQueue.remove(QUEUE_TIMEOUT_MS);
                if (ref != null) {
                    processReference(ref);
                    consecutiveErrors = 0; // Reset error counter on success
                }

                // Process multiple references if available (batch processing)
                while ((ref = sharedQueue.poll()) != null && !shutdown) {
                    processReference(ref);
                }

                // Periodically try to process pending cleanups if Context available
                processPendingCleanupsIfPossible();

                // Periodically clean up dead token index entries
                cleanupDeadTokenIndexEntries();

            } catch (InterruptedException e) {
                // Thread interrupted, exit
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log but continue processing
                consecutiveErrors++;
                Context.reportWarning("FinalizationQueueManager error: " + e.getMessage());

                // If too many consecutive errors, pause briefly
                if (consecutiveErrors > ERROR_THRESHOLD) {
                    try {
                        Thread.sleep(ERROR_PAUSE_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    // Process a single phantom reference that was enqueued
    private void processReference(Reference<? extends Scriptable> ref) {
        // Remove from our tracking map
        CleanupTask task = tasks.remove(ref);
        if (task == null) {
            // Already unregistered, nothing to do
            return;
        }

        // Clear the reference to help GC
        ref.clear();

        // Route to appropriate Context for execution
        routeToContext(task);
    }

    /**
     * Route a cleanup task to the appropriate Context for execution. This is the key integration
     * point with JavaScript's execution model.
     */
    private void routeToContext(CleanupTask task) {
        Context cx = Context.getCurrentContext();
        if (cx != null) {
            // We have an active Context - schedule cleanup
            cx.scheduleFinalizationCleanup(
                    () -> {
                        NativeFinalizationRegistry registry = task.registryRef.get();
                        if (registry != null) {
                            registry.executeCleanupCallback(task.heldValue);
                            totalCleanups.incrementAndGet();
                        }
                        // If registry was GC'd, skip cleanup (correct per spec)
                    });
        } else {
            // No Context available - queue for later (with size limit to prevent OOM)
            Integer maxPendingProp = Integer.getInteger(MAX_PENDING_PROPERTY);
            int maxPending = maxPendingProp != null ? maxPendingProp : MAX_PENDING_CLEANUPS;

            if (pendingCleanups.size() < maxPending) {
                pendingCleanups.offer(task);
            } else {
                // Log warning and drop oldest cleanup to make room
                Context.reportWarning(
                        "FinalizationRegistry: Pending cleanup queue full, dropping oldest cleanup");
                pendingCleanups.poll(); // Remove oldest
                pendingCleanups.offer(task); // Add new
            }
        }
    }

    /**
     * Process pending cleanups if a Context is available. Called periodically by the processor
     * thread. Note: The processor thread won't have a Context, so this will rarely process
     * cleanups. Most cleanup processing happens when Context.enter() is called.
     */
    private void processPendingCleanupsIfPossible() {
        // The background thread doesn't have a Context, so cleanups
        // will be processed when a Context becomes available via processPendingCleanups()
        // This method is kept for potential future use cases where
        // the processor thread might have a Context.
    }

    /**
     * Clean up token index entries where the registry has been garbage collected. This prevents
     * memory leaks from accumulating dead entries.
     */
    private void cleanupDeadTokenIndexEntries() {
        tokenIndex.entrySet().removeIf(entry -> !entry.getKey().isRegistryAlive());
    }

    /**
     * Called by Context when it becomes active to process any pending cleanups. Synchronized to
     * prevent concurrent processing from multiple threads.
     *
     * @param cx The newly active Context
     */
    synchronized void processPendingCleanups(Context cx) {
        if (pendingCleanups.isEmpty()) {
            return; // Early exit if nothing to process
        }

        CleanupTask task;
        int processed = 0;
        // Process up to batch size cleanups to avoid blocking
        Integer batchSizeProp = Integer.getInteger(BATCH_SIZE_PROPERTY);
        int batchSize = batchSizeProp != null ? batchSizeProp : DEFAULT_BATCH_SIZE;
        while ((task = pendingCleanups.poll()) != null && processed < batchSize) {
            final CleanupTask finalTask = task;
            cx.scheduleFinalizationCleanup(
                    () -> {
                        NativeFinalizationRegistry registry = finalTask.registryRef.get();
                        if (registry != null) {
                            registry.executeCleanupCallback(finalTask.heldValue);
                            totalCleanups.incrementAndGet();
                        }
                        // If registry was GC'd, skip cleanup (correct per spec)
                    });
            processed++;
        }
    }

    // Shutdown the processor thread (for testing/cleanup)
    synchronized void shutdown() {
        shutdown = true;
        if (processorThread != null) {
            processorThread.interrupt();
            try {
                processorThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Get the number of pending cleanups (for testing/monitoring)
    int getPendingCleanupCount() {
        return pendingCleanups.size();
    }

    // Get the number of registered references (for testing/monitoring)
    int getRegisteredReferenceCount() {
        return tasks.size();
    }

    // Get statistics for monitoring
    String getStatistics() {
        return String.format(
                "FinalizationQueueManager[registrations=%d, cleanups=%d, unregistrations=%d, pending=%d, active=%d]",
                totalRegistrations.get(),
                totalCleanups.get(),
                totalUnregistrations.get(),
                pendingCleanups.size(),
                tasks.size());
    }
}
