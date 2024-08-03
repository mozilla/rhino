/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

/**
 * Tests for Functions Rest Parameters.
 *
 * @author Ronald Brill
 */
public class FunctionsRestParametersTest {

    @Test
    public void oneRestArg() {
        String code =
                "function rest(...restArgs) {\n"
                        + "  return restArgs;\n"
                        + "}\n"
                        + "rest(1, 'abc', 2, '##').toString();\n";

        Utils.assertWithAllOptimizationLevelsES6("1,abc,2,##", code);
    }

    @Test
    public void oneRestArgActivation() {
        String code =
                "function rest(...restArgs) {\n"
                        + "  try {\n"
                        + "    return restArgs;\n"
                        + "  } catch {}\n"
                        + "}\n"
                        + "  rest(1, 'abc', 2, '##').toString();\n";

        Utils.assertWithAllOptimizationLevelsES6("1,abc,2,##", code);
    }

    @Test
    public void oneRestArgNothingProvided() {
        String code =
                "function rest(...restArgs) {\n"
                        + "  return restArgs;\n"
                        + "}\n"
                        + "var r = rest();\n"
                        + "'' + Array.isArray(r) + '-' + r.length;\n";

        Utils.assertWithAllOptimizationLevelsES6("true-0", code);
    }

    @Test
    public void oneRestArgNothingProvidedActivation() {
        String code =
                "function rest(...restArgs) {\n"
                        + "  try {\n"
                        + "    return restArgs;\n"
                        + "  } catch {}\n"
                        + "}\n"
                        + "var r = rest();\n"
                        + "'' + Array.isArray(r) + '-' + r.length;\n";

        Utils.assertWithAllOptimizationLevelsES6("true-0", code);
    }

    @Test
    public void oneRestArgOneProvided() {
        String code =
                "function rest(...restArgs) {\n"
                        + "  return restArgs;\n"
                        + "}\n"
                        + "var r = rest('xy');\n"
                        + "'' + Array.isArray(r) + '-' + r.length;\n";

        Utils.assertWithAllOptimizationLevelsES6("true-1", code);
    }

    @Test
    public void oneRestArgOneProvidedActivation() {
        String code =
                "function rest(...restArgs) {\n"
                        + "  try {\n"
                        + "    return restArgs;\n"
                        + "  } catch {}\n"
                        + "}\n"
                        + "var r = rest('xy');\n"
                        + "'' + Array.isArray(r) + '-' + r.length;\n";

        Utils.assertWithAllOptimizationLevelsES6("true-1", code);
    }

    @Test
    public void twoRestArg() {
        String code =
                "function rest(v, ...restArgs) {\n"
                        + "  return restArgs;\n"
                        + "}\n"
                        + "rest(1, 'abc', 2, '##').toString();\n";

        Utils.assertWithAllOptimizationLevelsES6("abc,2,##", code);
    }

    @Test
    public void twoRestArgActivation() {
        String code =
                "function rest(v, ...restArgs) {\n"
                        + "  try {\n"
                        + "    return restArgs;\n"
                        + "  } catch {}\n"
                        + "}\n"
                        + "rest(1, 'abc', 2, '##').toString();\n";

        Utils.assertWithAllOptimizationLevelsES6("abc,2,##", code);
    }

    @Test
    public void twoRestArgNothingProvided() {
        String code =
                "function rest(v, ...restArgs) {\n"
                        + "  return '' + typeof v + ' - ' + Array.isArray(restArgs) + '-' + restArgs.length;\n"
                        + "}\n"
                        + "rest();\n";

        Utils.assertWithAllOptimizationLevelsES6("undefined - true-0", code);
    }

    @Test
    public void twoRestArgNothingProvidedActivation() {
        String code =
                "function rest(v, ...restArgs) {\n"
                        + "  try {\n"
                        + "    return '' + typeof v + ' - ' + Array.isArray(restArgs) + '-' + restArgs.length;\n"
                        + "  } catch {}\n"
                        + "}\n"
                        + "rest();\n";

        Utils.assertWithAllOptimizationLevelsES6("undefined - true-0", code);
    }

