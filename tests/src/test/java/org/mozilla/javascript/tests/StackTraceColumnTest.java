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
     * Two statements on one line. The interpreter records each position separately and reports the
     * exact one; the compiled backend can only key on the line the JVM gives back, so it reports
     * the column as unknown rather than guessing between them.
     */
    @Test
    public void oneLineWithTwoStatements() {
        String source = "function f() { var a = 1; throw new Error(a); }\nf();";

        ScriptStackElement[] interpreted = stackOf(source, EvaluationMethod.Interpreter);
        assertEquals(27, interpreted[0].columnNumber, "interpreter resolves the exact position");

        ScriptStackElement[] compiled = stackOf(source, EvaluationMethod.Compiler);
        assertEquals(0, compiled[0].columnNumber, "compiled reports unknown rather than a guess");
        assertEquals(1, compiled[0].lineNumber, "the line is still exact");
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
