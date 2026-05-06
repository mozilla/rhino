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
 * AST node for a generator method definition in an object literal, i.e. `*key() {}` in an object
 * literal.
 */
public class GeneratorMethodDefinition extends AstNode {

    private AstNode methodName;

    private GeneratorMethodDefinition() {
        setType(Token.MUL);
    }

    public GeneratorMethodDefinition(int pos, int len, AstNode methodName) {
        super(pos, len);
        setType(Token.MUL);
        setMethodName(methodName);
    }

    public AstNode getMethodName() {
        return methodName;
    }

    public void setMethodName(AstNode methodName) {
        assertNotNull(methodName);
        this.methodName = methodName;
        methodName.setParent(this);
    }

    @Override
    public String toSource(int depth) {
        return makeIndent(depth) + '*' + methodName.toSource(depth);
    }

    @Override
    protected Node shallowCopy() {
        if (getClass() != GeneratorMethodDefinition.class) {
            throw new UnsupportedOperationException(
                    "shallowCopy() not implemented for " + getClass().getName());
        }
        GeneratorMethodDefinition copy = new GeneratorMethodDefinition();
        copy.type = this.type;
        copyAstFields(this, copy);
        copy.methodName = this.methodName;
        return copy;
    }

    @Override
    protected void cloneNamedChildren(Node copyNode, IdentityHashMap<Node, Node> map) {
        super.cloneNamedChildren(copyNode, map);
        GeneratorMethodDefinition copy = (GeneratorMethodDefinition) copyNode;
        if (this.methodName != null) {
            copy.methodName = (AstNode) this.methodName.cloneStructure(map);
        }
    }

    /** Visits this node, then the name. */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            methodName.visit(v);
        }
    }
}
