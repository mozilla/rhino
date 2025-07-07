/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/** AST node for a spread key, i.e. `...expression` in an object literal. */
public class Spread extends AstNode {

    private AstNode expression;

    {
        type = Token.DOTDOTDOT;
    }

    public Spread(int pos, int len) {
        super(pos, len);
    }

    public AstNode getExpression() {
        return expression;
    }

    public void setExpression(AstNode expression) {
        assertNotNull(expression);
        this.expression = expression;
        this.expression.setParent(this);
    }

    @Override
    public boolean hasSideEffects() {
        if (expression == null) codeBug();
        return expression.hasSideEffects();
    }

    @Override
    public String toSource(int depth) {
        return makeIndent(depth) + "..." + expression.toSource(depth);
    }

    /** Visits this node, then the expression. */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            expression.visit(v);
        }
    }
}
