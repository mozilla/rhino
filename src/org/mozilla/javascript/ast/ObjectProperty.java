/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/**
 * AST node for a single name:value entry in an Object literal. For simple entries, the node type is
 * {@link Token#COLON}, and the name (left side expression) is either a {@link Name}, a {@link
 * StringLiteral}, a {@link NumberLiteral} or a {@link BigIntLiteral}.
 *
 * <p>This node type is also used for getter/setter properties in object literals. In this case the
 * node bounds include the "get" or "set" keyword. The left-hand expression in this case is always a
 * {@link Name}, and the overall node type is {@link Token#GET} or {@link Token#SET}, as
 * appropriate. The {@code operatorPosition} field is meaningless if the node is a getter or setter.
 *
 * <pre><i>ObjectProperty</i> :
 *       PropertyName <b>:</b> AssignmentExpression
 * <i>PropertyName</i> :
 *       Identifier
 *       StringLiteral
 *       NumberLiteral
 *       BigIntLiteral</pre>
 */
public class ObjectProperty extends InfixExpression {

    {
        type = Token.COLON;
    }

    /**
     * Sets the node type. Must be one of {@link Token#COLON}, {@link Token#GET}, or {@link
     * Token#SET}.
     *
     * @throws IllegalArgumentException if {@code nodeType} is invalid
     */
    public void setNodeType(int nodeType) {
        if (nodeType != Token.COLON
                && nodeType != Token.GET
                && nodeType != Token.SET
                && nodeType != Token.METHOD)
            throw new IllegalArgumentException("invalid node type: " + nodeType);
        setType(nodeType);
    }

    public ObjectProperty() {}

    public ObjectProperty(int pos) {
        super(pos);
    }

    public ObjectProperty(int pos, int len) {
        super(pos, len);
    }

    /** Marks this node as a "getter" property. */
    public void setIsGetterMethod() {
        type = Token.GET;
    }

    /** Returns true if this is a getter function. */
    public boolean isGetterMethod() {
        return type == Token.GET;
    }

    /** Marks this node as a "setter" property. */
    public void setIsSetterMethod() {
        type = Token.SET;
    }

    /** Returns true if this is a setter function. */
    public boolean isSetterMethod() {
        return type == Token.SET;
    }

    public void setIsNormalMethod() {
        type = Token.METHOD;
    }

    public boolean isNormalMethod() {
        return type == Token.METHOD;
    }

    public boolean isMethod() {
        return isGetterMethod() || isSetterMethod() || isNormalMethod();
    }

    public void setIsShorthand(boolean shorthand) {
        this.shorthand = shorthand;
    }

    public boolean isShorthand() {
        return shorthand;
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth + 1));
        if (isGetterMethod()) {
            sb.append("get ");
        } else if (isSetterMethod()) {
            sb.append("set ");
        }
        sb.append(left.toSource(getType() == Token.COLON ? 0 : depth));

        if (!shorthand) {
            if (type == Token.COLON) {
                sb.append(": ");
            }
            sb.append(right.toSource(getType() == Token.COLON ? 0 : depth + 1));
        }
        return sb.toString();
    }

    private boolean shorthand;
}
