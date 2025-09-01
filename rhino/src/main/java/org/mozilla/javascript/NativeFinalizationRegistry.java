/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
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
 * @see <a href="https://tc39.es/ecma262/#sec-finalization-registry-objects">ECMAScript
 *     FinalizationRegistry Objects</a>
 */
public class NativeFinalizationRegistry extends ScriptableObject {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = "FinalizationRegistry";
    private static final String REGISTER_METHOD = "register";
    private static final String UNREGISTER_METHOD = "unregister";

    // Constants
    private static final String MSG_NO_CALLBACK = "msg.finalization.registry.no.callback";
    private static final String MSG_REGISTER_NOT_OBJECT =
            "msg.finalization.registry.register.not.object";
    private static final String MSG_SAME_TARGET = "msg.finalization.registry.register.same.target";
    private static final int CONSTRUCTOR_ARITY = 1;
    private static final int REGISTER_ARITY = 2;
    private static final int UNREGISTER_ARITY = 1;

    private final Function cleanupCallback;
    private final ReferenceQueue<Scriptable> referenceQueue;
    private final ConcurrentHashMap<FinalizationWeakReference, RegistrationRecord> registrations;
    private final ConcurrentHashMap<Object, Set<FinalizationWeakReference>> tokenMap;

    /** Custom WeakReference that can trigger cleanup when the referent is collected. */
    private class FinalizationWeakReference extends WeakReference<Scriptable> {
        FinalizationWeakReference(Scriptable referent) {
            super(referent, referenceQueue);
        }
    }

    /** Internal record holding cleanup data for a registration. */
    private static class RegistrationRecord {
        final Object heldValue;
        final Object unregisterToken;

        RegistrationRecord(Object heldValue, Object unregisterToken) {
            this.heldValue = heldValue;
            this.unregisterToken = unregisterToken;
        }
    }

    /**
     * Initializes the FinalizationRegistry constructor and prototype in the given scope.
     *
     * @param cx the JavaScript context
     * @param scope the scope to initialize in
     * @param sealed whether to seal the constructor and prototype
     * @return the FinalizationRegistry constructor
     */
    public static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor = createConstructor(scope);
        configurePrototype(constructor, scope);

