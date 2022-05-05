/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** Test for handling const variables. */
public class NativeString2Test {

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
    public void testGetOwnPropertyDescriptorWithIndex() {
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
    }

    @Test
    public void testNormalizeNoParam() {
        Object result = cx.evaluateString(scope, "'123'.normalize()", "test", 1, null);
        assertEquals("123", result);
    }

    @Test
    public void testNormalizeNoUndefined() {
        Object result = cx.evaluateString(scope, "'123'.normalize(undefined)", "test", 1, null);
        assertEquals("123", result);
    }

    @Test
    public void testNormalizeNoNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { " + "  '123'.normalize(null);" + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals(
                "The normalization form should be one of 'NFC', 'NFD', 'NFKC', 'NFKD'.", result);
    }

    @Test
    public void testReplaceReplacementAsString() {
        Object result = cx.evaluateString(scope, "'123'.replace('2', /x/);", "test", 1, null);

        assertEquals("1/x/3", result);
    }

    @Test
    public void testIndexOfEmpty() {
        Object result = cx.evaluateString(scope, "'1234'.indexOf('', 0);", "test", 1, null);
        assertEquals(0, result);

        result = cx.evaluateString(scope, "'1234'.indexOf('', 1);", "test", 1, null);
        assertEquals(1, result);

        result = cx.evaluateString(scope, "'1234'.indexOf('', 4);", "test", 1, null);
        assertEquals(4, result);

        result = cx.evaluateString(scope, "'1234'.indexOf('', 5);", "test", 1, null);
        assertEquals(4, result);

        result = cx.evaluateString(scope, "'1234'.indexOf('', 42);", "test", 1, null);
        assertEquals(4, result);
    }

    @Test
    public void testIncludesEmpty() {
        Boolean result =
                (Boolean) cx.evaluateString(scope, "'1234'.includes('');", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.includes('', 0);", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.includes('', 1);", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.includes('', 4);", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.includes('', 5);", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.includes('', 42);", "test", 1, null);
        assertTrue(result);
    }

    @Test
    public void testStartsWithEmpty() {
        Boolean result =
                (Boolean) cx.evaluateString(scope, "'1234'.startsWith('');", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.startsWith('', 0);", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.startsWith('', 1);", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.startsWith('', 4);", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.startsWith('', 5);", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.startsWith('', 42);", "test", 1, null);
        assertTrue(result);
    }

    @Test
    public void testEndsWithEmpty() {
        Boolean result =
                (Boolean) cx.evaluateString(scope, "'1234'.endsWith('');", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.endsWith('', 0);", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.endsWith('', 1);", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.endsWith('', 4);", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.endsWith('', 5);", "test", 1, null);
        assertTrue(result);

        result = (Boolean) cx.evaluateString(scope, "'1234'.endsWith('', 42);", "test", 1, null);
        assertTrue(result);
    }

    @Test
    public void testTagify() {
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

        result = cx.evaluateString(scope, "'tester'.fontsize(undefined)", "test", 1, null);
        assertEquals("<font size=\"undefined\">tester</font>", result);

        result = cx.evaluateString(scope, "'tester'.fontsize(123)", "test", 1, null);
        assertEquals("<font size=\"123\">tester</font>", result);

        result = cx.evaluateString(scope, "'tester'.fontsize('\"123\"')", "test", 1, null);
        assertEquals("<font size=\"&quot;123&quot;\">tester</font>", result);
    }

    @Test
    public void testTagifyPrototypeNull() {
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
                    "try { String.prototype." + call + ".call(null);} catch (e) { e.message }";
            Object result = cx.evaluateString(scope, code, "test", 1, null);
            assertEquals(
                    "String.prototype." + call + " method called on null or undefined", result);
        }
    }

    @Test
    public void testTagifyPrototypeUndefined() {
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
                    "try { String.prototype." + call + ".call(undefined);} catch (e) { e.message }";
            Object result = cx.evaluateString(scope, code, "test", 1, null);
            assertEquals(
                    "String.prototype." + call + " method called on null or undefined", result);
        }
    }
}
