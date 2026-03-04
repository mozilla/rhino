/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/**
 * AST node for an ES2017 {@code await} expression. Node type is {@link Token#AWAIT}.
 *
 * <pre><i>AwaitExpression</i> :
 *   <b>await</b> UnaryExpression</pre>
 */
public class AwaitExpression extends AstNode {

    private AstNode value;

    public AwaitExpression() {
        type = Token.AWAIT;
    }

    public AwaitExpression(int pos) {
        super(pos);
        type = Token.AWAIT;
    }

    public AwaitExpression(int pos, int len) {
        super(pos, len);
        type = Token.AWAIT;
    }

    public AwaitExpression(int pos, int len, AstNode value) {
        super(pos, len);
        type = Token.AWAIT;
        setValue(value);
    }

    /** Returns the awaited expression. */
    public AstNode getValue() {
        return value;
    }

    /**
     * Sets the awaited expression, and sets its parent to this node.
     *
     * @param expr the expression to await
     */
    public void setValue(AstNode expr) {
        this.value = expr;
        if (expr != null) expr.setParent(this);
    }

    @Override
    public String toSource(int depth) {
        return value == null ? "await" : "await " + value.toSource(0);
    }

    /** Visits this node, and if present, the awaited value. */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this) && value != null) {
            value.visit(v);
        }
    }
}
