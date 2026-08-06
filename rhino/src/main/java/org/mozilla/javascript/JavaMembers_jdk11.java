/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.reflect.Method;
import java.util.Map;
import org.mozilla.javascript.lc.ReflectUtils;

/** Version of {@link JavaMembers} for modular JDKs. */
class JavaMembers_jdk11 extends JavaMembers {

    JavaMembers_jdk11(VarScope scope, Class<?> cl, boolean includeProtected) {
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
        if (!ReflectUtils.IS_MODULAR_JAVA) {
            return true;
        }

        // `.getModule()` not present on Android, `.getPackageName()` not after API 31.
        // Android compatibility is ensured by gating it after IS_MODULAR_JAVA
        return clazz.getModule().isExported(clazz.getPackageName());
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
