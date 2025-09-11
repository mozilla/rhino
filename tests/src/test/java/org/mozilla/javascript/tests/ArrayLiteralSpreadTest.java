/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.testutils.Utils;

/** Tests for array literal spread syntax (...) functionality. */
public class ArrayLiteralSpreadTest {

    private Object evaluateScript(String script) {
        Context cx = Context.enter();
        try {
            cx.setInterpretedMode(true);
            cx.setLanguageVersion(Context.VERSION_ES6); // Enable ES6 features
            Scriptable scope = cx.initStandardObjects();
            return cx.evaluateString(scope, script, "test", 1, null);
        } finally {
            Context.exit();
        }
    }

    @Test
    public void testBasicSpread() {
        String script =
                "var arr1 = [1, 2, 3];"
                        + "var arr2 = [4, 5, 6];"
                        + "var result = [...arr1, ...arr2];"
                        + "result.join(',');";
        Utils.assertWithAllModes("1,2,3,4,5,6", script);
    }

    @Test
    public void testSpreadWithElements() {
        String script =
                "var arr1 = [2, 3];"
                        + "var arr2 = [5, 6];"
                        + "var result = [1, ...arr1, 4, ...arr2, 7];"
                        + "result.join(',');";
        Utils.assertWithAllModes("1,2,3,4,5,6,7", script);
    }

    @Test
    public void testEmptyArraySpread() {
        String script = "var empty = [];" + "var result = [...empty, 1, 2];" + "result.join(',');";
        Utils.assertWithAllModes("1,2", script);
    }

    @Test
    public void testSpreadEmptyArrays() {
        String script =
                "var empty1 = [];"
                        + "var empty2 = [];"
                        + "var result = [...empty1, ...empty2, 1];"
                        + "result.join(',');";
        Utils.assertWithAllModes("1", script);
    }

    @Test
    public void testSingleElementSpread() {
        String script =
                "var single = [42];" + "var result = [...single, ...single];" + "result.join(',');";
        Utils.assertWithAllModes("42,42", script);
    }

    @Test
    public void testNestedArraySpread() {
        String script =
                "var arr1 = [1, [2, 3]];"
                        + "var arr2 = [4, 5];"
                        + "var result = [...arr1, ...arr2];"
                        + "result.length + ',' + result[0] + ',' + result[1].join('-') + ',' + result[2] + ',' + result[3];";
        Utils.assertWithAllModes("4,1,2-3,4,5", script);
    }

    @Test
    public void testSpreadAtBeginning() {
        String script = "var arr = [1, 2];" + "var result = [...arr, 3, 4];" + "result.join(',');";
        Utils.assertWithAllModes("1,2,3,4", script);
    }

    @Test
    public void testSpreadAtEnd() {
        String script = "var arr = [3, 4];" + "var result = [1, 2, ...arr];" + "result.join(',');";
        Utils.assertWithAllModes("1,2,3,4", script);
    }

    @Test
    public void testSpreadInMiddle() {
        String script = "var arr = [2, 3];" + "var result = [1, ...arr, 4];" + "result.join(',');";
        Utils.assertWithAllModes("1,2,3,4", script);
    }

    @Test
    public void testMultipleConsecutiveSpreads() {
        String script =
                "var arr1 = [1, 2];"
                        + "var arr2 = [3, 4];"
                        + "var arr3 = [5, 6];"
                        + "var result = [...arr1, ...arr2, ...arr3];"
                        + "result.join(',');";
        Utils.assertWithAllModes("1,2,3,4,5,6", script);
    }

    @Test
    public void testSpreadWithUndefinedElements() {
        String script =
                "var sparse = [1, , 3];"
                        + "var result = [...sparse];"
                        + "result.length + ',' + result[0] + ',' + (result[1] === undefined) + ',' + result[2];";
        Utils.assertWithAllModes("3,1,true,3", script);
    }

    @Test
    public void testSpreadLargeArray() {
        String script =
                "var arr = [];"
                        + "for (var i = 0; i < 100; i++) arr.push(i);"
                        + "var result = [999, ...arr, 1000];"
                        + "result.length + ',' + result[0] + ',' + result[1] + ',' + result[100] + ',' + result[101];";
        Utils.assertWithAllModes("102,999,0,99,1000", script);
    }

    @Test
    public void testSpreadArrayLikeObjects() {
        String script =
                "var arrayLike = {0: 'a', 1: 'b', 2: 'c', length: 3};"
                        + "var arr = Array.prototype.slice.call(arrayLike);"
                        + "var result = ['x', ...arr, 'y'];"
                        + "result.join(',');";
        Utils.assertWithAllModes("x,a,b,c,y", script);
    }

    @Test
    public void testSpreadOnlyArray() {
        String script = "var arr = [1, 2, 3];" + "var result = [...arr];" + "result.join(',');";
        Utils.assertWithAllModes("1,2,3", script);
    }
}
