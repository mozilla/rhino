package org.mozilla.javascript;

/** An interface that can be used to implement a constructor function as a lambda. */
public interface Constructable {

    /**
     * Call the function as a constructor.
     *
     * <p>This method is invoked by the runtime in order to satisfy a use of the JavaScript {@code
     * new} operator. This method is expected to create a new object and return it.
     *
     * @param cx the current Context for this thread
     * @param scope an enclosing scope of the caller except when the function is called from a
     *     closure.
     * @param args the array of arguments
     * @return the allocated object
     */
    default Scriptable construct(Context cx, VarScope scope, Object[] args) {
        return construct(cx, this, scope, args);
    }

    /**
     * Call the function as a constructor.
     *
     * <p>If nt is equal to this then this call is equivalent `new f(...arguments), but reflection
     * and super constructor calls may pass in different values.
     *
     * @param cx the current Context for this thread
     * @param nt the new.target for this call,
     * @param s an enclosing scope of the caller except when the function is called from a closure.
     * @param args the array of arguments
     * @return the allocated object
     */
    Scriptable construct(Context cx, Object nt, VarScope s, Object[] args);
}