    @Test
    public void twoRestArgOneProvided() {
        String code =
                "function rest(v, ...restArgs) {\n"
                        + "  return v + ' - ' + Array.isArray(restArgs) + '-' + restArgs.length;\n"
                        + "}\n"
                        + "rest('77');";

        Utils.assertWithAllOptimizationLevelsES6("77 - true-0", code);
    }

    @Test
    public void twoRestArgOneProvidedActivation() {
        String code =
                "function rest(v, ...restArgs) {\n"
                        + "  try {\n"
                        + "    return v + ' - ' + Array.isArray(restArgs) + '-' + restArgs.length;\n"
                        + "  } catch {}\n"
                        + "}\n"
                        + "rest('77');";

        Utils.assertWithAllOptimizationLevelsES6("77 - true-0", code);
    }

    @Test
    public void arguments() {
        String code =
                "function rest(arg, ...restArgs) {\n"
                        + "  return arguments.length;\n"
                        + "}\n"
                        + "'' + rest('77') + '-' + rest(1, 2, 3, 4);\n";

        Utils.assertWithAllOptimizationLevelsES6("1-4", code);
    }

    @Test
    public void argumentsActivation() {
        String code =
                "function rest(arg, ...restArgs) {\n"
                        + "  try {\n"
                        + "    return arguments.length;\n"
                        + "  } catch {}\n"
                        + "}\n"
                        + "'' + rest('77') + '-' + rest(1, 2, 3, 4);\n";

        Utils.assertWithAllOptimizationLevelsES6("1-4", code);
    }

    @Test
    public void argLength() {
        String code =
                "function rest(...restArgs) {\n"
                        + "  return restArgs.length;\n"
                        + "}\n"
                        + "  rest(1,2) + '-' + rest(1) + '-' + rest();\n";

        Utils.assertWithAllOptimizationLevelsES6("2-1-0", code);
    }

    @Test
    public void argLengthActivation() {
        String code =
                "function rest(...restArgs) {\n"
                        + "  try {\n"
                        + "    return restArgs.length;\n"
                        + "  } catch {}\n"
                        + "}\n"
                        + "  rest(1,2) + '-' + rest(1) + '-' + rest();\n";

        Utils.assertWithAllOptimizationLevelsES6("2-1-0", code);
    }

    @Test
    public void length() {
        String code =
                "function foo1(...restArgs) {}\n"
                        + "function foo2(arg, ...restArgs) {}\n"
                        + "foo1.length + '-' + foo2.length;\n";

        Utils.assertWithAllOptimizationLevelsES6("0-1", code);
    }

    @Test
    public void string1() {
        String code =
                "function rest(...restArgs) {\n"
                        + "  return restArgs.length;\n"
                        + "}\n"
                        + "rest.toString();\n";

        Utils.assertWithAllOptimizationLevelsES6(
                "function rest(...restArgs) {\n  return restArgs.length;\n}", code);
    }

    @Test
    public void string2() {
        String code =
                "function rest( arg ,  ...restArgs ) {\n"
                        + "  return restArgs.length;\n"
                        + "}\n"
                        + "rest.toString();\n";

        Utils.assertWithAllOptimizationLevelsES6(
                "function rest( arg ,  ...restArgs ) {\n  return restArgs.length;\n}", code);
    }

    @Test
    public void trailingComma() {
        String code = "function rest(...restArgs,) {\n" + "  return restArgs;\n" + "}\n";

        Utils.assertEvaluatorExceptionES6("parameter after rest parameter (test#1)", code);
    }

    @Test
    public void twoRestParams() {
        String code = "function rest(...rest1, ...rest2) {\n" + "  return restArgs;\n" + "}\n";

        Utils.assertEvaluatorExceptionES6("parameter after rest parameter (test#1)", code);
    }

    @Test
    public void paramAfterRestParam() {
        String code = "function rest(...rest1, param) {\n" + "  return restArgs;\n" + "}\n";

        Utils.assertEvaluatorExceptionES6("parameter after rest parameter (test#1)", code);
    }
}
