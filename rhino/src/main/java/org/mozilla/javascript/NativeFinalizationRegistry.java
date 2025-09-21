/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.ref.PhantomReference;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the ES2021 FinalizationRegistry constructor and prototype.
 *
 * <p>FinalizationRegistry allows registering cleanup callbacks to be called when objects are
 * garbage collected. This is useful for resource cleanup, cache management, or any scenario where
 * you need to perform cleanup when objects are no longer reachable.
 *
 * <p>The FinalizationRegistry object provides two methods: register() to register an object for
 * cleanup, and unregister() to remove a registration using a token.
 *
 * <p>This implementation uses PhantomReferences for correct GC semantics and a shared
 * FinalizationQueueManager for efficient resource usage across all registries.
 *
 * @see <a href="https://tc39.es/ecma262/#sec-finalization-registry-objects">ECMAScript
 *     FinalizationRegistry Objects</a>
 */
public class NativeFinalizationRegistry extends ScriptableObject {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = "FinalizationRegistry";

    private boolean instanceOfFinalizationRegistry = false;
    private final Function cleanupCallback;
    // Thread-local override callback for cleanupSome()
    private volatile Function temporaryCallbackOverride = null;
    // Maps PhantomReferences to their unregister tokens for token-based unregistration
    private final ConcurrentHashMap<PhantomReference<?>, Object> referenceToTokenMap;
    // Maps unregister tokens to sets of PhantomReferences for efficient unregistration
    private final ConcurrentHashMap<Object, Set<PhantomReference<?>>> tokenToReferencesMap;

