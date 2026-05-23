package org.mozilla.javascript;

import java.io.Serial;

public class ScopeWrapper extends ScriptableObject {
    @Serial private static final long serialVersionUID = -3481312197060837332L;

    private final VarScope scope;

    public ScopeWrapper(VarScope scope) {
        this.scope = scope;
    }

    @Override
    public Scriptable getPrototype() {
        return null;
    }

    @Override
    public Object get(String name, Scriptable start) {
        return scope.get(name, scope);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        scope.put(name, scope, value);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return scope.has(name, scope);
    }

    @Override
    public VarScope getParentScope() {
        return scope.getParentScope();
    }

    @Override
    public String getClassName() {
        return "scope";
    }

    public VarScope getScope() {
        return scope;
    }
}
