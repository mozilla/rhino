/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.testutils.Utils;

/**
 * Tests for yield* with iterator.return() method. Tests andling when the delegated iterator's
 * return method is null, undefined, or returns null/undefined.
 */
public class GeneratorsYieldStarReturnTest {

    /**
     * Test that yield* properly delegates to the iterable's return() method and handles the case
     * when return() returns null.
     */
    @Test
    public void testYieldStarWithNullReturn() {
        String script =
                Utils.lines(
                        "var returnGets = 0;",
                        "var iterable = {",
                        "    next: function() {",
                        "        return {value: 1, done: false};",
                        "    },",
                        "    get return() {",
                        "        returnGets += 1;",
                        "        return null;",
                        "    },",
                        "};",
                        "",
                        "iterable[Symbol.iterator] = function() {",
                        "    return iterable;",
                        "};",
                        "",
                        "function* generator() {",
                        "    yield* iterable;",
                        "}",
                        "",
                        "var iterator = generator();",
                        "iterator.next();",
                        "",
                        "var result = iterator.return(2);");

        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    Scriptable scope = cx.initStandardObjects();
                    cx.evaluateString(scope, script, "test", 1, null);

                    Object result = scope.get("result", scope);
                    assertTrue("result should be a Scriptable", result instanceof Scriptable);

                    Scriptable resultObj = (Scriptable) result;
                    assertEquals(
                            "Result value should be 2",
                            2.0,
                            getNumberProperty(resultObj, "value"),
                            0.001);
                    assertEquals(
                            "Result should be done",
                            Boolean.TRUE,
                            resultObj.get("done", resultObj));

                    double returnGets = getNumberProperty(scope, "returnGets");
                    assertEquals("Return getter should be called once", 1.0, returnGets, 0.001);
                    return null;
                });
    }

    /**
     * Test that yield* properly delegates to the iterable's return() method and handles the case
     * when return() returns undefined.
     */
    @Test
    public void testYieldStarWithUndefinedReturn() {
        String script =
                Utils.lines(
                        "var returnCalled = false;",
                        "var iterable = {",
                        "    next: function() {",
                        "        return {value: 1, done: false};",
                        "    },",
                        "    return: function() {",
                        "        returnCalled = true;",
                        "        return undefined;",
                        "    },",
                        "};",
                        "",
                        "iterable[Symbol.iterator] = function() {",
                        "    return iterable;",
                        "};",
                        "",
                        "function* generator() {",
                        "    yield* iterable;",
                        "}",
                        "",
                        "var iterator = generator();",
                        "iterator.next();",
                        "",
                        "var result = iterator.return(42);");

        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    Scriptable scope = cx.initStandardObjects();
                    cx.evaluateString(scope, script, "test", 1, null);

                    Object result = scope.get("result", scope);
                    assertTrue("result should be a Scriptable", result instanceof Scriptable);

                    Scriptable resultObj = (Scriptable) result;
                    assertEquals(
                            "Result value should be 42",
                            42.0,
                            getNumberProperty(resultObj, "value"),
                            0.001);
                    assertEquals(
                            "Result should be done",
                            Boolean.TRUE,
                            resultObj.get("done", resultObj));

                    Object returnCalled = scope.get("returnCalled", scope);
                    assertEquals("Return method should be called", Boolean.TRUE, returnCalled);
                    return null;
                });
    }

    /** Test that yield* properly delegates when iterable doesn't have return(). */
    @Test
    public void testYieldStarWithoutReturn() {
        String script =
                Utils.lines(
                        "var iterable = {",
                        "    next: function() {",
                        "        return {value: 1, done: false};",
                        "    },",
                        "};",
                        "",
                        "iterable[Symbol.iterator] = function() {",
                        "    return iterable;",
                        "};",
                        "",
                        "function* generator() {",
                        "    yield* iterable;",
                        "}",
                        "",
                        "var iterator = generator();",
                        "iterator.next();",
                        "",
                        "var result = iterator.return(99);");

        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    Scriptable scope = cx.initStandardObjects();
                    cx.evaluateString(scope, script, "test", 1, null);

                    Object result = scope.get("result", scope);
                    assertTrue("result should be a Scriptable", result instanceof Scriptable);

                    Scriptable resultObj = (Scriptable) result;
                    assertEquals(
                            "Result value should be 99",
                            99.0,
                            getNumberProperty(resultObj, "value"),
                            0.001);
                    assertEquals(
                            "Result should be done",
                            Boolean.TRUE,
                            resultObj.get("done", resultObj));
                    return null;
                });
    }

    /**
     * Test that yield* properly delegates when iterable's return() returns a valid iterator result.
     */
    @Test
    public void testYieldStarWithValidReturn() {
        String script =
                Utils.lines(
                        "var returnCalled = false;",
                        "var iterable = {",
                        "    next: function() {",
                        "        return {value: 1, done: false};",
                        "    },",
                        "    return: function(value) {",
                        "        returnCalled = true;",
                        "        return {value: value + 10, done: true};",
                        "    },",
                        "};",
                        "",
                        "iterable[Symbol.iterator] = function() {",
                        "    return iterable;",
                        "};",
                        "",
                        "function* generator() {",
                        "    yield* iterable;",
                        "}",
                        "",
                        "var iterator = generator();",
                        "iterator.next();",
                        "",
                        "var result = iterator.return(5);");

        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    Scriptable scope = cx.initStandardObjects();
                    cx.evaluateString(scope, script, "test", 1, null);

                    Object result = scope.get("result", scope);
                    assertTrue("result should be a Scriptable", result instanceof Scriptable);

                    Scriptable resultObj = (Scriptable) result;
                    assertEquals(
                            "Result value should be 15",
                            15.0,
                            getNumberProperty(resultObj, "value"),
                            0.001);
                    assertEquals(
                            "Result should be done",
                            Boolean.TRUE,
                            resultObj.get("done", resultObj));

                    Object returnCalled = scope.get("returnCalled", scope);
                    assertEquals("Return method should be called", Boolean.TRUE, returnCalled);
                    return null;
                });
    }

    private double getNumberProperty(Scriptable obj, String property) {
        Object value = obj.get(property, obj);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new AssertionError("Property " + property + " is not a number: " + value);
    }
}
