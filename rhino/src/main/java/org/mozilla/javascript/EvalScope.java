/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serial;

/**
 * The new scope of an eval call (ES6 and later) that holds the let and const declarations of the
 * eval code. For non-strict eval code this scope is nested, the var and function declarations go to
 * the variable scope of the caller. For strict eval code it holds the var and function declarations
 * as well.
 */
class EvalScope extends DeclarationScope {
    @Serial private static final long serialVersionUID = 3591585925419474066L;

    private final boolean nested;

    EvalScope(VarScope parentScope, boolean nested) {
        super(parentScope);
        this.nested = nested;
    }

    @Override
    public boolean isNestedScope() {
        return nested;
    }

    @Override
    public Object get(String name, VarScope start) {
        return getInitializedBinding(name, start);
    }

    /** Creates an uninitialized binding for a let declaration of the eval code. */
    void defineLexicalDeclaration(String name) {
        defineProperty(name, UniqueTag.UNINITIALIZED_BINDING, ScriptableObject.PERMANENT);
    }
}
