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
 * A break statement. Node type is {@link Token#BREAK}.
 *
 * <pre><i>BreakStatement</i> :
 *   <b>break</b> [<i>no LineTerminator here</i>] [Identifier] ;</pre>
 */
public class BreakStatement extends Jump {

    private Name breakLabel;
    private AstNode targetNode;

    {
        type = Token.BREAK;
    }

    public BreakStatement() {}

    public BreakStatement(int pos) {
        // can't call super (Jump) for historical reasons
        position = pos;
    }

    public BreakStatement(int pos, int len) {
        position = pos;
        length = len;
    }

    /**
     * Returns the intended label of this break statement
     *
     * @return the break label. {@code null} if the source code did not specify a specific break
     *     label via "break &lt;target&gt;".
     */
    public Name getBreakLabel() {
        return breakLabel;
    }

    /**
     * Sets the intended label of this break statement, e.g. 'foo' in "break foo". Also sets the
     * parent of the label to this node.
     *
     * @param label the break label, or {@code null} if the statement is just the "break" keyword by
     *     itself.
     */
    public void setBreakLabel(Name label) {
        breakLabel = label;
        if (label != null) label.setParent(this);
    }

    /**
     * Returns the statement to break to
     *
     * @return the break target. Only {@code null} if the source code has an error in it.
     */
    public AstNode getBreakTarget() {
        return targetNode;
    }

    /**
     * Sets the statement to break to.
     *
     * @param target the statement to break to
     * @throws IllegalArgumentException if target is {@code null}
     */
    public void setBreakTarget(Jump target) {
        assertNotNull(target);
        this.targetNode = target;
        setJumpStatement(target);
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append("break");
        if (breakLabel != null) {
            sb.append(" ");
            sb.append(breakLabel.toSource(0));
        }
        sb.append(";\n");
        return sb.toString();
    }

    @Override
    protected Node shallowCopy() {
        if (getClass() != BreakStatement.class) {
            throw new UnsupportedOperationException(
                    "shallowCopy() not implemented for " + getClass().getName());
        }
        BreakStatement copy = new BreakStatement();
        copy.type = this.type;
        copyAstFields(this, copy);
        copy.copyJumpFieldsFrom(this);
        copy.breakLabel = this.breakLabel;
        copy.targetNode = this.targetNode;
        return copy;
    }

    @Override
    protected void cloneNamedChildren(Node copyNode, IdentityHashMap<Node, Node> map) {
        super.cloneNamedChildren(copyNode, map);
        BreakStatement copy = (BreakStatement) copyNode;
        if (this.breakLabel != null) {
            copy.breakLabel = (Name) this.breakLabel.cloneStructure(map);
        }
    }

    @Override
    protected void fixupReferences(IdentityHashMap<Node, Node> map) {
        if (breakLabel != null) {
            breakLabel.fixupReferences(map);
        }
        if (targetNode != null) {
            Node mapped = map.get(targetNode);
            if (mapped instanceof AstNode) {
                targetNode = (AstNode) mapped;
            }
        }
        super.fixupReferences(map);
    }

    /** Visits this node, then visits the break label if non-{@code null}. */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this) && breakLabel != null) {
            breakLabel.visit(v);
        }
    }
}
