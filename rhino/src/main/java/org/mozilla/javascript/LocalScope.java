package org.mozilla.javascript;

public class LocalScope extends DeclarationScope {
    private static final long serialVersionUID = -7471457301304454454L;

    public LocalScope(VarScope parentScope) {
        super(parentScope);
    }

    public boolean isNestedScope() {
        return true;
    }

    @Override
    public void putConst(String name, VarScope start, Object value) {
        super.put(name, start, value);
    }
}
