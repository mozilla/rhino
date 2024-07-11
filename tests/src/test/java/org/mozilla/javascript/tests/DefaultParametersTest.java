package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/*
   Many of these are taken from examples at developer.mozilla.org
*/
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
    @Ignore("temporal-dead-zone")
    public void functionDefaultArgsMultiFollowUsage() throws Exception {
        final String script =
                "function f(a = go()) {\n"
                        + "  function go() {\n"
                        + "    return \":P\";\n"
                        + "  }\n"
                        + " return a; "
                        + "}\n"
                        + "\n";
        assertIntEvaluates(24, script + "\nf(24)");
        assertThrows(
                "ReferenceError: \"go\" is not defined.", "function f() { go() }; var f1 = f()");
        assertThrows("ReferenceError: \"go\" is not defined.", script + "\nf()");
    }

    @Test
    @Ignore("temporal-dead-zone")
    public void functionDefaultArgsMultiReferEarlier() throws Exception {
        final String script = "var f = function(a = b * 2, b = 3) { return a * b; }\n";
        assertThrows("ReferenceError: \"b\" is not defined.", script + "\nf()");
    }

    @Test
    public void functionConstructor() throws Exception {
        final String script = "const f = new Function('a=2', 'b=a', 'return a + b');";
        assertIntEvaluates(4, script + "f()");
        assertIntEvaluates(6, script + "f(3)");
        assertIntEvaluates(16, script + "f(3, 13)");
    }

    @Test
    @Ignore("destructuring-not-supported")
    public void destructuringAssigmentDefaultArray() throws Exception {
        final String script =
                "function f([x = 1, y = 2] = [], [z = 1] = [4]) {\n"
                        + "  return x + y + z;\n"
                        + "}";

        assertIntEvaluates(3, script + "f()");
        assertIntEvaluates(3, script + "f([])");
        assertIntEvaluates(4, script + "f([2])");
        assertIntEvaluates(5, script + "f([2, 3])");
    }

    @Test
    @Ignore("destructuring-not-supported")
    public void destructuringAssigmentBasicArray() throws Exception {
        final String script = "function f([x = 1] = [2]) {\n" + "  return x;\n" + "}";

        //        assertIntEvaluates(1, script + "f([])");
        assertIntEvaluates(2, script + "f()");
        //        assertIntEvaluates(3, script + "f([3])");
    }

    @Test
    @Ignore("destructuring-not-supported")
    public void destructuringAssigmentBasicObject() throws Exception {
        final String script = "function f({x = 1} = {x: 2}) {\n" + "  return x;\n" + "}";

        assertIntEvaluates(1, script + "f({})");
        assertIntEvaluates(2, script + "f()");
        assertIntEvaluates(3, script + "f({x: 3})");
    }

    @Test
    @Ignore("destructuring-not-supported")
    public void destructuringAssigmentDefaultObject() throws Exception {
        final String script = "function f({ z = 3, x = 2 } = {}) {\n" + "  return z;\n" + "}\n";
        assertIntEvaluates(3, script + "f()");
        assertIntEvaluates(3, script + "f({})");
        assertIntEvaluates(2, script + "f({z: 2})");
    }

    @Test
    public void defaultParametersWithArgumentsObject() throws Exception {
        final String script =
                "function f(a = 55) {\n"
                        + "  arguments[0] = 99; // updating arguments[0] does not also update a\n"
                        + "  return a;\n"
                        + "}\n"
                        + "\n"
                        + "function g(a = 55) {\n"
                        + "  a = 99; // updating a does not also update arguments[0]\n"
                        + "  return arguments[0];\n"
                        + "}\n"
                        + "\n"
                        + "// An untracked default parameter\n"
                        + "function h(a = 55) {\n"
                        + "  return arguments.length;\n"
                        + "}\n";
        assertIntEvaluates(10, script + "f(10)");
        assertIntEvaluates(55, script + "f()");
        assertIntEvaluates(10, script + "g(10)");
        assertEvaluates(Undefined.instance, script + "g()");
        assertIntEvaluates(0, script + "h()");
        assertIntEvaluates(1, script + "h(10)");
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

    @Test
    public void functionDefaultArgsObject() throws Exception {
        final String script =
                "function append(key, value, obj = {}) {\n"
                        + "  obj[key]=value;\n"
                        + "  return obj;\n"
                        + "}\n"
                        + "\n";
        assertIntEvaluates(1, script + "append('a', 1)['a']");
        assertIntEvaluates(2, script + "append('a', 2)['a']");
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
                        try {
                            final Object rep = cx.evaluateString(scope, source, "test.js", 0, null);
                        } catch (EcmaError e) {
                            if (e instanceof EcmaError) {
                                assertTrue(((EcmaError) e).getMessage().startsWith(expected));
                                return true;
                            }
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

    private static void assertEvaluates(final Object expected, final String source) {
        assertIntEvaluatesWithLanguageLevel(expected, source, Context.VERSION_ES6);
    }

    private static void assertIntEvaluatesWithLanguageLevel(
            final Object expected, final String source, int languageLevel) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    if (cx.getOptimizationLevel() == -1) {
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
                    }
                    return null;
                });
    }
}
