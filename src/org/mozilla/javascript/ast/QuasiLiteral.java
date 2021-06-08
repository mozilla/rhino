/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Token;

/**
 * AST node for a Quasi literal.
 * <p>Node type is {@link Token#QUASI}.</p>
 */
public class QuasiLiteral extends AstNode {

    private List<AstNode> elements;

    {
        type = Token.QUASI;
    }

    public QuasiLiteral() {
    }

    public QuasiLiteral(int pos) {
        super(pos);
    }

    public QuasiLiteral(int pos, int len) {
        super(pos, len);
    }

    /**
     * Returns a list of all literal sections of this quasi
     */
    public List<QuasiCharacters> getQuasiStrings() {
        if (elements == null) return emptyList();
        List<QuasiCharacters> strings = new ArrayList<QuasiCharacters>();
        for (AstNode e : elements) {
            if (e.getType() == Token.QUASI_CHARS) {
                strings.add((QuasiCharacters) e);
            }
        }
        return unmodifiableList(strings);
    }

    /**
     * Returns a list of all substitutions of this quasi
     */
    public List<AstNode> getSubstitutions() {
        if (elements == null) return emptyList();
        List<AstNode> subs = new ArrayList<AstNode>();
        for (AstNode e : elements) {
            if (e.getType() != Token.QUASI_CHARS) {
                subs.add(e);
            }
        }
        return unmodifiableList(subs);
    }

    /**
     * Returns the element list
     * @return the element list.  If there are no elements, returns an immutable
     *         empty list.
     */
    public List<AstNode> getElements() {
        if (elements == null) return emptyList();
        return elements;
    }

    /**
     * Sets the element list, and sets each element's parent to this node.
     * @param elements the element list.  Can be {@code null}.
     */
    public void setElements(List<AstNode> elements) {
        if (elements == null) {
            this.elements = null;
        } else {
            if (this.elements != null)
                this.elements.clear();
            for (AstNode e : elements)
                addElement(e);
        }
    }

    /**
     * Adds an element to the list, and sets its parent to this node.
     * @param element the element to add
     * @throws IllegalArgumentException if element is {@code null}.
     */
    public void addElement(AstNode element) {
        assertNotNull(element);
        if (elements == null)
            elements = new ArrayList<AstNode>();
        elements.add(element);
        element.setParent(this);
    }

    /**
     * Returns the number of elements in this {@code Quasi} literal.
     */
    public int getSize() {
        return elements == null ? 0 : elements.size();
    }

    /**
     * Returns element at specified index.
     * @param index the index of the element to retrieve
     * @return the element
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public AstNode getElement(int index) {
        if (elements == null)
            throw new IndexOutOfBoundsException("no elements");
        return elements.get(index);
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append("`");
        for (AstNode e : getElements()) {
            if (e.getType() == Token.QUASI_CHARS) {
                sb.append(e.toSource(0));
            } else {
                sb.append("${").append(e.toSource(0)).append("}");
            }
        }
        sb.append("`");
        return sb.toString();
    }

    /**
     * Visits this node.
     */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            for (AstNode e : getElements()) {
                e.visit(v);
            }
        }
    }
}
