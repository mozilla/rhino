/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * The class for  Arrow Function Definitions
 * EcmaScript 6 Rev 14, March 8, 2013 Draft spec , 13.2
 */
public class ArrowFunction extends BaseFunction {
    
    static final long serialVersionUID = -7377989503697220633L;
    
    private final Callable targetFunction;
    private final Scriptable boundThis;

    public ArrowFunction(Context cx, Scriptable scope, Callable targetFunction, Scriptable boundThis)
    {
        this.targetFunction = targetFunction;
        this.boundThis = boundThis;

        ScriptRuntime.setFunctionProtoAndParent(this, scope);

        Function thrower = ScriptRuntime.typeErrorThrower();
        NativeObject throwing = new NativeObject();
        throwing.put("get", throwing, thrower);
        throwing.put("set", throwing, thrower);
        throwing.put("enumerable", throwing, false);
        throwing.put("configurable", throwing, false);
        throwing.preventExtensions();

        this.defineOwnProperty(cx, "caller", throwing, false);
        this.defineOwnProperty(cx, "arguments", throwing, false);
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
    {
        Scriptable callThis = boundThis != null ? boundThis : ScriptRuntime.getTopCallScope(cx);
        return targetFunction.call(cx, scope, callThis, args);
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        throw ScriptRuntime.typeError1("msg.not.ctor", decompile(0, 0));
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        if (targetFunction instanceof Function) {
            return ((Function) targetFunction).hasInstance(instance);
        }
        throw ScriptRuntime.typeError0("msg.not.ctor");
    }

    @Override
    public int getLength() {
        if (targetFunction instanceof BaseFunction) {
            return ((BaseFunction) targetFunction).getLength();
        }
        return 0;
    }

    @Override
    String decompile(int indent, int flags)
    {
        if (targetFunction instanceof BaseFunction) {
            return ((BaseFunction)targetFunction).decompile(indent, flags);
        }
        return super.decompile(indent, flags);
    }
}
