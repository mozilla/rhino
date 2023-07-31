/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tests.Utils;

/** @author Ronald Brill */
public class NativeRegExpTest {

    @Test
    public void regExIsCallableForBackwardCompatibility() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_1_8);
                    ScriptableObject scope = cx.initStandardObjects();

                    String source = "var a = new RegExp('1'); a(1).toString();";
                    assertEquals("1", cx.evaluateString(scope, source, "test", 0, null));

                    source = "/^\\{(.*)\\}$/('{1234}').toString();";
                    assertEquals("{1234},1234", cx.evaluateString(scope, source, "test", 0, null));

                    source = "RegExp('a|b','g')()";
                    assertNull(cx.evaluateString(scope, source, "test", 0, null));

                    source = "new /z/();";
                    assertNull(cx.evaluateString(scope, source, "test", 0, null));

                    source = "(new new RegExp).toString()";
                    assertEquals("", cx.evaluateString(scope, source, "test", 0, null));

                    return null;
                });
    }

    @Test
    public void regExMinusInRangeBorderCases() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_1_8);
                    ScriptableObject scope = cx.initStandardObjects();

                    String source = "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-]+/g, 'x');";
                    assertEquals(
                            "axbxc d efg 1 23", cx.evaluateString(scope, source, "test", 0, null));

                    source = "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\s]+/g, 'x');";
                    assertEquals(
                            "axbxcxdxefgx1x23", cx.evaluateString(scope, source, "test", 0, null));

                    source = "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\S]+/g, 'x');";
                    assertEquals("x x x x x", cx.evaluateString(scope, source, "test", 0, null));

                    source = "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\w]+/g, 'x');";
                    assertEquals("x x x x x", cx.evaluateString(scope, source, "test", 0, null));

                    source = "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\W]+/g, 'x');";
                    assertEquals(
                            "axbxcxdxefgx1x23", cx.evaluateString(scope, source, "test", 0, null));

                    source = "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\d]+/g, 'x');";
                    assertEquals(
                            "axbxc d efg x x", cx.evaluateString(scope, source, "test", 0, null));

                    source = "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\D]+/g, 'x');";
                    assertEquals("x1x23", cx.evaluateString(scope, source, "test", 0, null));

                    source = "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\a]+/g, 'x');";
                    assertEquals(
                            "x-bxc d efg 1 23", cx.evaluateString(scope, source, "test", 0, null));

                    return null;
                });
    }

    @Test
    public void regExIsNotCallable() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    String source = "var a = new RegExp('1'); a(1);";
                    try {
                        cx.evaluateString(scope, source, "test", 0, null);
                        fail();
                    } catch (EcmaError e) {
                        // expected
                        assertTrue(e.getMessage(), e.getMessage().startsWith("TypeError: "));
                    }

                    source = "/^\\{(.*)\\}$/('{1234}');";
                    try {
                        cx.evaluateString(scope, source, "test", 0, null);
                        fail();
                    } catch (EcmaError e) {
                        // expected
                        assertTrue(e.getMessage(), e.getMessage().startsWith("TypeError: "));
                    }

                    source = "RegExp('a|b','g')();";
                    try {
                        cx.evaluateString(scope, source, "test", 0, null);
                        fail();
                    } catch (EcmaError e) {
                        // expected
                        assertTrue(e.getMessage(), e.getMessage().startsWith("TypeError: "));
                    }

                    source = "new /z/();";
                    try {
                        cx.evaluateString(scope, source, "test", 0, null);
                        fail();
                    } catch (EcmaError e) {
                        // expected
                        assertTrue(e.getMessage(), e.getMessage().startsWith("TypeError: "));
                    }

                    source = "new new RegExp";
                    try {
                        cx.evaluateString(scope, source, "test", 0, null);
                        fail();
                    } catch (EcmaError e) {
                        // expected
                        assertTrue(e.getMessage(), e.getMessage().startsWith("TypeError: "));
                    }

                    return null;
                });
    }

    @Test
    public void lastIndexReadonly() {
        final String script =
                "try { "
                        + "  var r = /c/g;"
                        + "  Object.defineProperty(r, 'lastIndex', { writable: false });"
                        + "  r.exec('abc');"
                        + "} catch (e) { e.message }";
        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    final ScriptableObject scope = _cx.initStandardObjects();
                    final Object result = _cx.evaluateString(scope, script, "test script", 0, null);
                    assertEquals(
                            "Cannot modify readonly property: lastIndex.",
                            Context.toString(result));
                    return null;
                });
    }

    @Test
    public void search() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    String source = "'abc'.search(/b/);";
                    assertEquals(1, cx.evaluateString(scope, source, "test", 0, null));

                    source = "/b/[Symbol.search]('abc');";
                    assertEquals(1, cx.evaluateString(scope, source, "test", 0, null));

                    source = "'abc'.search(/d/);";
                    assertEquals(-1, cx.evaluateString(scope, source, "test", 0, null));

                    source = "/d/[Symbol.search]('abc');";
                    assertEquals(-1, cx.evaluateString(scope, source, "test", 0, null));

                    return null;
                });
    }

    @Test
    public void regExWrongQuantifier() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    ScriptableObject scope = cx.initStandardObjects();

                    String source = "'abc'.search(/b{2,1}/);";
                    try {
                        cx.evaluateString(scope, source, "test", 0, null);
                        fail("Shoud throw");
                    } catch (Exception e) {
                        assertEquals(
                                "SyntaxError: Invalid regular expression: The quantifier maximum '1' is less than the minimum '2'.",
                                e.getMessage());
                    }

                    return null;
                });
    }
}
