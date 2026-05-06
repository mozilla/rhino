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
 * Used for code generation. During codegen, the AST is transformed into an Intermediate
 * Representation (IR) in which loops, ifs, switches and other control-flow statements are rewritten
 * as labeled jumps. If the parser is set to IDE-mode, the resulting AST will not contain any
 * instances of this class.
 */
public class Jump extends AstNode {

    public Node target;
    private Node target2;
    private Jump jumpNode;

    public Jump() {
        type = Token.ERROR;
    }

    public Jump(int nodeType) {
        type = nodeType;
    }

    public Jump(int type, Node child) {
        this(type);
        addChildToBack(child);
    }

    public Jump getJumpStatement() {
        if (type != Token.BREAK && type != Token.CONTINUE) codeBug();
        return jumpNode;
    }

    public void setJumpStatement(Jump jumpStatement) {
        if (type != Token.BREAK && type != Token.CONTINUE) codeBug();
        if (jumpStatement == null) codeBug();
        if (this.jumpNode != null) codeBug(); // only once
        this.jumpNode = jumpStatement;
    }

    public Node getDefault() {
        if (type != Token.SWITCH) codeBug();
        return target2;
    }

    public void setDefault(Node defaultTarget) {
        if (type != Token.SWITCH) codeBug();
        if (defaultTarget.getType() != Token.TARGET) codeBug();
        if (target2 != null) codeBug(); // only once
        target2 = defaultTarget;
    }

    public Node getFinally() {
        if (type != Token.TRY) codeBug();
        return target2;
    }

    public void setFinally(Node finallyTarget) {
        if (type != Token.TRY) codeBug();
        if (finallyTarget.getType() != Token.TARGET) codeBug();
        if (target2 != null) codeBug(); // only once
        target2 = finallyTarget;
    }

    public Jump getLoop() {
        if (type != Token.LABEL) codeBug();
        return jumpNode;
    }

    public void setLoop(Jump loop) {
        if (type != Token.LABEL) codeBug();
        if (loop == null) codeBug();
        if (jumpNode != null) codeBug(); // only once
        jumpNode = loop;
    }

    public Node getContinue() {
        if (type != Token.LOOP) codeBug();
        return target2;
    }

    public void setContinue(Node continueTarget) {
        if (type != Token.LOOP) codeBug();
        if (continueTarget.getType() != Token.TARGET) codeBug();
        if (target2 != null) codeBug(); // only once
        target2 = continueTarget;
    }

    @Override
    protected Node shallowCopy() {
        if (getClass() != Jump.class) {
            throw new UnsupportedOperationException(
                    "shallowCopy() not implemented for " + getClass().getName());
        }
        Jump copy = new Jump(type);
        copyAstFields(this, copy);
        copy.copyJumpFieldsFrom(this);
        return copy;
    }

    /**
     * Copies the {@link Jump}-level cross-reference fields ({@code target}, {@code target2}, {@code
     * jumpNode}) from {@code source} into this node. Intended for use by subclass {@link
     * Node#shallowCopy()} implementations. The values are copied raw; remapping happens later in
     * {@link Jump#fixupReferences(IdentityHashMap)}.
     */
    protected void copyJumpFieldsFrom(Jump source) {
        this.target = source.target;
        this.target2 = source.target2;
        this.jumpNode = source.jumpNode;
    }

    @Override
    protected void fixupReferences(IdentityHashMap<Node, Node> map) {
        if (target != null) {
            Node mapped = map.get(target);
            if (mapped != null) {
                target = mapped;
            }
        }
        if (target2 != null) {
            Node mapped = map.get(target2);
            if (mapped != null) {
                target2 = mapped;
            }
        }
        if (jumpNode != null) {
            Node mapped = map.get(jumpNode);
            if (mapped instanceof Jump) {
                jumpNode = (Jump) mapped;
            }
        }
        super.fixupReferences(map);
    }

    /**
     * Jumps are only used directly during code generation, and do not support this interface.
     *
     * @throws UnsupportedOperationException always when called
     */
    @Override
    public void visit(NodeVisitor visitor) {
        throw new UnsupportedOperationException(this.toString());
    }

    /**
     * Jumps are only used directly during code generation, and do not support this interface.
     *
     * @throws UnsupportedOperationException always when called
     */
    @Override
    public String toSource(int depth) {
        throw new UnsupportedOperationException(this.toString());
    }
}
