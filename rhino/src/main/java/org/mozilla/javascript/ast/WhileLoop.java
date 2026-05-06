/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import java.util.IdentityHashMap;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/**
 * While statement. Node type is {@link Token#WHILE}.
 *
 * <pre><i>WhileStatement</i>:
 *     <b>while</b> <b>(</b> Expression <b>)</b> Statement</pre>
 */
public class WhileLoop extends Loop {

    private AstNode condition;

    {
        type = Token.WHILE;
    }

    public WhileLoop() {}

    public WhileLoop(int pos) {
        super(pos);
    }

    public WhileLoop(int pos, int len) {
        super(pos, len);
    }

    /** Returns loop condition */
    public AstNode getCondition() {
        return condition;
    }

    /**
     * Sets loop condition
     *
     * @throws IllegalArgumentException} if condition is {@code null}
     */
    public void setCondition(AstNode condition) {
        assertNotNull(condition);
        this.condition = condition;
        condition.setParent(this);
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append("while (");
        sb.append(condition.toSource(0));
        sb.append(") ");
        if (this.getInlineComment() != null) {
            sb.append(this.getInlineComment().toSource(depth + 1)).append("\n");
        }
        if (body.getType() == Token.BLOCK) {
            sb.append(body.toSource(depth).trim());
            sb.append("\n");
        } else {
            if (this.getInlineComment() == null) {
                sb.append("\n");
            }
            sb.append(body.toSource(depth + 1));
        }
        return sb.toString();
    }

    @Override
    protected Node shallowCopy() {
        if (getClass() != WhileLoop.class) {
            throw new UnsupportedOperationException(
                    "shallowCopy() not implemented for " + getClass().getName());
        }
        WhileLoop copy = new WhileLoop();
        copy.type = this.type;
        copyAstFields(this, copy);
        copy.copyJumpFieldsFrom(this);
        copy.copyScopeFieldsFrom(this);
        copy.copyLoopFieldsFrom(this);
        copy.condition = this.condition;
        return copy;
    }

    @Override
    protected void cloneNamedChildren(Node copyNode, IdentityHashMap<Node, Node> map) {
        super.cloneNamedChildren(copyNode, map);
        WhileLoop copy = (WhileLoop) copyNode;
        if (this.condition != null) {
            copy.condition = (AstNode) this.condition.cloneStructure(map);
        }
    }

    /** Visits this node, the condition, then the body. */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            condition.visit(v);
            body.visit(v);
        }
    }
}
