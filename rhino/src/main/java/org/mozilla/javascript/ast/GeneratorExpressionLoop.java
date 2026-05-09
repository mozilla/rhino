/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Node;

/** */
public class GeneratorExpressionLoop extends ForInLoop {

    public GeneratorExpressionLoop() {}

    public GeneratorExpressionLoop(int pos) {
        super(pos);
    }

    public GeneratorExpressionLoop(int pos, int len) {
        super(pos, len);
    }

    /** Returns whether the loop is a for-each loop */
    @Override
    public boolean isForEach() {
        return false;
    }

    /** Sets whether the loop is a for-each loop */
    @Override
    public void setIsForEach(boolean isForEach) {
        throw new UnsupportedOperationException("this node type does not support for each");
    }

    @Override
    public String toSource(int depth) {
        return makeIndent(depth)
                + " for "
                + (isForEach() ? "each " : "")
                + "("
                + iterator.toSource(0)
                + (isForOf() ? " of " : " in ")
                + iteratedObject.toSource(0)
                + ")";
    }

    @Override
    protected Node shallowCopy() {
        if (getClass() != GeneratorExpressionLoop.class) {
            throw new UnsupportedOperationException(
                    "shallowCopy() not implemented for " + getClass().getName());
        }
        GeneratorExpressionLoop copy = new GeneratorExpressionLoop();
        copy.type = this.type;
        copyAstFields(this, copy);
        copy.copyJumpFieldsFrom(this);
        copy.copyScopeFieldsFrom(this);
        copy.copyLoopFieldsFrom(this);
        copy.copyForInLoopFieldsFrom(this);
        return copy;
    }

    /**
     * Visits the iterator expression and the iterated object expression. There is no
     * body-expression for this loop type.
     */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            iterator.visit(v);
            iteratedObject.visit(v);
        }
    }
}
