/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** Test for the Object.hasOwn */
package org.mozilla.javascript.tests.es2022;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class NativeObjectTest {

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
    public void testHasStringOwn() {
        Object result =
                cx.evaluateString(
                        scope,
                        "let result = Object.hasOwn({ test: '123' }, 'test');\n"
                                + "'result = ' + result",
                        "test",
                        1,
                        null);

        assertEquals("result = true", result);
    }

    @Test
    public void testHasUndefinedOwn() {
        Object result =
                cx.evaluateString(
                        scope,
                        "let result = Object.hasOwn({ test: undefined }, 'test');\n"
                                + "'result = ' + result;",
                        "test",
                        1,
                        null);

        assertEquals("result = true", result);
    }

    @Test
    public void testHasNullOwn() {
        Object result =
                cx.evaluateString(
                        scope,
                        "let result = Object.hasOwn({ test: null }, 'test');\n"
                                + "'result = ' + result;",
                        "test",
                        1,
                        null);

        assertEquals("result = true", result);
    }

    @Test
    public void testHasArrayPropertyOwn() {
        Object result =
                cx.evaluateString(
                        scope,
                        "let dessert = [\"cake\", \"coffee\", \"chocolate\"];\n"
                                + "let result = Object.hasOwn(dessert, 2);\n"
                                + "'result = ' + result;",
                        "test",
                        1,
                        null);

        assertEquals("result = true", result);
    }

    @Test
    public void testHasNoOwn() {
        Object result =
                cx.evaluateString(
                        scope,
                        "let result = Object.hasOwn({ cake: 123 }, 'test');\n"
                                + "'result = ' + result",
                        "test",
                        1,
                        null);

        assertEquals("result = false", result);
    }

    @Test
    public void testCreateHasOwn() {
        Object result =
                cx.evaluateString(
                        scope,
                        "var foo = Object.create(null);\n"
                                + "foo.prop = 'test';\n"
                                + "var result = Object.hasOwn(foo, 'prop');\n"
                                + "'result = ' + result;",
                        "test",
                        1,
                        null);

        assertEquals("result = true", result);
    }

    @Test
    public void testCreateNoHasOwn() {
        Object result =
                cx.evaluateString(
                        scope,
                        "var result = Object.hasOwn(Object.create({ q: 321 }), 'q');\n"
                                + "'result = ' + result; ",
                        "test",
                        1,
                        null);

        assertEquals("result = false", result);
    }

    @Test
    public void testCalledTest() {
        Object result =
                cx.evaluateString(
                        scope,
                        "var called = false;\n"
                                + "try {\n"
                                + "          Object.hasOwn(null, { toString() { called = true } });\n"
                                + "} catch (e) {}\n"
                                + "'called = ' + called;",
                        "test",
                        1,
                        null);

        assertEquals("called = false", result);
    }
}
