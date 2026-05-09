package org.mozilla.javascript;

public class DeclarationScope extends ScopeObject {
    private static final long serialVersionUID = -7992031023451233550L;

    public DeclarationScope(VarScope parentScope) {
        super(parentScope);
    }
}
