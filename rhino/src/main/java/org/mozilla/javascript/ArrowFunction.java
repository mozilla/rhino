/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.EnumSet;

/** The class for Arrow Function Definitions EcmaScript 6 Rev 14, March 8, 2013 Draft spec , 13.2 */
public class ArrowFunction extends BaseFunction {

    private static final long serialVersionUID = -7377989503697220633L;

    private final Callable targetFunction;
    private final Scriptable boundThis;
    private final Scriptable boundHomeObject;

    public ArrowFunction(
            Context cx,
            Scriptable scope,
            Callable targetFunction,
            Scriptable boundThis,
            Scriptable boundHomeObject) {
        this.targetFunction = targetFunction;
        this.boundThis = boundThis;
        this.boundHomeObject = boundHomeObject;

        ScriptRuntime.setFunctionProtoAndParent(this, cx, scope, false);

        Function thrower = ScriptRuntime.typeErrorThrower(cx);
        NativeObject throwing = new NativeObject();
        ScriptRuntime.setBuiltinProtoAndParent(throwing, scope, TopLevel.Builtins.Object);
        throwing.put("get", throwing, thrower);
        throwing.put("set", throwing, thrower);
        throwing.put("enumerable", throwing, Boolean.FALSE);
        throwing.put("configurable", throwing, Boolean.FALSE);
        throwing.preventExtensions();

        this.defineOwnProperty(cx, "caller", throwing, false);
        this.defineOwnProperty(cx, "arguments", throwing, false);
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return targetFunction.call(cx, scope, getCallThis(cx), args);
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        throw ScriptRuntime.typeErrorById(
                "msg.not.ctor", decompile(0, EnumSet.noneOf(DecompilerFlag.class)));
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        if (targetFunction instanceof Function) {
            return ((Function) targetFunction).hasInstance(instance);
        }
        throw ScriptRuntime.typeErrorById("msg.not.ctor");
    }

    @Override
    public int getLength() {
        if (targetFunction instanceof BaseFunction) {
            return ((BaseFunction) targetFunction).getLength();
        }
        return 0;
    }

    @Override
    public int getArity() {
        return getLength();
    }

    @Override
    String decompile(int indent, EnumSet<DecompilerFlag> flags) {
        if (targetFunction instanceof BaseFunction) {
            return ((BaseFunction) targetFunction).decompile(indent, flags);
        }
        return super.decompile(indent, flags);
    }

    Scriptable getCallThis(Context cx) {
        return boundThis != null ? boundThis : ScriptRuntime.getTopCallScope(cx);
    }

    Scriptable getBoundHomeObject() {
        return this.boundHomeObject;
    }

    Callable getTargetFunction() {
        return targetFunction;
    }

    static boolean equalObjectGraphs(ArrowFunction f1, ArrowFunction f2, EqualObjectGraphs eq) {
        return eq.equalGraphs(f1.boundThis, f2.boundThis)
                && eq.equalGraphs(f1.targetFunction, f2.targetFunction);
    }
}
