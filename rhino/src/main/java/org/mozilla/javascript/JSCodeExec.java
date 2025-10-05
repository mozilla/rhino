package org.mozilla.javascript;

/**
 * This interface represent a thing which can be executed in JavaScript, whether that is a script,
 * function call, or function constructor.
 */
public interface JSCodeExec<T extends ScriptOrFn<T>> {

    /**
     * Executes the code represented by this object.
     *
     * @param cx should be context in which the call is being made
     * @param executableObject should be function or script which owns this code.
     * @param newTarget should be null in the case of script or function calls, and the `new.target`
     *     of constructors.
     * @param scope should be the scope in which the script, function, or constructor should
     *     execute. This should normally be derived from the scope which the function or constructor
     *     was declared, or for scripts should be the scope in which they are being executed.
     * @param thisObj should be the value of `this` that the code will see while executing.
     * @param args is the array of arguments passed to the code.
     * @return the result of executing the code.
     */
    Object execute(
            Context cx,
            T executableObject,
            Object newTarget,
            Scriptable scope,
            Object thisObj,
            Object[] args);
}
