/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.reflect.Type;

/**
 * Carries the reflection info for a {@link NativeJavaObject}.
 *
 * <p>This class holds as well the staticTypeInfo (left side) and the dynamicTypeInfo (right side)
 * for each java type.
 *
 * @author Roland Praml, FOCONIS AG
 */
public class JavaTypeResolver {

    private final JavaTypeInfo staticTypeInfo;
    private final JavaTypeInfo dynamicTypeInfo;

    public JavaTypeResolver(Scriptable scope, Type staticType, Type dynamicType) {
        staticTypeInfo = JavaTypeInfo.get(scope, staticType);
        dynamicTypeInfo = JavaTypeInfo.get(scope, dynamicType);
    }

    /**
     * Returns the 'best' candidate of the generic type for a given <code>classOfInterest</code>.
     * Check both (staticTypeInfo and dynamicTypeInfo) and returns the narrowest type.
     */
    public Class<?> resolve(Class<?> classOfInterest, int index) {
        Class<?> staticType = null;
        if (staticTypeInfo != null) {
            staticType = staticTypeInfo.resolve(classOfInterest, index);
        }

        Class<?> dynamicType = null;
        if (dynamicTypeInfo != null) {
            dynamicType = dynamicTypeInfo.resolve(classOfInterest, index);
        }

        return narrowType(staticType, dynamicType);
    }
    /** resolves multiple types (e.g. method arguments) */
    public Class<?>[] resolve(Type[] types) {
        Class<?>[] ret = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            ret[i] = resolve(types[i]);
        }
        return ret;
    }

    /**
     * Resolves given <code>type</code> and returns a concrete class.
     *
     * @param type type is normally a generic type
     * @return the 'best' <code>type</code>.
     */
    public Class<?> resolve(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        Class<?> dynamicType = null;
        if (dynamicTypeInfo != null) {
            type = dynamicTypeInfo.reverseResolve(type);
            dynamicType = dynamicTypeInfo.resolve(type);
        }
        Class<?> staticType = null;
        if (staticTypeInfo != null) {
            staticType = staticTypeInfo.resolve(type);
        }
        Class<?> resolved = narrowType(staticType, dynamicType);
        if (resolved == null) {
            return JavaTypeInfo.getRawType(type);
        } else {
            return narrowType(resolved, JavaTypeInfo.getRawType(type));
        }
    }

    private Class<?> narrowType(Class<?> staticType, Class<?> dynamicType) {
        if (staticType == null) {
            return dynamicType;
        } else if (dynamicType == null) {
            return staticType;
        } else if (staticType.isAssignableFrom(dynamicType)) {
            return dynamicType;
        } else {
            return staticType;
        }
    }
}
