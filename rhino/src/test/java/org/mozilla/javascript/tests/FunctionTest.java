/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Scriptable;

/**
 * Unit tests for Function.
 *
 * @author Marc Guillemot
 */
public class FunctionTest {

    /**
     * Test for bug #600479 https://bugzilla.mozilla.org/show_bug.cgi?id=600479 Syntax of function
     * built from Function's constructor string parameter was not correct when this string contained
     * "//".
     */
    @Test
    public void functionWithSlashSlash() {
        assertEvaluates(true, "new Function('return true//;').call()");
    }

    @Test
    public void functionHasNameOfVarStrictMode() throws Exception {
        final String script =
                ""
                        + "'use strict';\n"
                        + "var result = '';\n"
                        + "var abc = 1;\n"
                        + "var foo = function abc() { result += '-inner abc = ' + typeof abc; };\n"
                        + "result += '-outer abc = ' + abc;\n"
                        + "foo();\n"
                        + "result;";

        assertEvaluates("-outer abc = 1-inner abc = function", script);
    }

    @Test
    public void innerFunctionWithSameName() throws Exception {
        final String script =
                ""
                        + "var result = '';\n"
                        + "var a = function () {\n"
                        + "  var x = (function x () { result += 'a'; });\n"
                        + "  return function () { x(); };\n"
                        + "}();\n"
                        + "var b = function () {\n"
                        + "  var x = (function x () { result += 'b'; });\n"
                        + "  return function () { x(); };\n"
                        + "}();\n"
                        + "a();\n"
                        + "b();\n"
                        + "result;";

        assertEvaluates("ab", script);
    }

    @Test
    public void innerFunctionWithSameNameAsOutsideStrict() throws Exception {
        final String script =
                ""
                        + "var result = '';\n"
                        + "'use strict';\n"
                        + "var a = function () {\n"
                        + "  var x = (function x () { result += 'a'; });\n"
                        + "  return function () { x(); };\n"
                        + "}();\n"
                        + "var x = function () { result += 'x'; };\n"
                        + "a();\n"
                        + "result;";

        assertEvaluates("a", script);
    }

    @Test
    public void secondFunctionWithSameNameStrict() throws Exception {
        final String script =
                ""
                        + "var result = '';\n"
                        + "'use strict';\n"
                        + "function norm(foo) { return ('' + foo).replace(/(\\s)/gm,''); }\n"
                        + "function func () { result += 'outer'; }\n"
                        + "var x = function func() { result += norm(func); };\n"
                        + "x();\n"
                        + "func();\n"
                        + "result;";

        assertEvaluates("functionfunc(){result+=norm(func);}outer", script);
    }

    @Test
    public void functioNamesExceptionsStrict() throws Exception {
        final String script =
                ""
                        + "var result = '';\n"
                        + "  'use strict';\n"
                        + "  function f1() {"
                        + "    result += 'f1';"
                        + "    function f9() { result += 'f9'; }"
                        + "  }\n"
                        + "  var f2 = function () { result += 'f2'; };\n"
                        + "  var f3 = function f4() { result += 'f3'; };\n"
                        + "  var f5 = function f5() { result += 'f5'; };\n"
                        + "  !function f6() { result += 'f6'; };\n"
                        + "  (function f7() { result += 'f7'; });\n"
                        + "  void function f8() { result += 'f8'; };\n"
                        + "  try { f1(); } catch (e) { result += '!f1'; }"
                        + "  try { f2(); } catch (e) { result += '!f2'; }"
                        + "  try { f3(); } catch (e) { result += '!f3'; }"
                        + "  try { f4(); } catch (e) { result += '!f4'; }"
                        + "  try { f5(); } catch (e) { result += '!f5'; }"
                        + "  try { f6(); } catch (e) { result += '!f6'; }"
                        + "  try { f7(); } catch (e) { result += '!f7'; }"
                        + "  try { f8(); } catch (e) { result += '!f8'; }"
                        + "  {\n"
                        + "    function f10() { result += 'f10'; }\n"
                        + "    var f11 = function () { result += 'f11'; };\n"
                        + "    var f12 = function f12() { result += 'f12'; };\n"
                        + "    f10();\n"
                        + "    f11();\n"
                        + "    f12();\n"
                        + "  }\n"

                        // does not work so far
                        // + "  try { f10(); } catch (e) { result += '!f10'; }"
                        + "  try { f11(); } catch (e) { result += '!f11'; }"
                        + "  try { f12(); } catch (e) { result += '!f12'; }"
                        + "  function f13() { result += 'f13'; } + 1;"
                        + "  try { f13(); } catch (e) { '!f13'; }"
                        + "result;";

        // assertEvaluates("f1f2f3!f4f5!f6!f7!f8f10f11f12!f10f11f12f13", script);
        assertEvaluates("f1f2f3!f4f5!f6!f7!f8f10f11f12f11f12f13", script);
    }

    private static void assertEvaluates(final Object expected, final String source) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    final Object rep = cx.evaluateString(scope, source, "test.js", 0, null);
                    assertEquals(expected, rep);
                    return null;
                });
    }
}
