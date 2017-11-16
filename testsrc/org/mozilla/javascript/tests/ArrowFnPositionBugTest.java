/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ReturnStatement;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class ArrowFnPositionBugTest {
    /**
     * Util class that sifts through nodes for first arrow function,
     * then stores it and stops
     */
    private static class ArrowFnExtractor implements NodeVisitor {
        private FunctionNode functionNode;
        @Override
        public boolean visit(AstNode node) {
            if (functionNode != null) return false;
            if (node instanceof FunctionNode) {
                FunctionNode fn = (FunctionNode) node;
                if (fn.getFunctionType() == FunctionNode.ARROW_FUNCTION) {
                    functionNode = fn;
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Parses given source line and extracts a first (top) arrow function node
     */
    private FunctionNode parseAndExtractArrowFn(String src) {
        ArrowFnExtractor arrowFnExtractor = new ArrowFnExtractor();
        Parser p = new Parser();
        p.parse(src, "eval", 1).visit(arrowFnExtractor);
        return Objects.requireNonNull(arrowFnExtractor.functionNode);
    }

    @Test
    public void testArrowFnPositionInAssignment() {
        FunctionNode arrowFn = parseAndExtractArrowFn("var a = () => 1;");
        assertEquals(4, arrowFn.getPosition());
        assertEquals(8, arrowFn.getAbsolutePosition());
    }

    @Test
    public void testArrowFnPositionInCall() {
        FunctionNode arrowFn = parseAndExtractArrowFn("test(() => { return 2; }, a);");
        assertEquals(5, arrowFn.getPosition());
        assertEquals(5, arrowFn.getAbsolutePosition());
    }

    @Test
    public void testArrowFnWithArgsPosition() {
        FunctionNode arrowFn = parseAndExtractArrowFn("var a = (b, c) => b + c;");
        // ParenthesisedExpression copies position from its child, so
        // with an EmptyExpression it will be a position of opening paren,
        // but when there's parameters it's a start of parameters list
        // (i.e. it's shifted by one char compared to no-parameters case)
        assertEquals(5, arrowFn.getPosition());
        assertEquals(9, arrowFn.getAbsolutePosition());
    }

    @Test
    public void testArrowFnReturnPosition() {
        FunctionNode arrowFn = parseAndExtractArrowFn("test((cb) => cb() + 1);");
        ReturnStatement returnStatement = (ReturnStatement) arrowFn.getBody().getFirstChild();
        assertEquals(0, returnStatement.getPosition());
        assertEquals(13, returnStatement.getAbsolutePosition());
        assertEquals(8, returnStatement.getLength());
    }

}
