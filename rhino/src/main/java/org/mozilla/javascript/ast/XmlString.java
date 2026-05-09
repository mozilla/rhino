/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Node;

/**
 * AST node for an XML-text-only component of an XML literal expression. This node differs from a
 * {@link StringLiteral} in that it does not have quotes for delimiters.
 */
public class XmlString extends XmlFragment {

    private String xml;

    public XmlString() {}

    public XmlString(int pos) {
        super(pos);
    }

    public XmlString(int pos, String s) {
        super(pos);
        setXml(s);
    }

    /**
     * Sets the string for this XML component. Sets the length of the component to the length of the
     * passed string.
     *
     * @param s a string of xml text
     * @throws IllegalArgumentException} if {@code s} is {@code null}
     */
    public void setXml(String s) {
        assertNotNull(s);
        xml = s;
        setLength(s.length());
    }

    /**
     * Returns the xml string for this component. Note that it may not be well-formed XML; it is a
     * fragment.
     */
    public String getXml() {
        return xml;
    }

    @Override
    public String toSource(int depth) {
        return makeIndent(depth) + xml;
    }

    @Override
    protected Node shallowCopy() {
        if (getClass() != XmlString.class) {
            throw new UnsupportedOperationException(
                    "shallowCopy() not implemented for " + getClass().getName());
        }
        XmlString copy = new XmlString();
        copy.type = this.type;
        copyAstFields(this, copy);
        copy.xml = this.xml;
        return copy;
    }

    /** Visits this node. There are no children to visit. */
    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }
}
