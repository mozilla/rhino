/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context.EvaluationMethod;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptStackElement;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

/** Columns in stack traces, in both evaluation modes. */
public class StackTraceColumnTest {

    private static ScriptStackElement[] stackOf(String source, EvaluationMethod mode) {
        ScriptStackElement[][] out = new ScriptStackElement[1][];
        Utils.runWithMode(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    RhinoException e =
                            assertThrows(
                                    RhinoException.class,
                                    () -> cx.evaluateString(scope, source, "t.js", 1, null));
                    out[0] = e.getScriptStack();
                    return null;
                },
                mode);
        return out[0];
    }

    /**
     * One statement per line, so every line carries a single position and both backends can report
     * an exact column.
     */
    @Test
    public void exactColumnsInBothModes() {
        String source = "function f() {\n  throw new Error('x');\n}\nf();";
        for (EvaluationMethod mode : EvaluationMethod.values()) {
            ScriptStackElement[] stack = stackOf(source, mode);
            assertEquals(2, stack[0].lineNumber, mode + " line of the throw");
            assertEquals(3, stack[0].columnNumber, mode + " column of the throw");
            assertEquals(4, stack[1].lineNumber, mode + " line of the call");
            assertEquals(1, stack[1].columnNumber, mode + " column of the call");
        }
    }

    /**
     * Two statements on one line, so the line alone cannot say which one threw. The compiled
     * backend gives the second position a marker in place of a line number and maps it back, so
     * both modes still report the exact column.
     */
    @Test
    public void oneLineWithTwoStatements() {
        String source = "function f() { var a = 1; throw new Error(a); }\nf();";
        for (EvaluationMethod mode : EvaluationMethod.values()) {
            ScriptStackElement[] stack = stackOf(source, mode);
            assertEquals(1, stack[0].lineNumber, mode + " line");
            assertEquals(27, stack[0].columnNumber, mode + " column of the throw");
        }
    }

    /** Many positions on one line, well past what a single line number could distinguish. */
    @Test
    public void oneLineWithManyStatements() {
        String source =
                "function f() { var a = 1; var b = 2; var c = 3; throw new Error(a); }\nf();";
        for (EvaluationMethod mode : EvaluationMethod.values()) {
            ScriptStackElement[] stack = stackOf(source, mode);
            assertEquals(1, stack[0].lineNumber, mode + " line");
            assertEquals(49, stack[0].columnNumber, mode + " column of the throw");
        }
    }

    /**
     * A failed property read is attributed to the property, not to the start of the statement or of
     * the expression. This is what V8 reports for the same code.
     */
    @Test
    public void failedReadReportsThePropertyColumn() {
        String source = "var foo = {};\nfoo.bar.baz();";
        for (EvaluationMethod mode : EvaluationMethod.values()) {
            ScriptStackElement[] stack = stackOf(source, mode);
            assertEquals(2, stack[0].lineNumber, mode + " line");
            assertEquals(9, stack[0].columnNumber, mode + " the column of 'baz'");
        }
    }

    /** Calling a non-function is attributed to the call, not to the last argument evaluated. */
    @Test
    public void callingANonFunctionReportsTheCallColumn() {
        String source = "var foo = {};\nvar arg = 1;\nfoo.nope(arg);";
        for (EvaluationMethod mode : EvaluationMethod.values()) {
            ScriptStackElement[] stack = stackOf(source, mode);
            assertEquals(3, stack[0].lineNumber, mode + " line");
            assertEquals(1, stack[0].columnNumber, mode + " the column of the call");
        }
    }

    @Test
    public void columnAppearsInV8RenderedStack() {
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    RhinoException e =
                            assertThrows(
                                    RhinoException.class,
                                    () ->
                                            cx.evaluateString(
                                                    scope,
                                                    "function f() {\n  throw new Error('x');\n}\nf();",
                                                    "t.js",
                                                    1,
                                                    null));
                    String rendered = e.getScriptStackTrace();
                    assertTrue(
                            rendered.contains("t.js:2"),
                            "expected a position for the throw, got: " + rendered);
                    return null;
                });
    }
}
