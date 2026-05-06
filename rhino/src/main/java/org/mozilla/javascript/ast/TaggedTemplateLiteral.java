/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import java.util.IdentityHashMap;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/**
 * AST node for a Tagged Template Literal.
 *
 * <p>Node type is {@link Token#TAGGED_TEMPLATE_LITERAL}.
 */
public class TaggedTemplateLiteral extends AstNode {

    private AstNode target;
    private AstNode templateLiteral;

    {
        type = Token.TAGGED_TEMPLATE_LITERAL;
    }

    public TaggedTemplateLiteral() {}

    public TaggedTemplateLiteral(int pos) {
        super(pos);
    }

    public TaggedTemplateLiteral(int pos, int len) {
        super(pos, len);
    }

    public AstNode getTarget() {
        return target;
    }

    public void setTarget(AstNode target) {
        this.target = target;
        target.setParent(this);
    }

    public AstNode getTemplateLiteral() {
        return templateLiteral;
    }

    public void setTemplateLiteral(AstNode templateLiteral) {
        this.templateLiteral = templateLiteral;
        templateLiteral.setParent(this);
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append(target.toSource(0));
        sb.append(templateLiteral.toSource(0));
        return sb.toString();
    }

    @Override
    protected Node shallowCopy() {
        if (getClass() != TaggedTemplateLiteral.class) {
            throw new UnsupportedOperationException(
                    "shallowCopy() not implemented for " + getClass().getName());
        }
        TaggedTemplateLiteral copy = new TaggedTemplateLiteral();
        copy.type = this.type;
        copyAstFields(this, copy);
        copy.target = this.target;
        copy.templateLiteral = this.templateLiteral;
        return copy;
    }

    @Override
    protected void cloneNamedChildren(Node copyNode, IdentityHashMap<Node, Node> map) {
        super.cloneNamedChildren(copyNode, map);
        TaggedTemplateLiteral copy = (TaggedTemplateLiteral) copyNode;
        if (this.target != null) {
            copy.target = (AstNode) this.target.cloneStructure(map);
        }
        if (this.templateLiteral != null) {
            copy.templateLiteral = (AstNode) this.templateLiteral.cloneStructure(map);
        }
    }

    /** Visits this node. */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            target.visit(v);
            templateLiteral.visit(v);
        }
    }
}
