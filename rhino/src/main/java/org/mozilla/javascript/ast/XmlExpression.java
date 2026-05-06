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
 * AST node for an embedded JavaScript expression within an E4X XML literal. Node type, like {@link
 * XmlLiteral}, is {@link Token#XML}. The node length includes the curly braces.
 */
public class XmlExpression extends XmlFragment {

    private AstNode expression;
    private boolean isXmlAttribute;

    public XmlExpression() {}

    public XmlExpression(int pos) {
        super(pos);
    }

    public XmlExpression(int pos, int len) {
        super(pos, len);
    }

    public XmlExpression(int pos, AstNode expr) {
        super(pos);
        setExpression(expr);
    }

    /** Returns the expression embedded in {} */
    public AstNode getExpression() {
        return expression;
    }

    /**
     * Sets the expression embedded in {}, and sets its parent to this node.
     *
     * @throws IllegalArgumentException if {@code expression} is {@code null}
     */
    public void setExpression(AstNode expression) {
        assertNotNull(expression);
        this.expression = expression;
        expression.setParent(this);
    }

    /** Returns whether this is part of an xml attribute value */
    public boolean isXmlAttribute() {
        return isXmlAttribute;
    }

    /** Sets whether this is part of an xml attribute value */
    public void setIsXmlAttribute(boolean isXmlAttribute) {
        this.isXmlAttribute = isXmlAttribute;
    }

    @Override
    public String toSource(int depth) {
        return makeIndent(depth) + "{" + expression.toSource(depth) + "}";
    }

    @Override
    protected Node shallowCopy() {
        if (getClass() != XmlExpression.class) {
            throw new UnsupportedOperationException(
                    "shallowCopy() not implemented for " + getClass().getName());
        }
        XmlExpression copy = new XmlExpression();
        copy.type = this.type;
        copyAstFields(this, copy);
        copy.expression = this.expression;
        copy.isXmlAttribute = this.isXmlAttribute;
        return copy;
    }

    @Override
    protected void cloneNamedChildren(Node copyNode, IdentityHashMap<Node, Node> map) {
        super.cloneNamedChildren(copyNode, map);
        XmlExpression copy = (XmlExpression) copyNode;
        if (this.expression != null) {
            copy.expression = (AstNode) this.expression.cloneStructure(map);
        }
    }

    /** Visits this node, then the child expression. */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            expression.visit(v);
        }
    }
}
