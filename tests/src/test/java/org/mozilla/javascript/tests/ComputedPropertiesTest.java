/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptableObject;

public class ComputedPropertiesTest {
    @Test
    public void objectWithComputedPropertiesWorkInInterpretedMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1);
            assertObjectWithMixedPropertiesWorks(cx);
        }
    }

    @Test
    public void objectWithComputedPropertiesWorkInCompiledMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(0);
            assertObjectWithMixedPropertiesWorks(cx);
        }
    }

    @Test
    public void objectWithComputedPropertiesWorkInOptimizedMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(9);
            assertObjectWithMixedPropertiesWorks(cx);
        }
    }

    private static void assertObjectWithMixedPropertiesWorks(Context cx) {
        String script = "\n" +
                "function f(x) { return x; }\n" +
                "\n" +
                "var o = {\n" +
                "  a: 1,\n" +
                "  0: 2,\n" +
                "  [1]: 3\n," +
                "  [f('b')]: 4\n" +
                "};\n" +
                "o.a + o[0] + o['1'] + o.b";

        ScriptableObject scope = cx.initStandardObjects();
        Object value = cx.evaluateString(scope, script, "test", 1, null);
        assertTrue(value instanceof Number);
        assertEquals(10, ((Number) value).intValue());
    }

    @Test
    public void canCoerceFunctionToStringInInterpretedMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1);
            assertCanCoerceFunctionWithComputedPropertiesToString(cx);
        }
    }

    @Test
    public void canCoerceFunctionToStringInCompiledMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(0);
            assertCanCoerceFunctionWithComputedPropertiesToString(cx);
        }
    }

    private static void assertCanCoerceFunctionWithComputedPropertiesToString(Context cx) {
        String script = "\n" +
                "function f(x) {\n" +
                "  var o = {\n" +
                "    1: true,\n" +
                "    [2]: false,\n" +
                "    [g(x)]: 3\n" +
                "  };\n" +
                "}\n" +
                "f.toString()";

        ScriptableObject scope = cx.initStandardObjects();
        Object value = cx.evaluateString(scope, script, "test", 1, null);
        assertTrue(value instanceof String);
        assertEquals("\nfunction f(x) {\n" +
                "    var o = {1: true, [2]: false, [g(x)]: 3};\n" +
                "}\n", value);
    }

    @Test
    public void computedPropertiesWithSideEffectsWorkInInterpretedMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1);
            assertComputedPropertiesWithSideEffectsWork(cx);
        }
    }

    @Test
    public void computedPropertiesWithSideEffectsWorkInCompiledMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(0);
            assertComputedPropertiesWithSideEffectsWork(cx);
        }
    }

    @Test
    public void computedPropertiesWithSideEffectsWorkInOptimizedMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(9);
            assertComputedPropertiesWithSideEffectsWork(cx);
        }
    }

    private static void assertComputedPropertiesWithSideEffectsWork(Context cx) {
        String script = "'use strict';\n" +
                "var x = 0;\n" +
                "var o = {\n" +
                "  [++x]: 'x',\n" +
                "  a: ++x,\n" +
                "  [++x]: 'y'\n" +
                "};\n" +
                "o[1] + o.a + o[3]";

        ScriptableObject scope = cx.initStandardObjects();
        Object value = cx.evaluateString(scope, script, "test", 1, null);
        assertEquals("x2y", value);
    }

    @Test
    public void computedPropertyNameForGetterSetterWorkInInterpretedMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1);
            assertComputedPropertyNameForGetterSetterWorks(cx);
        }
    }

    @Test
    public void computedPropertyNameForGetterSetterWorkInCompiled() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(0);
            assertComputedPropertyNameForGetterSetterWorks(cx);
        }
    }

    @Test
    public void computedPropertyNameForGetterSetterWorkInOptimizedMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(9);
            assertComputedPropertyNameForGetterSetterWorks(cx);
        }
    }

    private static void assertComputedPropertyNameForGetterSetterWorks(Context cx) {
        String script = "\n" +
                "var o = {\n" +
                " get ['x' + 1]() { return 42; }\n" +
                "};\n" +
                "o.x1";

        ScriptableObject scope = cx.initStandardObjects();
        Object value = cx.evaluateString(scope, script, "test", 1, null);
        assertTrue(value instanceof Number);
        assertEquals(42, ((Number) value).intValue());
    }

    @Test
    public void yieldWorksForPropertyValuesInInterpretedMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1);
            assertYieldWorksForPropertyValues(cx);
        }
    }

    @Test
    public void yieldWorksForPropertyValuesInCompiledMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(0);
            assertYieldWorksForPropertyValues(cx);
        }
    }

    @Test
    public void yieldWorksForPropertyValuesInOptimizedMode() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(9);
            assertYieldWorksForPropertyValues(cx);
        }
    }

    private static void assertYieldWorksForPropertyValues(Context cx) {
        String script =
                "\n"
                        + "function *gen() {\n"
                        + " ({x: yield 1});\n"
                        + "}\n"
                        + "var g = gen()\n"
                        + "var res1 = g.next();\n"
                        + "var res2 = g.next();\n"
                        + "res1.value === 1 && !res1.done && res2.done\n";

        ScriptableObject scope = cx.initStandardObjects();
        Object value = cx.evaluateString(scope, script, "test", 1, null);
        assertEquals(Boolean.TRUE, value);
    }

    @Test
    public void cannotParseInvalidUnclosedBracket() {
        String script = "o = { [3 : 2 }";

        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            EvaluatorException ex = assertThrows(EvaluatorException.class, () ->
                    cx.compileString(script, "test", 1, null));
            assertEquals("invalid property id (test#1)", ex.getMessage());
        }
    }

    @Test
    public void notSupportedUnderVersionLesserThanEsLatest() {
        String script = "o = { [1] : 2 }";

        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_1_8);
            EvaluatorException ex = assertThrows(EvaluatorException.class, () ->
                    cx.compileString(script, "test", 1, null));
            assertEquals("invalid property id (test#1)", ex.getMessage());
        }
    }
}
