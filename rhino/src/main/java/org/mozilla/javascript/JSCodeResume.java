package org.mozilla.javascript;

/**
 * This interface represent a thing which can be executed in JavaScript, which could be a generator,
 * or potentially in future an async function.
 */
public interface JSCodeResume<T extends ScriptOrFn<T>> {

    /**
     * Resuem execution of the code represented by this object.
     *
     * @param cx should be context in which the call is being made
     * @param executableObject should be function or script which owns this code.
     * @param state should be the state object created at some previous point when execution was
     *     yielded.
     * @param scope should be the scope in which the code will be executed.
     * @param operation should be one of the constants defined on {@link NativeGenerator} and
     *     represents the operation being performed.
     * @param value represents the value being passed into the generator (see
     *     https://tc39.es/ecma262/#sec-generatorresume for details).
     * @return the result of executing the code.
     */
    Object resume(
            Context cx,
            T executableObject,
            Object state,
            Scriptable scope,
            int operation,
            Object value);

    public static JSCodeResume NULL_RESUMABLE =
            (cx, eo, state, scope, op, value) -> {
                Kit.codeBug("Attempt to resume a non-generator function");
                return null;
            };
}
