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
    public void ctorDateTime() {
        String js = "new Date('2021-12-18T22:23').toISOString()";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone("GMT"));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("2021-12-18T22:23:00.000Z", res);
                    return null;
                });
    }

    @Test
    public void ctorDateTimeBerlin() {
        String js = "new Date('2021-12-18T22:23').toISOString()";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("2021-12-18T21:23:00.000Z", res);
                    return null;
                });
    }

    @Test
    public void ctorDateTimeBerlinDaylightSavingTime() {
        String js = "new Date('2021-07-18T22:23').toISOString()";

        Utils.runWithAllOptimizationLevels(
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
    public void ctorDateTimeNewYork() {
        String js = "new Date('2021-12-18T22:23').toISOString()";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone("America/New_York"));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("2021-12-19T03:23:00.000Z", res);
                    return null;
                });
    }

    @Test
    public void ctorDate() {
        String js = "new Date('2021-12-18').toISOString()";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone("GMT"));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("2021-12-18T00:00:00.000Z", res);
                    return null;
                });
    }

    @Test
    public void ctorDateBerlin() {
        String js = "new Date('2021-12-18').toISOString()";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("2021-12-18T00:00:00.000Z", res);
                    return null;
                });
    }

    @Test
    public void ctorDateNewYork() {
        String js = "new Date('2021-12-18').toISOString()";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone("America/New_York"));

                    final Object res = cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals("2021-12-18T00:00:00.000Z", res);
                    return null;
                });
    }

    @Test
    public void timezoneOffset() {
        String js = "new Date(0).getTimezoneOffset()";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone("GMT"));

                    final Double res = (Double) cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals(0, res.doubleValue(), 0.0001);
                    return null;
                });
    }

    @Test
    public void timezoneOffsetBerlin() {
        String js = "new Date(0).getTimezoneOffset()";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));

                    final Double res = (Double) cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals(-60, res.doubleValue(), 0.0001);
                    return null;
                });
    }

    @Test
    public void timezoneOffsetNewYork() {
        String js = "new Date(0).getTimezoneOffset()";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.setTimeZone(TimeZone.getTimeZone("America/New_York"));

                    final Double res = (Double) cx.evaluateString(scope, js, "test.js", 0, null);
                    assertEquals(300, res.doubleValue(), 0.0001);
                    return null;
                });
    }
}
