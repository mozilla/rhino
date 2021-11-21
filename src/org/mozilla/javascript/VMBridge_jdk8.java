/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import org.mozilla.classfile.ClassFileWriter;

/** VMBridge for Java 8. */
public class VMBridge_jdk8 extends VMBridge {

    private static final ThreadLocal<Object[]> contextLocal = new ThreadLocal<Object[]>();

    @Override
    protected Object getThreadContextHelper() {
        // To make subsequent batch calls to getContext/setContext faster
        // associate permanently one element array with contextLocal
        // so getContext/setContext would need just to read/write the first
        // array element.
        // Note that it is necessary to use Object[], not Context[] to allow
        // garbage collection of Rhino classes. For details see comments
        // by Attila Szegedi in
        // https://bugzilla.mozilla.org/show_bug.cgi?id=281067#c5

        Object[] storage = contextLocal.get();
        if (storage == null) {
            storage = new Object[1];
            contextLocal.set(storage);
        }
        return storage;
    }

    @Override
    protected Context getContext(Object contextHelper) {
        Object[] storage = (Object[]) contextHelper;
        return (Context) storage[0];
    }

    @Override
    protected void setContext(Object contextHelper, Context cx) {
        Object[] storage = (Object[]) contextHelper;
        storage[0] = cx;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean tryToMakeAccessible(AccessibleObject accessible) {
        if (accessible.isAccessible()) {
            return true;
        }

        try {
            accessible.setAccessible(true);
        } catch (RuntimeException ex) {
        }

        return accessible.isAccessible();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Method getAccessibleMethod(Object target, Method method) {
        if (method.isAccessible()) {
            return method;
        }

        try {
            method.setAccessible(true);
        } catch (RuntimeException ex) {
            method = null;
        }

        return method;
    }

    @Override
    protected void generateOverride(
            ClassFileWriter cfw,
            String adapterName,
            String superName,
            Method method,
            ObjToIntMap generatedOverrides,
            ObjToIntMap generatedMethods,
            ObjToIntMap functionNames) {
        int mods = method.getModifiers();
        // if a method is marked abstract, must implement it or the
        // resulting class won't be instantiable. otherwise, if the object
        // has a property of the same name, then an override is intended.
        boolean isAbstractMethod = Modifier.isAbstract(mods);
        String methodName = method.getName();
        if (isAbstractMethod || functionNames.has(methodName)) {
            // make sure to generate only one instance of a particular
            // method/signature.
            Class<?>[] argTypes = method.getParameterTypes();
            String methodSignature = JavaAdapter.getMethodSignature(method, argTypes);
            String methodKey = methodName + methodSignature;
            if (!generatedOverrides.has(methodKey)) {
                JavaAdapter.generateMethod(
                        cfw, adapterName, methodName, argTypes, method.getReturnType(), true);
                generatedOverrides.put(methodKey, 0);
                generatedMethods.put(methodName, 0);

                // if a method was overridden, generate a "super$method"
                // which lets the delegate call the superclass' version.
                if (!isAbstractMethod) {
                    JavaAdapter.generateSuper(
                            cfw,
                            adapterName,
                            superName,
                            methodName,
                            methodSignature,
                            argTypes,
                            method.getReturnType());
                }
            }
        }
    }

    @Override
    protected Object getInterfaceProxyHelper(ContextFactory cf, Class<?>[] interfaces) {
        // XXX: How to handle interfaces array withclasses from different
        // class loaders? Using cf.getApplicationClassLoader() ?
        ClassLoader loader = interfaces[0].getClassLoader();
        @SuppressWarnings("deprecation")
        Class<?> cl = Proxy.getProxyClass(loader, interfaces);
        Constructor<?> c;
        try {
            c = cl.getConstructor(new Class[] {InvocationHandler.class});
        } catch (SecurityException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        } catch (NoSuchMethodException ex) {
            // Should not happen
            throw new IllegalStateException(ex);
        }
        return c;
    }

    @Override
    protected Object newInterfaceProxy(
            Object proxyHelper,
            final ContextFactory cf,
            final InterfaceAdapter adapter,
            final Object target,
            final Scriptable topScope) {
        Constructor<?> c = (Constructor<?>) proxyHelper;

        InvocationHandler handler =
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        // In addition to methods declared in the interface, proxies
                        // also route some java.lang.Object methods through the
                        // invocation handler.
                        if (method.getDeclaringClass() == Object.class) {
                            String methodName = method.getName();
                            if (methodName.equals("equals")) {
                                Object other = args[0];
                                // Note: we could compare a proxy and its wrapped function
                                // as equal here but that would break symmetry of equal().
                                // The reason == suffices here is that proxies are cached
                                // in ScriptableObject (see NativeJavaObject.coerceType())
                                return Boolean.valueOf(proxy == other);
                            }
                            if (methodName.equals("hashCode")) {
                                return Integer.valueOf(target.hashCode());
                            }
                            if (methodName.equals("toString")) {
                                return "Proxy[" + target.toString() + "]";
                            }
                        }
                        return adapter.invoke(cf, target, topScope, proxy, method, args);
                    }
                };
        Object proxy;
        try {
            proxy = c.newInstance(handler);
        } catch (InvocationTargetException ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        } catch (IllegalAccessException ex) {
            // Should not happen
            throw new IllegalStateException(ex);
        } catch (InstantiationException ex) {
            // Should not happen
            throw new IllegalStateException(ex);
        }
        return proxy;
    }
}
