/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript.lc;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LazilyLoadedCtor;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrapFactory;

/** WrapFactory for LiveConnect */
public class JavaWrapFactory implements WrapFactory {

    @Override
    public Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType) {
        if (obj == null || obj == Undefined.instance || obj instanceof Scriptable) {
            return obj;
        }
        if (staticType != null && staticType.isPrimitive()) {
            if (staticType == Void.TYPE) return Undefined.instance;
            if (staticType == Character.TYPE) return Integer.valueOf(((Character) obj).charValue());
            return obj;
        }
        if (!isJavaPrimitiveWrap()) {
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
        Class<?> cls = obj.getClass();
        if (cls.isArray()) {
            return NativeJavaArray.wrap(scope, obj);
        }
        return wrapAsJavaObject(cx, scope, obj, staticType);
    }

    @Override
    public void initStandardObjects(Context cx, ScriptableObject scope, boolean sealed) {
        NativeJavaObject.init(scope, sealed);
        NativeJavaMap.init(scope, sealed);
        new ClassCache().associate(scope);
        if (cx.getWrapFactory() == null) {
            cx.setWrapFactory(new JavaWrapFactory());
        }
        new LazilyLoadedCtor(
                scope, "Packages", "org.mozilla.javascript.lc.NativeJavaTopPackage", sealed, true);
        new LazilyLoadedCtor(
                scope, "getClass", "org.mozilla.javascript.lc.NativeJavaTopPackage", sealed, true);
        new LazilyLoadedCtor(
                scope, "JavaAdapter", "org.mozilla.javascript.lc.JavaAdapter", sealed, true);
        new LazilyLoadedCtor(
                scope, "JavaImporter", "org.mozilla.javascript.lc.ImporterTopLevel", sealed, true);

        for (String packageName : NativeJavaTopPackage.getTopPackageNames()) {
            new LazilyLoadedCtor(
                    scope,
                    packageName,
                    "org.mozilla.javascript.lc.NativeJavaTopPackage",
                    sealed,
                    true);
        }
    }

    @Override
    public Scriptable wrapNewObject(Context cx, Scriptable scope, Object obj) {
        if (obj instanceof Scriptable) {
            return (Scriptable) obj;
        }
        Class<?> cls = obj.getClass();
        if (cls.isArray()) {
            return NativeJavaArray.wrap(scope, obj);
        }
        return wrapAsJavaObject(cx, scope, obj, null);
    }

    @Override
    public Scriptable wrapAsJavaObject(
            Context cx, Scriptable scope, Object javaObject, Class<?> staticType) {
        if (List.class.isAssignableFrom(javaObject.getClass())) {
            return new NativeJavaList(scope, javaObject);
        } else if (Map.class.isAssignableFrom(javaObject.getClass())) {
            return new NativeJavaMap(scope, javaObject);
        }
        return new NativeJavaObject(scope, javaObject, staticType);
    }

    @Override
    public Scriptable wrapJavaClass(Context cx, Scriptable scope, Class<?> javaClass) {
        return new NativeJavaClass(scope, javaClass);
    }

    /**
     * Return <code>false</code> if result of Java method, which is instance of <code>String</code>,
     * <code>Number</code>, <code>Boolean</code> and <code>Character</code>, should be used directly
     * as JavaScript primitive type. By default the method returns true to indicate that instances
     * of <code>String</code>, <code>Number</code>, <code>Boolean</code> and <code>Character</code>
     * should be wrapped as any other Java object and scripts can access any Java method available
     * in these objects. Use {@link #setJavaPrimitiveWrap(boolean)} to change this.
     */
    public final boolean isJavaPrimitiveWrap() {
        return javaPrimitiveWrap;
    }

    /**
     * @see #isJavaPrimitiveWrap()
     */
    public final void setJavaPrimitiveWrap(boolean value) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.isSealed()) {
            Context.onSealedMutation();
        }
        javaPrimitiveWrap = value;
    }

    private boolean javaPrimitiveWrap = true;
}
