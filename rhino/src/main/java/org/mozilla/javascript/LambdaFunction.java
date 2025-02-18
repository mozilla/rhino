/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.EnumSet;

/**
 * This class implements a single JavaScript function that has the prototype of the built-in
 * Function class, and which is implemented using a single function that can easily be implemented
 * using a lambda expression.
 */
public class LambdaFunction extends BaseFunction {

    private static final long serialVersionUID = -8388132362854748293L;

    // The target is expected to be a lambda -- lambdas should not be serialized.
    protected final SerializableCallable target;
    private final String name;
    private final String className;
    private final int length;

    /**
     * Create a new function. The new object will have the Function prototype and no parent. The
     * caller is responsible for binding this object to the appropriate scope.
     *
     * @param scope scope of the calling context
     * @param name name of the function
     * @param length the arity of the function
     * @param target an object that implements the function in Java. Since Callable is a
     *     single-function interface this will typically be implemented as a lambda.
     */
    public LambdaFunction(Scriptable scope, String name, int length, SerializableCallable target) {
        this(scope, name, null, length, target);
    }

    /**
     * Create a new function. The new object will have the Function prototype and no parent. The
     * caller is responsible for binding this object to the appropriate scope.
     *
     * @param scope scope of the calling context
     * @param name name of the function
     * @param length the arity of the function
     * @param prototype prototype to set for this function
     * @param target an object that implements the function in Java. Since Callable is a
     *     single-function interface this will typically be implemented as a lambda.
     */
    public LambdaFunction(
            Scriptable scope,
            String name,
            int length,
            Object prototype,
            SerializableCallable target) {
        this(scope, name, null, length, prototype, target);
    }

    /** Create a new built-in function, with no name, and no default prototype. */
    public LambdaFunction(Scriptable scope, int length, SerializableCallable target) {
        this.target = target;
        this.length = length;
        this.name = "";
        ScriptRuntime.setFunctionProtoAndParent(this, Context.getCurrentContext(), scope);
        this.className = null;
    }

    public LambdaFunction(
            Scriptable scope,
            String name,
            String className,
            int length,
            SerializableCallable target) {
        this.target = target;
        this.name = name;
        this.length = length;
        ScriptRuntime.setFunctionProtoAndParent(this, Context.getCurrentContext(), scope);
        setupDefaultPrototype();
        this.className = className;
    }

    public LambdaFunction(
            Scriptable scope,
            String name,
            String className,
            int length,
            Object prototype,
            SerializableCallable target) {
        this.target = target;
        this.name = name;
        this.length = length;
        ScriptRuntime.setFunctionProtoAndParent(this, Context.getCurrentContext(), scope);
        setPrototypeProperty(prototype);
        this.className = className;
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return target.call(cx, scope, thisObj, args);
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        throw ScriptRuntime.typeErrorById("msg.no.new", getFunctionName());
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getArity() {
        return length;
    }

    @Override
    public String getFunctionName() {
        return name;
    }

    Callable getTarget() {
        return target;
    }

    @Override
    String decompile(int indent, EnumSet<DecompilerFlag> flags) {
        StringBuilder sb = new StringBuilder();
        boolean justbody = flags.contains(DecompilerFlag.ONLY_BODY);
        if (!justbody) {
            sb.append("function ");
            sb.append(getFunctionName());
            sb.append("() { ");
        }
        sb.append("[native code for ");
        if (className != null) {
            sb.append(className);
            sb.append('.');
        }
        sb.append(getFunctionName());
        sb.append(", arity=");
        sb.append(getArity());
        sb.append(justbody ? "]\n" : "] }\n");
        return sb.toString();
    }
}
