/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context.EvaluationMethod;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptStackElement;
import org.mozilla.javascript.StackStyle;
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

    /** Asserts where the throw is reported, in both modes, since they must not disagree. */
    private static void assertThrownAt(String source, int line, int column) {
        for (EvaluationMethod mode : EvaluationMethod.values()) {
            ScriptStackElement[] stack = stackOf(source, mode);
            assertEquals(line, stack[0].lineNumber, mode + " line");
            assertEquals(column, stack[0].columnNumber, mode + " column");
        }
    }

    /** One statement per line, so the line alone already identifies each position. */
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
     * Two statements on one line, so the line alone cannot say which one threw. The JVM backend
     * gives the second a marker in place of a line number and maps it back.
     */
    @Test
    public void oneLineWithTwoStatements() {
        assertThrownAt("function f() { var a = 1; throw new Error(a); }\nf();", 1, 27);
    }

    /** Many positions on one line, well past what a line number alone could distinguish. */
    @Test
    public void oneLineWithManyStatements() {
        assertThrownAt(
                "function f() { var a = 1; var b = 2; var c = 3; throw new Error(a); }\nf();",
                1,
                49);
    }

    /** A failed read blames the property, not the statement or the expression, as V8 does. */
    @Test
    public void failedReadReportsThePropertyColumn() {
        assertThrownAt("var foo = {};\nfoo.bar.baz();", 2, 9);
    }

    /** Calling a non-function blames the call, not the last argument evaluated. */
    @Test
    public void callingANonFunctionReportsTheCallColumn() {
        assertThrownAt("var foo = {};\nvar arg = 1;\nfoo.nope(arg);", 3, 1);
    }

    /** The JVM backend generates each call shape along its own path, so each needs covering. */
    @Test
    public void constructingANonConstructorReportsTheNewColumn() {
        assertThrownAt("var foo = {};\nvar arg = 1;\nnew foo.nope(arg);", 3, 1);
    }

    /** A call routed through the special-call path. */
    @Test
    public void aSpecialCallReportsTheCallColumn() {
        assertThrownAt("var o = {};\nfunction f() { return eval(o.nope.deeper); }\nf();", 2, 35);
    }

    /** A direct call to a top-level function, which the JVM backend specialises. */
    @Test
    public void aDirectCallReportsTheCallColumn() {
        String source =
                "function g(a) { return a.nope.deeper; }\nfunction f() { return g(1); }\nf();";
        for (EvaluationMethod mode : EvaluationMethod.values()) {
            ScriptStackElement[] stack = stackOf(source, mode);
            assertEquals(1, stack[0].lineNumber, mode + " callee line");
            assertEquals(2, stack[1].lineNumber, mode + " caller line");
            assertEquals(23, stack[1].columnNumber, mode + " column of the call to g");
        }
    }

    /** Only the V8 style renders a column, as file:line:column. */
    @Test
    public void columnAppearsInV8RenderedStack() {
        StackStyle style = RhinoException.getStackStyle();
        try {
            RhinoException.setStackStyle(StackStyle.V8);
            String ls = System.lineSeparator();
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
                        assertEquals(
                                "Error: x"
                                        + ls
                                        + "    at f (t.js:2:3)"
                                        + ls
                                        + "    at t.js:4:1"
                                        + ls,
                                e.getScriptStackTrace());
                        return null;
                    });
        } finally {
            RhinoException.setStackStyle(style);
        }
    }
}
