package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.*;

/** Tests for ES6+ tagged templates support. */
public class TaggedTemplateLiteralTest {

    /**
     * Target and {@link org.mozilla.javascript.ast.TemplateLiteral} nodes inside the tagged
     * template should have {@link org.mozilla.javascript.ast.TaggedTemplateLiteral} node as their
     * parent.
     *
     * <p>See <a href="https://github.com/mozilla/rhino/issues/1238">#1238</a> for details.
     */
    @Test
    public void taggedTemplateChildrenHaveParent() {
        String script = "tag`template`;";

        AstRoot root = new Parser().parse(script, "test", 0);
        TaggedTemplateFinder finder = new TaggedTemplateFinder();
        root.visit(finder);
        TaggedTemplateLiteral taggedTemplate = finder.getNode();

        assertNotNull(taggedTemplate);
        assertEquals(taggedTemplate, taggedTemplate.getTarget().getParent());
        assertEquals(taggedTemplate, taggedTemplate.getTemplateLiteral().getParent());
    }

    /**
     * Target and {@link org.mozilla.javascript.ast.TemplateLiteral} nodes inside the tagged
     * template should resolve the AST root.
     *
     * <p>See <a href="https://github.com/mozilla/rhino/issues/1238">#1238</a> for details.
     */
    @Test
    public void taggedTemplateChildrenHaveAstRoot() {
        String script = "tag`template`";

        AstRoot root = new Parser().parse(script, "test", 0);
        TaggedTemplateFinder finder = new TaggedTemplateFinder();
        root.visit(finder);
        TaggedTemplateLiteral taggedTemplate = finder.getNode();

        assertNotNull(taggedTemplate);
        assertEquals(root, taggedTemplate.getAstRoot());
        assertEquals(root, taggedTemplate.getTarget().getAstRoot());
        assertEquals(root, taggedTemplate.getTemplateLiteral().getAstRoot());
    }

    /**
     * AST nodes, which are descendants of a tagged template node should resolve the AST root.
     *
     * <p>See <a href="https://github.com/mozilla/rhino/issues/1238">#1238</a> for details.
     */
    @Test
    public void innerNodeHasAstRoot() {
        String script = "someObj.property()`template`";

        AstRoot root = new Parser().parse(script, "test", 0);
        NameFinder finder = new NameFinder("property");
        root.visit(finder);
        Name nameNode = finder.getNode();

        assertNotNull(nameNode);
        assertEquals(root, nameNode.getAstRoot());
    }

    /** Finds first {@link TaggedTemplateLiteral} node in the AST. */
    private static class TaggedTemplateFinder implements NodeVisitor {
        private TaggedTemplateLiteral node;

        public TaggedTemplateLiteral getNode() {
            return node;
        }

        @Override
        public boolean visit(AstNode astNode) {
            if (astNode instanceof TaggedTemplateLiteral) {
                this.node = (TaggedTemplateLiteral) astNode;
                return false;
            }
            return true;
        }
    }

    /** Finds first {@link Name} node for given identifier. */
    private static class NameFinder implements NodeVisitor {
        private Name node;
        private String name;

        public NameFinder(String name) {
            this.name = name;
        }

        public Name getNode() {
            return node;
        }

        @Override
        public boolean visit(AstNode astNode) {
            if (astNode instanceof Name) {
                Name nameNode = (Name) astNode;
                if (nameNode.getIdentifier().equals(name)) {
                    this.node = nameNode;
                    return false;
                }
            }
            return true;
        }
    }
}
