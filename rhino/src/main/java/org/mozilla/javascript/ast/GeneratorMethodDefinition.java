/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/**
 * AST node for a generator method definition in an object literal, i.e. `*key() {}` in an object
 * literal.
 */
public class GeneratorMethodDefinition extends AstNode {

    private AstNode methodName;

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

    /** Visits this node, then the name. */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            methodName.visit(v);
        }
    }
}
