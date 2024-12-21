/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import java.util.TimeZone;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tests.Utils;

/** Test for NativeDate. */
public class NativeDateTest {

    @Test
    public void ctorDateTimeStringGMT() {
        ctorDateTimeString(
                "2021-12-18T22:23:00.000Z",
                "new Date('2021-12-18T22:23:00.000+00:00').toISOString()");
        ctorDateTimeString(
                "2021-12-18T22:23:00.000Z",
                "new Date('Sat, 18 Dec 2021 22:23:00 GMT').toISOString()");
    }

    @Test
    public void ctorDateTimeStringUTC() {
        ctorDateTimeString(
                "2021-12-18T22:23:00.000Z",
                "new Date('2021-12-18T22:23:00.000+00:00').toISOString()");
        ctorDateTimeString(
                "2021-12-18T22:23:00.000Z",
                "new Date('Sat, 18 Dec 2021 22:23:00 UTC').toISOString()");
    }

    @Test
    public void ctorDateTimeStringUT() {
        ctorDateTimeString(
                "2021-12-18T22:23:00.000Z",
                "new Date('Sat, 18 Dec 2021 22:23:00 UT').toISOString()");
    }

    @Test
    public void ctorDateTimeStringEST() {
        ctorDateTimeString(
                "2021-12-18T12:23:00.000Z",
                "new Date('2021-12-18T17:23:00.000+05:00').toISOString()");
        ctorDateTimeString(
                "2021-12-19T03:23:00.000Z",
                "new Date('Sat, 18 Dec 2021 22:23:00 EST').toISOString()");
    }

    @Test
    public void ctorDateTimeStringEDT() {
        ctorDateTimeString(
                "2021-12-19T02:23:00.000Z",
                "new Date('Sat, 18 Dec 2021 22:23:00 EDT').toISOString()");
    }

    @Test
    public void ctorDateTimeStringCST() {
        ctorDateTimeString(
                "2021-12-19T04:23:00.000Z",
                "new Date('Sat, 18 Dec 2021 22:23:00 CST').toISOString()");
    }

    @Test
    public void ctorDateTimeStringCDT() {
        ctorDateTimeString(
                "2021-12-19T03:23:00.000Z",
                "new Date('Sat, 18 Dec 2021 22:23:00 CDT').toISOString()");
    }

    @Test
    public void ctorDateTimeStringMST() {
        ctorDateTimeString(
                "2021-12-19T05:23:00.000Z",
                "new Date('Sat, 18 Dec 2021 22:23:00 MST').toISOString()");
    }

    @Test
    public void ctorDateTimeStringMDT() {
        ctorDateTimeString(
                "2021-12-19T04:23:00.000Z",
                "new Date('Sat, 18 Dec 2021 22:23:00 MDT').toISOString()");
    }

    @Test
    public void ctorDateTimeStringPST() {
        ctorDateTimeString(
                "2021-12-19T06:23:00.000Z",
                "new Date('Sat, 18 Dec 2021 22:23:00 PST').toISOString()");
    }

    @Test
    public void ctorDateTimeStringPDT() {
        ctorDateTimeString(
                "2021-12-19T05:23:00.000Z",
                "new Date('Sat, 18 Dec 2021 22:23:00 PDT').toISOString()");
    }

    @Test
    public void ctorDateTimeStringBerlin() {
        ctorDateTimeString(
                "2021-12-18T18:23:00.000Z",
                "new Date('2021-12-18T17:23:00.000-01:00').toISOString()");
    }

    @Test
    public void ctorDateTimeStringTokyo() {
        ctorDateTimeString(
                "2021-12-19T02:23:00.000Z",
                "new Date('2021-12-18T17:23:00.000-09:00').toISOString()");
    }

    @Test
    public void ctorDateTimeStringJST() {
        ctorDateTimeString(
                "2021-12-19T02:23:00.000Z",
                "new Date('2021-12-18T17:23:00.000-09:00').toISOString()");
    }

    @Test
    public void ctorDateTimeStringNewYork() {
        ctorDateTimeString(
                "2021-12-18T12:23:00.000Z",
                "new Date('2021-12-18T17:23:00.000+05:00').toISOString()");
    }

