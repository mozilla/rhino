/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of ECMAScript 2021 FinalizationRegistry.
 *
 * <p>Allows JavaScript code to register cleanup callbacks for garbage collected objects. Uses
 * shared ReferenceQueue for efficiency and integrates with Rhino's Context system.
 *
 * @see <a href="https://tc39.es/ecma262/#sec-finalization-registry-objects">ECMAScript
 *     FinalizationRegistry</a>
 */
public class NativeFinalizationRegistry extends ScriptableObject {

    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = "FinalizationRegistry";

    /** Flag to ensure this instance was created through proper constructor */
    private boolean instanceOfFinalizationRegistry = false;

    /** The cleanup callback function provided to the constructor */
    private final Function cleanupCallback;

    /** Active registrations: reference -> cleanup task */
    private final Map<RegistrationReference, CleanupTask> activeRegistrations =
            new ConcurrentHashMap<>();

    /** Index for unregister token lookup: token -> set of references */
    private final Map<TokenKey, Set<RegistrationReference>> unregisterTokenIndex =
            new ConcurrentHashMap<>();

    /** Temporary callback override for cleanupSome() method */
    private volatile Function cleanupSomeCallback = null;

    /** Initialize FinalizationRegistry constructor and prototype. */
    public static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        1,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeFinalizationRegistry::jsConstructor);

        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

        // register method
        constructor.definePrototypeMethod(
                scope,
                "register",
                2,
                NativeFinalizationRegistry::register,
                DONTENUM,
                DONTENUM | READONLY);

        // unregister method
        constructor.definePrototypeMethod(
                scope,
                "unregister",
                1,
                NativeFinalizationRegistry::unregister,
                DONTENUM,
                DONTENUM | READONLY);

        // cleanupSome method (optional, useful for server-side)
        constructor.definePrototypeMethod(
                scope,
                "cleanupSome",
                0,
                NativeFinalizationRegistry::cleanupSome,
                DONTENUM,
                DONTENUM | READONLY);

        constructor.definePrototypeProperty(
                SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);

        if (sealed) {
            constructor.sealObject();
            ScriptableObject prototype = (ScriptableObject) constructor.getPrototypeProperty();
            if (prototype != null) {
                prototype.sealObject();
            }
        }

        ScriptableObject.defineProperty(scope, CLASS_NAME, constructor, DONTENUM);
        return constructor;
    }

    /** Private constructor for FinalizationRegistry instances. */
    private NativeFinalizationRegistry(Function cleanupCallback) {
        this.cleanupCallback = cleanupCallback;
    }

    /** JavaScript constructor implementation. */
    private static NativeFinalizationRegistry jsConstructor(
            Context cx, Scriptable scope, Object[] args) {
        if (args.length < 1 || !(args[0] instanceof Function)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.finalizationregistry.not.function",
                    args.length > 0 ? ScriptRuntime.toString(args[0]) : "undefined");
        }
        Function cleanupCallback = (Function) args[0];
        NativeFinalizationRegistry registry = new NativeFinalizationRegistry(cleanupCallback);
        registry.instanceOfFinalizationRegistry = true;
        registry.setPrototype(ScriptableObject.getClassPrototype(scope, CLASS_NAME));
        registry.setParentScope(scope);
        return registry;
    }

    /** JavaScript register() method implementation. */
    private static Object register(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeFinalizationRegistry registry = realThis(thisObj, "register");

        if (args.length < 2) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "FinalizationRegistry.register",
                    "2",
                    String.valueOf(args.length));
        }

        Object target = args[0];
        Object heldValue = args[1];
        Object unregisterToken = args.length > 2 ? args[2] : null;

        if (!isValidTarget(target)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.finalizationregistry.invalid.target", ScriptRuntime.typeof(target));
        }

        if (isSameValue(target, heldValue)) {
            throw ScriptRuntime.typeErrorById("msg.finalizationregistry.target.same.as.held");
        }

        if (unregisterToken != null && !canBeHeldWeakly(unregisterToken)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.finalizationregistry.invalid.unregister.token",
                    ScriptRuntime.typeof(unregisterToken));
        }

        registry.registerTarget(target, heldValue, unregisterToken);
        return Undefined.instance;
    }

    /** JavaScript unregister() method implementation. */
    private static Object unregister(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeFinalizationRegistry registry = realThis(thisObj, "unregister");
        return registry.unregisterToken(args);
    }

    /** JavaScript cleanupSome() method implementation. */
    private static Object cleanupSome(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeFinalizationRegistry registry = realThis(thisObj, "cleanupSome");

        Function callback = null;
        if (args.length > 0 && !Undefined.isUndefined(args[0])) {
            if (!(args[0] instanceof Function)) {
                throw ScriptRuntime.typeErrorById(
                        "msg.isnt.function",
                        ScriptRuntime.toString(args[0]),
                        ScriptRuntime.typeof(args[0]));
            }
            callback = (Function) args[0];
        }

        registry.performCleanupSome(cx, callback);
        return Undefined.instance;
    }

    /**
     * Validate and cast 'this' object to FinalizationRegistry.
     *
     * @param thisObj the 'this' object from JavaScript call
     * @param name method name for error messages
     * @return validated FinalizationRegistry instance
     * @throws TypeError if thisObj is not a proper FinalizationRegistry
     */
    private static NativeFinalizationRegistry realThis(Scriptable thisObj, String name) {
        if (!(thisObj instanceof NativeFinalizationRegistry)) {
            throw ScriptRuntime.typeErrorById("msg.incompat.call", CLASS_NAME + '.' + name);
        }
        NativeFinalizationRegistry registry = (NativeFinalizationRegistry) thisObj;
        if (!registry.instanceOfFinalizationRegistry) {
            throw ScriptRuntime.typeErrorById("msg.incompat.call", CLASS_NAME + '.' + name);
        }
        return registry;
    }

    /**
     * Register a target object for finalization cleanup.
     *
     * <p>Creates a PhantomReference to track the target and stores the cleanup task. If an
     * unregister token is provided, indexes the registration for later removal.
     *
     * @param target the object to watch for finalization
     * @param heldValue the value to pass to cleanup callback
     * @param unregisterToken optional token for later unregistration (may be null)
     */
    private void registerTarget(Object target, Object heldValue, Object unregisterToken) {
        CleanupTask task = new CleanupTask(heldValue);
        RegistrationReference ref = new RegistrationReference(target, this, task);

        activeRegistrations.put(ref, task);

        if (unregisterToken != null) {
            TokenKey key = new TokenKey(unregisterToken);
            Set<RegistrationReference> refs =
                    unregisterTokenIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
            refs.add(ref);
        }
    }

    /**
     * Remove registrations associated with the given token.
     *
     * @param args the method arguments (first should be the unregister token)
     * @return true if any registrations were removed, false otherwise
     */
    private Object unregisterToken(Object[] args) {
        if (args.length < 1 || args[0] == null || Undefined.isUndefined(args[0])) {
            return false;
        }

        Object token = args[0];
        if (!canBeHeldWeakly(token)) {
            // Per spec, unregister with non-object token just returns false
            return false;
        }

        TokenKey key = new TokenKey(token);
        Set<RegistrationReference> refs = unregisterTokenIndex.remove(key);

        if (refs == null || refs.isEmpty()) {
            return false;
        }

        for (RegistrationReference ref : refs) {
            activeRegistrations.remove(ref);
            ref.clear();
        }

        return true;
    }

    /**
     * Execute the cleanup callback for a held value.
     *
     * <p>Calls either the temporary callback (from cleanupSome) or the registry's main callback.
     * Catches and logs any exceptions to prevent cleanup errors from breaking execution.
     *
     * @param heldValue the value to pass to the cleanup callback
     */
    void executeCleanupCallback(Object heldValue) {
        Function callbackToUse =
                cleanupSomeCallback != null ? cleanupSomeCallback : cleanupCallback;

        if (callbackToUse == null) {
            return;
        }

        Context cx = Context.getCurrentContext();
        if (cx == null) {
            return;
        }

        try {
            Scriptable scope = callbackToUse.getParentScope();
            callbackToUse.call(cx, scope, scope, new Object[] {heldValue});
        } catch (Exception e) {
            // Cleanup callbacks shouldn't break execution
            Context.reportWarning("FinalizationRegistry cleanup callback error: " + e.getMessage());
        }
    }

    /**
     * Perform cleanupSome() - process some cleanups synchronously.
     *
     * <p>Processes up to 10 pending cleanups. If a callback override is provided, uses it
     * temporarily instead of the registry's main callback.
     *
     * @param cx the JavaScript context
     * @param callbackOverride optional callback to use instead of main callback
     */
    private void performCleanupSome(Context cx, Function callbackOverride) {
        if (callbackOverride != null) {
            try {
                this.cleanupSomeCallback = callbackOverride;
                processCleanups(cx, 10);
            } finally {
                this.cleanupSomeCallback = null;
            }
        } else {
            processCleanups(cx, 10);
        }
    }

    /**
     * Process pending cleanups for this registry.
     *
     * <p>Checks active registrations for any that have been enqueued (GC'd) and schedules cleanup
     * callbacks for them. Also polls the shared queue for additional finalized references.
     *
     * @param cx the JavaScript context
     * @param maxCleanups maximum number of cleanups to process
     */
    private void processCleanups(Context cx, int maxCleanups) {
        // Check our registrations for any that have been collected
        int processed = 0;
        Iterator<Map.Entry<RegistrationReference, CleanupTask>> it =
                activeRegistrations.entrySet().iterator();

        while (it.hasNext() && processed < maxCleanups) {
            Map.Entry<RegistrationReference, CleanupTask> entry = it.next();
            RegistrationReference ref = entry.getKey();

            // Check if the reference was enqueued (target was GC'd)
            if (ref.isEnqueued()) {
                it.remove();
                CleanupTask task = entry.getValue();

                // Schedule cleanup
                cx.scheduleFinalizationCleanup(
                        () -> {
                            executeCleanupCallback(task.heldValue);
                        });

                ref.clear();
                processed++;
            }
        }

        // Also poll the shared queue for any of our references
        FinalizationQueue.pollAndScheduleCleanups(cx, maxCleanups - processed);
    }

    /**
     * Clean up when this registry is being GC'd.
     *
     * <p>Uses finalize() over Cleaner API due to dynamic registration requirements. Recommended by
     * aardvark179 for this specific GC cleanup use case.
     */
    @Override
    @SuppressWarnings({"deprecation", "finalize", "Finalize"})
    // ErrorProne: Finalize is acceptable for FinalizationRegistry cleanup
    protected void finalize() throws Throwable {
        try {
            // Clear all our registrations to prevent memory leaks
            for (RegistrationReference ref : activeRegistrations.keySet()) {
                ref.clear();
            }
            activeRegistrations.clear();
            unregisterTokenIndex.clear();
        } finally {
            super.finalize();
        }
    }

    /**
     * Check if the given object can be used as a registration target.
     *
     * @param target the target object to validate
     * @return true if target is a valid object that can be registered
     */
    private static boolean isValidTarget(Object target) {
        return ScriptRuntime.isObject(target) || (target instanceof Symbol);
    }

    /**
     * Check if the given value can be held weakly (used for unregister tokens).
     *
     * @param value the value to check
     * @return true if value can be used as an unregister token
     */
    private static boolean canBeHeldWeakly(Object value) {
        return ScriptRuntime.isObject(value) || (value instanceof Symbol);
    }

    /**
     * Implement SameValue comparison per ECMAScript specification.
     *
     * <p>Used to check if target and heldValue are the same (which is forbidden). Handles special
     * cases like NaN === NaN.
     *
     * @param a first value to compare
     * @param b second value to compare
     * @return true if values are the same per SameValue algorithm
     */
    private static boolean isSameValue(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number) {
            double na = ((Number) a).doubleValue();
            double nb = ((Number) b).doubleValue();
            if (Double.isNaN(na) && Double.isNaN(nb)) return true;
        }
        return false;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    /**
     * Custom PhantomReference that tracks cleanup task and registry.
     *
     * <p>This reference is automatically enqueued when its target is garbage collected. It
     * maintains a weak reference to the registry to avoid circular dependencies, and holds the
     * cleanup task data needed for callback execution.
     */
    private static class RegistrationReference extends FinalizationQueue.TrackedPhantomReference {
        private final WeakReference<NativeFinalizationRegistry> registryRef;
        private final CleanupTask task;

        /**
         * Create a registration reference for the given target.
         *
         * @param target the object to track for finalization
         * @param registry the registry that owns this registration
         * @param task the cleanup task to execute when finalized
         */
        RegistrationReference(
                Object target, NativeFinalizationRegistry registry, CleanupTask task) {
            super(target);
            this.registryRef = new WeakReference<>(registry);
            this.task = task;
        }

        @Override
        protected void scheduleCleanup(Context cx) {
            NativeFinalizationRegistry registry = registryRef.get();
            if (registry != null) {
                cx.scheduleFinalizationCleanup(
                        () -> {
                            registry.executeCleanupCallback(task.heldValue);
                        });

                // Remove from registry's tracking
                registry.activeRegistrations.remove(this);
            }
        }
    }

    /**
     * Cleanup task data holder.
     *
     * <p>Simple immutable container for the value that should be passed to the cleanup callback
     * when finalization occurs.
     */
    private static class CleanupTask {
        final Object heldValue;

        CleanupTask(Object heldValue) {
            this.heldValue = heldValue;
        }
    }

    /**
     * Key for token-based lookup using identity equality.
     *
     * <p>Wraps unregister tokens to provide identity-based equality and hashing, as required by the
     * ECMAScript specification. Two TokenKeys are equal only if they wrap the exact same object
     * reference.
     */
    private static class TokenKey {
        private final Object token;
        private final int hashCode;

        /**
         * Create a token key for the given object.
         *
         * @param token the unregister token object
         */
        TokenKey(Object token) {
            this.token = token;
            this.hashCode = System.identityHashCode(token);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TokenKey)) return false;
            TokenKey other = (TokenKey) o;
            return token == other.token; // Identity equality
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
