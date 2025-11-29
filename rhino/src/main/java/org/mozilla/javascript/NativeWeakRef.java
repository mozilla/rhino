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
 * <p>Allows holding weak references to objects without preventing garbage collection. Provides
 * {@code deref()} method to access target if still alive.
 *
 * @see <a href="https://tc39.es/ecma262/#sec-weak-ref-objects">ECMAScript WeakRef Objects</a>
 */
public class NativeWeakRef extends ScriptableObject {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = "WeakRef";

    private WeakReference<Object> weakReference;

    /** Initialize WeakRef constructor and prototype. */
    static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        1,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeWeakRef::jsConstructor);

        constructor.definePrototypeMethod(scope, "deref", 0, NativeWeakRef::js_deref);

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
        if (args.length < 1) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    CLASS_NAME,
                    "1",
                    String.valueOf(args.length));
        }

        Object target = args[0];
        if (!isValidTarget(target)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.weakref.invalid.target", ScriptRuntime.typeof(target));
        }

        NativeWeakRef ref = new NativeWeakRef();
        ref.weakReference = new WeakReference<>(target);
        return ref;
    }

    /**
     * JavaScript deref() method implementation.
     *
     * <p>Returns the target object if it's still alive, or undefined if it has been garbage
     * collected.
     */
    private static Object js_deref(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!(thisObj instanceof NativeWeakRef)) {
            throw ScriptRuntime.typeErrorById("msg.incompat.call", CLASS_NAME + ".deref");
        }
        NativeWeakRef ref = (NativeWeakRef) thisObj;
        Object target = ref.weakReference.get();
        return target != null ? target : Undefined.instance;
    }

    /**
     * Check if the given object can be used as a WeakRef target.
     *
     * <p>Per ECMAScript spec, WeakRef targets can be:
     * - Any object (Scriptable)
     * - Unregistered symbols (created with Symbol(), not Symbol.for())
     * - Registered symbols (Symbol.for()) cannot be held weakly as they persist globally
     *
     * @param target the target object to validate
     * @return true if target is a valid object or unregistered symbol that can be weakly referenced
     */
    private static boolean isValidTarget(Object target) {
        if (target instanceof Scriptable && !(target instanceof Symbol)) {
            return true;
        }
        if (target instanceof Symbol) {
            Symbol symbol = (Symbol) target;
            // Only unregistered symbols can be held weakly
            return symbol.getKind() != Symbol.Kind.REGISTERED;
        }
        return false;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }
}
