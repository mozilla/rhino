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
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.VariableTypeInfo;

/**
 * Wrapper class for Method and Constructor instances to cache getParameterTypes() results, recover
 * from IllegalAccessException in some cases and provide serialization support.
 *
 * @author Igor Bukanov
 */
final class MemberBox implements Serializable {
    private static final long serialVersionUID = 6358550398665688245L;

    private transient Member memberObject;
    private transient List<TypeInfo> argTypeInfos;
    private transient TypeInfo returnTypeInfo;
    private transient NullabilityDetector.NullabilityAccessor argNullability;
    transient boolean vararg;

    transient Function asGetterFunction;
    transient Function asSetterFunction;
    transient Object delegateTo;

    private static final NullabilityDetector nullDetector =
            ScriptRuntime.loadOneServiceImplementation(NullabilityDetector.class);

    MemberBox(Method method, TypeInfoFactory factory) {
        init(method, factory, method.getDeclaringClass());
    }

    MemberBox(Constructor<?> constructor, TypeInfoFactory factory) {
        init(constructor, factory);
    }

    MemberBox(Method method, TypeInfoFactory factory, Class<?> parent) {
        init(method, factory, parent);
    }

    private void init(Method method, TypeInfoFactory factory, Class<?> parent) {
        this.memberObject = method;
        if (nullDetector == null) {
            this.argNullability = NullabilityDetector.NullabilityAccessor.FALSE;
        }
        this.vararg = method.isVarArgs();
        this.argTypeInfos = factory.createList(method.getGenericParameterTypes());
        this.returnTypeInfo = factory.create(method.getGenericReturnType());

        var mapping = factory.getConsolidationMapping(parent);
        this.argTypeInfos = TypeInfoFactory.consolidateAll(this.argTypeInfos, mapping);
        this.returnTypeInfo = returnTypeInfo.consolidate(mapping);
    }

    private void init(Constructor<?> constructor, TypeInfoFactory factory) {
        this.memberObject = constructor;
        if (nullDetector == null) {
            this.argNullability = NullabilityDetector.NullabilityAccessor.FALSE;
        }
        this.vararg = constructor.isVarArgs();
        this.argTypeInfos = factory.createList(constructor.getGenericParameterTypes());
        this.returnTypeInfo = TypeInfo.NONE;

        // Type consolidation not required for constructor.
        //
        // consider this example:
        // class A<T> {
        //     A(T value) { ... }
        // }
        // class B extends A<String> {
        //     B(String value) { super(value); }
        // }
        // for class B, the constructor must have "String" instead of "T" as parameter type,
        // otherwise it won't compile. So param types are already concrete types.
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

    List<TypeInfo> getArgTypes() {
        return argTypeInfos;
    }

    public NullabilityDetector.NullabilityAccessor getArgNullability() {
        var got = this.argNullability;
        if (got == null) {
            // synchronization is optional, because `getParameterNullability(...)` will always
            // give `NullabilityAccessor` with same behaviour, which is because arg nullability
            // for a certain method/constructor will not change at runtime
            got =
                    this.isMethod()
                            ? nullDetector.getParameterNullability(this.method())
                            : nullDetector.getParameterNullability(this.ctor());
            this.argNullability = got;
        }
        return got;
    }

    TypeInfo getReturnType() {
        return returnTypeInfo;
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
        sb.append(JavaMembers.liveConnectSignature(getArgTypes()));
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
                                                    nativeSetter.getArgTypes().get(0).getTypeTag(),
                                                    nativeSetter.getArgNullability().isNullable(0))
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
                Method accessible =
                        searchAccessibleMethod(
                                method,
                                getArgTypes().stream()
                                        .map(TypeInfo::asClass)
                                        .toArray(Class[]::new));
                if (accessible != null) {
                    memberObject = accessible;
                    method = accessible;
                } else {
                    if (!tryToMakeAccessible(method)) {
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
                if (!tryToMakeAccessible(ctor)) {
                    throw Context.throwAsScriptRuntimeEx(ex);
                }
            }
            return ctor.newInstance(args);
        } catch (Exception ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        }
    }

    Object[] wrapArgsInternal(Object[] args, Map<VariableTypeInfo, TypeInfo> mapping) {
        var argTypes = getArgTypes();
        var argTypesLen = argTypes.size();
        var argLen = args.length;
        final var shouldConsolidate = !mapping.isEmpty();

        if (!this.vararg) {
            // fast path for getter
            if (argLen == 0) {
                return args;
            }

            var wrappedArgs = args;
            for (int i = 0; i < argLen; i++) {
                var arg = args[i];
                var argType = argTypes.get(i);
                if (shouldConsolidate) {
                    argType = argType.consolidate(mapping);
                }

                var coerced = Context.jsToJava(arg, argType);
                if (coerced != arg) {
                    if (wrappedArgs == args) {
                        wrappedArgs = args.clone();
                    }
                    wrappedArgs[i] = coerced;
                }
            }
            return wrappedArgs;
        }

        // marshall the explicit parameters
        var wrappedArgs = new Object[argTypesLen];
        for (int i = 0; i < argTypesLen - 1; i++) {
            var argType = argTypes.get(i);
            if (shouldConsolidate) {
                argType = argType.consolidate(mapping);
            }
            wrappedArgs[i] = Context.jsToJava(args[i], argType);
        }

        // Handle special situation where a single variable parameter
        // is given, and it is a Java or ECMA array or is null.
        if (argLen == argTypesLen) {
            var lastArg = args[argLen - 1];
            var lastArgType = argTypes.get(argTypesLen - 1);
            if (shouldConsolidate) {
                lastArgType = lastArgType.consolidate(mapping);
            }
            if (lastArg == null
                    || lastArg instanceof NativeArray
                    || lastArg instanceof NativeJavaArray) {
                // convert the ECMA array into a native array
                wrappedArgs[argLen - 1] = Context.jsToJava(lastArg, lastArgType);
                return wrappedArgs;
            }
        }

        // marshall the variable parameters
        var lastArgType = argTypes.get(argTypesLen - 1).getComponentType();
        if (shouldConsolidate) {
            lastArgType = lastArgType.consolidate(mapping);
        }
        var varArgs = lastArgType.newArray(argLen - argTypesLen + 1);
        for (int i = 0, arrayLen = Array.getLength(varArgs); i < arrayLen; i++) {
            Array.set(varArgs, i, Context.jsToJava(args[argTypesLen - 1 + i], lastArgType));
        }
        wrappedArgs[argTypesLen - 1] = varArgs;

        return wrappedArgs;
    }

    @SuppressWarnings("deprecation")
    private static boolean tryToMakeAccessible(AccessibleObject accessible) {
        if (!accessible.isAccessible()) {
            accessible.setAccessible(true);
        }
        return true;
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
                        } catch (NoSuchMethodException | SecurityException ex) {
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
                        } catch (NoSuchMethodException | SecurityException ex) {
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
            init((Method) member, TypeInfoFactory.GLOBAL, member.getDeclaringClass());
        } else {
            init((Constructor<?>) member, TypeInfoFactory.GLOBAL);
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
