package org.mozilla.javascript;

import java.io.Serial;

public class FunctionScope extends DeclarationScope {
    @Serial private static final long serialVersionUID = 4760825497832652202L;

    public FunctionScope(ScopeObject parentScope) {
        super(parentScope);
    }
}
