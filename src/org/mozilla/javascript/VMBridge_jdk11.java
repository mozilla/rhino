/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * VMBridge for Java 11 and later.
 *
 * <p>The reason that Java 11 is used instead of 9 is due to version 9 being no longer supported.
 * Java 11 is the lowest supported modular Java.
 */
public class VMBridge_jdk11 extends VMBridge_jdk8 {

    @Override
    protected boolean tryToMakeAccessible(AccessibleObject accessible) {
        try {
            accessible.setAccessible(true);
        } catch (RuntimeException ex) {
        }

        Method canAccess;
        try {
            canAccess = accessible.getClass().getMethod("canAccess");
        } catch (NoSuchMethodException | SecurityException e) {
            return false;
        }

        boolean can;
        try {
            can = (boolean) canAccess.invoke(this, accessible);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            can = false;
        }

        return can;
    }

    @Override
    protected Object getInterfaceProxyHelper(ContextFactory cf, Class<?>[] interfaces) {
        return interfaces;
    }

    @Override
    protected Object newInterfaceProxy(
            Object proxyHelper,
            final ContextFactory cf,
            final InterfaceAdapter adapter,
            final Object target,
            final Scriptable topScope) {
        Class<?>[] c = (Class<?>[]) proxyHelper;

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
            proxy = Proxy.newProxyInstance(handler.getClass().getClassLoader(), c, handler);
        } catch (Exception ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        }
        return proxy;
    }
}
