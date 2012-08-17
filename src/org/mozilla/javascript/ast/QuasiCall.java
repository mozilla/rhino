/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/**
 * AST node for a Tagged Quasi.
 * <p>Node type is {@link Token#QUASI_CALL}.</p>
 */
public class QuasiCall extends AstNode {

    private AstNode target;
    private AstNode quasi;

    {
        type = Token.QUASI_CALL;
    }

    public QuasiCall() {
    }

    public QuasiCall(int pos) {
        super(pos);
    }

    public QuasiCall(int pos, int len) {
        super(pos, len);
    }

    public AstNode getTarget() {
        return target;
    }

    public void setTarget(AstNode target) {
        this.target = target;
    }

    public AstNode getQuasi() {
        return quasi;
    }

    public void setQuasi(AstNode quasi) {
        this.quasi = quasi;
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append(target.toSource(0));
        sb.append(quasi.toSource(0));
        return sb.toString();
    }

    /**
     * Visits this node.
     */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            target.visit(v);
            quasi.visit(v);
        }
    }
}
