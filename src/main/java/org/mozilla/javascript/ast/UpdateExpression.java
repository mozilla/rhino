/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/**
 * AST node representing update operators such as {@code ++}. The type field is set to the
 * appropriate Token type for the operator. The node length spans from the operator to the end of
 * the operand (for prefix operators) or from the start of the operand to the operator (for
 * postfix).
 */
public class UpdateExpression extends AstNode {

    private AstNode operand;
    private boolean isPostfix;

    public UpdateExpression() {}

    public UpdateExpression(int pos) {
        super(pos);
    }

    /** Constructs a new postfix UpdateExpression. */
    public UpdateExpression(int pos, int len) {
        super(pos, len);
    }

    /** Constructs a new prefix UpdateExpression. */
    public UpdateExpression(int operator, int operatorPosition, AstNode operand) {
        this(operator, operatorPosition, operand, false);
    }

    /**
     * Constructs a new UpdateExpression with the specified operator and operand. It sets the parent
     * of the operand, and sets its own bounds to encompass the operator and operand.
     *
     * @param operator the node type
     * @param operatorPosition the absolute position of the operator.
     * @param operand the operand expression
     * @param postFix true if the operator follows the operand. Int
     * @throws IllegalArgumentException} if {@code operand} is {@code null}
     */
    public UpdateExpression(int operator, int operatorPosition, AstNode operand, boolean postFix) {
        assertNotNull(operand);
        int beg = postFix ? operand.getPosition() : operatorPosition;
        // JavaScript only has ++ and -- postfix operators, so length is 2
        int end = postFix ? operatorPosition + 2 : operand.getPosition() + operand.getLength();
        setBounds(beg, end);
        setOperator(operator);
        setOperand(operand);
        isPostfix = postFix;
    }

    /** Returns operator token &ndash; alias for {@link #getType} */
    public int getOperator() {
        return type;
    }

    /**
     * Sets operator &ndash; same as {@link #setType}, but throws an exception if the operator is
     * invalid
     *
     * @throws IllegalArgumentException if operator is not a valid Token code
     */
    public void setOperator(int operator) {
        if (!Token.isValidToken(operator))
            throw new IllegalArgumentException("Invalid token: " + operator);
        setType(operator);
    }

    public AstNode getOperand() {
        return operand;
    }

    /**
     * Sets the operand, and sets its parent to be this node.
     *
     * @throws IllegalArgumentException} if {@code operand} is {@code null}
     */
    public void setOperand(AstNode operand) {
        assertNotNull(operand);
        this.operand = operand;
        operand.setParent(this);
    }

    /** Returns whether the operator is postfix */
    public boolean isPostfix() {
        return isPostfix;
    }

    /** Returns whether the operator is prefix */
    public boolean isPrefix() {
        return !isPostfix;
    }

    /** Sets whether the operator is postfix */
    public void setIsPostfix(boolean isPostfix) {
        this.isPostfix = isPostfix;
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        int type = getType();
        if (!isPostfix) {
            sb.append(operatorToString(type));
        }
        sb.append(operand.toSource());
        if (isPostfix) {
            sb.append(operatorToString(type));
        }
        return sb.toString();
    }

    /** Visits this node, then the operand. */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            operand.visit(v);
        }
    }
}
