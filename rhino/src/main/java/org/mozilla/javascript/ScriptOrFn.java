package org.mozilla.javascript;

public interface ScriptOrFn<T extends ScriptOrFn<T>> {

    public default Scriptable getHomeObject() {
        return null;
    }

    public default JSDescriptor<T> getDescriptor() {
        return null;
    }

    public default Scriptable getParentScope() {
        return null;
    }
}
