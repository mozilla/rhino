/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Column and source-position tracking in the interpreter backend. */
class InterpreterSourcePositionTest {

    private static RhinoException throwFrom(String source, int baseLine) {
        try (Context cx = Context.enter()) {
            cx.setEvaluationMethod(Context.EvaluationMethod.Interpreter);
            TopLevel scope = cx.initStandardObjects();
            return assertThrows(
                    RhinoException.class,
                    () -> cx.evaluateString(scope, source, "t.js", baseLine, null));
        }
    }

    @Test
    void stackFrameCarriesColumn() {
        RhinoException e = throwFrom("function f() { throw new Error('x'); }\nf();", 1);
        ScriptStackElement[] stack = e.getScriptStack();
        assertEquals(16, stack[0].columnNumber, "column of the throw statement");
        assertEquals(1, stack[1].columnNumber, "column of the top-level call");
    }

    @Test
    void statementsOnOneLineGetDistinctColumns() {
        RhinoException e = throwFrom("function f() { var a = 1; throw new Error(a); }\nf();", 1);
        assertEquals(27, e.getScriptStack()[0].columnNumber);
    }

    /**
     * Positions are recorded per statement, so a failing sub-expression is attributed to the start
     * of its statement rather than to the operation itself. V8 would report the column of {@code
     * b}; expression-level positions would change this to match.
     */
    @Test
    void subExpressionsReportTheStatementColumn() {
        RhinoException e = throwFrom("var o = {};\n  o.a.b;", 1);
        assertEquals(2, e.lineNumber());
        assertEquals(3, e.columnNumber(), "start of the statement, not of '.b'");
    }

    @Test
    void engineThrownErrorsCarryAColumn() {
        RhinoException e = throwFrom("null.x;", 1);
        assertEquals(1, e.lineNumber());
        assertEquals(1, e.columnNumber());
    }

    @Test
    void columnsSurviveANonDefaultBaseLine() {
        RhinoException e = throwFrom("function f() { throw new Error('x'); }\nf();", 100);
        ScriptStackElement[] stack = e.getScriptStack();
        assertEquals(100, stack[0].lineNumber);
        assertEquals(16, stack[0].columnNumber);
    }

    /**
     * Breakpoint lines come from the LINE icodes, so the distinct-line set must stay exactly what
     * it was before positions carried columns.
     */
    @Test
    void debuggerLineNumbersAreUnchangedByColumnTracking() {
        try (Context cx = Context.enter()) {
            cx.setEvaluationMethod(Context.EvaluationMethod.Interpreter);
            cx.setGeneratingDebug(true);
            TopLevel scope = cx.initStandardObjects();
            final int[][] seen = new int[1][];
            cx.setDebugger(
                    (c, fnOrScript) -> {
                        if (seen[0] == null) {
                            seen[0] = fnOrScript.getLineNumbers();
                        }
                        return null;
                    },
                    null);
            cx.evaluateString(
                    scope, "var a = 1;\nvar b = 2; var c = 3;\na + b + c;", "t.js", 1, null);
            assertNotNull(seen[0]);
            int[] lines = seen[0].clone();
            Arrays.sort(lines);
            assertEquals("[1, 2, 3]", Arrays.toString(lines));
        }
    }
}
