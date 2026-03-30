package org.mozilla.javascript;

import static org.mozilla.javascript.Scriptable.NOT_FOUND;

public class WithScope implements VarScope {
    private static final long serialVersionUID = -7471457301304454454L;

    // This cannot be final because XML dot queries mutate it!
    private Scriptable obj;
    private final VarScope parent;

    public WithScope(VarScope parentScope, Scriptable obj) {
        this.parent = parentScope;
        this.obj = obj;
    }

    @Override
    public VarScope getParentScope() {
        return parent;
    }

    public void setObject(Scriptable obj) {
        this.obj = obj;
    }

    @Override
    public void delete(int index) {}

    @Override
    public void delete(Symbol arg0) {}

    @Override
    public Object get(int index, VarScope start) {
        return NOT_FOUND;
    }

    @Override
    public Object get(Symbol arg0, VarScope arg1) {
        return NOT_FOUND;
    }

    @Override
    public boolean has(int index, VarScope start) {
        return false;
    }

    @Override
    public boolean has(Symbol arg0, VarScope arg1) {
        return false;
    }

    @Override
    public void put(int index, VarScope start, Object value) {}

    @Override
    public void put(Symbol arg0, VarScope arg1, Object arg2) {}

    @Override
    public void defineConst(String name, VarScope start) {
        Kit.codeBug("Attempt to define a const on a `with` scope.");
    }

    @Override
    public boolean isConst(String name) {
        return false;
    }

    @Override
    public void putConst(String name, VarScope start, Object value) {
        Kit.codeBug("Attempt to define a const on a `with` scope.");
    }

    @Override
    public Object get(String name, VarScope start) {
        return ScriptableObject.getProperty(obj, name);
    }

    @Override
    public void put(String name, VarScope start, Object value) {
        ScriptableObject.putProperty(obj, name, value);
    }

    @Override
    public boolean has(String name, VarScope start) {
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

    /** Must return null to continue looping or the final collection result. */
    protected Object updateDotQuery(boolean value) {
        // NativeWith itself does not support it
        throw new IllegalStateException();
    }
}
