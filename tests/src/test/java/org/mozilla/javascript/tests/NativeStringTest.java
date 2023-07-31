/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * @author Marc Guillemot
 * @author Ronald Brill
 */
public class NativeStringTest {

    /**
     * Test for bug #492359 https://bugzilla.mozilla.org/show_bug.cgi?id=492359 Calling generic
     * String or Array functions without arguments was causing ArrayIndexOutOfBoundsException in
     * 1.7R2
     */
    @Test
    public void toLowerCaseApply() {
        assertEvaluates("hello", "var x = String.toLowerCase; x.apply('HELLO')");
        assertEvaluates(
                "hello",
                "String.toLowerCase('HELLO')"); // first patch proposed to #492359 was breaking this
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

    @Test
    public void toLocaleLowerCase() {
        String js = "'\\u0130'.toLocaleLowerCase()";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setLocale(new Locale("en"));

                    final Object rep = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("\u0069\u0307", rep);
                    return null;
                });

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setLocale(new Locale("tr"));

                    final Object rep = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("\u0069", rep);
                    return null;
                });
    }

    @Test
    public void toLocaleLowerCaseIgnoreParams() {
        String js = "'\\u0130'.toLocaleLowerCase('en')";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setLocale(new Locale("en"));

                    final Object rep = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("\u0069\u0307", rep);
                    return null;
                });

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setLocale(new Locale("tr"));

                    final Object rep = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("\u0069", rep);
                    return null;
                });
    }
}
