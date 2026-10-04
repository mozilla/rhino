/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript.reflect;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.AutomaticWrapper;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

/**
 * This is the default implementation of the WrapProcessor interface. It uses Java reflection to map
 * Java classes to JavaScript objects by reflecting on their methods and properties according to the
 * LiveConnect specification.
 */
public class AutomaticWrapperImpl implements AutomaticWrapper {
    /**
     * Wrap the object.
     *
     * <p>The value returned must be one of
     *
     * <UL>
     *   <LI>java.lang.Boolean
     *   <LI>java.lang.String
     *   <LI>java.lang.Number
     *   <LI>org.mozilla.javascript.Scriptable objects
     *   <LI>The value returned by Context.getUndefinedValue()
     *   <LI>null
     * </UL>
     *
     * @param cx the current Context for this thread
     * @param scope the scope of the executing script
     * @param obj the object to be wrapped. Note it can be null.
     * @param staticType type hint. It will be used for improving generic support and fallback type
     *     when security restrictions prevent wrapping object based on object class
     * @return the wrapped value.
     * @since 1.9.0
     */
    @Override
    public Object wrap(
            Context cx, VarScope scope, Object obj, TypeInfo staticType, boolean wrapPrimitives) {
        if (obj == null || obj == Undefined.instance || obj instanceof Scriptable) {
            return obj;
        }
        if (staticType.isPrimitive()) {
            if (staticType == TypeInfo.PRIMITIVE_VOID) {
                return Undefined.instance;
            } else if (staticType == TypeInfo.PRIMITIVE_CHARACTER) {
                return (int) (Character) obj;
            }
            return obj;
        }
        if (!wrapPrimitives) {
            if (obj instanceof String
                    || obj instanceof Boolean
                    || obj instanceof Integer
                    || obj instanceof Byte
                    || obj instanceof Short
                    || obj instanceof Long
                    || obj instanceof Float
                    || obj instanceof Double
                    || obj instanceof BigInteger) {
                return obj;
            } else if (obj instanceof Character) {
                return String.valueOf(((Character) obj).charValue());
            }
        }
        return wrapAsJavaObject(cx, scope, obj, staticType);
    }

    /**
     * Wrap an object newly created by a constructor call.
     *
     * @param cx the current Context for this thread
     * @param scope the scope of the executing script
     * @param obj the object to be wrapped
     * @return the wrapped value.
     */
    @Override
    public Scriptable wrapNewObject(Context cx, VarScope scope, Object obj) {
        if (obj instanceof Scriptable) {
            return (Scriptable) obj;
        }
        return wrapAsJavaObject(cx, scope, obj, TypeInfo.NONE);
    }

    /**
     * Wrap Java object as Scriptable instance to allow full access to its methods and fields from
     * JavaScript.
     *
     * @param cx the current Context for this thread
     * @param scope the scope of the executing script
     * @param javaObject the object to be wrapped
     * @param staticType type hint. It will be used for improving generic support and fallback type
     *     when security restrictions prevent wrapping object based on object class
     * @return the wrapped value which shall not be null
     * @since 1.9.0
     */
    public Scriptable wrapAsJavaObject(
            Context cx, VarScope scope, Object javaObject, TypeInfo staticType) {
        if (staticType.shouldReplace() && javaObject != null) {
            staticType =
                    TypeInfoFactory.getOrElse(scope, TypeInfoFactory.GLOBAL)
                            .create(javaObject.getClass());
        }
        if (List.class.isAssignableFrom(staticType.asClass())) {
            return new NativeJavaList(scope, javaObject, staticType);
        } else if (Map.class.isAssignableFrom(staticType.asClass())) {
            return new NativeJavaMap(scope, javaObject, staticType);
        } else if (staticType.isArray()) {
            return new NativeJavaArray(scope, javaObject, staticType);
        }
        return new NativeJavaObject(scope, javaObject, staticType);
    }

    /**
     * Wrap a Java class as Scriptable instance to allow access to its static members and fields and
     * use as constructor from JavaScript.
     *
     * @param cx the current Context for this thread
     * @param scope the scope of the executing script
     * @param javaClass the class to be wrapped
     * @return the wrapped value which shall not be null
     * @since 1.7R3
     */
    @Override
    public Scriptable wrapJavaClass(Context cx, VarScope scope, Class<?> javaClass) {
        return new NativeJavaClass(scope, javaClass);
    }
}
