/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tests.Utils;

/**
 * Tests for Functions Rest Parameters.
 *
 * @author Ronald Brill
 */
public class FunctionsRestParametersTest {

    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void oneRestArg() {
        String code =
                "function rest(...theArgs) {\n"
                        + "  return theArgs;\n"
                        + "}\n"
                        + "try {\n"
                        + "  rest(1, 'abc', 2, '##').toString();\n"
                        + "} catch (e) { e.message }";

        test("1,abc,2,##", code);
    }

    @Test
    public void oneRestArgNothingProvided() {
        String code =
                "function rest(...theArgs) {\n"
                        + "  return theArgs;\n"
                        + "}\n"
                        + "try {\n"
                        + "  var r = rest();\n"
                        + "  '' + Array.isArray(r) + '-' + r.length;\n"
                        + "} catch (e) { e.message }";

        test("true-0", code);
    }

    @Test
    public void oneRestArgOneProvided() {
        String code =
                "function rest(...theArgs) {\n"
                        + "  return theArgs;\n"
                        + "}\n"
                        + "try {\n"
                        + "  var r = rest('xy');\n"
                        + "  '' + Array.isArray(r) + '-' + r.length;\n"
                        + "} catch (e) { e.message }";

        test("true-1", code);
    }

    @Test
    public void twoRestArg() {
        String code =
                "function rest(v, ...theArgs) {\n"
                        + "  return theArgs;\n"
                        + "}\n"
                        + "try {\n"
                        + "  rest(1, 'abc', 2, '##').toString();\n"
                        + "} catch (e) { e.message }";

        test("abc,2,##", code);
    }

    @Test
    public void twoRestArgNothingProvided() {
        String code =
                "function rest(v, ...theArgs) {\n"
                        + "  return '' + typeof v + ' - ' + Array.isArray(theArgs) + '-' + theArgs.length;\n"
                        + "}\n"
                        + "try {\n"
                        + "  rest();\n"
                        + "} catch (e) { e.message }";

        test("undefined - true-0", code);
    }

    @Test
    public void twoRestArgOneProvided() {
        String code =
                "function rest(v, ...theArgs) {\n"
                        + "  return v + ' - ' + Array.isArray(theArgs) + '-' + theArgs.length;\n"
                        + "}\n"
                        + "try {\n"
                        + "  rest('77');\n"
                        + "} catch (e) { e.message }";

        test("77 - true-0", code);
    }

    @Test
    public void arguments() {
        String code =
                "function rest(arg, ...restArgs) {\n"
                        + "  return arguments.length;\n"
                        + "}\n"
                        + "try {\n"
                        + "  '' + rest('77') + '-' + rest(1, 2, 3, 4);\n"
                        + "} catch (e) { e.message }";

        test("1-4", code);
    }

    @Test
    public void length() {
        String code =
                "function foo1(...theArgs) {}\n"
                        + "function foo2(arg, ...theArgs) {}\n"
                        + "try {\n"
                        + "  foo1.length + '-' + foo2.length;\n"
                        + "} catch (e) { e.message }";

        test("0-1", code);
    }

    @Test
    public void argLength() {
        String code =
                "function rest(...theArgs) {\n"
                        + "  return theArgs.length;\n"
                        + "}\n"
                        + "try {\n"
                        + "  rest(1,2) + '-' + rest(1) + '-' + rest();\n"
                        + "} catch (e) { e.message }";

        test("2-1-0", code);
    }

    @Test
    public void string1() {
        String code =
                "function rest(...theArgs) {\n"
                        + "  return theArgs.length;\n"
                        + "}\n"
                        + "try {\n"
                        + "  rest.toString();\n"
                        + "} catch (e) { e.message }";

        test("\nfunction rest(...theArgs) {\n    return theArgs.length;\n}\n", code);
    }

    @Test
    public void string2() {
        String code =
                "function rest( arg ,  ...theArgs ) {\n"
                        + "  return theArgs.length;\n"
                        + "}\n"
                        + "try {\n"
                        + "  rest.toString();\n"
                        + "} catch (e) { e.message }";

        test("\nfunction rest(arg, ...theArgs) {\n    return theArgs.length;\n}\n", code);
    }

    @Test
    public void trailingComma() {
        String code = "function rest(...theArgs,) {\n" + "  return theArgs;\n" + "}\n";

        assertEvaluatorException("parameter after rest parameter (test#1)", code);
    }

    @Test
    public void twoRestParams() {
        String code = "function rest(...rest1, ...rest2) {\n" + "  return theArgs;\n" + "}\n";

        assertEvaluatorException("parameter after rest parameter (test#1)", code);
    }

    @Test
    public void paramAfterRestParam() {
        String code = "function rest(...rest1, param) {\n" + "  return theArgs;\n" + "}\n";

        assertEvaluatorException("parameter after rest parameter (test#1)", code);
    }

    private static void test(Object expected, String js) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result = cx.evaluateString(scope, js, "test", 1, null);
                    assertEquals(expected, result);

                    return null;
                });
    }

    private static void assertEvaluatorException(String expected, String js) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    try {
                        cx.evaluateString(scope, js, "test", 1, null);
                        fail("EvaluatorException expected");
                    } catch (EvaluatorException e) {
                        assertEquals(expected, e.getMessage());
                    }

                    return null;
                });
    }
}
