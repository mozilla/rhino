/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements a JavaScript function that may be used as a constructor by delegating to an
 * interface that can be easily implemented as a lambda. The LambdaFunction class may be used to add
 * functions to the prototype that are also implemented as lambdas.
 *
 * <p>In micro benchmarks (as of 2021) using this class to implement a built-in class is about 15%
 * more efficient than using IdScriptableObject, and about 25% faster than using reflection via the
 * ScriptableObject.defineClass() family of methods. Furthermore, it results in code that more
 * directly maps to JavaScript idioms than either methods, it is much easier to implement than
 * IdScriptableObject, and the lambda pattern makes it easier to maintain state in various ways that
 * don't always map directly to the existing concepts.
 */
public class LambdaConstructor extends LambdaFunction {

    private static final long serialVersionUID = 2691205302914111400L;

    /** If this flag is set, the constructor may be invoked as an ordinary function */
    public static final int CONSTRUCTOR_FUNCTION = 1;

    /** If this flag is set, the constructor may be invoked using "new" */
    public static final int CONSTRUCTOR_NEW = 1 << 1;

    /** By default, the constructor may be invoked either way */
    public static final int CONSTRUCTOR_DEFAULT = CONSTRUCTOR_FUNCTION | CONSTRUCTOR_NEW;

    // Lambdas should not be serialized.
    private final transient Constructable targetConstructor;
    private final int flags;

    /**
     * Create a new function that may be used as a constructor. The new object will have the
     * Function prototype and no parent. The caller is responsible for binding this object to the
     * appropriate scope.
     *
     * @param scope scope of the calling context
     * @param name name of the function
     * @param length the arity of the function
     * @param target an object that implements the function in Java. Since Constructable is a
     *     single-function interface this will typically be implemented as a lambda.
     */
    public LambdaConstructor(Scriptable scope, String name, int length, Constructable target) {
        super(scope, name, length, null);
        this.targetConstructor = target;
        this.flags = CONSTRUCTOR_DEFAULT;
    }

    /**
     * Create a new function and control whether it may be invoked using new, as a function, or
     * both.
     */
    public LambdaConstructor(
            Scriptable scope, String name, int length, int flags, Constructable target) {
        super(scope, name, length, null);
        this.targetConstructor = target;
        this.flags = flags;
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if ((flags & CONSTRUCTOR_FUNCTION) == 0) {
            throw ScriptRuntime.typeErrorById("msg.constructor.no.function", getFunctionName());
        }
        return targetConstructor.construct(cx, scope, args);
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        if ((flags & CONSTRUCTOR_NEW) == 0) {
            throw ScriptRuntime.typeErrorById("msg.no.new", getFunctionName());
        }
        Scriptable obj = targetConstructor.construct(cx, scope, args);
        obj.setPrototype(getClassPrototype());
        obj.setParentScope(scope);
        return obj;
    }

    /**
     * Define a function property on the prototype of the constructor using a LambdaFunction under
     * the covers.
     */
    public void definePrototypeMethod(Scriptable scope, String name, int length, Callable target) {
        LambdaFunction f = new LambdaFunction(scope, name, length, target);
        ScriptableObject proto = getPrototypeScriptable();
        proto.defineProperty(name, f, 0);
    }

    /**
     * Define a function property on the prototype of the constructor using a LambdaFunction under
     * the covers.
     */
    public void definePrototypeMethod(
            Scriptable scope,
            String name,
            int length,
            Callable target,
            int attributes,
            int propertyAttributes) {
        LambdaFunction f = new LambdaFunction(scope, name, length, target);
        f.setStandardPropertyAttributes(propertyAttributes);
        ScriptableObject proto = getPrototypeScriptable();
        proto.defineProperty(name, f, attributes);
    }

    /** Define a property that may be of any type on the prototype of this constructor. */
    public void definePrototypeProperty(String name, Object value, int attributes) {
        ScriptableObject proto = getPrototypeScriptable();
        proto.defineProperty(name, value, attributes);
    }

    public void definePrototypeProperty(Symbol key, Object value, int attributes) {
        ScriptableObject proto = getPrototypeScriptable();
        proto.defineProperty(key, value, attributes);
    }

    /**
     * Define a function property directly on the constructor that is implemented under the covers
     * by a LambdaFunction.
     *
     * @param name the key to use to look up the new function property, and also the value to return
     *     for the "name" property of the Function object
     * @param length the value to return for the "length" property of the Function object
     * @param target the target to call when the method is invoked
     * @param attributes the attributes to set on the new property
     */
    public void defineConstructorMethod(
            Scriptable scope, String name, int length, Callable target, int attributes) {
        LambdaFunction f = new LambdaFunction(scope, name, length, target);
        defineProperty(name, f, attributes);
    }

    /**
     * Define a function property directly on the constructor that is implemented under the covers
     * by a LambdaFunction.
     *
     * @param key the Symbol to use to look up the property
     * @param name the value to return for the "name" property of the Function object
     * @param length the value to return for the "length" property of the Function object
     * @param target the target to call when the method is invoked
     * @param attributes the attributes to set on the new property
     */
    public void defineConstructorMethod(
            Scriptable scope,
            Symbol key,
            String name,
            int length,
            Callable target,
            int attributes) {
        LambdaFunction f = new LambdaFunction(scope, name, length, target);
        defineProperty(key, f, attributes);
    }

    /**
     * Define a function property directly on the constructor that is implemented under the covers
     * by a LambdaFunction, and override the properties of its "name", "length", and "arity"
     * properties.
     */
    public void defineConstructorMethod(
            Scriptable scope,
            String name,
            int length,
            Callable target,
            int attributes,
            int propertyAttributes) {
        LambdaFunction f = new LambdaFunction(scope, name, length, target);
        f.setStandardPropertyAttributes(propertyAttributes);
        defineProperty(name, f, attributes);
    }

    /**
     * A convenience method to convert JavaScript's "this" object into a target class and throw a
     * TypeError if it does not match. This is useful for implementing lambda functions, as "this"
     * in JavaScript doesn't necessarily map to an instance of the class.
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertThisObject(Scriptable thisObj, Class<T> targetClass) {
        if (!targetClass.isInstance(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.this.not.instance");
        }
        return (T) thisObj;
    }

    private ScriptableObject getPrototypeScriptable() {
        Object prop = getPrototypeProperty();
        if (!(prop instanceof ScriptableObject)) {
            throw ScriptRuntime.typeError("Not properly a lambda constructor");
        }
        return (ScriptableObject) prop;
    }
}
