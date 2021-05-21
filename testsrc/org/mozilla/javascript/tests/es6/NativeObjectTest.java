/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop), Object.keys, Object.values and Object.entries method
 */
package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** Test for NativeObject. */
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
    public void testAssignPropertyGetter() {
        Object result =
                cx.evaluateString(
                        scope,
                        "var obj = Object.defineProperty({}, 'propA', {\n"
                                + "                 enumerable: true,\n"
                                + "                 get: function () {\n"
                                + "                        Object.defineProperty(this, 'propB', {\n"
                                + "                                 value: 3,\n"
                                + "                                 enumerable: false\n"
                                + "                        });\n"
                                + "                      }\n"
                                + "          });\n"
                                + "Object.assign(obj, {propB: 2});\n"
                                + "Object.assign({propB: 1}, obj);\n"
                                + "'obj.propB = ' + obj.propB + ', enumerable = ' + Object.getOwnPropertyDescriptor(obj, 'propB').enumerable",
                        "test",
                        1,
                        null);

        assertEquals("obj.propB = 3, enumerable = false", result);
    }

    @Test
    public void testAssignOneParameter() {
        Object result =
                cx.evaluateString(
                        scope,
                        "var obj = {};" + "res = Object.assign(obj);" + "res === obj;",
                        "test",
                        1,
                        null);

        assertEquals(true, result);
    }

    @Test
    public void testAssignMissingParameters() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { " + "  Object.assign();" + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);

        assertEquals("Cannot convert undefined to an object.", result);
    }

    @Test
    public void testAssignNumericPropertyGetter() {
        Object result =
                cx.evaluateString(
                        scope,
                        "var obj = Object.defineProperty({}, 1, {\n"
                                + "                 enumerable: true,\n"
                                + "                 get: function () {\n"
                                + "                        Object.defineProperty(this, 2, {\n"
                                + "                                 value: 3,\n"
                                + "                                 enumerable: false\n"
                                + "                        });\n"
                                + "                      }\n"
                                + "          });\n"
                                + "Object.assign(obj, {2: 2});\n"
                                + "Object.assign({2: 1}, obj);\n"
                                + "'obj[2] = ' + obj[2] + ', enumerable = ' + Object.getOwnPropertyDescriptor(obj, 2).enumerable",
                        "test",
                        1,
                        null);

        assertEquals("obj[2] = 3, enumerable = false", result);
    }

    @Test
    public void testSetPrototypeOfNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Object.setPrototypeOf(null, new Object());"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);

        assertEquals("Object.prototype.setPrototypeOf method called on null or undefined", result);
    }

    @Test
    public void testSetPrototypeOfUndefined() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Object.setPrototypeOf(undefined, new Object());"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);

        assertEquals("Object.prototype.setPrototypeOf method called on null or undefined", result);
    }

    @Test
    public void testSetPrototypeOfMissingParameters() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { " + "  Object.setPrototypeOf();" + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);

        assertEquals(
                "Object.setPrototypeOf: At least 2 arguments required, but only 0 passed", result);

        result =
                cx.evaluateString(
                        scope,
                        "try { " + "  Object.setPrototypeOf({});" + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);

        assertEquals(
                "Object.setPrototypeOf: At least 2 arguments required, but only 1 passed", result);
    }

    @Test
    public void testKeysMissingParameter() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { " + "  Object.keys();" + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);

        assertEquals("Cannot convert undefined to an object.", result);
    }

    @Test
    public void testKeysOnObjectParameter() {
        evaluateAndAssert(
                "Object.keys({'foo':'bar', 2: 'y', 1: 'x'})", Arrays.asList("1", "2", "foo"));
    }

    @Test
    public void testKeysOnArray() {
        evaluateAndAssert("Object.keys(['x','y','z'])", Arrays.asList("0", "1", "2"));
    }

    @Test
    public void testKeysOnArrayWithProp() {
        evaluateAndAssert(
                "var arr = ['x','y','z'];\n" + "arr['foo'] = 'bar'; Object.keys(arr)",
                Arrays.asList("0", "1", "2", "foo"));
    }

    @Test
    public void testValuesMissingParameter() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { " + "  Object.values();" + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);

        assertEquals("Cannot convert undefined to an object.", result);
    }

    @Test
    public void testValuesOnObjectParameter() {
        evaluateAndAssert(
                "Object.values({'foo':'bar', 2: 'y', 1: 'x'})", Arrays.asList("x", "y", "bar"));
    }

    @Test
    public void testValuesOnArray() {
        evaluateAndAssert("Object.values(['x','y','z'])", Arrays.asList("x", "y", "z"));
    }

    @Test
    public void testValuesOnArrayWithProp() {
        evaluateAndAssert(
                "var arr = [3,4,5];\n" + "arr['foo'] = 'bar'; Object.values(arr)",
                Arrays.asList(3, 4, 5, "bar"));
    }

    @Test
    public void testEntriesMissingParameter() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { " + "  Object.entries();" + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);

        assertEquals("Cannot convert undefined to an object.", result);
    }

    @Test
    public void testEntriesOnObjectParameter() {
        evaluateAndAssert(
                "Object.entries({'foo':'bar', 2: 'y', 1: 'x'})",
                Arrays.asList(
                        Arrays.asList("1", "x"),
                        Arrays.asList("2", "y"),
                        Arrays.asList("foo", "bar")));
    }

    @Test
    public void testEntriesOnArray() {
        evaluateAndAssert(
                "Object.entries(['x','y','z'])",
                Arrays.asList(
                        Arrays.asList("0", "x"), Arrays.asList("1", "y"), Arrays.asList("2", "z")));
    }

    @Test
    public void testEntriesOnArrayWithProp() {
        evaluateAndAssert(
                "var arr = [3,4,5];\n" + "arr['foo'] = 'bar'; Object.entries(arr)",
                Arrays.asList(
                        Arrays.asList("0", 3),
                        Arrays.asList("1", 4),
                        Arrays.asList("2", 5),
                        Arrays.asList("foo", "bar")));
    }

    @Test
    public void testFromEntriesMissingParameter() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { " + "  Object.fromEntries();" + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);

        assertEquals("Cannot convert undefined to an object.", result);
    }

    @Test
    public void testFromEntriesOnArray() {
        Map<Object, Object> map = new HashMap<>();
        map.put("0", "x");
        map.put("1", "y");
        map.put("2", "z");
        evaluateAndAssert("Object.fromEntries(Object.entries(['x','y','z']))", map);
    }

    private void evaluateAndAssert(String script, Object expected) {
        Object result = cx.evaluateString(scope, script, "test", 1, null);
        assertEquals(expected, result);
    }
}
