/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/**
 * Synthetic AST node emitted by {@code IRFactory} while injecting field initializers into a class
 * constructor. It stands in for a computed field key that has already been evaluated at class
 * declaration time; its type is {@link Token#GET_CLASS_COMPUTED_KEY} and the index property
 * identifies which stored key to load.
 */
public class ClassComputedKeyRef extends AstNode {

    {
        type = Token.GET_CLASS_COMPUTED_KEY;
    }

    public ClassComputedKeyRef(int index) {
        putIntProp(Node.LITERAL_INDEX_PROP, index);
    }

    public int getIndex() {
        return getExistingIntProp(Node.LITERAL_INDEX_PROP);
    }

    @Override
    public String toSource(int depth) {
        return "<computed-key>";
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }
}
