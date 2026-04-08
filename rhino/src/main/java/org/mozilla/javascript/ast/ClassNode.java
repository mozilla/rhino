/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/**
 * AST node for ES6 class declarations and expressions.
 *
 * <pre>
 * <i>ClassDeclaration</i> :
 *        <b>class</b> BindingIdentifier ClassTail
 * <i>ClassExpression</i> :
 *        <b>class</b> BindingIdentifieropt ClassTail
 * <i>ClassTail</i> :
 *        ClassHeritageopt { ClassBody }
 * </pre>
 */
public class ClassNode extends AstNode {

    private Name className;
    private AstNode superClass;
    private FunctionNode constructor;
    private boolean isStatement;

    {
        type = Token.CLASS;
    }

    public ClassNode() {}

    public ClassNode(int pos) {
        super(pos);
    }

    public ClassNode(int pos, int len) {
        super(pos, len);
    }

    public Name getClassName() {
        return className;
    }

    public void setClassName(Name className) {
        this.className = className;
        if (className != null) {
            className.setParent(this);
        }
    }

    public AstNode getSuperClass() {
        return superClass;
    }

    public void setSuperClass(AstNode superClass) {
        this.superClass = superClass;
        if (superClass != null) {
            superClass.setParent(this);
        }
    }

    public FunctionNode getConstructor() {
        return constructor;
    }

    public void setConstructor(FunctionNode constructor) {
        this.constructor = constructor;
        if (constructor != null) {
            constructor.setParent(this);
        }
    }

    public boolean isStatement() {
        return isStatement;
    }

    public void setIsStatement(boolean isStatement) {
        this.isStatement = isStatement;
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append("class");
        if (className != null) {
            sb.append(" ");
            sb.append(className.toSource(0));
        }
        if (superClass != null) {
            sb.append(" extends ");
            sb.append(superClass.toSource(0));
        }
        sb.append(" {\n");
        if (constructor != null) {
            sb.append(makeIndent(depth + 1));
            sb.append("constructor");
            sb.append(constructor.toSource(0).substring(constructor.toSource(0).indexOf('(')));
            sb.append("\n");
        }
        sb.append(makeIndent(depth));
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            if (className != null) {
                className.visit(v);
            }
            if (superClass != null) {
                superClass.visit(v);
            }
            if (constructor != null) {
                constructor.visit(v);
            }
        }
    }
}
