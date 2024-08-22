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
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ParenthesizedExpression;

/** Tests position of ParenthesizedExpression node in source code in different cases. */
public class Issue129Test {
    private static final String SOURCE_URI = "issue129test.js";

    private Parser parser;

    @Before
    public void setUp() {
        parser = new Parser();
    }

    @Test
    public void getPosition() {
        String script = "(a);";
        AstRoot root = parser.parse(script, SOURCE_URI, 0);
        ParenthesizedExprVisitor visitor = new ParenthesizedExprVisitor();
        root.visitAll(visitor);

        ParenthesizedExpression pe = visitor.getFirstExpression();
        assertNotNull(pe);
        assertEquals(0, pe.getPosition());
    }

    @Test
    public void getLength() {
        String script = "(a);";
        AstRoot root = parser.parse(script, SOURCE_URI, 0);
        ParenthesizedExprVisitor visitor = new ParenthesizedExprVisitor();
        root.visitAll(visitor);

        ParenthesizedExpression pe = visitor.getFirstExpression();
        assertNotNull(pe);
        assertEquals(3, pe.getLength());
    }

    @Test
    public void getAbsolutePosition() {
        String script = "var a = (b).c()";
        AstRoot root = parser.parse(script, SOURCE_URI, 0);
        ParenthesizedExprVisitor visitor = new ParenthesizedExprVisitor();
        root.visitAll(visitor);

        ParenthesizedExpression pe = visitor.getFirstExpression();
        assertNotNull(pe);
        assertEquals(8, pe.getAbsolutePosition());
    }

    @Test
    public void multiline() {
        String script = "var a =\n" + "b +\n" + "(c +\n" + "d);";
        AstRoot root = parser.parse(script, SOURCE_URI, 0);
        ParenthesizedExprVisitor visitor = new ParenthesizedExprVisitor();
        root.visitAll(visitor);

        ParenthesizedExpression pe = visitor.getFirstExpression();
        assertNotNull(pe);
        assertEquals("(c +\nd)", getFromSource(script, pe));
    }

    @Test
    public void nested() {
        String script = "var a = (b * (c + d));";
        AstRoot root = parser.parse(script, SOURCE_URI, 0);
        ParenthesizedExprVisitor visitor = new ParenthesizedExprVisitor();
        root.visitAll(visitor);

        List<ParenthesizedExpression> exprs = visitor.getExpressions();
        assertEquals(2, exprs.size());
        for (ParenthesizedExpression pe : exprs) {
            if (pe.getExpression().getType() == Token.MUL)
                assertEquals("(b * (c + d))", getFromSource(script, pe));
            else assertEquals("(c + d)", getFromSource(script, pe));
        }
    }

    private static String getFromSource(String source, AstNode node) {
        return source.substring(
                node.getAbsolutePosition(), node.getAbsolutePosition() + node.getLength());
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
