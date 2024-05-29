package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Scriptable;

public class DefaultParametersTest {
    @Test
    public void functionDefaultArgsBasic() throws Exception {
        final String script = "function foo(a = 2) {" + "   return a;" + "}";
        assertIntEvaluates(32, script + "\nfoo(32)");
        assertIntEvaluates(2, script + "\nfoo()");
        assertIntEvaluates(2, script + "\nfoo(undefined)");
    }

    @Test
    public void functionDefaultArgsBasicArrow() throws Exception {
        final String script = "((a = 2, b) => { return a; })";
        assertIntEvaluates(32, script + "(32, 12)");
        assertIntEvaluates(12, script + "(12)");
        assertIntEvaluates(2, script + "()");
    }

    @Test
    public void functionDefaultArgsMulti() throws Exception {
        final String script = "function foo(a = 2, b = 23) {" + "   return a + b;" + "}";
        assertIntEvaluates(55, script + "\nfoo(32)");
        assertIntEvaluates(25, script + "\nfoo()");
        assertIntEvaluates(34, script + "\nfoo(32, 2)");
        assertIntEvaluates(25, script + "\nfoo(undefined, undefined)");
    }

    @Test
    public void functionDefaultArgsUsage() throws Exception {
        final String script = "function foo(a = 2, b = a * 2) {" + "   return a + b;" + "}";
        assertIntEvaluates(96, script + "\nfoo(32)");
        assertIntEvaluates(6, script + "\nfoo()");
        assertIntEvaluates(34, script + "\nfoo(32, 2)");
    }

    @Test
    public void functionDefaultArgsMultiFollowUsage() throws Exception {
        final String script =
                "function f(a = go()) {\n"
                        + "  function go() {\n"
                        + "    return \":P\";\n"
                        + "  }\n"
                        + "}\n"
                        + "\n";
        assertThrows("go is not defined", script + "\nf()");
    }

    @Test
    public void functionDefaultArgsArray() throws Exception {
        final String script =
                "function append(value, array = []) {\n"
                        + "  array.push(value);\n"
                        + "  return array;\n"
                        + "}\n"
                        + "\n";
        assertIntEvaluates(1, script + "append(1)[0]");
        assertIntEvaluates(2, script + "append(2)[0]");
    }

    private static void assertThrows(final String expected, final String source) {
        assertThrowsWithLanguageLevel(expected, source, Context.VERSION_ES6);
    }

    private static void assertThrowsWithLanguageLevel(
            String expected, final String source, int languageLevel) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    int oldVersion = cx.getLanguageVersion();
                    cx.setLanguageVersion(languageLevel);
                    try {
                        final Scriptable scope = cx.initStandardObjects();
                        final Object rep = cx.evaluateString(scope, source, "test.js", 0, null);
                        if (rep instanceof EcmaError) {
                            assertEquals(((EcmaError) rep).getMessage(), expected);
                        }
                        fail();
                        return null;
                    } finally {
                        cx.setLanguageVersion(oldVersion);
                    }
                });
    }

    private static void assertIntEvaluates(final Object expected, final String source) {
        assertIntEvaluatesWithLanguageLevel(expected, source, Context.VERSION_ES6);
    }

    private static void assertIntEvaluatesWithLanguageLevel(
            final Object expected, final String source, int languageLevel) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    int oldVersion = cx.getLanguageVersion();
                    cx.setLanguageVersion(languageLevel);
                    try {
                        final Scriptable scope = cx.initStandardObjects();
                        final Object rep = cx.evaluateString(scope, source, "test.js", 0, null);
                        if (rep instanceof Double)
                            assertEquals((int) expected, ((Double) rep).intValue());
                        else assertEquals(expected, rep);
                        return null;
                    } finally {
                        cx.setLanguageVersion(oldVersion);
                    }
                });
    }
}
