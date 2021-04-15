package org.mozilla.javascript;

/** An interface that can be used to implement a constructor function as a lambda. */
public interface Constructable {

    /**
     * Call the function as a constructor.
     *
     * <p>This method is invoked by the runtime in order to satisfy a use of the JavaScript <code>
     * new</code> operator. This method is expected to create a new object and return it.
     *
     * @param cx the current Context for this thread
     * @param scope an enclosing scope of the caller except when the function is called from a
     *     closure.
     * @param args the array of arguments
     * @return the allocated object
     */
    Scriptable construct(Context cx, Scriptable scope, Object[] args);
}
