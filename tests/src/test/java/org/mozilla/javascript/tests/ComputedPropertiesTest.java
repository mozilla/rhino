/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;

public class ComputedPropertiesTest {
    @Test
    public void objectWithComputedPropertiesWorks() {
        String script =
                "function f(x) { return x; }\n"
                        + "var o = {\n"
                        + "  a: 1,\n"
                        + "  0: 2,\n"
                        + "  [-1]: 3\n,"
                        + "  [f('b')]: 4\n"
                        + "};\n"
                        + "o.a + o[0] + o['-1'] + o.b";
        Utils.assertWithAllModes_ES6(10, script);
    }

    @Test
    public void canCoerceFunctionToString() {
        String script =
                "function f(x) {\n"
                        + "  var o = {\n"
                        + "    1: true,\n"
                        + "    [2]: false,\n"
                        + "    [g(x)]: 3\n"
                        + "  };\n"
                        + "}\n"
                        + "f.toString()";
        String expected =
                "function f(x) {\n"
                        + "  var o = {\n"
                        + "    1: true,\n"
                        + "    [2]: false,\n"
                        + "    [g(x)]: 3\n"
                        + "  };\n"
                        + "}";

        Utils.assertWithAllModes_ES6(expected, script);
    }

    @Test
    public void computedPropertiesWithSideEffectsWork() {
        String script =
                "'use strict';\n"
                        + "var x = 0;\n"
                        + "var o = {\n"
                        + "  [++x]: 'x',\n"
                        + "  a: ++x,\n"
                        + "  [++x]: 'y'\n"
                        + "};\n"
                        + "o[1] + o.a + o[3]";
        Utils.assertWithAllModes_ES6("x2y", script);
    }

    @Test
    public void computedPropertyNameForGetterSetterWork() {
        Utils.assertWithAllModes_ES6(42, "var o = { get ['x' + 1]() { return 42; }}; o.x1");
    }

    @Test
    public void computedPropertyNameAsSymbolForGetterSetterWork() {
        Utils.assertWithAllModes_ES6(
                "[object foo]",
                "var o = { get [Symbol.toStringTag]() { return 'foo'; }}; o.toString()");
    }

    @Test
    public void yieldWorksForPropertyValues() {
        String script =
                "function *gen() {\n"
                        + " ({x: yield 1});\n"
                        + "}\n"
                        + "var g = gen()\n"
                        + "var res1 = g.next();\n"
                        + "var res2 = g.next();\n"
                        + "res1.value === 1 && !res1.done && res2.done\n";

        Utils.assertWithAllModes_ES6(Boolean.TRUE, script);
    }

    @Test
    public void cannotParseInvalidUnclosedBracket() {
        Utils.assertEvaluatorExceptionES6("invalid property id (test#1)", "o = { [3 : 2 }");
    }

    @Test
    public void notSupportedUnderVersionLesserThanEsLatest() {
        Utils.assertEvaluatorException_1_8("invalid property id (test#1)", "o = { [1] : 2 }");
    }

    @Test
    public void unsupportedInDestructuringInFunctionArguments() {
        Utils.assertEvaluatorExceptionES6(
                "Unsupported computed property in destructuring. (test#1)",
                "function({ [a]: b }) {};");
    }

    @Test
    public void unsupportedInDestructuringInVariableDeclaration() {
        Utils.assertEvaluatorExceptionES6(
                "Unsupported computed property in destructuring. (test#1)", "var { [a]: b } = {};");
    }
}
