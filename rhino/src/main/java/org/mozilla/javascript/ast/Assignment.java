/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Node;

/**
 * AST node representing the set of assignment operators such as {@code =}, {@code *=} and {@code
 * +=}.
 */
public class Assignment extends InfixExpression {

    public Assignment() {}

    public Assignment(int pos) {
        super(pos);
    }

    public Assignment(int pos, int len) {
        super(pos, len);
    }

    public Assignment(int pos, int len, AstNode left, AstNode right) {
        super(pos, len, left, right);
    }

    public Assignment(AstNode left, AstNode right) {
        super(left, right);
    }

    public Assignment(int operator, AstNode left, AstNode right, int operatorPos) {
        super(operator, left, right, operatorPos);
    }

    @Override
    protected Node shallowCopy() {
        if (getClass() != Assignment.class) {
            throw new UnsupportedOperationException(
                    "shallowCopy() not implemented for " + getClass().getName());
        }
        Assignment copy = new Assignment();
        copy.type = this.type;
        copyAstFields(this, copy);
        copy.copyInfixFieldsFrom(this);
        return copy;
    }
}
