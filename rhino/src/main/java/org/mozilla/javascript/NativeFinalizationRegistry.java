/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of ECMAScript 2021 FinalizationRegistry.
 *
 * <p>Allows registering cleanup callbacks that are invoked when registered objects are garbage
 * collected. Uses {@link PhantomReference} for GC detection, which is available on all platforms
 * including Android.
 *
 * @see <a
 *     href="https://tc39.es/ecma262/#sec-finalization-registry-objects">ECMAScript
 *     FinalizationRegistry Objects</a>
 */
public class NativeFinalizationRegistry extends ScriptableObject {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = "FinalizationRegistry";

    // Shared reference queue for efficient GC detection across all registries
    private static final ReferenceQueue<Object> SHARED_QUEUE = new ReferenceQueue<>();

    private final Function cleanupCallback;
    private final Scriptable parentScope;
    private final Set<RegistrationReference> activeRegistrations = ConcurrentHashMap.newKeySet();
    private final Map<TokenKey, Set<RegistrationReference>> tokenIndex =
            new ConcurrentHashMap<>();

    /** Initialize FinalizationRegistry constructor and prototype. */
    static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        1,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeFinalizationRegistry::jsConstructor);

        constructor.definePrototypeMethod(
                scope, "register", 2, NativeFinalizationRegistry::js_register);
        constructor.definePrototypeMethod(
                scope, "unregister", 1, NativeFinalizationRegistry::js_unregister);

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

    /** JavaScript constructor implementation. */
    private static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
        if (args.length < 1 || !(args[0] instanceof Function)) {
            throw ScriptRuntime.typeErrorById("msg.finalization.callback.required");
        }

        NativeFinalizationRegistry registry =
                new NativeFinalizationRegistry((Function) args[0], scope);
        return registry;
    }

    private NativeFinalizationRegistry(Function cleanupCallback, Scriptable scope) {
        this.cleanupCallback = cleanupCallback;
        this.parentScope = scope;
    }

    /** JavaScript register() method implementation. */
    private static Object js_register(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!(thisObj instanceof NativeFinalizationRegistry)) {
            throw ScriptRuntime.typeErrorById("msg.incompat.call", CLASS_NAME + ".register");
        }
        NativeFinalizationRegistry registry = (NativeFinalizationRegistry) thisObj;
        return registry.register(cx, scope, args);
    }

    private Object register(Context cx, Scriptable scope, Object[] args) {
        if (args.length < 2) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    CLASS_NAME + ".register",
                    "2",
                    String.valueOf(args.length));
        }

        Object target = args[0];
        Object heldValue = args[1];
        Object unregisterToken = args.length > 2 ? args[2] : Undefined.instance;

        if (!isValidTarget(target)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.finalization.invalid.target", ScriptRuntime.typeof(target));
        }

        // Per spec: target and heldValue cannot be the same
        if (target == heldValue) {
            throw ScriptRuntime.typeErrorById("msg.finalization.target.same.as.held");
        }

        // Per spec: target and unregisterToken cannot be the same
        if (!Undefined.isUndefined(unregisterToken) && target == unregisterToken) {
            throw ScriptRuntime.typeErrorById("msg.finalization.target.same.as.token");
        }

        // Validate unregisterToken if provided
        if (!Undefined.isUndefined(unregisterToken) && !isValidToken(unregisterToken)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.finalization.invalid.token", ScriptRuntime.typeof(unregisterToken));
        }

        // Create registration reference
        RegistrationReference ref = new RegistrationReference(target, this, heldValue);
        activeRegistrations.add(ref);

        if (!Undefined.isUndefined(unregisterToken)) {
            TokenKey key = new TokenKey(unregisterToken);
            Set<RegistrationReference> refs =
                    tokenIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
            refs.add(ref);
        }

        // Process any pending finalizations
        processCleanups(cx, 10);

        return Undefined.instance;
    }

    /** JavaScript unregister() method implementation. */
    private static Object js_unregister(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!(thisObj instanceof NativeFinalizationRegistry)) {
            throw ScriptRuntime.typeErrorById("msg.incompat.call", CLASS_NAME + ".unregister");
        }
        NativeFinalizationRegistry registry = (NativeFinalizationRegistry) thisObj;

        if (args.length < 1) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    CLASS_NAME + ".unregister",
                    "1",
                    String.valueOf(args.length));
        }

        Object token = args[0];
        if (!registry.isValidToken(token)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.finalization.invalid.token", ScriptRuntime.typeof(token));
        }

        TokenKey key = new TokenKey(token);
        Set<RegistrationReference> refs = registry.tokenIndex.remove(key);
        if (refs != null && !refs.isEmpty()) {
            for (RegistrationReference ref : refs) {
                registry.activeRegistrations.remove(ref);
                ref.clear();
            }
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Process pending cleanup callbacks for finalized objects.
     *
     * @param cx the JavaScript execution context
     * @param maxCleanups maximum number of cleanups to process
     */
    private void processCleanups(Context cx, int maxCleanups) {
        int processed = 0;
        java.lang.ref.Reference<?> ref;

        while (processed < maxCleanups && (ref = SHARED_QUEUE.poll()) != null) {
            if (ref instanceof RegistrationReference) {
                RegistrationReference regRef = (RegistrationReference) ref;
                if (regRef.registry == this && activeRegistrations.remove(regRef)) {
                    executeCleanup(cx, regRef.heldValue);
                    processed++;
                }
            }
            ref.clear();
        }
    }

    /**
     * Execute the cleanup callback for a finalized object.
     *
     * @param cx the JavaScript execution context
     * @param heldValue the value to pass to the cleanup callback
     */
    private void executeCleanup(Context cx, Object heldValue) {
        try {
            cleanupCallback.call(cx, parentScope, this, new Object[] {heldValue});
        } catch (RhinoException e) {
            // Per spec, errors in cleanup callbacks don't propagate
            Context.reportWarning(
                    "FinalizationRegistry cleanup callback threw: " + e.getMessage());
        }
    }

    /**
     * Check if the given object can be used as a FinalizationRegistry target.
     *
     * <p>Per ECMAScript spec, FinalizationRegistry targets can be:
     * - Any object (Scriptable)
     * - Unregistered symbols (created with Symbol(), not Symbol.for())
     * - Registered symbols (Symbol.for()) cannot be registered as they persist globally
     *
     * @param target the target object to validate
     * @return true if target is a valid object or unregistered symbol that can be registered
     */
    private static boolean isValidTarget(Object target) {
        if (target instanceof Scriptable && !(target instanceof Symbol)) {
            return true;
        }
        if (target instanceof Symbol) {
            Symbol symbol = (Symbol) target;
            // Only unregistered symbols can be used as targets
            return symbol.getKind() != Symbol.Kind.REGISTERED;
        }
        return false;
    }

    /**
     * Check if the given value can be used as an unregister token.
     *
     * <p>Per ECMAScript spec, registered symbols (created with Symbol.for()) cannot be held
     * weakly because they persist in the global registry.
     *
     * @param token the token to validate
     * @return true if token can be used as an unregister token
     */
    private boolean isValidToken(Object token) {
        if (token instanceof Scriptable && !(token instanceof Symbol)) {
            return true;
        }
        if (token instanceof Symbol) {
            Symbol symbol = (Symbol) token;
            return symbol.getKind() != Symbol.Kind.REGISTERED;
        }
        return false;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    /** PhantomReference that tracks registered objects for finalization. */
    private static class RegistrationReference extends PhantomReference<Object> {
        private final NativeFinalizationRegistry registry;
        private final Object heldValue;

        RegistrationReference(
                Object target, NativeFinalizationRegistry registry, Object heldValue) {
            super(target, SHARED_QUEUE);
            this.registry = registry;
            this.heldValue = heldValue;
        }
    }

    /** Wrapper for unregister tokens providing identity-based equality. */
    private static class TokenKey {
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