    private static void ctorDateTimeString(final String expected, final String js) {
        Utils.runWithAllModes(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone("GMT"));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals(expected, res);
                    return null;
                });
    }

    @Test
    public void ctorDateTimeGMT() {
        ctorDateTime("2021-12-18T22:23:00.000Z", "GMT");
    }

    @Test
    public void ctorDateTimeUTC() {
        ctorDateTime("2021-12-18T22:23:00.000Z", "UTC");
    }

    @Test
    public void ctorDateTimeEST() {
        ctorDateTime("2021-12-19T03:23:00.000Z", "EST");
    }

    @Test
    public void ctorDateTimeBerlin() {
        ctorDateTime("2021-12-18T21:23:00.000Z", "Europe/Berlin");
    }

    @Test
    public void ctorDateTimeBerlinDaylightSavingTime() {
        String js = "new Date('2021-07-18T22:23').toISOString()";

        Utils.runWithAllModes(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("2021-07-18T20:23:00.000Z", res);
                    return null;
                });
    }

    @Test
    public void ctorDateTimeTokyo() {
        ctorDateTime("2021-12-18T13:23:00.000Z", "Asia/Tokyo");
    }

    @Test
    public void ctorDateTimeJST() {
        ctorDateTime("2021-12-18T13:23:00.000Z", "JST");
    }

    @Test
    public void ctorDateTimeNewYork() {
        ctorDateTime("2021-12-19T03:23:00.000Z", "America/New_York");
    }

    private static void ctorDateTime(final String expected, final String tz) {
        final String js = "new Date('2021-12-18T22:23').toISOString()";
        Utils.runWithAllModes(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone(tz));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals(expected, res);
                    return null;
                });
    }

    @Test
    public void ctorDateGMT() {
        ctorDate("2021-12-18T00:00:00.000Z", "GMT");
    }

    @Test
    public void ctorDateUTC() {
        ctorDate("2021-12-18T00:00:00.000Z", "UTC");
    }

    @Test
    public void ctorDateEST() {
        ctorDate("2021-12-18T00:00:00.000Z", "EST");
    }

    @Test
    public void ctorDateBerlin() {
        ctorDate("2021-12-18T00:00:00.000Z", "Europe/Berlin");
    }

    @Test
    public void ctorDateNewYork() {
        ctorDate("2021-12-18T00:00:00.000Z", "America/New_York");
    }

    @Test
    public void ctorDateTokyo() {
        ctorDate("2021-12-18T00:00:00.000Z", "Asia/Tokyo");
    }

    @Test
    public void ctorDateJST() {
        ctorDate("2021-12-18T00:00:00.000Z", "JST");
    }

    private static void ctorDate(final String expected, final String tz) {
        final String js = "new Date('2021-12-18').toISOString()";
        Utils.runWithAllModes(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone(tz));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals(expected, res);
                    return null;
                });
    }

    @Test
    public void ctorInt() {
        Utils.assertWithAllModes(
                "2000-02-28T23:59:59.000Z", "new Date(951782399000).toISOString()");
    }

    @Test
    public void ctorDouble() {
        Utils.assertWithAllModes("2035-11-30T01:46:40.000Z", "new Date(208e10).toISOString()");
    }

    @Test
    public void toLocaleEnUs() {
        // real browser toLocale("12/18/2021, 10:23:00 PM", "new
        // Date('2021-12-18T22:23').toLocaleString('en-US')");
        // toLocale("12/18/21 10:23 PM", "new Date('2021-12-18T22:23').toLocaleString('en-US')");
        toLocale("12/18/21, 10:23 PM", "new Date('2021-12-18T22:23').toLocaleString('en-US')");

        // real browser toLocale("12/18/2021", "new
        // Date('2021-12-18T22:23').toLocaleDateString('en-US')");
        toLocale("12/18/21", "new Date('2021-12-18T22:23').toLocaleDateString('en-US')");

        // real browser toLocale("10:23:00 PM", "new
        // Date('2021-12-18T22:23').toLocaleTimeString('en-US')");
        toLocale("10:23 PM", "new Date('2021-12-18T22:23').toLocaleTimeString('en-US')");
    }

    @Test
    public void toLocaleDeDe() {
        // real browser toLocale("18.12.2021, 22:23:00", "new
        // Date('2021-12-18T22:23').toLocaleString('de-DE')");
        // toLocale("18.12.21 22:23", "new Date('2021-12-18T22:23').toLocaleString('de-DE')");
        toLocale("18.12.21, 22:23", "new Date('2021-12-18T22:23').toLocaleString('de-DE')");

        // real browser toLocale("18.12.2021", "new
        // Date('2021-12-18T22:23').toLocaleDateString('de-DE')");
        toLocale("18.12.21", "new Date('2021-12-18T22:23').toLocaleDateString('de-DE')");

        // real browser toLocale("22:23:00", "new
        // Date('2021-12-18T22:23').toLocaleTimeString('de-DE')");
        toLocale("22:23", "new Date('2021-12-18T22:23').toLocaleTimeString('de-DE')");
    }

    @Test
    public void toLocaleJaJp() {
        // real browser toLocale("2021/12/18 22:23:00", "new
        // Date('2021-12-18T22:23').toLocaleString('ja-JP')");
        // toLocale("21/12/18 22:23", "new Date('2021-12-18T22:23').toLocaleString('ja-JP')");
        toLocale("2021/12/18 22:23", "new Date('2021-12-18T22:23').toLocaleString('ja-JP')");

        // real browser toLocale("2021/12/18", "new
        // Date('2021-12-18T22:23').toLocaleDateString('ja-JP')");
        // toLocale("21/12/18", "new Date('2021-12-18T22:23').toLocaleDateString('ja-JP')");
        toLocale("2021/12/18", "new Date('2021-12-18T22:23').toLocaleDateString('ja-JP')");

        // real browser toLocale("22:23:00", "new
        // Date('2021-12-18T22:23').toLocaleTimeString('ja-JP')");
        toLocale("22:23", "new Date('2021-12-18T22:23').toLocaleTimeString('ja-JP')");
    }

    @Test
    public void toLocaleArray() {
        // real browser toLocale("2021/12/18 22:23:00", "new
        // Date('2021-12-18T22:23').toLocaleString(['foo', 'ja-JP', 'en-US'])");
        // toLocale("21/12/18 22:23", "new Date('2021-12-18T22:23').toLocaleString(['foo', 'ja-JP',
        // 'en-US'])");
        toLocale(
                "2021/12/18 22:23",
                "new Date('2021-12-18T22:23').toLocaleString(['foo', 'ja-JP', 'en-US'])");

        // real browser toLocale("2021/12/18", "new
        // Date('2021-12-18T22:23').toLocaleDateString(['foo', 'ja-JP', 'en-US'])");
        // toLocale("21/12/18", "new Date('2021-12-18T22:23').toLocaleDateString(['foo', 'ja-JP',
        // 'en-US'])");
        toLocale(
                "2021/12/18",
                "new Date('2021-12-18T22:23').toLocaleDateString(['foo', 'ja-JP', 'en-US'])");

        // real browser toLocale("22:23:00", "new
        // Date('2021-12-18T22:23').toLocaleTimeString(['foo', 'ja-JP', 'en-US'])");
        toLocale(
                "22:23",
                "new Date('2021-12-18T22:23').toLocaleTimeString(['foo', 'ja-JP', 'en-US'])");
    }

    private static void toLocale(final String expected, final String js) {
        Utils.runWithAllModes(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone("GMT"));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals(expected, res);
                    return null;
                });
    }

    @Test
    public void toDateStringGMT() {
        toDateString("Sat Dec 18 2021", "GMT");
    }

    @Test
    public void toDateStringUTC() {
        toDateString("Sat Dec 18 2021", "UTC");
    }

    @Test
    public void toDateStringEST() {
        toDateString("Sat Dec 18 2021", "EST");
    }

    @Test
    public void toDateStringBerlin() {
        toDateString("Sat Dec 18 2021", "Europe/Berlin");
    }

    @Test
    public void toDateStringNewYork() {
        toDateString("Sat Dec 18 2021", "America/New_York");
    }

    @Test
    public void toDateStringTokyo() {
        toDateString("Sun Dec 19 2021", "Asia/Tokyo");
    }

    @Test
    public void toDateStringJST() {
        toDateString("Sun Dec 19 2021", "JST");
    }

    private static void toDateString(final String expected, final String tz) {
        final String js = "new Date('Sat, 18 Dec 2021 22:23:00 UTC').toDateString()";
        Utils.runWithAllModes(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone(tz));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals(expected, res);
                    return null;
                });
    }

    @Test
    public void toTimeStringGMT() {
        toTimeString("22:23:00 GMT-0000 (GMT)", "GMT");
    }

    @Test
    public void toTimeStringUTC() {
        toTimeString("22:23:00 GMT-0000 (UTC)", "UTC");
    }

    @Test
    public void toTimeStringEST() {
        toTimeString("17:23:00 GMT-0500 (-05:00)", "EST");
    }

    @Test
    public void toTimeStringBerlin() {
        // real browser toTimeString("22:23:00 GMT+0100 (Central European Standard Time)",
        // "Europe/Berlin");
        toTimeString("23:23:00 GMT+0100 (CET)", "Europe/Berlin");
    }

    @Test
    public void toTimeStringNewYork() {
        toTimeString("17:23:00 GMT-0500 (EST)", "America/New_York");
    }

    @Test
    public void toTimeStringTokyo() {
        toTimeString("07:23:00 GMT+0900 (JST)", "Asia/Tokyo");
    }

    @Test
    public void toTimeStringJST() {
        toTimeString("07:23:00 GMT+0900 (JST)", "JST");
    }

    private static void toTimeString(final String expected, final String tz) {
        final String js = "new Date('Sat, 18 Dec 2021 22:23:00 UTC').toTimeString()";
        Utils.runWithAllModes(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone(tz));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals(expected, res);
                    return null;
                });
    }

    @Test
    public void toUTCStringGMT() {
        toUTCString("Sat, 18 Dec 2021 22:23:00 GMT", "GMT");
    }

    @Test
    public void toUTCStringUTC() {
        toUTCString("Sat, 18 Dec 2021 22:23:00 GMT", "UTC");
    }

    @Test
    public void toUTCStringEST() {
        toUTCString("Sat, 18 Dec 2021 22:23:00 GMT", "EST");
    }

    @Test
    public void toUTCStringBerlin() {
        toUTCString("Sat, 18 Dec 2021 22:23:00 GMT", "Europe/Berlin");
    }

    @Test
    public void toUTCStringNewYork() {
        toUTCString("Sat, 18 Dec 2021 22:23:00 GMT", "America/New_York");
    }

    @Test
    public void toUTCStringTokyo() {
        toUTCString("Sat, 18 Dec 2021 22:23:00 GMT", "Asia/Tokyo");
    }

    @Test
    public void toUTCStringJST() {
        toUTCString("Sat, 18 Dec 2021 22:23:00 GMT", "JST");
    }

    private static void toUTCString(final String expected, final String tz) {
        final String js = "new Date('Sat, 18 Dec 2021 22:23:00 UTC').toUTCString()";
        Utils.runWithAllModes(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone(tz));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals(expected, res);
                    return null;
                });
    }

    @Test
    public void timezoneOffsetGMT() {
        timezoneOffset(0, "GMT");
    }

    @Test
    public void timezoneOffsetUTC() {
        timezoneOffset(0, "UTC");
    }

    @Test
    public void timezoneOffsetEST() {
        timezoneOffset(300, "EST");
    }

    @Test
    public void timezoneOffsetBerlin() {
        timezoneOffset(-60, "Europe/Berlin");
    }

    @Test
    public void timezoneOffsetNewYork() {
        timezoneOffset(300, "America/New_York");
    }

    @Test
    public void timezoneOffsetTokyo() {
        timezoneOffset(-540, "Asia/Tokyo");
    }

    @Test
    public void timezoneOffsetJST() {
        timezoneOffset(-540, "JST");
    }

    private static void timezoneOffset(final int expected, final String tz) {
        final String js = "new Date(0).getTimezoneOffset()";
        Utils.runWithAllModes(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone(tz));

                    final Double res = (Double) cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals(expected, res.doubleValue(), 0.0001);
                    return null;
                });
    }
}
