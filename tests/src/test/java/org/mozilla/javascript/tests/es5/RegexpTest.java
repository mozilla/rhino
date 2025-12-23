/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptableObject;

public class RegexpTest {

    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_1_8);
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    /** see https://github.com/mozilla/rhino/issues/684. */
    @Test
    public void sideEffect() {
        Object result2 =
                cx.evaluateString(
                        scope,
                        "var a = 'hello world';"
                                + "a.replace(/(.)/g, '#');"
                                + "var res = '';"
                                + "a.replace([], function (b, c) { res += c; });"
                                + "res;",
                        "test",
                        1,
                        null);
        assertEquals("0", result2);
    }

    @Test
    public void unsupportedFlag() {
        try {
            cx.evaluateString(scope, "/abc/o;", "test", 1, null);
            fail("EvaluatorException expected");
        } catch (EvaluatorException e) {
            assertEquals("invalid flag 'o' after regular expression (test#1)", e.getMessage());
        }
    }

    @Test
    public void unsupportedFlagCtor() {
        try {
            cx.evaluateString(scope, "new RegExp('abc', 'o');", "test", 1, null);
            fail("EcmaError expected");
        } catch (EcmaError e) {
            assertEquals(
                    "SyntaxError: invalid flag 'o' after regular expression (test#1)",
                    e.getMessage());
        }
    }

    /** ES2025: Test RegExp.escape() static method */
    @Test
    public void testRegExpEscape() {
        // Test basic escaping
        Object result1 = cx.evaluateString(scope, "RegExp.escape('Hello.World');", "test", 1, null);
        assertEquals("Hello\\.World", result1);

        // Test escaping parentheses and pipe
        Object result2 = cx.evaluateString(scope, "RegExp.escape('(foo|bar)');", "test", 1, null);
        assertEquals("\\(foo\\|bar\\)", result2);

        // Test escaping dollar sign
        Object result3 = cx.evaluateString(scope, "RegExp.escape('$100');", "test", 1, null);
        assertEquals("\\$100", result3);

        // Test all special characters
        Object result4 =
                cx.evaluateString(scope, "RegExp.escape('^$\\\\.*+?()[]{}|');", "test", 1, null);
        assertEquals("\\^\\$\\\\\\.\\*\\+\\?\\(\\)\\[\\]\\{\\}\\|", result4);

        // Test empty string
        Object result5 = cx.evaluateString(scope, "RegExp.escape('');", "test", 1, null);
        assertEquals("", result5);

        // Test no arguments (converts to "undefined")
        Object result6 = cx.evaluateString(scope, "RegExp.escape();", "test", 1, null);
        assertEquals("undefined", result6);

        // Test usage in actual RegExp
        Object result7 =
                cx.evaluateString(
                        scope,
                        "var str = 'Hello.World';"
                                + "var escaped = RegExp.escape(str);"
                                + "var re = new RegExp(escaped);"
                                + "re.test('Hello.World');",
                        "test",
                        1,
                        null);
        assertEquals(Boolean.TRUE, result7);

        // Test that escaped pattern doesn't match with '.' as any char
        Object result8 =
                cx.evaluateString(
                        scope,
                        "var str = 'Hello.World';"
                                + "var escaped = RegExp.escape(str);"
                                + "var re = new RegExp(escaped);"
                                + "re.test('HelloXWorld');",
                        "test",
                        1,
                        null);
        assertEquals(Boolean.FALSE, result8);
    }
}
