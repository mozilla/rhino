/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import java.util.Locale;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
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
        Utils.assertWithAllModes402("1", "let n = 1; n.toLocaleString()");
        Utils.assertWithAllModes402("1", "let n = 1.0; n.toLocaleString()");

        Utils.assertWithAllModes402("3.14", "let n = 3.14; n.toLocaleString()");
        Utils.assertWithAllModes402("-0.007", "let n = -0.007; n.toLocaleString()");

        Utils.assertWithAllModes402("10,000", "1e4.toLocaleString()");
        Utils.assertWithAllModes402("0", "let n = 1e-10; n.toLocaleString()");
        Utils.assertWithAllModes402("0", "1e-10.toLocaleString()");

        Utils.assertWithAllModes402("0.01", "1e-2.toLocaleString()");

        Utils.assertWithAllModes402("NaN", "NaN.toLocaleString()");
        Utils.assertWithAllModes402("∞", "Infinity.toLocaleString()");
    }

    @Test
    public void toLocaleString_Invalid() {
        Utils.assertException(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                Context.VERSION_ES6,
                EcmaError.class,
                "RangeError: Invalid language tag: 77",
                "let n = 1; n.toLocaleString('77')");

        Utils.assertWithAllModes402("1", "let n = 1; n.toLocaleString(77)");
    }

    @Test
    public void toLocaleString_GermanContext() {
        Utils.assertWithAllModes402("1", "let n = 1; n.toLocaleString()", Locale.GERMAN);
        Utils.assertWithAllModes402("1", "let n = 1.0; n.toLocaleString()", Locale.GERMAN);

        Utils.assertWithAllModes402("3,14", "let n = 3.14; n.toLocaleString()", Locale.GERMAN);
        Utils.assertWithAllModes402("-0,007", "let n = -0.007; n.toLocaleString()", Locale.GERMAN);

        Utils.assertWithAllModes402("10.000", "1e4.toLocaleString()", Locale.GERMAN);
        Utils.assertWithAllModes402("0", "let n = 1e-10; n.toLocaleString()", Locale.GERMAN);
        Utils.assertWithAllModes402("0", "1e-10.toLocaleString()", Locale.GERMAN);

        Utils.assertWithAllModes402("0,01", "1e-2.toLocaleString()", Locale.GERMAN);

        Utils.assertWithAllModes402("NaN", "NaN.toLocaleString()", Locale.GERMAN);
        Utils.assertWithAllModes402("∞", "Infinity.toLocaleString()", Locale.GERMAN);
    }

    @Test
    public void toLocaleString_JapanContext() {
        Utils.assertWithAllModes402("1", "let n = 1; n.toLocaleString()", Locale.JAPAN);
        Utils.assertWithAllModes402("1", "let n = 1.0; n.toLocaleString()", Locale.JAPAN);

        Utils.assertWithAllModes402("3.14", "let n = 3.14; n.toLocaleString()", Locale.JAPAN);
        Utils.assertWithAllModes402("-0.007", "let n = -0.007; n.toLocaleString()", Locale.JAPAN);

        Utils.assertWithAllModes402("10,000", "1e4.toLocaleString()", Locale.JAPAN);
        Utils.assertWithAllModes402("0", "let n = 1e-10; n.toLocaleString()", Locale.JAPAN);
        Utils.assertWithAllModes402("0", "1e-10.toLocaleString()", Locale.JAPAN);

        Utils.assertWithAllModes402("0.01", "1e-2.toLocaleString()", Locale.JAPAN);

        Utils.assertWithAllModes402("NaN", "NaN.toLocaleString()", Locale.JAPAN);
        Utils.assertWithAllModes402("∞", "Infinity.toLocaleString()", Locale.JAPAN);
    }

    @Test
    public void toLocaleString_ArabicContext() {
        Utils.assertWithAllModes402(
                "\u0661", "let n = 1; n.toLocaleString('ar-SA')", Locale.forLanguageTag("ar-SA"));
        Utils.assertWithAllModes402(
                "\u0661", "let n = 1.0; n.toLocaleString('ar-SA')", Locale.forLanguageTag("ar-SA"));

        Utils.assertWithAllModes402(
                "\u0663\u066b\u0661\u0664",
                "let n = 3.14; n.toLocaleString('ar-SA')",
                Locale.forLanguageTag("ar-SA"));

        // jdk11 produces different results
        // Utils.assertWithAllModes402(
        //         "\u061c\u002d\u0660\u066b\u0660\u0660\u0667",
        //         "let n = -0.007; n.toLocaleString('ar-SA')",
        //         Locale.forLanguageTag("ar-SA"));

        Utils.assertWithAllModes402(
                "\u0661\u0660\u066c\u0660\u0660\u0660",
                "1e4.toLocaleString('ar-SA')",
                Locale.forLanguageTag("ar-SA"));
        Utils.assertWithAllModes402(
                "\u0660",
                "let n = 1e-10; n.toLocaleString('ar-SA')",
                Locale.forLanguageTag("ar-SA"));
        Utils.assertWithAllModes402(
                "\u0660", "1e-10.toLocaleString('ar-SA')", Locale.forLanguageTag("ar-SA"));

        Utils.assertWithAllModes402(
                "\u0660\u066b\u0660\u0661",
                "1e-2.toLocaleString('ar-SA')",
                Locale.forLanguageTag("ar-SA"));

        Utils.assertWithAllModes402(
                "\u0644\u064a\u0633\u00a0\u0631\u0642\u0645",
                "NaN.toLocaleString('ar-SA')",
                Locale.forLanguageTag("ar-SA"));
        Utils.assertWithAllModes402(
                "\u221e", "Infinity.toLocaleString('ar-SA')", Locale.forLanguageTag("ar-SA"));
    }

    @Test
    public void toLocaleString_de() {
        Utils.assertWithAllModes402("1", "let n = 1; n.toLocaleString('de')");
        Utils.assertWithAllModes402("1", "let n = 1.0; n.toLocaleString('de')");

        Utils.assertWithAllModes402("3,14", "let n = 3.14; n.toLocaleString('de')");
        Utils.assertWithAllModes402("-0,007", "let n = -0.007; n.toLocaleString('de')");

        Utils.assertWithAllModes402("10.000", "1e4.toLocaleString('de')");
        Utils.assertWithAllModes402("0", "let n = 1e-10; n.toLocaleString('de')");
        Utils.assertWithAllModes402("0", "1e-10.toLocaleString('de')");

        Utils.assertWithAllModes402("0,01", "1e-2.toLocaleString('de')");

        Utils.assertWithAllModes402("NaN", "NaN.toLocaleString('de')");
        Utils.assertWithAllModes402("∞", "Infinity.toLocaleString('de')");
    }

    @Test
    public void toLocaleString_deDE() {
        Utils.assertWithAllModes402("1", "let n = 1; n.toLocaleString('de-DE')");
        Utils.assertWithAllModes402("1", "let n = 1.0; n.toLocaleString('de-DE')");

        Utils.assertWithAllModes402("3,14", "let n = 3.14; n.toLocaleString('de-DE')");
        Utils.assertWithAllModes402("-0,007", "let n = -0.007; n.toLocaleString('de-DE')");

        Utils.assertWithAllModes402("10.000", "1e4.toLocaleString('de-DE')");
        Utils.assertWithAllModes402("0", "let n = 1e-10; n.toLocaleString('de-DE')");
        Utils.assertWithAllModes402("0", "1e-10.toLocaleString('de-DE')");

        Utils.assertWithAllModes402("0,01", "1e-2.toLocaleString('de-DE')");

        Utils.assertWithAllModes402("NaN", "NaN.toLocaleString('de-DE')");
        Utils.assertWithAllModes402("∞", "Infinity.toLocaleString('de-DE')");
    }

    @Test
    public void toLocaleString_Japan() {
        Utils.assertWithAllModes402("1", "let n = 1; n.toLocaleString('japan')");
        Utils.assertWithAllModes402("1", "let n = 1.0; n.toLocaleString('japan')");

        Utils.assertWithAllModes402("3.14", "let n = 3.14; n.toLocaleString('japan')");
        Utils.assertWithAllModes402("-0.007", "let n = -0.007; n.toLocaleString('japan')");

        Utils.assertWithAllModes402("10,000", "1e4.toLocaleString('japan')");
        Utils.assertWithAllModes402("0", "let n = 1e-10; n.toLocaleString('japan')");
        Utils.assertWithAllModes402("0", "1e-10.toLocaleString('japan')");

        Utils.assertWithAllModes402("0.01", "1e-2.toLocaleString('japan')");

        Utils.assertWithAllModes402("NaN", "NaN.toLocaleString('japan')");
        Utils.assertWithAllModes402("∞", "Infinity.toLocaleString('japan')");
    }

    @Test
    public void toLocaleString_Arabic() {
        Utils.assertWithAllModes402("\u0661", "let n = 1; n.toLocaleString('ar-SA')");
        Utils.assertWithAllModes402("\u0661", "let n = 1.0; n.toLocaleString('ar-SA')");

        Utils.assertWithAllModes402(
                "\u0663\u066b\u0661\u0664", "let n = 3.14; n.toLocaleString('ar-SA')");

        // jdk11 produces different results
        // Utils.assertWithAllModes402(
        //         "\u061c\u002d\u0660\u066b\u0660\u0660\u0667",
        //         "let n = -0.007; n.toLocaleString('ar-SA')");

        Utils.assertWithAllModes402(
                "\u0661\u0660\u066c\u0660\u0660\u0660", "1e4.toLocaleString('ar-SA')");
        Utils.assertWithAllModes402("\u0660", "let n = 1e-10; n.toLocaleString('ar-SA')");
        Utils.assertWithAllModes402("\u0660", "1e-10.toLocaleString('ar-SA')");

        Utils.assertWithAllModes402("\u0660\u066b\u0660\u0661", "1e-2.toLocaleString('ar-SA')");

        Utils.assertWithAllModes402(
                "\u0644\u064a\u0633\u00a0\u0631\u0642\u0645", "NaN.toLocaleString('ar-SA')");
        Utils.assertWithAllModes402("\u221e", "Infinity.toLocaleString('ar-SA')");
    }
}
