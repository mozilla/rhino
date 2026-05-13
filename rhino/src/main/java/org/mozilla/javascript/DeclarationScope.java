package org.mozilla.javascript;

import java.io.Serial;

public class DeclarationScope extends ScopeObject {
    @Serial private static final long serialVersionUID = -7992031023451233550L;

    public DeclarationScope(VarScope parentScope) {
        super(parentScope);
    }
}
