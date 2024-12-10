package org.mozilla.javascript;

/**
 * Embeddings that wish to provide their own custom wrappings for Java objects may extend this class
 * and call {@link Context#setWrapFactory(WrapFactory)} Once an instance of this class or an
 * extension of this class is enabled for a given context (by calling setWrapFactory on that
 * context), Rhino will call the methods of this class whenever it needs to wrap a value resulting
 * from a call to a Java method or an access to a Java field.
 *
 * @see org.mozilla.javascript.Context#setWrapFactory(WrapFactory)
 * @since 1.5 Release 4
 */
public interface WrapFactory {

    void initStandardObjects(Context cx, ScriptableObject scope, boolean sealed);

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
     * @param staticType type hint. If security restrictions prevent to wrap object based on its
     *     class, staticType will be used instead.
     * @return the wrapped value.
     */
    Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType);

    /**
     * Wrap an object newly created by a constructor call.
     *
     * @param cx the current Context for this thread
     * @param scope the scope of the executing script
     * @param obj the object to be wrapped
     * @return the wrapped value.
     */
    Scriptable wrapNewObject(Context cx, Scriptable scope, Object obj);

    /**
     * Wrap Java object as Scriptable instance to allow full access to its methods and fields from
     * JavaScript.
     *
     * <p>{@link #wrap(Context, Scriptable, Object, Class)} and {@link #wrapNewObject(Context,
     * Scriptable, Object)} call this method when they can not convert <code>javaObject</code> to
     * JavaScript primitive value or JavaScript array.
     *
     * <p>Subclasses can override the method to provide custom wrappers for Java objects.
     *
     * @param cx the current Context for this thread
     * @param scope the scope of the executing script
     * @param javaObject the object to be wrapped
     * @param staticType type hint. If security restrictions prevent to wrap object based on its
     *     class, staticType will be used instead.
     * @return the wrapped value which shall not be null
     */
    Scriptable wrapAsJavaObject(
            Context cx, Scriptable scope, Object javaObject, Class<?> staticType);

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
    Scriptable wrapJavaClass(Context cx, Scriptable scope, Class<?> javaClass);
}
