/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests.intl402;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.testutils.Utils;

/**
 * @author Ronald Brill
 */
public class NativeStringTest {

    @Test
    public void toLocaleLowerCase() {
        String js = "'\\u0130'.toLocaleLowerCase()";

        Utils.runWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setLocale(new Locale("en"));

                    final Object rep = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("\u0069\u0307", rep);
                    return null;
                });

        Utils.runWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
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
    public void toLocaleLowerCaseParam() {
        Utils.assertWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                Context.VERSION_ES6,
                null,
                "\u0069\u0307",
                "'\\u0130'.toLocaleLowerCase('en')");
        Utils.assertWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                Context.VERSION_ES6,
                null,
                "\u0069",
                "'\\u0130'.toLocaleLowerCase('tr')");

        Utils.assertWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                Context.VERSION_ES6,
                null,
                "\u0069\u0307",
                "'\\u0130'.toLocaleLowerCase('Absurdistan')");
    }

    @Test
    public void toLocaleUpperCase() {
        String js = "'\\u0069'.toLocaleUpperCase()";

        Utils.runWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setLocale(new Locale("en"));

                    final Object rep = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("\u0049", rep);
                    return null;
                });

        Utils.runWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setLocale(new Locale("tr"));

                    final Object rep = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("\u0130", rep);
                    return null;
                });
    }

    @Test
    public void toLocaleUpperCaseParam() {
        Utils.assertWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                Context.VERSION_ES6,
                null,
                "\u0049",
                "'\\u0069'.toLocaleUpperCase('en')");
        Utils.assertWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                Context.VERSION_ES6,
                null,
                "\u0130",
                "'\\u0069'.toLocaleUpperCase('tr')");

        Utils.assertWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                Context.VERSION_ES6,
                null,
                "\u0049",
                "'\\u0069'.toLocaleUpperCase('Absurdistan')");
    }
}
