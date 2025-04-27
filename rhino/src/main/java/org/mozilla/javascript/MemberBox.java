/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Wrapper class for Method and Constructor instances to cache getParameterTypes() results, recover
 * from IllegalAccessException in some cases and provide serialization support.
 *
 * @author Igor Bukanov
 */
final class MemberBox implements Serializable {
    private static final long serialVersionUID = 6358550398665688245L;

    private transient Member memberObject;
    transient Class<?>[] argTypes;
    transient boolean[] argNullability;
    transient boolean vararg;

    transient Function asGetterFunction;
    transient Function asSetterFunction;
    transient Object delegateTo;

    private static final NullabilityDetector nullDetector =
            ScriptRuntime.loadOneServiceImplementation(NullabilityDetector.class);

    MemberBox(Method method) {
        init(method);
    }

    MemberBox(Constructor<?> constructor) {
        init(constructor);
    }

    private void init(Method method) {
        this.memberObject = method;
        this.argTypes = method.getParameterTypes();
        this.argNullability =
                nullDetector == null
                        ? new boolean[method.getParameters().length]
                        : nullDetector.getParameterNullability(method);
        this.vararg = method.isVarArgs();
    }

    private void init(Constructor<?> constructor) {
        this.memberObject = constructor;
        this.argTypes = constructor.getParameterTypes();
        this.argNullability =
                nullDetector == null
                        ? new boolean[constructor.getParameters().length]
                        : nullDetector.getParameterNullability(constructor);
        this.vararg = constructor.isVarArgs();
    }

    Method method() {
        return (Method) memberObject;
    }

    Constructor<?> ctor() {
        return (Constructor<?>) memberObject;
    }

    Member member() {
        return memberObject;
    }

    boolean isMethod() {
        return memberObject instanceof Method;
    }

    boolean isCtor() {
        return memberObject instanceof Constructor;
    }

    boolean isStatic() {
        return Modifier.isStatic(memberObject.getModifiers());
    }

    boolean isPublic() {
        return Modifier.isPublic(memberObject.getModifiers());
    }

    String getName() {
        return memberObject.getName();
    }

    Class<?> getDeclaringClass() {
        return memberObject.getDeclaringClass();
    }

    String toJavaDeclaration() {
        StringBuilder sb = new StringBuilder();
        if (isMethod()) {
            Method method = method();
            sb.append(method.getReturnType());
            sb.append(' ');
            sb.append(method.getName());
        } else {
            Constructor<?> ctor = ctor();
            String name = ctor.getDeclaringClass().getName();
            int lastDot = name.lastIndexOf('.');
            if (lastDot >= 0) {
                name = name.substring(lastDot + 1);
            }
            sb.append(name);
        }
        sb.append(JavaMembers.liveConnectSignature(argTypes));
        return sb.toString();
    }

    @Override
    public String toString() {
        return memberObject.toString();
    }

    boolean isSameGetterFunction(Object function) {
        var f = asGetterFunction == null ? Undefined.instance : asGetterFunction;
        return ScriptRuntime.shallowEq(function, f);
    }

    boolean isSameSetterFunction(Object function) {
        var f = asSetterFunction == null ? Undefined.instance : asSetterFunction;
        return ScriptRuntime.shallowEq(function, f);
    }

