package org.mozilla.javascript;

public interface JSCodeExec<T extends ScriptOrFn> {

    Object execute(
            Context cx,
            T executableObject,
            Object newTarget,
            Scriptable scope,
            Object thisObj,
            Object[] args);
}
