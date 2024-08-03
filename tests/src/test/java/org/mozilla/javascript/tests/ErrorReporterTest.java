/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptableObject;

public class ErrorReporterTest {

    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() throws Exception {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void errorInfo() {
        ErrorCollector errorCollector = new ErrorCollector();
        cx.setErrorReporter(errorCollector);

        try {
            cx.evaluateString(scope, "0xGGGG", "", 1, null);
        } catch (EvaluatorException e) {
        }

        assertTrue(errorCollector.reportedRuntimeError);
        assertEquals(1, errorCollector.errors.size());

        ErrorInfo errorInfo = errorCollector.errors.get(0);
        assertEquals("number format error", errorInfo.message);
        assertEquals(1, errorInfo.line);
        assertEquals("0xGGGG", errorInfo.lineSource);
    }

    @Test
    public void errorLine() {
        ErrorCollector errorCollector = new ErrorCollector();
        cx.setErrorReporter(errorCollector);

        try {
            cx.evaluateString(
                    scope,
                    "var a = 'a';\nvar b = true;\nvar c = 0xGGGG;\n var d = 123;",
                    "",
                    1,
                    null);
        } catch (EvaluatorException e) {
        }

        assertTrue(errorCollector.reportedRuntimeError);
        assertEquals(2, errorCollector.errors.size());

        ErrorInfo error1 = errorCollector.errors.get(0);
        assertEquals("number format error", error1.message);
        assertEquals(3, error1.line);
        assertEquals("var c = 0xGGGG;", error1.lineSource);

        ErrorInfo error2 = errorCollector.errors.get(1);
        assertEquals("missing ; before statement", error2.message);
        assertEquals(3, error2.line);
        assertEquals("var c = 0xGGGG;", error2.lineSource);
    }

    @Test
    public void catchParserExceptionInIRFactory() {
        ErrorCollector errorCollector = new ErrorCollector();
        cx.setErrorReporter(errorCollector);

        try {
            // The test for errors in re-parse with IRFactory after parsing with Parser.
            cx.evaluateString(scope, "var a = 123;\n1=1", "", 1, null);
        } catch (EvaluatorException e) {
        }

        assertTrue(errorCollector.reportedRuntimeError);
        assertEquals(1, errorCollector.errors.size());

        ErrorInfo errorInfo = errorCollector.errors.get(0);
        assertEquals("Invalid assignment left-hand side.", errorInfo.message);
        assertEquals(2, errorInfo.line);
        assertEquals("1=1", errorInfo.lineSource);
    }

    @Test
    public void errorLineInIRFactory() {
        ErrorCollector errorCollector = new ErrorCollector();
        cx.setErrorReporter(errorCollector);

        cx.evaluateString(scope, "var a = 123;\n\nfor (let [x, x] in {}) {}", "", 1, null);

        assertEquals(1, errorCollector.errors.size());

        ErrorInfo errorInfo = errorCollector.errors.get(0);
        assertEquals("redeclaration of variable x.", errorInfo.message);
        assertEquals(3, errorInfo.line);
        assertEquals("for (let [x, x] in {}) {}", errorInfo.lineSource);
    }

    private static class ErrorInfo {
        String message;
        int line;
        String lineSource;

        ErrorInfo(String message, int line, String lineSource) {
            this.message = message;
            this.line = line;
            this.lineSource = lineSource;
        }
    }

    private static class ErrorCollector implements ErrorReporter {
        ArrayList<ErrorInfo> errors;
        ArrayList<ErrorInfo> warnings;
        boolean reportedRuntimeError = false;

        ErrorCollector() {
            errors = new ArrayList<>();
            warnings = new ArrayList<>();
        }

        @Override
        public void error(
                String message, String sourceName, int line, String lineSource, int lineOffset) {
            errors.add(new ErrorInfo(message, line, lineSource));
        }

        @Override
        public void warning(
                String message, String sourceName, int line, String lineSource, int lineOffset) {
            warnings.add(new ErrorInfo(message, line, lineSource));
        }

        @Override
        public EvaluatorException runtimeError(
                String message, String sourceName, int line, String lineSource, int lineOffset) {
            reportedRuntimeError = true;
            return new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
        }
    }
}
