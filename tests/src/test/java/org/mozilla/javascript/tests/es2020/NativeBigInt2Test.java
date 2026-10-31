/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2020;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.testutils.Utils;

public class NativeBigInt2Test {

    @Test
    public void toLocaleString_no402() {
        Utils.assertWithAllModes("1", "let n = 1n; n.toLocaleString()");
        Utils.assertWithAllModes("-7", "(-7n).toLocaleString()");
        Utils.assertWithAllModes("10000", "let n = 10000n; n.toLocaleString()");
        Utils.assertWithAllModes(
                "12345678901234567890", "let n = 12345678901234567890n; n.toLocaleString()");
    }

    @Test
    public void toLocaleString_no402IgnoreParams() {
        Utils.assertWithAllModes("1234", "let n = 1234n; n.toLocaleString('en-US')");
        Utils.assertWithAllModes("1234", "let n = 1234n; n.toLocaleString('de')");
        Utils.assertWithAllModes("1234", "let n = 1234n; n.toLocaleString('de-DE')");
        Utils.assertWithAllModes("255", "let n = 255n; n.toLocaleString(16)");
    }

    @Test
    public void toLocaleString() {
        Utils.assertWithAllModes402("1", "let n = 1n; n.toLocaleString()");
        Utils.assertWithAllModes402("-7", "(-7n).toLocaleString()");
        Utils.assertWithAllModes402("10,000", "let n = 10000n; n.toLocaleString()");
        Utils.assertWithAllModes402(
                "12,345,678,901,234,567,890", "let n = 12345678901234567890n; n.toLocaleString()");
    }

    @Test
    public void toLocaleString_Invalid() {
        Utils.assertException(
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTL_402),
                Context.VERSION_ES6,
                EcmaError.class,
                "RangeError: Invalid language tag: 77",
                "let n = 1n; n.toLocaleString('77')");

        Utils.assertWithAllModes402("1", "let n = 1n; n.toLocaleString(77)");
    }

    @Test
    public void toLocaleString_GermanContext() {
        Utils.assertWithAllModes402("1", "let n = 1n; n.toLocaleString()", Locale.GERMAN);
        Utils.assertWithAllModes402("-7", "(-7n).toLocaleString()", Locale.GERMAN);
        Utils.assertWithAllModes402("10.000", "let n = 10000n; n.toLocaleString()", Locale.GERMAN);
        Utils.assertWithAllModes402(
                "12.345.678.901.234.567.890",
                "let n = 12345678901234567890n; n.toLocaleString()",
                Locale.GERMAN);
    }

    @Test
    public void toLocaleString_JapanContext() {
        Utils.assertWithAllModes402("1", "let n = 1n; n.toLocaleString()", Locale.JAPAN);
        Utils.assertWithAllModes402("-7", "(-7n).toLocaleString()", Locale.JAPAN);
        Utils.assertWithAllModes402("10,000", "let n = 10000n; n.toLocaleString()", Locale.JAPAN);
        Utils.assertWithAllModes402(
                "12,345,678,901,234,567,890",
                "let n = 12345678901234567890n; n.toLocaleString()",
                Locale.JAPAN);
    }

    @Test
    public void toLocaleString_ArabicContext() {
        Utils.assertWithAllModes402(
                "\u0661", "let n = 1n; n.toLocaleString('ar-SA')", Locale.forLanguageTag("ar-SA"));
        Utils.assertWithAllModes402(
                "\u0661\u0660\u066c\u0660\u0660\u0660",
                "let n = 10000n; n.toLocaleString('ar-SA')",
                Locale.forLanguageTag("ar-SA"));
    }

    @Test
    public void toLocaleString_de() {
        Utils.assertWithAllModes402("1", "let n = 1n; n.toLocaleString('de')");
        Utils.assertWithAllModes402("-7", "(-7n).toLocaleString('de')");
        Utils.assertWithAllModes402("10.000", "let n = 10000n; n.toLocaleString('de')");
        Utils.assertWithAllModes402(
                "12.345.678.901.234.567.890",
                "let n = 12345678901234567890n; n.toLocaleString('de')");
    }

    @Test
    public void toLocaleString_deDE() {
        Utils.assertWithAllModes402("1", "let n = 1n; n.toLocaleString('de-DE')");
        Utils.assertWithAllModes402("-7", "(-7n).toLocaleString('de-DE')");
        Utils.assertWithAllModes402("10.000", "let n = 10000n; n.toLocaleString('de-DE')");
        Utils.assertWithAllModes402(
                "12.345.678.901.234.567.890",
                "let n = 12345678901234567890n; n.toLocaleString('de-DE')");
    }

    @Test
    public void toLocaleString_Japan() {
        Utils.assertWithAllModes402("1", "let n = 1n; n.toLocaleString('japan')");
        Utils.assertWithAllModes402("-7", "(-7n).toLocaleString('japan')");
        Utils.assertWithAllModes402("10,000", "let n = 10000n; n.toLocaleString('japan')");
        Utils.assertWithAllModes402(
                "12,345,678,901,234,567,890",
                "let n = 12345678901234567890n; n.toLocaleString('japan')");
    }

    @Test
    public void toLocaleString_Arabic() {
        Utils.assertWithAllModes402("\u0661", "let n = 1n; n.toLocaleString('ar-SA')");
        Utils.assertWithAllModes402(
                "\u0661\u0660\u066c\u0660\u0660\u0660",
                "let n = 10000n; n.toLocaleString('ar-SA')");
    }
}
