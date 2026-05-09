/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import java.util.IdentityHashMap;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/** AST node for a spread `...expression` in an object literal. */
public class SpreadObjectProperty extends AbstractObjectProperty {

    private Spread spreadNode;

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
    protected Node shallowCopy() {
        if (getClass() != SpreadObjectProperty.class) {
            throw new UnsupportedOperationException(
                    "shallowCopy() not implemented for " + getClass().getName());
        }
        SpreadObjectProperty copy = new SpreadObjectProperty(this.spreadNode);
        copy.type = this.type;
        copyAstFields(this, copy);
        return copy;
    }

    @Override
    protected void cloneNamedChildren(Node copyNode, IdentityHashMap<Node, Node> map) {
        super.cloneNamedChildren(copyNode, map);
        SpreadObjectProperty copy = (SpreadObjectProperty) copyNode;
        if (this.spreadNode != null) {
            copy.spreadNode = (Spread) this.spreadNode.cloneStructure(map);
        }
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            spreadNode.visit(v);
        }
    }
}
