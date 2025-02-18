/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class ComputedPropertiesTest {
    @Test
    public void objectWithComputedPropertiesWorks() {
        Utils.assertWithAllModes_ES6(
                10,
                Utils.lines(
                        "function f(x) { return x; }",
                        "var o = {",
                        "  a: 1,",
                        "  0: 2,",
                        "  [-1]: 3,",
                        "  [f('b')]: 4",
                        "};",
                        "o.a + o[0] + o['-1'] + o.b"));
    }

    @Test
    public void canCoerceFunctionToString() {
        String script =
                Utils.lines(
                        "function f(x) {",
                        "  var o = {",
                        "    1: true,",
                        "    [2]: false,",
                        "    [g(x)]: 3",
                        "  };",
                        "}",
                        "f.toString()");
        String expected =
                Utils.lines(
                        "function f(x) {",
                        "  var o = {",
                        "    1: true,",
                        "    [2]: false,",
                        "    [g(x)]: 3",
                        "  };",
                        "}");

        Utils.assertWithAllModes_ES6(expected, script);
    }

    @Test
    public void computedPropertiesWithSideEffectsWork() {
        String script =
                Utils.lines(
                        "'use strict';",
                        "var x = 0;",
                        "var o = {",
                        "  [++x]: 'x',",
                        "  a: ++x,",
                        "  [++x]: 'y'",
                        "};",
                        "o[1] + o.a + o[3]");
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
                Utils.lines(
                        "function *gen() {",
                        " ({x: yield 1});",
                        "}",
                        "var g = gen()",
                        "var res1 = g.next();",
                        "var res2 = g.next();",
                        "res1.value === 1 && !res1.done && res2.done");

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
