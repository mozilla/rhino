/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages registrations and cleanup scheduling for FinalizationRegistry.
 *
 * <p>Handles the complex logic of tracking PhantomReferences, managing unregister token indexes,
 * and coordinating with the shared FinalizationQueue for efficient cleanup processing.
 */
final class FinalizationRegistrationManager {

    /** Active registrations using thread-safe concurrent storage */
    private final Set<RegistrationReference> activeRegistrations = ConcurrentHashMap.newKeySet();

    /** Token-based lookup index for efficient unregistration */
    private final Map<TokenKey, Set<RegistrationReference>> tokenIndex =
            new ConcurrentHashMap<>(16);

    /**
     * Register a target for finalization cleanup.
     *
     * @param target the object to track
     * @param heldValue the value to pass to cleanup
     * @param unregisterToken optional token for later removal
     * @param registry the FinalizationRegistry instance
     */
    void register(Object target, Object heldValue, Object unregisterToken,
                  NativeFinalizationRegistry registry) {
        RegistrationReference ref = new RegistrationReference(target, registry, heldValue);

        activeRegistrations.add(ref);

        if (unregisterToken != null && !Undefined.isUndefined(unregisterToken)) {
            TokenKey key = new TokenKey(unregisterToken);
            Set<RegistrationReference> refs =
                tokenIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
            refs.add(ref);
        }
    }

    /**
     * Unregister all registrations associated with a token.
     *
     * @param token the unregister token
     * @return true if any registrations were removed
     */
    boolean unregister(Object token) {
        TokenKey key = new TokenKey(token);
        Set<RegistrationReference> refs = tokenIndex.remove(key);

        if (refs == null || refs.isEmpty()) {
            return false;
        }

        // Remove from active registrations and clear references
        for (RegistrationReference ref : refs) {
            activeRegistrations.remove(ref);
            ref.clear();
        }

        return true;
    }

    /**
     * Process cleanup callbacks for finalized objects.
     *
     * @param maxCleanups maximum number to process
     * @param cleanupExecutor callback to execute cleanup
     */
    void processCleanups(int maxCleanups, CleanupExecutor cleanupExecutor) {
        Iterator<RegistrationReference> iterator = activeRegistrations.iterator();
        int processed = 0;

        while (iterator.hasNext() && processed < maxCleanups) {
            RegistrationReference ref = iterator.next();

            if (ref.isEnqueued()) {
                iterator.remove();
                cleanupExecutor.executeCleanup(ref.getHeldValue());
                ref.clear();
                processed++;
            }
        }
    }

    /**
     * Get the current number of active registrations.
     */
    int getActiveCount() {
        return activeRegistrations.size();
    }

    /**
     * Clear all registrations (for finalization cleanup).
     */
    void clear() {
        activeRegistrations.clear();
        tokenIndex.clear();
    }

    /**
     * Callback interface for executing cleanup operations.
     */
    interface CleanupExecutor {
        void executeCleanup(Object heldValue);
    }

    /**
     * PhantomReference that tracks target objects for finalization.
     */
    private static final class RegistrationReference extends FinalizationQueue.TrackedPhantomReference {
        private final NativeFinalizationRegistry registry;
        private final Object heldValue;

        RegistrationReference(Object target, NativeFinalizationRegistry registry, Object heldValue) {
            super(target);
            this.registry = registry;
            this.heldValue = heldValue;
        }

        Object getHeldValue() {
            return heldValue;
        }

        @Override
        protected void scheduleJSCodeCleanup(Context cx) {
            // Schedule cleanup to be executed in the Context processing loop
            cx.scheduleFinalizationCleanup(() -> {
                registry.executeCleanupCallback(cx, heldValue);
            });
        }
    }

    /**
     * Wrapper for unregister tokens providing identity-based equality.
     */
    private static final class TokenKey {
        private final Object token;

        TokenKey(Object token) {
            this.token = token;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TokenKey)) return false;
            return token == ((TokenKey) obj).token;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(token);
        }
    }
}