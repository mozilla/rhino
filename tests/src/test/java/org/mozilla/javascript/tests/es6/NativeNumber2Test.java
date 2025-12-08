/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.testutils.Utils;

public class NativeNumber2Test {

    @Test
    public void toLocaleString_no402() {
        Utils.assertWithAllModes("1", "let n = 1; n.toLocaleString()");
        Utils.assertWithAllModes("1", "(1.0).toLocaleString()");

        Utils.assertWithAllModes("3.14", "let n = 3.14; n.toLocaleString()");
        Utils.assertWithAllModes("-0.007", "let n = -0.007; n.toLocaleString()");

        Utils.assertWithAllModes("10000", "1e4.toLocaleString()");
        Utils.assertWithAllModes("1e-10", "let n = 1e-10; n.toLocaleString()");
        Utils.assertWithAllModes("1e-10", "1e-10.toLocaleString()");

        Utils.assertWithAllModes("0.01", "1e-2.toLocaleString()");

        Utils.assertWithAllModes("NaN", "NaN.toLocaleString()");
        Utils.assertWithAllModes("Infinity", "Infinity.toLocaleString()");
    }

    @Test
    public void toLocaleString_no402IgnoreParams() {
        Utils.assertWithAllModes("1.234", "let n = 1.234; n.toLocaleString('en-US')");
        Utils.assertWithAllModes("1.234", "let n = 1.234; n.toLocaleString('de')");
        Utils.assertWithAllModes("1.234", "let n = 1.234; n.toLocaleString('de-DE')");
    }

    @Test
    public void toLocaleString() {
        assertWithAllModes402("1", "let n = 1; n.toLocaleString()");
        assertWithAllModes402("1", "let n = 1.0; n.toLocaleString()");

        assertWithAllModes402("3.14", "let n = 3.14; n.toLocaleString()");
        assertWithAllModes402("-0.007", "let n = -0.007; n.toLocaleString()");

        assertWithAllModes402("10,000", "1e4.toLocaleString()");
        assertWithAllModes402("0", "let n = 1e-10; n.toLocaleString()");
        assertWithAllModes402("0", "1e-10.toLocaleString()");

        assertWithAllModes402("0.01", "1e-2.toLocaleString()");

        assertWithAllModes402("NaN", "NaN.toLocaleString()");
        assertWithAllModes402("∞", "Infinity.toLocaleString()");
    }

    @Test
    public void toLocaleString_Invalid() {
        Utils.assertException(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                Context.VERSION_ES6,
                EcmaError.class,
                "RangeError: Invalid language tag: 77",
                "let n = 1; n.toLocaleString('77')");

        assertWithAllModes402("1", "let n = 1; n.toLocaleString(77)");
    }

    @Test
    public void toLocaleString_GermanContext() {
        assertWithAllModes402("1", "let n = 1; n.toLocaleString()", Locale.GERMAN);
        assertWithAllModes402("1", "let n = 1.0; n.toLocaleString()", Locale.GERMAN);

        assertWithAllModes402("3,14", "let n = 3.14; n.toLocaleString()", Locale.GERMAN);
        assertWithAllModes402("-0,007", "let n = -0.007; n.toLocaleString()", Locale.GERMAN);

        assertWithAllModes402("10.000", "1e4.toLocaleString()", Locale.GERMAN);
        assertWithAllModes402("0", "let n = 1e-10; n.toLocaleString()", Locale.GERMAN);
        assertWithAllModes402("0", "1e-10.toLocaleString()", Locale.GERMAN);

        assertWithAllModes402("0,01", "1e-2.toLocaleString()", Locale.GERMAN);

        assertWithAllModes402("NaN", "NaN.toLocaleString()", Locale.GERMAN);
        assertWithAllModes402("∞", "Infinity.toLocaleString()", Locale.GERMAN);
    }

