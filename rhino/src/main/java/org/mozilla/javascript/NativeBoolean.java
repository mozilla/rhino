/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements the Boolean native object. See ECMA 15.6.
 *
 * @author Norris Boyd
 */
final class NativeBoolean extends ScriptableObject {
    private static final long serialVersionUID = -3716996899943880933L;

    private static final String CLASS_NAME = "Boolean";

    private final boolean booleanValue;

    static void init(Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        1,
                        NativeBoolean::js_constructorFunc,
                        NativeBoolean::js_constructor);
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        // Boolean is an unusual object in that the prototype is itself a Boolean
        constructor.setPrototypeScriptable(new NativeBoolean(false));

        constructor.definePrototypeMethod(
                scope, "toString", 0, NativeBoolean::js_toString, DONTENUM, DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope, "toSource", 0, NativeBoolean::js_toSource, DONTENUM, DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope, "valueOf", 0, NativeBoolean::js_valueOf, DONTENUM, DONTENUM | READONLY);

        ScriptableObject.defineProperty(scope, CLASS_NAME, constructor, DONTENUM);
        if (sealed) {
            constructor.sealObject();
        }
    }

    NativeBoolean(boolean b) {
        booleanValue = b;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static boolean toValue(Scriptable thisObj) {
        return LambdaConstructor.ensureType(thisObj, NativeBoolean.class, "Boolean").booleanValue;
    }

    @Override
    public Object getDefaultValue(Class<?> typeHint) {
        // This is actually non-ECMA, but will be proposed
        // as a change in round 2.
        if (typeHint == ScriptRuntime.BooleanClass) return ScriptRuntime.wrapBoolean(booleanValue);
        return super.getDefaultValue(typeHint);
    }

    private static Object js_constructorFunc(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        boolean b = ScriptRuntime.toBoolean(args.length > 0 ? args[0] : Undefined.instance);
        return ScriptRuntime.wrapBoolean(b);
    }

    private static NativeBoolean js_constructor(Context cx, Scriptable scope, Object[] args) {
        boolean b = ScriptRuntime.toBoolean(args.length > 0 ? args[0] : Undefined.instance);
        return new NativeBoolean(b);
    }

    private static String js_toString(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return toValue(thisObj) ? "true" : "false";
    }

    private static Object js_valueOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return toValue(thisObj);
    }

    private static Object js_toSource(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return "(new Boolean(" + ScriptRuntime.toString(toValue(thisObj)) + "))";
    }
}