        if (sealed) {
            sealConstructor(constructor);
        }
        return constructor;
    }

    private static LambdaConstructor createConstructor(Scriptable scope) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        CONSTRUCTOR_ARITY,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeFinalizationRegistry::constructor);
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        return constructor;
    }

    private static void configurePrototype(LambdaConstructor constructor, Scriptable scope) {
        // Define prototype methods
        constructor.definePrototypeMethod(
                scope,
                REGISTER_METHOD,
                REGISTER_ARITY,
                NativeFinalizationRegistry::register,
                DONTENUM,
                DONTENUM | READONLY);

        constructor.definePrototypeMethod(
                scope,
                UNREGISTER_METHOD,
                UNREGISTER_ARITY,
                NativeFinalizationRegistry::unregister,
                DONTENUM,
                DONTENUM | READONLY);

        // Define Symbol.toStringTag
        constructor.definePrototypeProperty(
                SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);
    }

    private static void sealConstructor(LambdaConstructor constructor) {
        constructor.sealObject();
        ScriptableObject prototype = (ScriptableObject) constructor.getPrototypeProperty();
        if (prototype != null) {
            prototype.sealObject();
        }
    }

    /**
     * FinalizationRegistry constructor implementation. Creates a new FinalizationRegistry instance
     * with the given cleanup callback.
     *
     * @param cx the current context
     * @param scope the scope
     * @param args constructor arguments, expects exactly one function argument
     * @return the new FinalizationRegistry instance
     * @throws TypeError if no argument provided or argument is not a function
     */
    private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
        validateConstructorArgs(args);
        return createFinalizationRegistry((Function) args[0]);
    }

    private static NativeFinalizationRegistry createFinalizationRegistry(Function cleanupCallback) {
        return new NativeFinalizationRegistry(cleanupCallback);
    }

    /**
     * Validates constructor arguments according to ES2021 spec.
     *
     * @param args the constructor arguments
     * @throws TypeError if validation fails
     */
    private static void validateConstructorArgs(Object[] args) {
        if (args.length < 1 || !(args[0] instanceof Function)) {
            throw ScriptRuntime.typeErrorById(MSG_NO_CALLBACK);
        }
    }

    /**
     * Private constructor that initializes the registry with a cleanup callback.
     *
     * @param cleanupCallback the function to call when objects are collected
     */
    private NativeFinalizationRegistry(Function cleanupCallback) {
        this.cleanupCallback = cleanupCallback;
        this.referenceQueue = new ReferenceQueue<>();
        this.registrations = new ConcurrentHashMap<>();
        this.tokenMap = new ConcurrentHashMap<>();
    }

    /**
     * FinalizationRegistry.prototype.register() implementation. Registers an object for cleanup
     * when it's garbage collected.
     *
     * @param cx the current context
     * @param scope the scope
     * @param thisObj the 'this' object
     * @param args method arguments: target, heldValue, [unregisterToken]
     * @return undefined
     */
    private static Object register(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeFinalizationRegistry registry = ensureFinalizationRegistry(thisObj);
        return registry.registerTarget(args);
    }

    /**
     * FinalizationRegistry.prototype.unregister() implementation. Unregisters all registrations
     * associated with the given token.
     *
     * @param cx the current context
     * @param scope the scope
     * @param thisObj the 'this' object
     * @param args method arguments: unregisterToken
     * @return boolean indicating if any registrations were removed
     */
    private static Object unregister(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeFinalizationRegistry registry = ensureFinalizationRegistry(thisObj);
        return registry.unregisterToken(args);
    }

    private static NativeFinalizationRegistry ensureFinalizationRegistry(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeFinalizationRegistry.class);
    }

    /**
     * Registers a target object for cleanup.
     *
     * @param args the method arguments
     * @return Undefined.instance
     */
    private Object registerTarget(Object[] args) {
        if (args.length < 2) {
            throw ScriptRuntime.typeErrorById(MSG_REGISTER_NOT_OBJECT);
        }

        Object target = args[0];
        Object heldValue = args[1];
        Object unregisterToken = args.length > 2 ? args[2] : null;

        if (!isValidTarget(target)) {
            throw ScriptRuntime.typeErrorById(MSG_REGISTER_NOT_OBJECT);
        }

        if (target == heldValue) {
            throw ScriptRuntime.typeErrorById(MSG_SAME_TARGET);
        }

        // Process any pending cleanups before registering new ones
        processPendingCleanups();

        Scriptable scriptableTarget = (Scriptable) target;
        FinalizationWeakReference weakRef = new FinalizationWeakReference(scriptableTarget);
        RegistrationRecord record = new RegistrationRecord(heldValue, unregisterToken);

        registrations.put(weakRef, record);

        if (unregisterToken != null) {
            tokenMap.computeIfAbsent(unregisterToken, k -> ConcurrentHashMap.newKeySet())
                    .add(weakRef);
        }

        return Undefined.instance;
    }

    /**
     * Unregisters all registrations associated with the given token.
     *
     * @param args the method arguments
     * @return Boolean indicating if any registrations were removed
     */
    private Object unregisterToken(Object[] args) {
        if (args.length < 1) {
            return Boolean.FALSE;
        }

        Object token = args[0];
        Set<FinalizationWeakReference> refs = tokenMap.remove(token);

        if (refs == null || refs.isEmpty()) {
            return Boolean.FALSE;
        }

        boolean unregistered = false;
        for (FinalizationWeakReference ref : refs) {
            if (registrations.remove(ref) != null) {
                unregistered = true;
            }
        }

        return unregistered ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Processes pending cleanup callbacks from the reference queue. This method should be called
     * periodically to ensure timely cleanup callback execution.
     */
    private void processPendingCleanups() {
        @SuppressWarnings("unchecked")
        FinalizationWeakReference ref;
        while ((ref = (FinalizationWeakReference) referenceQueue.poll()) != null) {
            processCleanup(ref);
        }
    }

    /**
     * Processes cleanup for a specific weak reference.
     *
     * @param ref the weak reference to process
     */
    private void processCleanup(FinalizationWeakReference ref) {
        // Remove registration record and get cleanup data atomically
        RegistrationRecord record = registrations.remove(ref);
        if (record == null) {
            return; // Already processed or unregistered
        }

        // Remove from token map atomically to prevent race conditions
        if (record.unregisterToken != null) {
            tokenMap.computeIfPresent(
                    record.unregisterToken,
                    (token, refs) -> {
                        refs.remove(ref);
                        return refs.isEmpty() ? null : refs;
                    });
        }

        // Call the cleanup callback with proper error handling
        executeCleanupCallback(record.heldValue);
    }

    /**
     * Executes the cleanup callback with proper Context management and error handling.
     *
     * @param heldValue the held value to pass to the callback
     */
    private void executeCleanupCallback(Object heldValue) {
        if (cleanupCallback == null) {
            return;
        }

        Context cx = Context.getCurrentContext();
        if (cx == null) {
            cx = Context.enter();
            try {
                callCleanupCallback(cx, heldValue);
            } finally {
                Context.exit();
            }
        } else {
            callCleanupCallback(cx, heldValue);
        }
    }

    /**
     * Calls the cleanup callback with the held value.
     *
     * @param cx the context to use
     * @param heldValue the held value to pass to the callback
     */
    private void callCleanupCallback(Context cx, Object heldValue) {
        try {
            Scriptable scope = cleanupCallback.getParentScope();
            if (scope == null) {
                scope = cx.initStandardObjects();
            }
            cleanupCallback.call(cx, scope, scope, new Object[] {heldValue});
        } catch (RhinoException e) {
            // Cleanup callback errors should not break the cleanup process
            // In a production environment, this might be logged
        } catch (Exception e) {
            // Catch other runtime exceptions but avoid catching system errors
            // In a production environment, this might be logged
        }
    }

    /**
     * Checks if a value is a valid FinalizationRegistry target. According to the spec, only objects
     * (excluding null, undefined, and symbols) can be registered.
     *
     * @param target the value to check
     * @return true if the target is a valid object reference
     */
    private static boolean isValidTarget(Object target) {
        return target instanceof Scriptable
                && target != Undefined.instance
                && target != null
                && !(target instanceof Symbol);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }
}