    @Test
    public void toLocaleString_JapanContext() {
        assertWithAllModes402("1", "let n = 1; n.toLocaleString()", Locale.JAPAN);
        assertWithAllModes402("1", "let n = 1.0; n.toLocaleString()", Locale.JAPAN);

        assertWithAllModes402("3.14", "let n = 3.14; n.toLocaleString()", Locale.JAPAN);
        assertWithAllModes402("-0.007", "let n = -0.007; n.toLocaleString()", Locale.JAPAN);

        assertWithAllModes402("10,000", "1e4.toLocaleString()", Locale.JAPAN);
        assertWithAllModes402("0", "let n = 1e-10; n.toLocaleString()", Locale.JAPAN);
        assertWithAllModes402("0", "1e-10.toLocaleString()", Locale.JAPAN);

        assertWithAllModes402("0.01", "1e-2.toLocaleString()", Locale.JAPAN);

        assertWithAllModes402("NaN", "NaN.toLocaleString()", Locale.JAPAN);
        assertWithAllModes402("∞", "Infinity.toLocaleString()", Locale.JAPAN);
    }

    @Test
    public void toLocaleString_ArabicContext() {
        assertWithAllModes402(
                "\u0661", "let n = 1; n.toLocaleString('ar-SA')", Locale.forLanguageTag("ar-SA"));
        assertWithAllModes402(
                "\u0661", "let n = 1.0; n.toLocaleString('ar-SA')", Locale.forLanguageTag("ar-SA"));

        assertWithAllModes402(
                "\u0663\u066b\u0661\u0664",
                "let n = 3.14; n.toLocaleString('ar-SA')",
                Locale.forLanguageTag("ar-SA"));
        assertWithAllModes402(
                "\u061c\u002d\u0660\u066b\u0660\u0660\u0667",
                "let n = -0.007; n.toLocaleString('ar-SA')",
                Locale.forLanguageTag("ar-SA"));

        assertWithAllModes402(
                "\u0661\u0660\u066c\u0660\u0660\u0660",
                "1e4.toLocaleString('ar-SA')",
                Locale.forLanguageTag("ar-SA"));
        assertWithAllModes402(
                "\u0660",
                "let n = 1e-10; n.toLocaleString('ar-SA')",
                Locale.forLanguageTag("ar-SA"));
        assertWithAllModes402(
                "\u0660", "1e-10.toLocaleString('ar-SA')", Locale.forLanguageTag("ar-SA"));

        assertWithAllModes402(
                "\u0660\u066b\u0660\u0661",
                "1e-2.toLocaleString('ar-SA')",
                Locale.forLanguageTag("ar-SA"));

        assertWithAllModes402(
                "\u0644\u064a\u0633\u00a0\u0631\u0642\u0645",
                "NaN.toLocaleString('ar-SA')",
                Locale.forLanguageTag("ar-SA"));
        assertWithAllModes402(
                "\u221e", "Infinity.toLocaleString('ar-SA')", Locale.forLanguageTag("ar-SA"));
    }

    @Test
    public void toLocaleString_de() {
        assertWithAllModes402("1", "let n = 1; n.toLocaleString('de')");
        assertWithAllModes402("1", "let n = 1.0; n.toLocaleString('de')");

        assertWithAllModes402("3,14", "let n = 3.14; n.toLocaleString('de')");
        assertWithAllModes402("-0,007", "let n = -0.007; n.toLocaleString('de')");

        assertWithAllModes402("10.000", "1e4.toLocaleString('de')");
        assertWithAllModes402("0", "let n = 1e-10; n.toLocaleString('de')");
        assertWithAllModes402("0", "1e-10.toLocaleString('de')");

        assertWithAllModes402("0,01", "1e-2.toLocaleString('de')");

        assertWithAllModes402("NaN", "NaN.toLocaleString('de')");
        assertWithAllModes402("∞", "Infinity.toLocaleString('de')");
    }

