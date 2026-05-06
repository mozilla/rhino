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
 * Do statement. Node type is {@link Token#DO}.
 *
 * <pre><i>DoLoop</i>:
 * <b>do</b> Statement <b>while</b> <b>(</b> Expression <b>)</b> <b>;</b></pre>
 */
public class DoLoop extends Loop {

    private AstNode condition;
    private int whilePosition = -1;

    {
        type = Token.DO;
    }

    public DoLoop() {}

    public DoLoop(int pos) {
        super(pos);
    }

    public DoLoop(int pos, int len) {
        super(pos, len);
    }

    /** Returns loop condition */
    public AstNode getCondition() {
        return condition;
    }

    /**
     * Sets loop condition, and sets its parent to this node.
     *
     * @throws IllegalArgumentException if condition is null
     */
    public void setCondition(AstNode condition) {
        assertNotNull(condition);
        this.condition = condition;
        condition.setParent(this);
    }

    /** Returns source position of "while" keyword */
    public int getWhilePosition() {
        return whilePosition;
    }

    /** Sets source position of "while" keyword */
    public void setWhilePosition(int whilePosition) {
        this.whilePosition = whilePosition;
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append("do ");
        if (this.getInlineComment() != null) {
            sb.append(this.getInlineComment().toSource(depth + 1)).append("\n");
        }
        sb.append(body.toSource(depth).trim());
        sb.append(" while (");
        sb.append(condition.toSource(0));
        sb.append(");\n");
        return sb.toString();
    }

    @Override
    protected Node shallowCopy() {
        if (getClass() != DoLoop.class) {
            throw new UnsupportedOperationException(
                    "shallowCopy() not implemented for " + getClass().getName());
        }
        DoLoop copy = new DoLoop();
        copy.type = this.type;
        copyAstFields(this, copy);
        copy.copyJumpFieldsFrom(this);
        copy.copyScopeFieldsFrom(this);
        copy.copyLoopFieldsFrom(this);
        copy.condition = this.condition;
        copy.whilePosition = this.whilePosition;
        return copy;
    }

    @Override
    protected void cloneNamedChildren(Node copyNode, IdentityHashMap<Node, Node> map) {
        super.cloneNamedChildren(copyNode, map);
        DoLoop copy = (DoLoop) copyNode;
        if (this.condition != null) {
            copy.condition = (AstNode) this.condition.cloneStructure(map);
        }
    }

    /** Visits this node, the body, and then the while-expression. */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            body.visit(v);
            condition.visit(v);
        }
    }
}
