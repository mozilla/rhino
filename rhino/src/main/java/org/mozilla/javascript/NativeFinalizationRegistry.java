/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

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

    /** Manages all registration and cleanup operations */
    private final FinalizationRegistrationManager registrationManager =
            new FinalizationRegistrationManager();

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
        Object unregisterToken = args.length > 2 ? args[2] : Undefined.instance;

        FinalizationValidation.validateTarget(target);
        FinalizationValidation.validateNotSameValue(target, heldValue);
        FinalizationValidation.validateUnregisterToken(unregisterToken);

        registry.registrationManager.register(target, heldValue, unregisterToken, registry);
        return Undefined.instance;
    }

    /** JavaScript unregister() method implementation. */
    private static Object unregister(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeFinalizationRegistry registry = realThis(thisObj, "unregister");
        if (args.length < 1) {
            return false;
        }

        Object token = args[0];
        FinalizationValidation.validateUnregisterTokenStrict(token);

        return registry.registrationManager.unregister(token);
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

        registry.cleanupSomeCallback = callback;
        registry.registrationManager.processCleanups(100, heldValue -> {
            registry.executeCleanupCallback(cx, heldValue);
        });
        registry.cleanupSomeCallback = null;
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
     * Execute the cleanup callback using JSCode architecture (Context-safe).
     *
     * <p>Uses JSCode execution to avoid Context capture issues. Creates a FinalizationCleanupCode
     * instance for Context-safe callback execution per aardvark179's architecture patterns.
     *
     * @param cx the JavaScript execution context (fresh, never stored)
     * @param heldValue the value to pass to the cleanup callback
     */
    void executeCleanupCallback(Context cx, Object heldValue) {
        Function callbackToUse =
                cleanupSomeCallback != null ? cleanupSomeCallback : cleanupCallback;

        if (callbackToUse == null) {
            return;
        }

        // Execute cleanup callback with fresh Context (Context-safe)
        try {
            Scriptable callbackScope = callbackToUse.getParentScope();
            if (callbackScope == null) {
                callbackScope = this.getParentScope();
            }
            callbackToUse.call(cx, callbackScope, callbackScope, new Object[] {heldValue});
        } catch (Exception e) {
            // Cleanup errors should not propagate per ECMAScript specification
            if (cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS)) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Legacy cleanup method - deprecated but kept for backward compatibility. Required by existing
     * test infrastructure that expects this signature.
     *
     * @deprecated Use {@link #executeCleanupCallback(Context, Object)} instead
     */
    @Deprecated
    void executeCleanupCallback(Object heldValue) {
        executeCleanupWithFreshContext(heldValue);
    }

    /**
     * Execute cleanup callback with fresh Context acquisition. Consolidates the pattern used in
     * scheduled cleanup tasks.
     */
    private void executeCleanupWithFreshContext(Object heldValue) {
        Context cx = Context.getCurrentContext();
        if (cx != null) {
            executeCleanupCallback(cx, heldValue);
        }
    }

    /**
     * Clean up when this registry is being GC'd.
     *
     * <p>Uses finalize() over Cleaner API due to dynamic registration requirements. This approach
     * was specifically recommended by aardvark179 for FinalizationRegistry's unique GC cleanup
     * patterns where references are added/removed dynamically during execution.
     */
    @Override
    @SuppressWarnings({"deprecation", "finalize", "Finalize"})
    // ErrorProne: Finalize is acceptable for FinalizationRegistry cleanup
    protected void finalize() throws Throwable {
        try {
            // Clear all registrations to prevent memory leaks
            registrationManager.clear();
        } finally {
            super.finalize();
        }
    }







    @Override
    public String getClassName() {
        return CLASS_NAME;
    }
}

