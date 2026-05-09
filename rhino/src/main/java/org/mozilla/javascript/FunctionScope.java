package org.mozilla.javascript;

public class FunctionScope extends DeclarationScope {
    private static final long serialVersionUID = 4760825497832652202L;

    public FunctionScope(ScopeObject parentScope) {
        super(parentScope);
    }
}
