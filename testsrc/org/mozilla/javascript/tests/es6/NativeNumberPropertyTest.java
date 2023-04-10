package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tests.Utils;

public class NativeNumberPropertyTest {

    @Test
    public void testDefiningAProperty() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var func = function (number) {"
                                            + "   number.snippetText = 'abc';"
                                            + "   return number.snippetText;"
                                            + "};"
                                            + "try { "
                                            + "  '' + func(-334918463);"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals("undefined", result);

                    return null;
                });
    }

    @Test
    public void testDefiningAPropertyStrict() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var func = function (number) {"
                                            + "  'use strict';"
                                            + "   number.snippetText = 'abc';"
                                            + "   return number.snippetText;"
                                            + "};"
                                            + "try { "
                                            + "  '' + func(-334918463);"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals(
                            "Cannot set property \"snippetText\" of -334918463 to \"abc\"", result);

                    return null;
                });
    }

    @Test
    public void testExtensible() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var func = function (number) {"
                                            + "   return Object.isExtensible(number) + ' ' + Object.isExtensible(new Object(number));"
                                            + "};"
                                            + "try { "
                                            + "  func(-334918463);"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals("false true", result);

                    return null;
                });
    }

    @Test
    public void testExtensibleStrict() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var func = function (number) {"
                                            + "  'use strict';"
                                            + "   return Object.isExtensible(number) + ' ' + Object.isExtensible(new Object(number));"
                                            + "};"
                                            + "try { "
                                            + "  func(-334918463);"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals("false true", result);

                    return null;
                });
    }

    @Test
    public void testSealed() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var func = function (number) {"
                                            + "   return Object.isSealed(number) + ' ' + Object.isSealed(new Object(number));"
                                            + "};"
                                            + "try { "
                                            + "  func(-334918463);"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals("true false", result);

                    return null;
                });
    }

    @Test
    public void testSealedStrict() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var func = function (number) {"
                                            + "  'use strict';"
                                            + "   return Object.isSealed(number) + ' ' + Object.isSealed(new Object(number));"
                                            + "};"
                                            + "try { "
                                            + "  func(-334918463);"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals("true false", result);

                    return null;
                });
    }
}
