/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.StackStyle;

/** @author Marc Guillemot */
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
            final String result = "\tat test.js (f2)" + LS + "\tat test.js" + LS;

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
            final String result = "f2()@test.js" + LS + "@test.js" + LS;

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
            final String result = "f2()@test.js\n@test.js\n";

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
            final String result =
                    "hello" + LS + "    at f2 (test.js:0:0)" + LS + "    at test.js:0:0" + LS;

            runWithExpectedStackTrace(source1, result);
            runWithExpectedStackTrace(source2, result);
            runWithExpectedStackTrace(source3, result);
        } finally {
            RhinoException.setStackStyle(stackStyle);
        }
    }

    private static void runWithExpectedStackTrace(
            final String _source, final String _expectedStackTrace) {
        Utils.runWithOptimizationLevel(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    try {
                        cx.evaluateString(scope, _source, "test.js", 0, null);
                    } catch (final JavaScriptException e) {
                        assertEquals(_expectedStackTrace, e.getScriptStackTrace());
                        return null;
                    }
                    throw new RuntimeException("Exception expected!");
                },
                -1);
    }
}