    public static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        1,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeFinalizationRegistry::constructor);
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        constructor.definePrototypeMethod(
                scope,
                "register",
                2,
                NativeFinalizationRegistry::register,
                DONTENUM,
                DONTENUM | READONLY);

        constructor.definePrototypeMethod(
                scope,
                "unregister",
                1,
                NativeFinalizationRegistry::unregister,
                DONTENUM,
                DONTENUM | READONLY);

        // cleanupSome - Optional method for synchronous cleanup
        // Not exposed in browser environments per spec, but useful for server-side
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
        return constructor;
    }

    private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
        if (args.length < 1 || !(args[0] instanceof Function)) {
            throw ScriptRuntime.typeErrorById("msg.finalization.registry.no.callback");
        }
        NativeFinalizationRegistry registry = new NativeFinalizationRegistry((Function) args[0]);
        registry.instanceOfFinalizationRegistry = true;
        return registry;
    }

    private NativeFinalizationRegistry(Function cleanupCallback) {
        this.cleanupCallback = cleanupCallback;
        this.referenceToTokenMap = new ConcurrentHashMap<>();
        this.tokenToReferencesMap = new ConcurrentHashMap<>();
    }

    private static Object register(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeFinalizationRegistry registry = realThis(thisObj, "register");
        return registry.registerTarget(args);
    }

    private static Object unregister(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeFinalizationRegistry registry = realThis(thisObj, "unregister");
        return registry.unregisterToken(args);
    }

    private static Object cleanupSome(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeFinalizationRegistry registry = realThis(thisObj, "cleanupSome");

        // Validate optional callback parameter
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

        // Per spec: cleanupSome synchronously processes any available finalization cleanups
        // If a callback is provided, it's used instead of the registry's callback
        // Process any pending cleanups immediately
        registry.performCleanupSome(cx, callback);

        return Undefined.instance;
    }

    private static NativeFinalizationRegistry realThis(Scriptable thisObj, String name) {
        if (thisObj instanceof NativeFinalizationRegistry) {
            NativeFinalizationRegistry registry = (NativeFinalizationRegistry) thisObj;
            if (registry.instanceOfFinalizationRegistry) {
                return registry;
            }
        }
        throw ScriptRuntime.typeErrorById("msg.incompat.call", name);
    }

    private Object registerTarget(Object[] args) {
        if (args.length < 2) {
            throw ScriptRuntime.typeErrorById("msg.finalization.registry.register.not.object");
        }

        Object target = args[0];
        Object heldValue = args[1];
        Object unregisterToken =
                args.length > 2 && !Undefined.isUndefined(args[2]) ? args[2] : null;

        // Per spec: target must be an object (not a symbol)
        if (!isValidTarget(target)) {
            throw ScriptRuntime.typeErrorById("msg.finalization.registry.register.not.object");
        }

        // Per spec: if unregisterToken is provided, it must be able to be held weakly
        if (unregisterToken != null && !canBeHeldWeakly(unregisterToken)) {
            throw ScriptRuntime.typeErrorById("msg.finalization.registry.register.not.object");
        }

        // Per spec: SameValue(target, heldValue) must be false
        if (ScriptRuntime.same(target, heldValue)) {
            throw ScriptRuntime.typeErrorById("msg.finalization.registry.register.same.target");
        }

        // Register with the FinalizationQueueManager using PhantomReference
        Scriptable scriptableTarget = (Scriptable) target;
        FinalizationQueueManager manager = FinalizationQueueManager.getInstance();
        PhantomReference<Scriptable> ref =
                manager.register(scriptableTarget, this, heldValue, unregisterToken);

        // Track the reference for unregistration - atomic operation
        if (unregisterToken != null) {
            // Perform both map updates atomically to prevent inconsistent state
            synchronized (this) {
                referenceToTokenMap.put(ref, unregisterToken);
                tokenToReferencesMap
                        .computeIfAbsent(unregisterToken, k -> ConcurrentHashMap.newKeySet())
                        .add(ref);
            }
        }

        return Undefined.instance;
    }

    private Object unregisterToken(Object[] args) {
        // Per spec, if no arguments return false
        if (args.length < 1 || Undefined.isUndefined(args[0])) {
            return Boolean.FALSE;
        }

        Object token = args[0];

        // Per spec, check if token can be held weakly
        if (!canBeHeldWeakly(token)) {
            return Boolean.FALSE;
        }

        // Perform map operations atomically to prevent race conditions
        Set<PhantomReference<?>> refs;
        synchronized (this) {
            refs = tokenToReferencesMap.remove(token);
            if (refs == null || refs.isEmpty()) {
                return Boolean.FALSE;
            }

            // Remove from the reference map while still synchronized
            for (PhantomReference<?> ref : refs) {
                referenceToTokenMap.remove(ref);
            }
        }

        // Unregister outside synchronized block to avoid potential deadlock
        FinalizationQueueManager manager = FinalizationQueueManager.getInstance();
        boolean unregistered = false;
        for (PhantomReference<?> ref : refs) {
            if (manager.unregister(ref)) {
                unregistered = true;
            }
        }

        return Boolean.valueOf(unregistered);
    }

    /**
     * Called by FinalizationQueueManager when a registered object has been garbage collected. This
     * method is called in the appropriate JavaScript Context.
     *
     * @param heldValue The value that was registered with the object
     */
    void executeCleanupCallback(Object heldValue) {
        // Use override callback if set (for cleanupSome), otherwise use the normal callback
        Function callbackToUse =
                temporaryCallbackOverride != null ? temporaryCallbackOverride : cleanupCallback;

        if (callbackToUse == null) {
            return;
        }

        Context cx = Context.getCurrentContext();
        if (cx == null) {
            // This should not happen as we're called from Context
            return;
        }

        try {
            Scriptable scope = callbackToUse.getParentScope();
            callbackToUse.call(cx, scope, scope, new Object[] {heldValue});
        } catch (ThreadDeath td) {
            // Always rethrow ThreadDeath to allow thread termination
            throw td;
        } catch (Throwable t) {
            // Per spec, errors in cleanup callbacks should not propagate
            // Catch all errors to prevent cleanup thread death
            String msg = "FinalizationRegistry cleanup callback error: ";
            if (t.getMessage() != null) {
                msg += t.getMessage();
            } else {
                msg += t.getClass().getName();
            }
            Context.reportWarning(msg);
        }
    }

    /**
     * Performs synchronous cleanup for cleanupSome() method. Per spec, this should process any
     * available cleanups immediately.
     *
     * @param cx The current Context
     * @param callbackOverride Optional callback to use instead of the registry's callback
     */
    private void performCleanupSome(Context cx, Function callbackOverride) {
        // Set the temporary callback override if provided
        if (callbackOverride != null) {
            try {
                // Set the override - executeCleanupCallback will use it
                this.temporaryCallbackOverride = callbackOverride;

                // Process cleanups with the override callback
                cx.processFinalizationCleanups();
            } finally {
                // Always clear the override
                this.temporaryCallbackOverride = null;
            }
        } else {
            // Process cleanups with the normal callback
            cx.processFinalizationCleanups();
        }
    }

    private static boolean isValidTarget(Object target) {
        return ScriptRuntime.isObject(target);
    }

    private static boolean canBeHeldWeakly(Object value) {
        // ES2021: Only objects can be held weakly as unregister tokens
        return ScriptRuntime.isObject(value);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }
}
