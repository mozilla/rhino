/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context.EvaluationMethod;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.StackStyle;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

/**
 * @author Marc Guillemot
 */
public class StackTraceTest {

    static final String LS = System.getProperty("line.separator");

    /**
     * As of CVS head on May, 11. 2009, stacktrace information is lost when a call to some native
     * function has been made.
     */
    @Test
    public void failureStackTraceRhino() {
        final StackStyle stackStyle = RhinoException.getStackStyle();
        try {
            RhinoException.setStackStyle(StackStyle.RHINO);

            final String source1 = "function f2() { throw 'hello'; }; f2();";
            final String source2 = "function f2() { 'H'.toLowerCase(); throw 'hello'; }; f2();";
            final String source3 =
                    "function f2() { new java.lang.String('H').toLowerCase(); throw 'hello'; }; f2();";
            final String result = "\tat test.js:0 (f2)" + LS + "\tat test.js:0" + LS;

            runWithExpectedStackTrace(source1, result);
            runWithExpectedStackTrace(source2, result);
            runWithExpectedStackTrace(source3, result);
        } finally {
            RhinoException.setStackStyle(stackStyle);
        }
    }

    /**
     * As of CVS head on May, 11. 2009, stacktrace information is lost when a call to some native
     * function has been made.
     */
    @Test
    public void failureStackTraceMozilla() {
        final StackStyle stackStyle = RhinoException.getStackStyle();
        try {
            RhinoException.setStackStyle(StackStyle.MOZILLA);

            final String source1 = "function f2() { throw 'hello'; }; f2();";
            final String source2 = "function f2() { 'H'.toLowerCase(); throw 'hello'; }; f2();";
            final String source3 =
                    "function f2() { new java.lang.String('H').toLowerCase(); throw 'hello'; }; f2();";
            final String result = "f2()@test.js:0" + LS + "@test.js:0" + LS;

            runWithExpectedStackTrace(source1, result);
            runWithExpectedStackTrace(source2, result);
            runWithExpectedStackTrace(source3, result);
        } finally {
            RhinoException.setStackStyle(stackStyle);
        }
    }

    /**
     * As of CVS head on May, 11. 2009, stacktrace information is lost when a call to some native
     * function has been made.
     */
    @Test
    public void failureStackTraceMozillaLf() {
        final StackStyle stackStyle = RhinoException.getStackStyle();
        try {
            RhinoException.setStackStyle(StackStyle.MOZILLA_LF);

            final String source1 = "function f2() { throw 'hello'; }; f2();";
            final String source2 = "function f2() { 'H'.toLowerCase(); throw 'hello'; }; f2();";
            final String source3 =
                    "function f2() { new java.lang.String('H').toLowerCase(); throw 'hello'; }; f2();";
            final String result = "f2()@test.js:0\n@test.js:0\n";

            runWithExpectedStackTrace(source1, result);
            runWithExpectedStackTrace(source2, result);
            runWithExpectedStackTrace(source3, result);
        } finally {
            RhinoException.setStackStyle(stackStyle);
        }
    }

    /**
     * As of CVS head on May, 11. 2009, stacktrace information is lost when a call to some native
     * function has been made.
     */
    @Test
    public void failureStackTraceV8() {
        final StackStyle stackStyle = RhinoException.getStackStyle();
        try {
            RhinoException.setStackStyle(StackStyle.V8);

            final String source1 = "function f2() { throw 'hello'; }; f2();";
            final String source2 = "function f2() { 'H'.toLowerCase(); throw 'hello'; }; f2();";
            final String source3 =
                    "function f2() { new java.lang.String('H').toLowerCase(); throw 'hello'; }; f2();";
            // Unlike the other styles, V8 renders columns, so each source has its own: the
            // first is the "throw" keyword, the second the top-level "f2()" call.
            runWithExpectedStackTrace(source1, v8Result(17, 35));
            runWithExpectedStackTrace(source2, v8Result(36, 54));
            runWithExpectedStackTrace(source3, v8Result(58, 76));
        } finally {
            RhinoException.setStackStyle(stackStyle);
        }
    }

    private static String v8Result(int throwColumn, int callColumn) {
        return "hello"
                + LS
                + "    at f2 (test.js:0:"
                + throwColumn
                + ")"
                + LS
                + "    at test.js:0:"
                + callColumn
                + LS;
    }

    private static void runWithExpectedStackTrace(
            final String _source, final String _expectedStackTrace) {
        Utils.runWithMode(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    try {
                        cx.evaluateString(scope, _source, "test.js", 0, null);
                    } catch (final JavaScriptException e) {
                        assertEquals(_expectedStackTrace, e.getScriptStackTrace());
                        return null;
                    }
                    throw new RuntimeException("Exception expected!");
                },
                EvaluationMethod.Interpreter);
    }
}
