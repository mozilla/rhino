/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/** AST node for a spread `...expression` in an object literal. */
public class SpreadObjectProperty extends AbstractObjectProperty {

    private final Spread spreadNode;

    {
        type = Token.DOTDOTDOT;
    }

    public SpreadObjectProperty(Spread spreadNode) {
        super(spreadNode.getPosition(), spreadNode.getLength());
        this.spreadNode = spreadNode;
        this.spreadNode.setParent(this);
        this.setLineColumnNumber(spreadNode.getLineno(), spreadNode.getColumn());
    }

    public Spread getSpreadNode() {
        return spreadNode;
    }

    @Override
    public boolean hasSideEffects() {
        return spreadNode.hasSideEffects();
    }

    @Override
    public String toSource(int depth) {
        return spreadNode.toSource(depth);
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            spreadNode.visit(v);
        }
    }
}
