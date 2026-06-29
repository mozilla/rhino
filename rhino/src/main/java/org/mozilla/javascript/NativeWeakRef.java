/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

import java.lang.ref.WeakReference;

public class NativeWeakRef extends ScriptableObject {
    private static final String CLASS_NAME = "WeakRef";

    private static final ClassDescriptor DESCRIPTOR;

    private boolean instanceOfWeakRef = false;

    private final WeakReference<Object> target;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(CLASS_NAME, 1, NativeWeakRef::js_constructor)
                        .withMethod(PROTO, "deref", 0, NativeWeakRef::deref)
                        .withProp(
                                PROTO,
                                SymbolKey.TO_STRING_TAG,
                                value(CLASS_NAME, DONTENUM | READONLY))
                        .build();
    }

    static JSFunction init(Context cx, VarScope s, boolean sealed) {
        return DESCRIPTOR.buildConstructor(cx, s, new NativeObject(), sealed);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @SuppressWarnings("unchecked")
    private NativeWeakRef(Object to) {
        this.target = new WeakReference(to);
    }

    private static Scriptable js_constructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object to, Object[] args) {
        if (args.length < 1 || Undefined.isUndefined(args[0])) {
            throw ScriptRuntime.typeErrorById("msg.method.missing.parameter", "WeakRef", 1, 0);
        }
        var target = args[0];
        if (!canBeHeldWeakly(target)) {
            throw ScriptRuntime.typeErrorById("msg.weakref.cant.hold.weakly");
        }
        var w = new NativeWeakRef(target);
        w.instanceOfWeakRef = true;
        w.setParentScope(f.getDeclarationScope());
        w.setPrototype((Scriptable) f.getPrototypeProperty());
        return w;
    }

    private static Object deref(
            Context cx, JSFunction f, Object nt, VarScope s, Object to, Object[] args) {
        NativeWeakRef self = realThis(to, "deref");
        var val = self.target.get();
        return (val == null ? Undefined.instance : val);
    }

    private static NativeWeakRef realThis(Object thisObj, String name) {
        NativeWeakRef nr = LambdaConstructor.convertThisObject(thisObj, NativeWeakRef.class);
        if (!nr.instanceOfWeakRef) {
            throw ScriptRuntime.typeErrorById("msg.incompat.call", name);
        }
        return nr;
    }

    private static boolean canBeHeldWeakly(Object o) {
        return ScriptRuntime.isObject(o)
                || (o instanceof Symbol s && s.getKind() != Symbol.Kind.REGISTERED);
    }
}
