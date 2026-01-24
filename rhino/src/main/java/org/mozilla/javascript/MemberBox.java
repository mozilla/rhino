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
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
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
    private static final long serialVersionUID = 8260700214130563887L;

    private transient Member memberObject;
    private transient Class<?>[] argTypes;
    private transient Class<?> returnType;
    private transient NullabilityDetector.NullabilityAccessor argNullability;

    transient Function asGetterFunction;
    transient Function asSetterFunction;
    transient Object delegateTo;

    private final Scriptable scope;

    private static final NullabilityDetector nullDetector =
            ScriptRuntime.loadOneServiceImplementation(NullabilityDetector.class);

    MemberBox(Scriptable scope, Method method) {
        this.scope = scope;
        init(method);
    }

    MemberBox(Scriptable scope, Constructor<?> constructor) {
        this.scope = scope;
        init(constructor);
    }

    private void init(Method method) {
        this.memberObject = method;
        this.argTypes = method.getParameterTypes();
        this.returnType = method.getReturnType();
    }

    private void init(Constructor<?> constructor) {
        this.memberObject = constructor;
        this.argTypes = constructor.getParameterTypes();
        this.returnType = null;
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

    Class<?>[] getArgTypes() {
        return argTypes;
    }

    public NullabilityDetector.NullabilityAccessor getArgNullability() {
        var got = this.argNullability;
        if (got == null) {
            // synchronization is optional, because `getParameterNullability(...)` will always
            // give `NullabilityAccessor` with same behavior for the same method/constructor
            if (nullDetector == null) {
                got = NullabilityDetector.NullabilityAccessor.FALSE;
            } else if (this.isMethod()) {
                got = nullDetector.getParameterNullability(this.method());
            } else {
                got = nullDetector.getParameterNullability(this.ctor());
            }
            this.argNullability = got;
        }
        return got;
    }

    Class<?> getReturnType() {
        return returnType;
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
    Function asGetterFunction(final String name) {
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
                            if (nativeGetter.delegateTo == null) {
                                return nativeGetter.invoke(thisObj, ScriptRuntime.emptyArgs);
                            } else {
                                return nativeGetter.invoke(
                                        nativeGetter.delegateTo, new Object[] {thisObj});
                            }
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
    Function asSetterFunction(final String name) {
        // Note: scope is the scriptable this function is related to; therefore this function
        // is constant for this member box.
        // Because of this we can cache the function in the attribute
        if (asSetterFunction == null) {
            var setterTypeTag = FunctionObject.getTypeTag(this.argTypes[0]);
            asSetterFunction =
                    new BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
                        @Override
                        public Object call(
                                Context cx,
                                Scriptable callScope,
                                Scriptable thisObj,
                                Object[] originalArgs) {
                            MemberBox nativeSetter = MemberBox.this;
                            Object value =
                                    originalArgs.length > 0
                                            ? FunctionObject.convertArg(
                                                    cx,
                                                    thisObj,
                                                    originalArgs[0],
                                                    setterTypeTag,
                                                    nativeSetter.getArgNullability().isNullable(0))
                                            : Undefined.instance;
                            if (nativeSetter.delegateTo == null) {
                                return nativeSetter.invoke(thisObj, new Object[] {value});
                            } else {
                                return nativeSetter.invoke(
                                        nativeSetter.delegateTo, new Object[] {thisObj, value});
                            }
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
                Method accessible = searchAccessibleMethod(method, getArgTypes());
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
                // Retry after recovery
                return ctor.newInstance(args);
            }
        } catch (Exception ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        }
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
            // public instance method being inaccessible
            // If the declaring is also not accessible (for example, not public), we can try to look
            // for an accessible method in superclass/interfaces, that the method overrides, and is
            // declared by an accessible class

            Class<?> c = method.getDeclaringClass();
            if (Modifier.isPublic(c.getModifiers())) {
                // declaring class is accessible, nothing we can do for now
                return null;
            }

            String name = method.getName();

            // search in interfaces
            for (Class<?> intf : c.getInterfaces()) {
                if (Modifier.isPublic(intf.getModifiers())) {
                    try {
                        return intf.getMethod(name, params);
                    } catch (NoSuchMethodException | SecurityException ignored) {
                    }
                }
            }

            // search in superclasses
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
                    } catch (NoSuchMethodException | SecurityException ignored) {
                    }
                }
            }
        }
        return null;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        boolean isMethod = in.readBoolean();
        String name = (String) in.readObject();
        Class<?> declaring = (Class<?>) in.readObject();
        Class<?>[] parms =
                MethodType.fromMethodDescriptorString(
                                (String) in.readObject(), MemberBox.class.getClassLoader())
                        .parameterArray();

        try {
            if (isMethod) {
                var member = declaring.getMethod(name, parms);
                init(member);
            } else {
                var member = declaring.getConstructor(parms);
                init(member);
            }
        } catch (NoSuchMethodException e) {
            throw new IOException("Cannot find member: " + e);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeBoolean(memberObject instanceof Method);
        out.writeObject(memberObject.getName());
        out.writeObject(memberObject.getDeclaringClass());

        // we only care about parameter types, so return type is always void
        out.writeObject(MethodType.methodType(void.class, argTypes).toMethodDescriptorString());
    }
}
