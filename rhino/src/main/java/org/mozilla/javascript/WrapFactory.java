/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript;

import org.mozilla.javascript.lc.type.TypeInfo;

/**
 * Embeddings that wish to provide their own custom wrappings for Java objects may extend this class
 * and call {@link Context#setWrapFactory(WrapFactory)} Once an instance of this class or an
 * extension of this class is enabled for a given context (by calling setWrapFactory on that
 * context), Rhino will call the methods of this class whenever it needs to wrap a value resulting
 * from calling a Java method or accessing a Java field.
 *
 * @see org.mozilla.javascript.Context#setWrapFactory(WrapFactory)
 * @since 1.5 Release 4
 */
public class WrapFactory {
    private final WrapProcessor processor;

    private boolean javaPrimitiveWrap = true;

    public WrapFactory() {
        // Set the processor to null if reflection is not available
        processor = LiveConnectSupport.get().getWrapProcessor();
    }

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
    public Object wrap(Context cx, VarScope scope, Object obj, Class<?> staticType) {
        checkReflectionSupport();
        return processor.wrap(cx, scope, obj, staticType, javaPrimitiveWrap);
    }

    public Object wrap(Context cx, VarScope scope, Object obj, TypeInfo type) {
        checkReflectionSupport();
        return processor.wrap(cx, scope, obj, type, javaPrimitiveWrap);
    }

    /**
     * Wrap an object newly created by a constructor call.
     *
     * @param cx the current Context for this thread
     * @param scope the scope of the executing script
     * @param obj the object to be wrapped
     * @return the wrapped value.
     */
    public Scriptable wrapNewObject(Context cx, VarScope scope, Object obj) {
        checkReflectionSupport();
        return processor.wrapNewObject(cx, scope, obj);
    }

    /**
     * Wrap Java object as Scriptable instance to allow full access to its methods and fields from
     * JavaScript.
     *
     * <p>{@link #wrap(Context, VarScope, Object, Class)} and {@link #wrapNewObject(Context,
     * VarScope, Object)} call this method when they can not convert {@code javaObject} to
     * JavaScript primitive value or JavaScript array.
     *
     * <p>Subclasses can override the method to provide custom wrappers for Java objects.
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
            Context cx, VarScope scope, Object javaObject, Class<?> staticType) {
        checkReflectionSupport();
        return processor.wrapAsJavaObject(cx, scope, javaObject, staticType);
    }

    public Scriptable wrapAsJavaObject(
            Context cx, VarScope scope, Object javaObject, TypeInfo type) {
        checkReflectionSupport();
        return processor.wrapAsJavaObject(cx, scope, javaObject, type);
    }

    /**
     * Wrap a Java class as Scriptable instance to allow access to its static members and fields and
     * use as constructor from JavaScript.
     *
     * <p>Subclasses can override this method to provide custom wrappers for Java classes.
     *
     * @param cx the current Context for this thread
     * @param scope the scope of the executing script
     * @param javaClass the class to be wrapped
     * @return the wrapped value which shall not be null
     * @since 1.7R3
     */
    public Scriptable wrapJavaClass(Context cx, VarScope scope, Class<?> javaClass) {
        checkReflectionSupport();
        return processor.wrapJavaClass(cx, scope, javaClass);
    }

    /**
     * Return {@code false} if result of Java method, which is instance of {@code String}, {@code
     * Number}, {@code Boolean} and {@code Character}, should be used directly as JavaScript
     * primitive type. By default the method returns true to indicate that instances of {@code
     * String}, {@code Number}, {@code Boolean} and {@code Character} should be wrapped as any other
     * Java object and scripts can access any Java method available in these objects. Use {@link
     * #setJavaPrimitiveWrap(boolean)} to change this.
     */
    public final boolean isJavaPrimitiveWrap() {
        return javaPrimitiveWrap;
    }

    /**
     * @see #isJavaPrimitiveWrap()
     */
    public final void setJavaPrimitiveWrap(boolean value) {
        if (value) {
            checkReflectionSupport();
        }
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.isSealed()) {
            Context.onSealedMutation();
        }
        javaPrimitiveWrap = value;
    }

    private void checkReflectionSupport() {
        if (processor == null) {
            // TODO message
            throw ScriptRuntime.constructError("Error", "Java reflection is not supported");
        }
    }
}