    @Test
    public void toLocaleString_deDE() {
        assertWithAllModes402("1", "let n = 1; n.toLocaleString('de-DE')");
        assertWithAllModes402("1", "let n = 1.0; n.toLocaleString('de-DE')");

        assertWithAllModes402("3,14", "let n = 3.14; n.toLocaleString('de-DE')");
        assertWithAllModes402("-0,007", "let n = -0.007; n.toLocaleString('de-DE')");

        assertWithAllModes402("10.000", "1e4.toLocaleString('de-DE')");
        assertWithAllModes402("0", "let n = 1e-10; n.toLocaleString('de-DE')");
        assertWithAllModes402("0", "1e-10.toLocaleString('de-DE')");

        assertWithAllModes402("0,01", "1e-2.toLocaleString('de-DE')");

        assertWithAllModes402("NaN", "NaN.toLocaleString('de-DE')");
        assertWithAllModes402("∞", "Infinity.toLocaleString('de-DE')");
    }

    @Test
    public void toLocaleString_Japan() {
        assertWithAllModes402("1", "let n = 1; n.toLocaleString('japan')");
        assertWithAllModes402("1", "let n = 1.0; n.toLocaleString('japan')");

        assertWithAllModes402("3.14", "let n = 3.14; n.toLocaleString('japan')");
        assertWithAllModes402("-0.007", "let n = -0.007; n.toLocaleString('japan')");

        assertWithAllModes402("10,000", "1e4.toLocaleString('japan')");
        assertWithAllModes402("0", "let n = 1e-10; n.toLocaleString('japan')");
        assertWithAllModes402("0", "1e-10.toLocaleString('japan')");

        assertWithAllModes402("0.01", "1e-2.toLocaleString('japan')");

        assertWithAllModes402("NaN", "NaN.toLocaleString('japan')");
        assertWithAllModes402("∞", "Infinity.toLocaleString('japan')");
    }

    @Test
    public void toLocaleString_Arabic() {
        assertWithAllModes402("\u0661", "let n = 1; n.toLocaleString('ar-SA')");
        assertWithAllModes402("\u0661", "let n = 1.0; n.toLocaleString('ar-SA')");

        assertWithAllModes402(
                "\u0663\u066b\u0661\u0664", "let n = 3.14; n.toLocaleString('ar-SA')");
        assertWithAllModes402(
                "\u061c\u002d\u0660\u066b\u0660\u0660\u0667",
                "let n = -0.007; n.toLocaleString('ar-SA')");

        assertWithAllModes402(
                "\u0661\u0660\u066c\u0660\u0660\u0660", "1e4.toLocaleString('ar-SA')");
        assertWithAllModes402("\u0660", "let n = 1e-10; n.toLocaleString('ar-SA')");
        assertWithAllModes402("\u0660", "1e-10.toLocaleString('ar-SA')");

        assertWithAllModes402("\u0660\u066b\u0660\u0661", "1e-2.toLocaleString('ar-SA')");

        assertWithAllModes402(
                "\u0644\u064a\u0633\u00a0\u0631\u0642\u0645", "NaN.toLocaleString('ar-SA')");
        assertWithAllModes402("\u221e", "Infinity.toLocaleString('ar-SA')");
    }

    private void assertWithAllModes402(final Object expected, final String script) {
        Utils.runWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();

                    final Object res = cx.evaluateString(scope, script, "test.js", 0, null);
                    assertEquals(expected, res);
                    return null;
                });
    }

    private void assertWithAllModes402(final Object expected, final String script, Locale locale) {
        Utils.runWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLocale(locale);

                    final Object res = cx.evaluateString(scope, script, "test.js", 0, null);

                    final StringBuilder hex = new StringBuilder();
                    for (final char c : res.toString().toCharArray()) {
                        hex.append("\\u")
                                .append(String.format("%04X", (int) c).toLowerCase(Locale.ROOT));
                    }
                    System.out.println(hex.toString());

                    assertEquals(expected, res);
                    return null;
                });
    }
}
