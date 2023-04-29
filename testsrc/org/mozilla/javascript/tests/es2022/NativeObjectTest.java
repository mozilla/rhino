/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** Test for the Object.hasOwn */
package org.mozilla.javascript.tests.es2022;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tests.Utils;

public class NativeObjectTest {

    @Test
    public void hasStringOwn() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "let result = Object.hasOwn({ test: '123' }, 'test');\n"
                                            + "'result = ' + result",
                                    "test",
                                    1,
                                    null);
                    assertEquals("result = true", result);

                    return null;
                });
    }

    @Test
    public void hasUndefinedOwn() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "let result = Object.hasOwn({ test: undefined }, 'test');\n"
                                            + "'result = ' + result;",
                                    "test",
                                    1,
                                    null);
                    assertEquals("result = true", result);

                    return null;
                });
    }

    @Test
    public void hasNullOwn() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "let result = Object.hasOwn({ test: null }, 'test');\n"
                                            + "'result = ' + result;",
                                    "test",
                                    1,
                                    null);
                    assertEquals("result = true", result);

                    return null;
                });
    }

    @Test
    public void hasArrayPropertyOwn() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

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

                    return null;
                });
    }

    @Test
    public void hHasNoOwn() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "let result = Object.hasOwn({ cake: 123 }, 'test');\n"
                                            + "'result = ' + result",
                                    "test",
                                    1,
                                    null);
                    assertEquals("result = false", result);

                    return null;
                });
    }

    @Test
    public void createHasOwn() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

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

                    return null;
                });
    }

    @Test
    public void createNoHasOwn() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var result = Object.hasOwn(Object.create({ q: 321 }), 'q');\n"
                                            + "'result = ' + result; ",
                                    "test",
                                    1,
                                    null);
                    assertEquals("result = false", result);

                    return null;
                });
    }

    @Test
    public void calledTest() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

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

                    return null;
                });
    }
}
