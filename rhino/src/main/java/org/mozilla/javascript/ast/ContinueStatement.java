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
 * A continue statement. Node type is {@link Token#CONTINUE}.
 *
 * <pre><i>ContinueStatement</i> :
 *   <b>continue</b> [<i>no LineTerminator here</i>] [Identifier] ;</pre>
 */
public class ContinueStatement extends Jump {

    private Name label;
    private Loop targetLoop;

    {
        type = Token.CONTINUE;
    }

    public ContinueStatement() {}

    public ContinueStatement(int pos) {
        this(pos, -1);
    }

    public ContinueStatement(int pos, int len) {
        // can't call super (Jump) for historical reasons
        position = pos;
        length = len;
    }

    public ContinueStatement(Name label) {
        setLabel(label);
    }

    public ContinueStatement(int pos, Name label) {
        this(pos);
        setLabel(label);
    }

    public ContinueStatement(int pos, int len, Name label) {
        this(pos, len);
        setLabel(label);
    }

    /** Returns continue target */
    public Loop getTarget() {
        return targetLoop;
    }

    /**
     * Sets continue target. Does NOT set the parent of the target node: the target node is an
     * ancestor of this node.
     *
     * @param target continue target
     * @throws IllegalArgumentException if target is {@code null}
     */
    public void setTarget(Loop target) {
        assertNotNull(target);
        this.targetLoop = target;
        setJumpStatement(targetLoop);
    }

    /**
     * Returns the intended label of this continue statement
     *
     * @return the continue label. Will be {@code null} if the statement consisted only of the
     *     keyword "continue".
     */
    public Name getLabel() {
        return label;
    }

    /**
     * Sets the intended label of this continue statement. Only applies if the statement was of the
     * form "continue &lt;label&gt;".
     *
     * @param label the continue label, or {@code null} if not present.
     */
    public void setLabel(Name label) {
        this.label = label;
        if (label != null) label.setParent(this);
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append("continue");
        if (label != null) {
            sb.append(" ");
            sb.append(label.toSource(0));
        }
        sb.append(";\n");
        return sb.toString();
    }

    @Override
    protected Node shallowCopy() {
        if (getClass() != ContinueStatement.class) {
            throw new UnsupportedOperationException(
                    "shallowCopy() not implemented for " + getClass().getName());
        }
        ContinueStatement copy = new ContinueStatement();
        copy.type = this.type;
        copyAstFields(this, copy);
        copy.copyJumpFieldsFrom(this);
        copy.label = this.label;
        copy.targetLoop = this.targetLoop;
        return copy;
    }

    @Override
    protected void cloneNamedChildren(Node copyNode, IdentityHashMap<Node, Node> map) {
        super.cloneNamedChildren(copyNode, map);
        ContinueStatement copy = (ContinueStatement) copyNode;
        if (this.label != null) {
            copy.label = (Name) this.label.cloneStructure(map);
        }
    }

    @Override
    protected void fixupReferences(IdentityHashMap<Node, Node> map) {
        if (label != null) {
            label.fixupReferences(map);
        }
        if (targetLoop != null) {
            Node mapped = map.get(targetLoop);
            if (mapped instanceof Loop) {
                targetLoop = (Loop) mapped;
            }
        }
        super.fixupReferences(map);
    }

    /** Visits this node, then visits the label if non-{@code null}. */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this) && label != null) {
            label.visit(v);
        }
    }
}
