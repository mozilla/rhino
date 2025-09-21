/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.ref.WeakReference;

/**
 * Implementation of the ES2021 WeakRef constructor and prototype.
 *
 * <p>WeakRef allows holding a weak reference to an object without preventing its garbage
 * collection. This is useful for caches, mappings, or any scenario where you want to reference an
 * object without keeping it alive.
 *
 * <p>The WeakRef object provides a single method, deref(), which returns the referenced object if
 * it's still alive, or undefined if it has been collected.
 *
 * @see <a href="https://tc39.es/ecma262/#sec-weak-ref-objects">ECMAScript WeakRef Objects</a>
 */
public class NativeWeakRef extends ScriptableObject {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = "WeakRef";

    private boolean instanceOfWeakRef = false;
    private WeakReference<Object> weakReference;

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
            ((ScriptableObject) constructor.getPrototypeProperty()).sealObject();
        }
        return constructor;
    }

    private static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
        if (args.length < 1) {
            throw ScriptRuntime.typeErrorById("msg.weakref.no.target");
        }

        Object target = args[0];
        if (!canBeHeldWeakly(target)) {
            throw ScriptRuntime.typeErrorById("msg.weakref.target.not.object");
        }

        NativeWeakRef ref = new NativeWeakRef();
        ref.instanceOfWeakRef = true;
        ref.weakReference = new WeakReference<>(target);
        return ref;
    }

    private static boolean canBeHeldWeakly(Object target) {
        return ScriptRuntime.isObject(target);
    }

    private static NativeWeakRef realThis(Scriptable thisObj, String name) {
        if (thisObj instanceof NativeWeakRef) {
            NativeWeakRef ref = (NativeWeakRef) thisObj;
            if (ref.instanceOfWeakRef) {
                return ref;
            }
        }
        throw ScriptRuntime.typeErrorById("msg.incompat.call", name);
    }

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
