/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/** Version of {@link JavaMembers} for modular JDKs. */
class JavaMembers_jdk11 extends JavaMembers {

    JavaMembers_jdk11(Scriptable scope, Class<?> cl, boolean includeProtected) {
        super(scope, cl, includeProtected);
    }

    @Override
    void discoverPublicMethods(Class<?> clazz, Map<MethodSignature, Method> map) {
        if (isExportedClass(clazz)) {
            super.discoverPublicMethods(clazz, map);
        } else {
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                method = findAccessibleMethod(method);
                registerMethod(map, method);
            }
        }
    }

    private static boolean isExportedClass(Class<?> clazz) {
        /*
         * We are going to invoke, using reflection, the approximate equivalent
         * to the following Java 9 code:
         *
         * return clazz.getModule().isExported(clazz.getPackageName());
         */

        // First, determine the package name
        String pname;
        Package pkg = clazz.getPackage();
        if (pkg == null) {
            // Primitive types, arrays, proxy classes
            if (!Proxy.isProxyClass(clazz)) {
                // Should be a primitive type or array
                return true;
            }
            String clName = clazz.getName();
            pname = clName.substring(0, clName.lastIndexOf('.'));
        } else {
            pname = pkg.getName();
        }

        // Obtain the module
        Method getmodule;
        @SuppressWarnings("GetClassOnClass")
        Class<?> cl = clazz.getClass();
        try {
            getmodule = cl.getMethod("getModule");
        } catch (NoSuchMethodException e) {
            // We are on non-modular Java
            return true;
        }

        Object module;
        try {
            module = getmodule.invoke(clazz);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            return false;
        }

        Class<?> moduleClass = module.getClass();

        // Execute isExported()
        boolean exported;
        try {
            Method isexported = moduleClass.getMethod("isExported", String.class);
            exported = (boolean) isexported.invoke(module, pname);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            // If we reached this part of the code, this cannot happen
            exported = false;
        }
        return exported;
    }

    private static Method findAccessibleMethod(Method method) {
        Class<?> cl = method.getDeclaringClass();
        final String methodName = method.getName();
        final Class<?>[] methodTypes = method.getParameterTypes();
        topLoop:
        do {
            for (Class<?> intface : cl.getInterfaces()) {
                try {
                    method = intface.getMethod(methodName, methodTypes);
                    break topLoop;
                } catch (NoSuchMethodException e) {
                }
            }
            cl = cl.getSuperclass();
            if (cl == null) {
                break;
            }
            if (isExportedClass(cl)) {
                try {
                    method = cl.getMethod(methodName, methodTypes);
                    break;
                } catch (NoSuchMethodException e) {
                }
            }
        } while (true);
        return method;
    }
}
