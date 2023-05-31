/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;

/**
 * Unit tests to check error handling. Especially, we expect to get a correct cause, when an error
 * happened in Java.
 *
 * @author Roland Praml
 */
public class ErrorHandlingTest {

    public static void generateJavaError() {
        throw new java.lang.RuntimeException("foo");
    }

    // string contains stack element with correct line number. e.g:
    // ' at
    // org.mozilla.javascript.tests.ErrorHandlingTest.generateJavaError(ErrorHandlingTest.java:30)'
    private static final String EXPECTED_LINE_IN_STACK = findLine();

    @Test
    public void throwError() {
        testIt(
                "throw new Error('foo')",
                e -> {
                    Assert.assertEquals(JavaScriptException.class, e.getClass());
                    Assert.assertEquals("Error: foo (myScript.js#1)", e.getMessage());
                });
        testIt(
                "try { throw new Error('foo') } catch (e) { throw e }",
                e -> {
                    Assert.assertEquals(JavaScriptException.class, e.getClass());
                    Assert.assertEquals("Error: foo (myScript.js#1)", e.getMessage());
                });
    }

    @Test
    public void javaErrorFromInvocation() {

        testIt(
                "org.mozilla.javascript.tests.ErrorHandlingTest.generateJavaError()",
                e -> {
                    Assert.assertEquals(WrappedException.class, e.getClass());
                    Assert.assertEquals(
                            "Wrapped java.lang.RuntimeException: foo (myScript.js#1)",
                            e.getMessage());
                    Assert.assertEquals(RuntimeException.class, e.getCause().getClass());
                    Assert.assertEquals("foo", e.getCause().getMessage());
                    Assert.assertTrue(stackToLines(e).contains(EXPECTED_LINE_IN_STACK));
                });
        testIt(
                "try { org.mozilla.javascript.tests.ErrorHandlingTest.generateJavaError() } catch (e) { throw e }",
                e -> {
                    Assert.assertEquals(JavaScriptException.class, e.getClass());
                    Assert.assertEquals(
                            "JavaException: java.lang.RuntimeException: foo (myScript.js#1)",
                            e.getMessage());
                    Assert.assertEquals(RuntimeException.class, e.getCause().getClass());
                    Assert.assertEquals("foo", e.getCause().getMessage());
                    Assert.assertTrue(stackToLines(e).contains(EXPECTED_LINE_IN_STACK));
                });
    }

    @Test
    public void javaErrorThrown() {

        testIt(
                "throw new java.lang.RuntimeException('foo')",
                e -> {
                    Assert.assertEquals(JavaScriptException.class, e.getClass());
                    Assert.assertEquals(
                            "java.lang.RuntimeException: foo (myScript.js#1)", e.getMessage());
                    Assert.assertEquals(RuntimeException.class, e.getCause().getClass());
                    Assert.assertEquals("foo", e.getCause().getMessage());
                });
        testIt(
                "try { throw new java.lang.RuntimeException('foo') } catch (e) { throw e }",
                e -> {
                    Assert.assertEquals(JavaScriptException.class, e.getClass());
                    Assert.assertEquals(
                            "java.lang.RuntimeException: foo (myScript.js#1)", e.getMessage());
                    Assert.assertEquals(RuntimeException.class, e.getCause().getClass());
                    Assert.assertEquals("foo", e.getCause().getMessage());
                });
    }

    private void testIt(final String script, final Consumer<Throwable> exception) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    try {
                        final ScriptableObject scope = cx.initStandardObjects();
                        cx.evaluateString(scope, script, "myScript.js", 1, null);
                        Assert.fail("No error was thrown");
                    } catch (final Throwable t) {
                        exception.accept(t);
                    }
                    return null;
                });
    }

    static List<String> stackToLines(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String[] tmp = sw.toString().replace("\r\n", "\n").split("\n");
        return Arrays.asList(tmp);
    }

    private static String findLine() {
        try {
            generateJavaError();
        } catch (Throwable t) {
            for (String line : stackToLines(t)) {
                if (line.contains("generateJavaError")) {
                    System.out.println(line);
                    return line;
                }
            }
        }
        throw new UnsupportedOperationException("Did not find expected line");
    }
}
