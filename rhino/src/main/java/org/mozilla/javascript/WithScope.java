package org.mozilla.javascript;

public class WithScope implements VarScope {
    private static final long serialVersionUID = -7471457301304454454L;

    private final Scriptable obj;
    private final VarScope parent;

    public WithScope(VarScope parentScope, Scriptable obj) {
        this.parent = parentScope;
        this.obj = obj;
    }

    @Override
    public VarScope getParentScope() {
        return parent;
    }

    @Override
    public void delete(int index) {}

    @Override
    public void delete(Symbol arg0) {}

    @Override
    public Object get(int index, Scriptable start) {
        return NOT_FOUND;
    }

    @Override
    public Object get(Symbol arg0, Scriptable arg1) {
        return NOT_FOUND;
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return false;
    }

    @Override
    public boolean has(Symbol arg0, Scriptable arg1) {
        return false;
    }

    @Override
    public void put(int index, Scriptable start, Object value) {}

    @Override
    public void put(Symbol arg0, Scriptable arg1, Object arg2) {}

    @Override
    public void defineConst(String name, Scriptable start) {
        Kit.codeBug("Attempt to define a const on a `with` scope.");
    }

    @Override
    public boolean isConst(String name) {
        return false;
    }

    @Override
    public void putConst(String name, Scriptable start, Object value) {
        Kit.codeBug("Attempt to define a const on a `with` scope.");
    }

    @Override
    public Object get(String name, Scriptable start) {
        return ScriptableObject.getProperty(obj, name);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        ScriptableObject.putProperty(obj, name, value);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return ScriptableObject.hasProperty(obj, name);
    }

    @Override
    public void delete(String name) {
        ScriptableObject.deleteProperty(obj, name);
    }

    @Override
    public Object[] getIds() {
        return obj.getIds();
    }

    public Scriptable getObject() {
        return obj;
    }
}
