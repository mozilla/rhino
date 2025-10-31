/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

/**
 * Shared finalization queue infrastructure for FinalizationRegistry.
 *
 * <p>Provides shared ReferenceQueue for JVM efficiency and GC detection polling. Uses JSCode
 * architecture for Context-safe cleanup execution.
 */
public class FinalizationQueue {

    // Single shared queue for all registries (JVM efficient)
    private static final ReferenceQueue<Object> SHARED_QUEUE = new ReferenceQueue<>();

    /**
     * Get the shared reference queue for PhantomReference registration.
     *
     * @return the shared ReferenceQueue used by all FinalizationRegistry instances
     */
    public static ReferenceQueue<Object> getSharedQueue() {
        return SHARED_QUEUE;
    }

    /**
     * Poll for finalized objects and schedule cleanups using JSCode architecture.
     *
     * <p>Called from Context during JavaScript execution to process recently finalized objects.
     * Processes at most maxCleanups items to bound execution time. Uses JSCode execution for
     * Context safety per aardvark179's architecture patterns.
     *
     * @param cx the JavaScript execution context
     * @param maxCleanups maximum number of cleanup tasks to process
     */
    public static void pollAndScheduleCleanups(Context cx, int maxCleanups) {
        if (cx == null) return;

        int processed = 0;
        Reference<?> ref;

        while (processed < maxCleanups && (ref = SHARED_QUEUE.poll()) != null) {
            if (ref instanceof TrackedPhantomReference) {
                TrackedPhantomReference trackedRef = (TrackedPhantomReference) ref;
                // Use JSCode execution instead of direct Context capture
                trackedRef.scheduleJSCodeCleanup(cx);
                processed++;
            }
            ref.clear();
        }
    }

    /**
     * PhantomReference that knows how to schedule its own cleanup using JSCode architecture.
     *
     * <p>Base class for references that need to perform cleanup when their target is garbage
     * collected. Automatically registers with the shared queue. Uses JSCode execution for
     * Context-safe cleanup per aardvark179's architecture patterns.
     */
    public abstract static class TrackedPhantomReference extends PhantomReference<Object> {

        protected TrackedPhantomReference(Object target) {
            super(target, SHARED_QUEUE);
        }

        /**
         * Schedule cleanup using JSCode execution (Context-safe).
         *
         * <p>Called when the referenced object has been garbage collected. Uses JSCode architecture
         * to avoid Context capture issues.
         *
         * @param cx the JavaScript execution context (fresh, never stored)
         */
        protected abstract void scheduleJSCodeCleanup(Context cx);
    }
}
