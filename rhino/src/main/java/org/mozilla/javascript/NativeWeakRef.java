/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.ref.WeakReference;

/**
 * Implementation of ECMAScript 2021 WeakRef.
 *
 * <p>WeakRef allows holding a weak reference to an object without preventing its garbage
 * collection. This is useful for caches, mappings, or any scenario where you want to reference an
 * object without keeping it alive.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Weak reference semantics - target can be garbage collected
 *   <li>Single {@code deref()} method to access target if still alive
 *   <li>Returns undefined if target has been collected
 *   <li>Thread-safe access using standard Java WeakReference
 *   <li>Coordinates with FinalizationRegistry for consistent GC behavior
 * </ul>
 *
 * <p>Unlike FinalizationRegistry which reacts to object finalization, WeakRef simply provides a way
 * to check if an object is still reachable without keeping it alive.
 *
 * @see <a href="https://tc39.es/ecma262/#sec-weak-ref-objects">ECMAScript WeakRef Objects</a>
 * @see NativeFinalizationRegistry
 */
public class NativeWeakRef extends ScriptableObject {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = "WeakRef";

    /** Flag to ensure this instance was created through proper constructor */
    private boolean instanceOfWeakRef = false;

    /** The weak reference to the target object */
    private WeakReference<Object> weakReference;

    /**
     * Initialize the WeakRef constructor and prototype in the given scope.
     *
     * @param cx the JavaScript context
     * @param scope the scope to install WeakRef in
     * @param sealed whether to seal the constructor and prototype
     * @return the WeakRef constructor function
     */
    static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        1,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeWeakRef::jsConstructor);
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

        constructor.definePrototypeMethod(
                scope,
                "deref",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "deref").js_deref(),
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

    /**
     * JavaScript constructor implementation.
     *
     * @param cx the JavaScript context
     * @param scope the constructor scope
     * @param args constructor arguments (first must be an object)
     * @return new WeakRef instance
     * @throws TypeError if target is not an object
     */
    private static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
        if (args.length < 1) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter", "WeakRef", "1", String.valueOf(args.length));
        }

        Object target = args[0];
        if (!isValidTarget(target)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.weakref.invalid.target", ScriptRuntime.typeof(target));
        }

        NativeWeakRef ref = new NativeWeakRef();
        ref.instanceOfWeakRef = true;
        ref.setPrototype(ScriptableObject.getClassPrototype(scope, CLASS_NAME));
        ref.setParentScope(scope);

        ref.weakReference = new WeakReference<>(target);
        return ref;
    }

    /**
     * Check if the given object can be used as a WeakRef target.
     *
     * @param target the target object to validate
     * @return true if target is a valid object that can be weakly referenced
     */
    private static boolean isValidTarget(Object target) {
        return ScriptRuntime.isObject(target);
    }

    /**
     * Validate and cast 'this' object to WeakRef.
     *
     * @param thisObj the 'this' object from JavaScript call
     * @param name method name for error messages
     * @return validated WeakRef instance
     * @throws TypeError if thisObj is not a proper WeakRef
     */
    private static NativeWeakRef realThis(Scriptable thisObj, String name) {
        if (!(thisObj instanceof NativeWeakRef)) {
            throw ScriptRuntime.typeErrorById("msg.incompat.call", CLASS_NAME + '.' + name);
        }
        NativeWeakRef ref = (NativeWeakRef) thisObj;
        if (!ref.instanceOfWeakRef) {
            throw ScriptRuntime.typeErrorById("msg.incompat.call", CLASS_NAME + '.' + name);
        }
        return ref;
    }

    /**
     * JavaScript deref() method implementation.
     *
     * <p>Returns the target object if it's still alive, or undefined if it has been garbage
     * collected. This method can be called multiple times and may return different results if the
     * target is collected between calls.
     *
     * @return the target object if still alive, otherwise undefined
     */
    private Object js_deref() {
        if (weakReference == null) {
            return Undefined.instance;
        }

        Object target = weakReference.get();
        return (target == null) ? Undefined.instance : target;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }
}
