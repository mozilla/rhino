package org.mozilla.javascript;

import static org.mozilla.javascript.Scriptable.NOT_FOUND;

public class WithScope implements VarScope {
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
    public boolean delete(int index) {
        return false;
    }

    @Override
    public boolean delete(Symbol arg0) {
        return false;
    }

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
    public boolean put(int index, VarScope start, Object value) {
        return false;
    }

    @Override
    public boolean put(Symbol arg0, VarScope arg1, Object arg2) {
        return false;
    }

    @Override
    public void defineConst(Context cx, String name, VarScope start) {
        Kit.codeBug("Attempt to define a const on a `with` scope.");
    }

    @Override
    public boolean isConst(Context cx, String name) {
        return false;
    }

    @Override
    public void putConst(Context cx, String name, VarScope start, Object value) {
        Kit.codeBug("Attempt to define a const on a `with` scope.");
    }

    @Override
    public Object get(String name, VarScope start) {
        return ScriptableObject.getProperty(obj, name);
    }

    @Override
    public boolean put(String name, VarScope start, Object value) {
        return ScriptableObject.putProperty(obj, name, value);
    }

    @Override
    public boolean has(String name, VarScope start) {
        return ScriptableObject.hasProperty(obj, name);
    }

    @Override
    public boolean delete(String name) {
        return ScriptableObject.deleteProperty(obj, name);
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
        // WithScope itself does not support it
        throw new IllegalStateException();
    }

    @Override
    public boolean isNestedScope() {
        return true;
    }

    @Override
    public boolean createMutableBinding(String name, boolean deletable) {
        Kit.codeBug();
        return false;
    }

    @Override
    public boolean createImmutableBinding(String name, boolean strict) {
        Kit.codeBug();
        return false;
    }

    public boolean initializeBinding(String name, Object value) {
        Kit.codeBug();
        return true;
    }
}
