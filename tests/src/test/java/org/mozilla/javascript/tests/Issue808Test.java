/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ParenthesizedExpression;

/** Tests position of nodes in the body of labeled loops. */
public class Issue808Test {
    private static final String SOURCE_URI = "issue808test.js";

    private Parser parser;

    @Before
    public void setUp() {
        parser = new Parser();
    }

    @Test
    public void absolutePositionInLabeledForLoop() {
        String script =
                "function foo() {\n"
                        + "  loop1:\n"
                        + "  for (el in obj) {\n"
                        + "    (el);\n"
                        + "  }\n"
                        + "}";
        AstRoot root = parser.parse(script, SOURCE_URI, 0);
        ParenthesizedExprVisitor visitor = new ParenthesizedExprVisitor();
        root.visitAll(visitor);

        ParenthesizedExpression pe = visitor.getFirstExpression();
        assertNotNull(pe);
        assertEquals(50, pe.getAbsolutePosition());
    }

    @Test
    public void absolutePositionInLabeledWhileLoop() {
        String script =
                "function foo() {\n"
                        + "  loop1:\n"
                        + "  while (i < 10) {\n"
                        + "    (el);\n"
                        + "  }\n"
                        + "}";
        AstRoot root = parser.parse(script, SOURCE_URI, 0);
        ParenthesizedExprVisitor visitor = new ParenthesizedExprVisitor();
        root.visitAll(visitor);

        ParenthesizedExpression pe = visitor.getFirstExpression();
        assertNotNull(pe);
        assertEquals(49, pe.getAbsolutePosition());
    }

    /** Visitor stores all visited ParenthesizedExpression nodes. */
    private static class ParenthesizedExprVisitor implements NodeVisitor {
        private List<ParenthesizedExpression> expressions = new ArrayList<>();

        /**
         * Gets first encountered ParenthesizedExpression node.
         *
         * @return First found ParenthesizedExpression node or {@code null} if no nodes of this type
         *     were found
         */
        public ParenthesizedExpression getFirstExpression() {
            return expressions.isEmpty() ? null : expressions.get(0);
        }

        /**
         * Gets all found ParenthesizedExpression nodes.
         *
         * @return Found nodes
         */
        public List<ParenthesizedExpression> getExpressions() {
            return expressions;
        }

        @Override
        public boolean visit(AstNode node) {
            if (node instanceof ParenthesizedExpression)
                expressions.add((ParenthesizedExpression) node);
            return true;
        }
    }
}
