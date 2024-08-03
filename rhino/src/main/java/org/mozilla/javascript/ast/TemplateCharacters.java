/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/**
 * AST node for Template Literal Characters.
 *
 * <p>Node type is {@link Token#TEMPLATE_CHARS}.
 */
public class TemplateCharacters extends AstNode {

    private String value;
    private String rawValue;

    {
        type = Token.TEMPLATE_CHARS;
    }

    public TemplateCharacters() {}

    public TemplateCharacters(int pos) {
        super(pos);
    }

    public TemplateCharacters(int pos, int len) {
        super(pos, len);
    }

    /**
     * Returns the node's value: the parsed template-literal-value (QV)
     *
     * @return the node's value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the node's value. Can be null in case of illegal escape sequences, which are allowed in
     * Template Literals but will have an undefined cooked value
     *
     * @param value the node's value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the node's raw-value: the parsed template-literal-raw-value (QRV)
     *
     * @return the node's raw-value
     */
    public String getRawValue() {
        return rawValue;
    }

    /**
     * Sets the node's raw-value.
     *
     * @param rawValue the node's raw-value
     * @throws IllegalArgumentException} if rawValue is {@code null}
     */
    public void setRawValue(String rawValue) {
        assertNotNull(rawValue);
        this.rawValue = rawValue;
    }

    @Override
    public String toSource(int depth) {
        return makeIndent(depth) + rawValue;
    }

    /** Visits this node. There are no children to visit. */
    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }
}
