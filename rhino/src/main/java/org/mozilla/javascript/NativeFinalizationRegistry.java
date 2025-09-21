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

    private final Function cleanupCallback;
    private final ReferenceQueue<Scriptable> referenceQueue;
    private final ConcurrentHashMap<FinalizationWeakReference, RegistrationRecord> registrations;
    private final ConcurrentHashMap<Object, Set<FinalizationWeakReference>> tokenMap;

    private class FinalizationWeakReference extends WeakReference<Scriptable> {
        FinalizationWeakReference(Scriptable referent) {
            super(referent, referenceQueue);
        }
    }

    private static class RegistrationRecord {
        final Object heldValue;
        final Object unregisterToken;

        RegistrationRecord(Object heldValue, Object unregisterToken) {
            this.heldValue = heldValue;
            this.unregisterToken = unregisterToken;
        }
    }

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
                        1,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeFinalizationRegistry::constructor);
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        return constructor;
    }

    private static void configurePrototype(LambdaConstructor constructor, Scriptable scope) {
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
    }

    private static void sealConstructor(LambdaConstructor constructor) {
        constructor.sealObject();
        ScriptableObject prototype = (ScriptableObject) constructor.getPrototypeProperty();
        if (prototype != null) {
            prototype.sealObject();
        }
    }

    private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
        if (args.length < 1 || !(args[0] instanceof Function)) {
            throw ScriptRuntime.typeErrorById("msg.finalization.registry.no.callback");
        }
        return new NativeFinalizationRegistry((Function) args[0]);
    }

    private NativeFinalizationRegistry(Function cleanupCallback) {
        this.cleanupCallback = cleanupCallback;
        this.referenceQueue = new ReferenceQueue<>();
        this.registrations = new ConcurrentHashMap<>();
        this.tokenMap = new ConcurrentHashMap<>();
    }

    private static Object register(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeFinalizationRegistry registry = ensureFinalizationRegistry(thisObj);
        return registry.registerTarget(args);
    }

    private static Object unregister(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeFinalizationRegistry registry = ensureFinalizationRegistry(thisObj);
        return registry.unregisterToken(args);
    }

    private static Object cleanupSome(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeFinalizationRegistry registry = ensureFinalizationRegistry(thisObj);

        Function callback = null;
        if (args.length > 0) {
            if (!Undefined.isUndefined(args[0]) && !(args[0] instanceof Function)) {
                throw ScriptRuntime.typeErrorById(
                        "msg.isnt.function",
                        ScriptRuntime.toString(args[0]),
                        ScriptRuntime.typeof(args[0]));
            }
            if (args[0] instanceof Function) {
                callback = (Function) args[0];
            }
        }

        registry.performCleanupSome(cx, callback);
        return Undefined.instance;
    }

    private static NativeFinalizationRegistry ensureFinalizationRegistry(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeFinalizationRegistry.class);
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

        return Boolean.valueOf(unregistered);
    }

    private void processPendingCleanups() {
        @SuppressWarnings("unchecked")
        FinalizationWeakReference ref;
        while ((ref = (FinalizationWeakReference) referenceQueue.poll()) != null) {
            processCleanup(ref);
        }
    }

    private void processCleanup(FinalizationWeakReference ref) {
        // Remove registration record and get cleanup data atomically
        RegistrationRecord record = registrations.remove(ref);
        if (record == null) {
            return;
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

        executeCleanupCallback(record.heldValue);
    }

    private void performCleanupSome(Context cx, Function callback) {
        Function callbackToUse = (callback != null) ? callback : this.cleanupCallback;
        if (callbackToUse == null) {
            return;
        }

        int cleanupCount = 0;
        int maxCleanups = 100;

        @SuppressWarnings("unchecked")
        FinalizationWeakReference ref;
        while (cleanupCount < maxCleanups
                && (ref = (FinalizationWeakReference) referenceQueue.poll()) != null) {
            RegistrationRecord record = registrations.remove(ref);
            if (record != null) {
                if (record.unregisterToken != null) {
                    final FinalizationWeakReference finalRef = ref;
                    tokenMap.computeIfPresent(
                            record.unregisterToken,
                            (token, refs) -> {
                                refs.remove(finalRef);
                                return refs.isEmpty() ? null : refs;
                            });
                }

                try {
                    Scriptable scope = callbackToUse.getParentScope();
                    callbackToUse.call(cx, scope, scope, new Object[] {record.heldValue});
                    cleanupCount++;
                } catch (RhinoException e) {
                    Context.reportWarning(
                            "FinalizationRegistry cleanup callback error: " + e.getMessage());
                }
            }
        }
    }

    private void executeCleanupCallback(Object heldValue) {
        if (cleanupCallback == null) {
            return;
        }

        Context cx = Context.getCurrentContext();
        if (cx == null) {
            try (Context enteredCx = Context.enter()) {
                callCleanupCallback(enteredCx, heldValue);
            }
        } else {
            callCleanupCallback(cx, heldValue);
        }
    }

    private void callCleanupCallback(Context cx, Object heldValue) {
        try {
            Scriptable scope = cleanupCallback.getParentScope();
            cleanupCallback.call(cx, scope, scope, new Object[] {heldValue});
        } catch (RhinoException e) {
            Context.reportWarning("FinalizationRegistry cleanup callback error: " + e.getMessage());
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
