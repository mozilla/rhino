package org.mozilla.javascript;

public interface JSCodeResume<T extends ScriptOrFn> {
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
