package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.*;
import org.mozilla.javascript.testutils.Utils;

public class GeneratorStackTraceTest {

    static final String LS = System.getProperty("line.separator");

    @Test
    public void testGeneratorStackTraces() {
        String script =
                "function* f2() { yield 1; throw 'hello'; yield 3; }; var g = f2(); g.next(); g.next();";
        String expected = "\tat test.js:0 (f2)" + LS + "\tat test.js:0" + LS;
        runWithExpectedStackTrace(script, expected);
    }

    private static void runWithExpectedStackTrace(final String _source, final String expected) {
        Utils.runWithAllModes(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    try {
                        cx.evaluateString(scope, _source, "test.js", 0, null);
                    } catch (final JavaScriptException e) {
                        assertEquals(expected, e.getScriptStackTrace());
                        return null;
                    }
                    throw new RuntimeException("Exception expected!");
                });
    }

    @Test
    public void testNestedGeneratorsException() {
        String jsCode =
                ""
                        + "function* innerGen() {\n"
                        + "    yield 1;\n"
                        + "    throw new Error('Inner Exception'); // Line 4\n"
                        + "}\n"
                        + "function* outerGen() {\n"
                        + "    var f = innerGen();\n"
                        + "    yield f.next().value;\n"
                        + "    yield f.next().value;\n"
                        + "}\n"
                        + "var g = outerGen();\n"
                        + "g.next();\n"
                        + "g.next();"; // This should throw

        Utils.runWithAllModes(
                context -> {
                    try {
                        final Scriptable scope = context.initStandardObjects();
                        context.evaluateString(scope, jsCode, "nestedGeneratorTest.js", 1, null);
                        Assert.fail("Expected exception from nested generator not thrown.");
                    } catch (JavaScriptException e) {
                        String stack = e.getScriptStackTrace();

                        // Validate that both generator functions appear in the stack
                        Assert.assertTrue(
                                "Stack trace should include innerGen", stack.contains("innerGen"));
                        Assert.assertTrue(
                                "Stack trace should include outerGen", stack.contains("outerGen"));

                        // Validate line number for the throw
                        assertEquals("Line number of exception should be 3", 3, e.lineNumber());

                        System.out.println("Nested Generator Exception Stack: \n" + stack);
                    }
                    return null;
                });
    }

    // Doesn't work due to our implementation of yield*.
    @Test(expected = AssertionError.class)
    public void testNestedGeneratorsYieldStarException() {
        String jsCode =
                ""
                        + "function* innerGen() {\n"
                        + "    yield 1;\n"
                        + "    throw new Error('Inner Exception'); // Line 4\n"
                        + "}\n"
                        + "function* outerGen() {\n"
                        + "    yield* innerGen();\n"
                        + "}\n"
                        + "var g = outerGen();\n"
                        + "g.next();\n"
                        + "g.next();"; // This should throw

        Utils.runWithAllModes(
                context -> {
                    try {
                        final Scriptable scope = context.initStandardObjects();
                        context.evaluateString(scope, jsCode, "nestedGeneratorTest.js", 1, null);
                        Assert.fail("Expected exception from nested generator not thrown.");
                    } catch (JavaScriptException e) {
                        String stack = e.getScriptStackTrace();

                        // Validate that both generator functions appear in the stack
                        Assert.assertTrue(
                                "Stack trace should include innerGen", stack.contains("innerGen"));
                        Assert.assertTrue(
                                "Stack trace should include outerGen", stack.contains("outerGen"));

                        // Validate line number for the throw
                        assertEquals("Line number of exception should be 3", 3, e.lineNumber());

                        System.out.println("Nested Generator Exception Stack: \n" + stack);
                    }
                    return null;
                });
    }

    @Test
    public void testJavaCallbackThrowsFromGenerator() {
        Utils.runWithAllModes(
                context -> {
                    Scriptable scope = context.initStandardObjects();
                    LambdaFunction f =
                            new LambdaFunction(
                                    scope,
                                    "javaHelper",
                                    0,
                                    (Context ctx,
                                            Scriptable scope2,
                                            Scriptable thisObj,
                                            Object[] args) -> {
                                        throw new RuntimeException("Java-side failure!");
                                    });
                    ScriptableObject.putProperty(scope, "javaHelper", f);

                    String jsCode =
                            ""
                                    + "function* generatorWithCallback() {\n"
                                    + "    yield 1;\n"
                                    + "    javaHelper();\n"
                                    + // This should throw from Java
                                    "}\n"
                                    + "var g = generatorWithCallback();\n"
                                    + "g.next();\n"
                                    + // Move to the first yield
                                    "g.next();"; // This should throw

                    try {
                        context.evaluateString(scope, jsCode, "javaCallbackTest.js", 1, null);
                        Assert.fail("Expected Java exception not thrown.");
                    } catch (RuntimeException e) {
                        // Rhino should surface the Java exception directly
                        Assert.assertTrue(
                                "Exception message should contain 'Java-side failure!'",
                                e.getMessage().contains("Java-side failure!"));
                        System.out.println("Caught Java Exception from JS: " + e.getMessage());
                    }
                    return null;
                });
    }
}
