package org.mozilla.javascript;

public class FunctionScope extends DeclarationScope {
    private static final long serialVersionUID = -7471457301304454454L;

    public FunctionScope(ScopeObject parentScope) {
        super(parentScope);
    }
}
