/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tests.Utils;

/** Tests for Arguments support. */
public class ArgumentsTest {

    @Test
    public void argumentsSymbolIterator() {
        String code =
                "function foo() {"
                        + "  return arguments[Symbol.iterator] === Array.prototype.values;"
                        + "}"
                        + "foo()";

        test(true, code);
    }

    @Test
    public void argumentsSymbolIterator2() {
        String code =
                "function foo() {"
                        + "  return arguments[Symbol.iterator] === [][Symbol.iterator];"
                        + "}"
                        + "foo()";

        test(true, code);
    }

    @Test
    public void argumentsForOf() {
        String code =
                "function foo() {"
                        + "  var res = '';"
                        + "  for (arg of arguments) {"
                        + "    res += arg;"
                        + "  }"
                        + "  return res;"
                        + "}"
                        + "foo(1, 2, 3, 5)";

        test("1235", code);
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
}