    /** Function returned by calls to __lookupGetter__ */
    Function asGetterFunction(final String name, final Scriptable scope) {
        // Note: scope is the scriptable this function is related to; therefore this function
        // is constant for this member box.
        // Because of this we can cache the function in the attribute
        if (asGetterFunction == null) {
            asGetterFunction =
                    new BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
                        @Override
                        public Object call(
                                Context cx,
                                Scriptable callScope,
                                Scriptable thisObj,
                                Object[] originalArgs) {
                            MemberBox nativeGetter = MemberBox.this;
                            Object getterThis;
                            Object[] args;
                            if (nativeGetter.delegateTo == null) {
                                getterThis = thisObj;
                                args = ScriptRuntime.emptyArgs;
                            } else {
                                getterThis = nativeGetter.delegateTo;
                                args = new Object[] {thisObj};
                            }
                            return nativeGetter.invoke(getterThis, args);
                        }

                        @Override
                        public String getFunctionName() {
                            return name;
                        }
                    };
        }
        return asGetterFunction;
    }

    /** Function returned by calls to __lookupSetter__ */
    Function asSetterFunction(final String name, final Scriptable scope) {
        // Note: scope is the scriptable this function is related to; therefore this function
        // is constant for this member box.
        // Because of this we can cache the function in the attribute
        if (asSetterFunction == null) {
            asSetterFunction =
                    new BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
                        @Override
                        public Object call(
                                Context cx,
                                Scriptable callScope,
                                Scriptable thisObj,
                                Object[] originalArgs) {
                            MemberBox nativeSetter = MemberBox.this;
                            Object setterThis;
                            Object[] args;
                            Object value =
                                    originalArgs.length > 0
                                            ? FunctionObject.convertArg(
                                                    cx,
                                                    thisObj,
                                                    originalArgs[0],
                                                    FunctionObject.getTypeTag(
                                                            nativeSetter.argTypes[0]),
                                                    nativeSetter.argNullability[0])
                                            : Undefined.instance;
                            if (nativeSetter.delegateTo == null) {
                                setterThis = thisObj;
                                args = new Object[] {value};
                            } else {
                                setterThis = nativeSetter.delegateTo;
                                args = new Object[] {thisObj, value};
                            }
                            return nativeSetter.invoke(setterThis, args);
                        }

                        @Override
                        public String getFunctionName() {
                            return name;
                        }
                    };
        }
        return asSetterFunction;
    }

    Object invoke(Object target, Object[] args) {
        Method method = method();

        // handle delegators
        if (target instanceof Delegator) {
            target = ((Delegator) target).getDelegee();
        }
        if (args != null) {
            for (int i = 0; i < args.length; ++i) {
                if (args[i] instanceof Delegator) {
                    args[i] = ((Delegator) args[i]).getDelegee();
                }
            }
        }

        try {
            try {
                return method.invoke(target, args);
            } catch (IllegalAccessException ex) {
                Method accessible = searchAccessibleMethod(method, argTypes);
                if (accessible != null) {
                    memberObject = accessible;
                    method = accessible;
                } else {
                    if (!VMBridge.instance.tryToMakeAccessible(method)) {
                        throw Context.throwAsScriptRuntimeEx(ex);
                    }
                }
                // Retry after recovery
                return method.invoke(target, args);
            }
        } catch (InvocationTargetException ite) {
            // Must allow ContinuationPending exceptions to propagate unhindered
            Throwable e = ite;
            do {
                e = ((InvocationTargetException) e).getTargetException();
            } while ((e instanceof InvocationTargetException));
            if (e instanceof ContinuationPending) throw (ContinuationPending) e;
            throw Context.throwAsScriptRuntimeEx(e);
        } catch (Exception ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        }
    }

    Object newInstance(Object[] args) {
        Constructor<?> ctor = ctor();
        try {
            try {
                return ctor.newInstance(args);
            } catch (IllegalAccessException ex) {
                if (!VMBridge.instance.tryToMakeAccessible(ctor)) {
                    throw Context.throwAsScriptRuntimeEx(ex);
                }
            }
            return ctor.newInstance(args);
        } catch (Exception ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        }
    }

    private static Method searchAccessibleMethod(Method method, Class<?>[] params) {
        int modifiers = method.getModifiers();
        if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
            Class<?> c = method.getDeclaringClass();
            if (!Modifier.isPublic(c.getModifiers())) {
                String name = method.getName();
                Class<?>[] intfs = c.getInterfaces();
                for (int i = 0, N = intfs.length; i != N; ++i) {
                    Class<?> intf = intfs[i];
                    if (Modifier.isPublic(intf.getModifiers())) {
                        try {
                            return intf.getMethod(name, params);
                        } catch (NoSuchMethodException ex) {
                        } catch (SecurityException ex) {
                        }
                    }
                }
                for (; ; ) {
                    c = c.getSuperclass();
                    if (c == null) {
                        break;
                    }
                    if (Modifier.isPublic(c.getModifiers())) {
                        try {
                            Method m = c.getMethod(name, params);
                            int mModifiers = m.getModifiers();
                            if (Modifier.isPublic(mModifiers) && !Modifier.isStatic(mModifiers)) {
                                return m;
                            }
                        } catch (NoSuchMethodException ex) {
                        } catch (SecurityException ex) {
                        }
                    }
                }
            }
        }
        return null;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Member member = readMember(in);
        if (member instanceof Method) {
            init((Method) member);
        } else {
            init((Constructor<?>) member);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        writeMember(out, memberObject);
    }

    /**
     * Writes a Constructor or Method object.
     *
     * <p>Methods and Constructors are not serializable, so we must serialize information about the
     * class, the name, and the parameters and recreate upon deserialization.
     */
    private static void writeMember(ObjectOutputStream out, Member member) throws IOException {
        if (member == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        if (!(member instanceof Method || member instanceof Constructor))
            throw new IllegalArgumentException("not Method or Constructor");
        out.writeBoolean(member instanceof Method);
        out.writeObject(member.getName());
        out.writeObject(member.getDeclaringClass());
        if (member instanceof Method) {
            writeParameters(out, ((Method) member).getParameterTypes());
        } else {
            writeParameters(out, ((Constructor<?>) member).getParameterTypes());
        }
    }

    /** Reads a Method or a Constructor from the stream. */
    private static Member readMember(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        if (!in.readBoolean()) return null;
        boolean isMethod = in.readBoolean();
        String name = (String) in.readObject();
        Class<?> declaring = (Class<?>) in.readObject();
        Class<?>[] parms = readParameters(in);
        try {
            if (isMethod) {
                return declaring.getMethod(name, parms);
            }
            return declaring.getConstructor(parms);
        } catch (NoSuchMethodException e) {
            throw new IOException("Cannot find member: " + e);
        }
    }

    private static final Class<?>[] primitives = {
        Boolean.TYPE,
        Byte.TYPE,
        Character.TYPE,
        Double.TYPE,
        Float.TYPE,
        Integer.TYPE,
        Long.TYPE,
        Short.TYPE,
        Void.TYPE
    };

    /**
     * Writes an array of parameter types to the stream.
     *
     * <p>Requires special handling because primitive types cannot be found upon deserialization by
     * the default Java implementation.
     */
    private static void writeParameters(ObjectOutputStream out, Class<?>[] parms)
            throws IOException {
        out.writeShort(parms.length);
        outer:
        for (Class<?> parm : parms) {
            boolean primitive = parm.isPrimitive();
            out.writeBoolean(primitive);
            if (!primitive) {
                out.writeObject(parm);
                continue;
            }
            for (int j = 0; j < primitives.length; j++) {
                if (parm.equals(primitives[j])) {
                    out.writeByte(j);
                    continue outer;
                }
            }
            throw new IllegalArgumentException("Primitive " + parm + " not found");
        }
    }

    /** Reads an array of parameter types from the stream. */
    private static Class<?>[] readParameters(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        Class<?>[] result = new Class[in.readShort()];
        for (int i = 0; i < result.length; i++) {
            if (!in.readBoolean()) {
                result[i] = (Class<?>) in.readObject();
                continue;
            }
            result[i] = primitives[in.readByte()];
        }
        return result;
    }
}
