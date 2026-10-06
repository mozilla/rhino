package org.mozilla.javascript;

public class LocalScope extends DeclarationScope {
    private static final long serialVersionUID = -7471457301304454454L;

    public LocalScope(VarScope parentScope) {
        super(parentScope);
    }

    @Override
    public boolean isNestedScope() {
        return true;
    }

    @Override
    public LocalScope copyScope() {
        var res = new LocalScope(getParentScope());
        res.setMap(getMap().copyMap());
        return res;
    }
}
