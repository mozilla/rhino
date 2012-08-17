/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/**
 * AST node for Quasi Characters.
 * <p>Node type is {@link Token#QUASI_CHARS}.</p>
 */
public class QuasiCharacters extends AstNode {

    private String value;
    private String rawValue;

    {
        type = Token.QUASI_CHARS;
    }

    public QuasiCharacters() {
    }

    public QuasiCharacters(int pos) {
        super(pos);
    }

    public QuasiCharacters(int pos, int len) {
        super(pos, len);
    }

    /**
     * Returns the node's value: the parsed quasi-value (QV)
     * @return the node's value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the node's value.
     * @param value the node's value
     * @throws IllegalArgumentException} if value is {@code null}
     */
    public void setValue(String value) {
        assertNotNull(value);
        this.value = value;
    }

    /**
     * Returns the node's raw-value: the parsed quasi-raw-value (QRV)
     * @return the node's raw-value
     */
    public String getRawValue() {
        return rawValue;
    }

    /**
     * Sets the node's raw-value.
     * @param rawValue the node's raw-value
     * @throws IllegalArgumentException} if rawValue is {@code null}
     */
    public void setRawValue(String rawValue) {
        assertNotNull(rawValue);
        this.rawValue = rawValue;
    }

    @Override
    public String toSource(int depth) {
        return new StringBuilder(makeIndent(depth))
                .append(rawValue)
                .toString();
    }

    /**
     * Visits this node.  There are no children to visit.
     */
    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }
}
