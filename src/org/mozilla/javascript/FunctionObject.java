/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class FunctionObject extends BaseFunction
{
    private static final long serialVersionUID = -5332312783643935019L;

    /**
     * Create a JavaScript function object from a Java method.
     *
     * <p>The <code>member</code> argument must be either a java.lang.reflect.Method
     * or a java.lang.reflect.Constructor and must match one of two forms.<p>
     *
     * The first form is a member with zero or more parameters
     * of the following types: Object, String, boolean, Scriptable,
     * int, or double. The Long type is not supported
     * because the double representation of a long (which is the
     * EMCA-mandated storage type for Numbers) may lose precision.
     * If the member is a Method, the return value must be void or one
     * of the types allowed for parameters.<p>
     *
     * The runtime will perform appropriate conversions based
     * upon the type of the parameter. A parameter type of
     * Object specifies that no conversions are to be done. A parameter
     * of type String will use Context.toString to convert arguments.
     * Similarly, parameters of type double, boolean, and Scriptable
     * will cause Context.toNumber, Context.toBoolean, and
     * Context.toObject, respectively, to be called.<p>
     *
     * If the method is not static, the Java 'this' value will
     * correspond to the JavaScript 'this' value. Any attempt
     * to call the function with a 'this' value that is not
     * of the right Java type will result in an error.<p>
     *
     * The second form is the variable arguments (or "varargs")
     * form. If the FunctionObject will be used as a constructor,
     * the member must have the following parameters
     * <pre>
     *      (Context cx, Object[] args, Function ctorObj,
     *       boolean inNewExpr)</pre>
     * and if it is a Method, be static and return an Object result.<p>
     *
     * Otherwise, if the FunctionObject will <i>not</i> be used to define a
     * constructor, the member must be a static Method with parameters
     * <pre>
     *      (Context cx, Scriptable thisObj, Object[] args,
     *       Function funObj) </pre>
     * and an Object result.<p>
     *
     * When the function varargs form is called as part of a function call,
     * the <code>args</code> parameter contains the
     * arguments, with <code>thisObj</code>
     * set to the JavaScript 'this' value. <code>funObj</code>
     * is the function object for the invoked function.<p>
     *
     * When the constructor varargs form is called or invoked while evaluating
     * a <code>new</code> expression, <code>args</code> contains the
     * arguments, <code>ctorObj</code> refers to this FunctionObject, and
     * <code>inNewExpr</code> is true if and only if  a <code>new</code>
     * expression caused the call. This supports defining a function that
     * has different behavior when called as a constructor than when
     * invoked as a normal function call. (For example, the Boolean
     * constructor, when called as a function,
     * will convert to boolean rather than creating a new object.)<p>
     *
     * @param name the name of the function
     * @param methodOrConstructor a java.lang.reflect.Method or a java.lang.reflect.Constructor
     *                            that defines the object
     * @param scope enclosing scope of function
     * @see org.mozilla.javascript.Scriptable
     */
    public FunctionObject(String name, Member methodOrConstructor,
                          Scriptable scope)
    {
        if (methodOrConstructor instanceof Constructor) {
            member = new MemberBox((Constructor<?>) methodOrConstructor);
            isStatic = true; // well, doesn't take a 'this'
        } else {
            member = new MemberBox((Method) methodOrConstructor);
            isStatic = member.isStatic();
        }
        String methodName = member.getName();
        this.functionName = name;
        Class<?>[] types = member.argTypes;
        int arity = types.length;
        if (arity == 4 && (types[1].isArray() || types[2].isArray())) {
            // Either variable args or an error.
            if (types[1].isArray()) {
                if (!isStatic ||
                    types[0] != ScriptRuntime.ContextClass ||
                    types[1].getComponentType() != ScriptRuntime.ObjectClass ||
                    types[2] != ScriptRuntime.FunctionClass ||
                    types[3] != Boolean.TYPE)
                {
                    throw Context.reportRuntimeError1(
                        "msg.varargs.ctor", methodName);
                }
                parmsLength = VARARGS_CTOR;
            } else {
                if (!isStatic ||
                    types[0] != ScriptRuntime.ContextClass ||
                    types[1] != ScriptRuntime.ScriptableClass ||
                    types[2].getComponentType() != ScriptRuntime.ObjectClass ||
                    types[3] != ScriptRuntime.FunctionClass)
                {
                    throw Context.reportRuntimeError1(
                        "msg.varargs.fun", methodName);
                }
                parmsLength = VARARGS_METHOD;
            }
        } else {
            parmsLength = arity;
            if (arity > 0) {
                typeTags = new byte[arity];
                for (int i = 0; i != arity; ++i) {
                    int tag = getTypeTag(types[i]);
                    if (tag == JAVA_UNSUPPORTED_TYPE) {
                        throw Context.reportRuntimeError2(
                            "msg.bad.parms", types[i].getName(), methodName);
                    }
                    typeTags[i] = (byte)tag;
                }
            }
        }

        if (member.isMethod()) {
            Method method = member.method();
            Class<?> returnType = method.getReturnType();
            if (returnType == Void.TYPE) {
                hasVoidReturn = true;
            } else {
                returnTypeTag = getTypeTag(returnType);
            }
        } else {
            Class<?> ctorType = member.getDeclaringClass();
            if (!ScriptRuntime.ScriptableClass.isAssignableFrom(ctorType)) {
                throw Context.reportRuntimeError1(
                    "msg.bad.ctor.return", ctorType.getName());
            }
        }

        ScriptRuntime.setFunctionProtoAndParent(this, scope);
    }

    /**
     * @return One of <code>JAVA_*_TYPE</code> constants to indicate desired type
     *         or {@link #JAVA_UNSUPPORTED_TYPE} if the convertion is not
     *         possible
     */
    public static int getTypeTag(Class<?> type)
    {
        if (type == ScriptRuntime.StringClass)
            return JAVA_STRING_TYPE;
        if (type == ScriptRuntime.IntegerClass || type == Integer.TYPE)
            return JAVA_INT_TYPE;
        if (type == ScriptRuntime.BooleanClass || type == Boolean.TYPE)
            return JAVA_BOOLEAN_TYPE;
        if (type == ScriptRuntime.DoubleClass || type == Double.TYPE)
            return JAVA_DOUBLE_TYPE;
        if (ScriptRuntime.ScriptableClass.isAssignableFrom(type))
            return JAVA_SCRIPTABLE_TYPE;
        if (type == ScriptRuntime.ObjectClass)
            return JAVA_OBJECT_TYPE;

        // Note that the long type is not supported; see the javadoc for
        // the constructor for this class

        return JAVA_UNSUPPORTED_TYPE;
    }

    public static Object convertArg(Context cx, Scriptable scope,
                                    Object arg, int typeTag)
    {
        switch (typeTag) {
          case JAVA_STRING_TYPE:
              if (arg instanceof String)
                return arg;
            return ScriptRuntime.toString(arg);
          case JAVA_INT_TYPE:
              if (arg instanceof Integer)
                return arg;
            return Integer.valueOf(ScriptRuntime.toInt32(arg));
          case JAVA_BOOLEAN_TYPE:
              if (arg instanceof Boolean)
                return arg;
            return ScriptRuntime.toBoolean(arg) ? Boolean.TRUE
                                                : Boolean.FALSE;
          case JAVA_DOUBLE_TYPE:
            if (arg instanceof Double)
                return arg;
            return Double.valueOf(ScriptRuntime.toNumber(arg));
          case JAVA_SCRIPTABLE_TYPE:
              return ScriptRuntime.toObjectOrNull(cx, arg, scope);
          case JAVA_OBJECT_TYPE:
            return arg;
          default:
            throw new IllegalArgumentException();
        }
    }

    /**
     * Return the value defined by  the method used to construct the object
     * (number of parameters of the method, or 1 if the method is a "varargs"
     * form).
     */
    @Override
    public int getArity() {
        return parmsLength < 0 ? 1 : parmsLength;
    }

    /**
     * Return the same value as {@link #getArity()}.
     */
    @Override
    public int getLength() {
        return getArity();
    }

    @Override
    public String getFunctionName()
    {
        return (functionName == null) ? "" : functionName;
    }

    /**
     * Get Java method or constructor this function represent.
     */
    public Member getMethodOrConstructor()
    {
        if (member.isMethod()) {
            return member.method();
        } else {
            return member.ctor();
        }
    }

    static Method findSingleMethod(Method[] methods, String name)
    {
        Method found = null;
        for (int i = 0, N = methods.length; i != N; ++i) {
            Method method = methods[i];
            if (method != null && name.equals(method.getName())) {
                if (found != null) {
                    throw Context.reportRuntimeError2(
                        "msg.no.overload", name,
                        method.getDeclaringClass().getName());
                }
                found = method;
            }
        }
        return found;
    }

    /**
     * Returns all public methods declared by the specified class. This excludes
     * inherited methods.
     *
     * @param clazz the class from which to pull public declared methods
     * @return the public methods declared in the specified class
     * @see Class#getDeclaredMethods()
     */
    static Method[] getMethodList(Class<?> clazz) {
        Method[] methods = null;
        try {
            // getDeclaredMethods may be rejected by the security manager
            // but getMethods is more expensive
            if (!sawSecurityException)
                methods = clazz.getDeclaredMethods();
        } catch (SecurityException e) {
            // If we get an exception once, give up on getDeclaredMethods
            sawSecurityException = true;
        }
        if (methods == null) {
            methods = clazz.getMethods();
        }
        int count = 0;
        for (int i=0; i < methods.length; i++) {
            if (sawSecurityException
                ? methods[i].getDeclaringClass() != clazz
                : !Modifier.isPublic(methods[i].getModifiers()))
            {
                methods[i] = null;
            } else {
                count++;
            }
        }
        Method[] result = new Method[count];
        int j=0;
        for (int i=0; i < methods.length; i++) {
            if (methods[i] != null)
                result[j++] = methods[i];
        }
        return result;
    }

    /**
     * Define this function as a JavaScript constructor.
     * <p>
     * Sets up the "prototype" and "constructor" properties. Also
     * calls setParent and setPrototype with appropriate values.
     * Then adds the function object as a property of the given scope, using
     *      <code>prototype.getClassName()</code>
     * as the name of the property.
     *
     * @param scope the scope in which to define the constructor (typically
     *              the global object)
     * @param prototype the prototype object
     * @see org.mozilla.javascript.Scriptable#setParentScope
     * @see org.mozilla.javascript.Scriptable#setPrototype
     * @see org.mozilla.javascript.Scriptable#getClassName
     */
    public void addAsConstructor(Scriptable scope, Scriptable prototype)
    {
        initAsConstructor(scope, prototype);
        defineProperty(scope, prototype.getClassName(),
                       this, ScriptableObject.DONTENUM);
    }

    void initAsConstructor(Scriptable scope, Scriptable prototype)
    {
        ScriptRuntime.setFunctionProtoAndParent(this, scope);
        setImmunePrototypeProperty(prototype);

        prototype.setParentScope(this);

        defineProperty(prototype, "constructor", this,
                       ScriptableObject.DONTENUM  |
                       ScriptableObject.PERMANENT |
                       ScriptableObject.READONLY);
        setParentScope(scope);
    }

    /**
     * @deprecated Use {@link #getTypeTag(Class)}
     * and {@link #convertArg(Context, Scriptable, Object, int)}
     * for type conversion.
     */
    @Deprecated
    public static Object convertArg(Context cx, Scriptable scope,
                                    Object arg, Class<?> desired)
    {
        int tag = getTypeTag(desired);
        if (tag == JAVA_UNSUPPORTED_TYPE) {
            throw Context.reportRuntimeError1
                ("msg.cant.convert", desired.getName());
        }
        return convertArg(cx, scope, arg, tag);
    }

    /**
     * Performs conversions on argument types if needed and
     * invokes the underlying Java method or constructor.
     * <p>
     * Implements Function.call.
     *
     * @see org.mozilla.javascript.Function#call(
     *          Context, Scriptable, Scriptable, Object[])
     */
    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj,
                       Object[] args)
    {
        Object result;
        boolean checkMethodResult = false;
        int argsLength = args.length;

        for (int i = 0; i < argsLength; i++) {
            // flatten cons-strings before passing them as arguments
            if (args[i] instanceof ConsString) {
                args[i] = args[i].toString();
            }
        }

        if (parmsLength < 0) {
            if (parmsLength == VARARGS_METHOD) {
                Object[] invokeArgs = { cx, thisObj, args, this };
                result = member.invoke(null, invokeArgs);
                checkMethodResult = true;
            } else {
                boolean inNewExpr = (thisObj == null);
                Boolean b = inNewExpr ? Boolean.TRUE : Boolean.FALSE;
                Object[] invokeArgs = { cx, args, this, b };
                result = (member.isCtor())
                         ? member.newInstance(invokeArgs)
                         : member.invoke(null, invokeArgs);
            }

        } else {
            if (!isStatic) {
                Class<?> clazz = member.getDeclaringClass();
                if (!clazz.isInstance(thisObj)) {
                    boolean compatible = false;
                    if (thisObj == scope) {
                        Scriptable parentScope = getParentScope();
                        if (scope != parentScope) {
                            // Call with dynamic scope for standalone function,
                            // use parentScope as thisObj
                            compatible = clazz.isInstance(parentScope);
                            if (compatible) {
                                thisObj = parentScope;
                            }
                        }
                    }
                    if (!compatible) {
                        // Couldn't find an object to call this on.
                        throw ScriptRuntime.typeError1("msg.incompat.call",
                                                       functionName);
                    }
                }
            }

            Object[] invokeArgs;
            if (parmsLength == argsLength) {
                // Do not allocate new argument array if java arguments are
                // the same as the original js ones.
                invokeArgs = args;
                for (int i = 0; i != parmsLength; ++i) {
                    Object arg = args[i];
                    Object converted = convertArg(cx, scope, arg, typeTags[i]);
                    if (arg != converted) {
                        if (invokeArgs == args) {
                            invokeArgs = args.clone();
                        }
                        invokeArgs[i] = converted;
                    }
                }
            } else if (parmsLength == 0) {
                invokeArgs = ScriptRuntime.emptyArgs;
            } else {
                invokeArgs = new Object[parmsLength];
                for (int i = 0; i != parmsLength; ++i) {
                    Object arg = (i < argsLength)
                                 ? args[i]
                                 : Undefined.instance;
                    invokeArgs[i] = convertArg(cx, scope, arg, typeTags[i]);
                }
            }

            if (member.isMethod()) {
                result = member.invoke(thisObj, invokeArgs);
                checkMethodResult = true;
            } else {
                result = member.newInstance(invokeArgs);
            }

        }

        if (checkMethodResult) {
            if (hasVoidReturn) {
                result = Undefined.instance;
            } else if (returnTypeTag == JAVA_UNSUPPORTED_TYPE) {
                result = cx.getWrapFactory().wrap(cx, scope, result, null);
            }
            // XXX: the code assumes that if returnTypeTag == JAVA_OBJECT_TYPE
            // then the Java method did a proper job of converting the
            // result to JS primitive or Scriptable to avoid
            // potentially costly Context.javaToJS call.
        }

        return result;
    }

    /**
     * Return new {@link Scriptable} instance using the default
     * constructor for the class of the underlying Java method.
     * Return null to indicate that the call method should be used to create
     * new objects.
     */
    @Override
    public Scriptable createObject(Context cx, Scriptable scope) {
        if (member.isCtor() || parmsLength == VARARGS_CTOR) {
            return null;
        }
        Scriptable result;
        try {
            result = (Scriptable) member.getDeclaringClass().newInstance();
        } catch (Exception ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        }

        result.setPrototype(getClassPrototype());
        result.setParentScope(getParentScope());
        return result;
    }

    boolean isVarArgsMethod() {
        return parmsLength == VARARGS_METHOD;
    }

    boolean isVarArgsConstructor() {
        return parmsLength == VARARGS_CTOR;
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        if (parmsLength > 0) {
            Class<?>[] types = member.argTypes;
            typeTags = new byte[parmsLength];
            for (int i = 0; i != parmsLength; ++i) {
                typeTags[i] = (byte)getTypeTag(types[i]);
            }
        }
        if (member.isMethod()) {
            Method method = member.method();
            Class<?> returnType = method.getReturnType();
            if (returnType == Void.TYPE) {
                hasVoidReturn = true;
            } else {
                returnTypeTag = getTypeTag(returnType);
            }
        }
    }

    private static final short VARARGS_METHOD = -1;
    private static final short VARARGS_CTOR =   -2;

    private static boolean sawSecurityException;

    public static final int JAVA_UNSUPPORTED_TYPE = 0;
    public static final int JAVA_STRING_TYPE      = 1;
    public static final int JAVA_INT_TYPE         = 2;
    public static final int JAVA_BOOLEAN_TYPE     = 3;
    public static final int JAVA_DOUBLE_TYPE      = 4;
    public static final int JAVA_SCRIPTABLE_TYPE  = 5;
    public static final int JAVA_OBJECT_TYPE      = 6;

    MemberBox member;
    private String functionName;
    private transient byte[] typeTags;
    private int parmsLength;
    private transient boolean hasVoidReturn;
    private transient int returnTypeTag;
    private boolean isStatic;
}
