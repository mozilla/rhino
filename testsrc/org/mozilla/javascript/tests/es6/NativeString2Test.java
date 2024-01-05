/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tests.Utils;

/** Test for handling const variables. */
public class NativeString2Test {

    @Test
    public void getOwnPropertyDescriptorWithIndex() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "  var res = 'hello'.hasOwnProperty('0');"
                                            + "  res += ';';"
                                            + "  desc = Object.getOwnPropertyDescriptor('hello', '0');"
                                            + "  res += desc.value;"
                                            + "  res += ';';"
                                            + "  res += desc.writable;"
                                            + "  res += ';';"
                                            + "  res += desc.enumerable;"
                                            + "  res += ';';"
                                            + "  res += desc.configurable;"
                                            + "  res += ';';"
                                            + "  res;",
                                    "test",
                                    1,
                                    null);
                    assertEquals("true;h;false;true;false;", result);

                    return null;
                });
    }

    @Test
    public void normalizeNoParam() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result = cx.evaluateString(scope, "'123'.normalize()", "test", 1, null);
                    assertEquals("123", result);

                    return null;
                });
    }

    @Test
    public void normalizeNoUndefined() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(scope, "'123'.normalize(undefined)", "test", 1, null);
                    assertEquals("123", result);

                    return null;
                });
    }

    @Test
    public void normalizeNoNull() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "try { "
                                            + "  '123'.normalize(null);"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals(
                            "The normalization form should be one of 'NFC', 'NFD', 'NFKC', 'NFKD'.",
                            result);

                    return null;
                });
    }

    @Test
    public void replaceReplacementAsString() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(scope, "'123'.replace('2', /x/);", "test", 1, null);
                    assertEquals("1/x/3", result);

                    return null;
                });
    }

    @Test
    public void indexOfEmpty() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(scope, "'1234'.indexOf('', 0);", "test", 1, null);
                    assertEquals(0, result);

                    result = cx.evaluateString(scope, "'1234'.indexOf('', 1);", "test", 1, null);
                    assertEquals(1, result);

                    result = cx.evaluateString(scope, "'1234'.indexOf('', 4);", "test", 1, null);
                    assertEquals(4, result);

                    result = cx.evaluateString(scope, "'1234'.indexOf('', 5);", "test", 1, null);
                    assertEquals(4, result);

                    result = cx.evaluateString(scope, "'1234'.indexOf('', 42);", "test", 1, null);
                    assertEquals(4, result);

                    return null;
                });
    }

    @Test
    public void includesEmpty() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Boolean result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.includes('');", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.includes('', 0);", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.includes('', 1);", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.includes('', 4);", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.includes('', 5);", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.includes('', 42);", "test", 1, null);
                    assertTrue(result);

                    return null;
                });
    }

    @Test
    public void startsWithEmpty() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Boolean result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.startsWith('');", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.startsWith('', 0);", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.startsWith('', 1);", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.startsWith('', 4);", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.startsWith('', 5);", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.startsWith('', 42);", "test", 1, null);
                    assertTrue(result);

                    return null;
                });
    }

    @Test
    public void endsWithEmpty() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Boolean result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.endsWith('');", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.endsWith('', 0);", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.endsWith('', 1);", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.endsWith('', 4);", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.endsWith('', 5);", "test", 1, null);
                    assertTrue(result);

                    result =
                            (Boolean)
                                    cx.evaluateString(
                                            scope, "'1234'.endsWith('', 42);", "test", 1, null);
                    assertTrue(result);

                    return null;
                });
    }

    @Test
    public void tagify() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result = cx.evaluateString(scope, "'tester'.big()", "test", 1, null);
                    assertEquals("<big>tester</big>", result);

                    result = cx.evaluateString(scope, "'\"tester\"'.big()", "test", 1, null);
                    assertEquals("<big>\"tester\"</big>", result);

                    result = cx.evaluateString(scope, "'\"tester\"'.big()", "test", 1, null);
                    assertEquals("<big>\"tester\"</big>", result);

                    result = cx.evaluateString(scope, "'tester'.fontsize()", "test", 1, null);
                    assertEquals("<font size=\"undefined\">tester</font>", result);

                    result = cx.evaluateString(scope, "'tester'.fontsize(null)", "test", 1, null);
                    assertEquals("<font size=\"null\">tester</font>", result);

                    result =
                            cx.evaluateString(
                                    scope, "'tester'.fontsize(undefined)", "test", 1, null);
                    assertEquals("<font size=\"undefined\">tester</font>", result);

                    result = cx.evaluateString(scope, "'tester'.fontsize(123)", "test", 1, null);
                    assertEquals("<font size=\"123\">tester</font>", result);

                    result =
                            cx.evaluateString(
                                    scope, "'tester'.fontsize('\"123\"')", "test", 1, null);
                    assertEquals("<font size=\"&quot;123&quot;\">tester</font>", result);

                    return null;
                });
    }

    @Test
    public void tagifyPrototypeNull() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    for (String call :
                            new String[] {
                                "big",
                                "blink",
                                "bold",
                                "fixed",
                                "fontcolor",
                                "fontsize",
                                "italics",
                                "link",
                                "small",
                                "strike",
                                "sub",
                                "sup"
                            }) {
                        String code =
                                "try { String.prototype."
                                        + call
                                        + ".call(null);} catch (e) { e.message }";
                        Object result = cx.evaluateString(scope, code, "test", 1, null);
                        assertEquals(
                                "String.prototype." + call + " method called on null or undefined",
                                result);
                    }

                    return null;
                });
    }

    @Test
    public void tagifyPrototypeUndefined() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    for (String call :
                            new String[] {
                                "big",
                                "blink",
                                "bold",
                                "fixed",
                                "fontcolor",
                                "fontsize",
                                "italics",
                                "link",
                                "small",
                                "strike",
                                "sub",
                                "sup"
                            }) {
                        String code =
                                "try { String.prototype."
                                        + call
                                        + ".call(undefined);} catch (e) { e.message }";
                        Object result = cx.evaluateString(scope, code, "test", 1, null);
                        assertEquals(
                                "String.prototype." + call + " method called on null or undefined",
                                result);
                    }

                    return null;
                });
    }

    @Test
    public void stringReplace() {
        assertEvaluates("xyz", "''.replace('', 'xyz')");
        assertEvaluates("1", "'121'.replace('21', '')");
        assertEvaluates("xyz121", "'121'.replace('', 'xyz')");
        assertEvaluates("a$c21", "'121'.replace('1', 'a$c')");
        assertEvaluates("a121", "'121'.replace('1', 'a$&')");
        assertEvaluates("a$c21", "'121'.replace('1', 'a$$c')");
        assertEvaluates("abaabe", "'abcde'.replace('cd', 'a$`')");
        assertEvaluates("a21", "'121'.replace('1', 'a$`')");
        assertEvaluates("abaee", "'abcde'.replace('cd', \"a$'\")");
        assertEvaluates("aba", "'abcd'.replace('cd', \"a$'\")");
        assertEvaluates("aba$0", "'abcd'.replace('cd', 'a$0')");
        assertEvaluates("aba$1", "'abcd'.replace('cd', 'a$1')");
        assertEvaluates(
                "abCD",
                "'abcd'.replace('cd', function (matched) { return matched.toUpperCase() })");
        assertEvaluates("", "'123456'.replace(/\\d+/, '')");
        assertEvaluates(
                "123ABCD321abcd",
                "'123abcd321abcd'.replace(/[a-z]+/, function (matched) { return matched.toUpperCase() })");
    }

    @Test
    public void stringReplaceAll() {
        assertEvaluates("xyz", "''.replaceAll('', 'xyz')");
        assertEvaluates("1", "'12121'.replaceAll('21', '')");
        assertEvaluates("xyz1xyz2xyz1xyz", "'121'.replaceAll('', 'xyz')");
        assertEvaluates("a$c2a$c", "'121'.replaceAll('1', 'a$c')");
        assertEvaluates("a12a1", "'121'.replaceAll('1', 'a$&')");
        assertEvaluates("a$c2a$c", "'121'.replaceAll('1', 'a$$c')");
        assertEvaluates("aaadaaabcda", "'abcdabc'.replaceAll('bc', 'a$`')");
        assertEvaluates("a2a12", "'121'.replaceAll('1', 'a$`')");
        assertEvaluates("aadabcdaa", "'abcdabc'.replaceAll('bc', \"a$'\")");
        assertEvaluates("aadabcdaa", "'abcdabc'.replaceAll('bc', \"a$'\")");
        assertEvaluates("aa$0daa$0", "'abcdabc'.replaceAll('bc', 'a$0')");
        assertEvaluates("aa$1daa$1", "'abcdabc'.replaceAll('bc', 'a$1')");
        assertEvaluates("", "'123456'.replaceAll(/\\d+/g, '')");
        assertEvaluates("123456", "'123456'.replaceAll(undefined, '')");
        assertEvaluates("afoobarb", "'afoob'.replaceAll(/(foo)/g, '$1bar')");
        assertEvaluates("foobarb", "'foob'.replaceAll(/(foo)/gy, '$1bar')");
        assertEvaluates("hllo", "'hello'.replaceAll(/(h)e/gy, '$1')");
        assertEvaluates("$1llo", "'hello'.replaceAll(/he/g, '$1')");
        assertEvaluates(
                "I$want$these$periods$to$be$$s",
                "'I.want.these.periods.to.be.$s'.replaceAll(/\\./g, '$')");
        assertEvaluates("food bar", "'foo bar'.replaceAll(/foo/g, '$&d')");
        assertEvaluates("foo foo ", "'foo bar'.replaceAll(/bar/g, '$`')");
        assertEvaluates(" bar bar", "'foo bar'.replaceAll(/foo/g, '$\\'')");
        assertEvaluates("$' bar", "'foo bar'.replaceAll(/foo/g, '$$\\'')");
        assertEvaluates("ad$0db", "'afoob'.replaceAll(/(foo)/g, 'd$0d')");
        assertEvaluates("ad$0db", "'afkxxxkob'.replace(/(f)k(.*)k(o)/g, 'd$0d')");
        assertEvaluates("ad$0dbd$0dc", "'afoobfuoc'.replaceAll(/(f.o)/g, 'd$0d')");
        assertEvaluates(
                "123FOOBAR321BARFOO123",
                "'123foobar321barfoo123'.replace(/[a-z]+/g, function (matched) { return matched.toUpperCase() })");

        assertEvaluates(
                "TypeError: replaceAll must be called with a global RegExp",
                "try { 'hello'.replaceAll(/he/i, 'x'); } catch (e) { '' + e }");
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
