/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for array literal spread syntax (...) functionality. */
class ArrayLiteralSpreadTest {
    @Test
    void unsupportedInES5() {
        String script = "var arr = [1, 2, 3];" + "var result = [...arr]";
        Utils.assertEvaluatorException_1_8("syntax error (test#1)", script);
    }

    @Test
    void testBasicSpread() {
        String script =
                "var arr1 = [1, 2, 3];"
                        + "var arr2 = [4, 5, 6];"
                        + "var result = [...arr1, ...arr2];"
                        + "result.join(',');";
        Utils.assertWithAllModes_ES6("1,2,3,4,5,6", script);
    }

    @Test
    void testSpreadWithElements() {
        String script =
                "var arr1 = [2, 3];"
                        + "var arr2 = [5, 6];"
                        + "var result = [1, ...arr1, 4, ...arr2, 7];"
                        + "result.join(',');";
        Utils.assertWithAllModes_ES6("1,2,3,4,5,6,7", script);
    }

    @Test
    void testEmptyArraySpread() {
        String script = "var empty = [];" + "var result = [...empty, 1, 2];" + "result.join(',');";
        Utils.assertWithAllModes_ES6("1,2", script);
    }

    @Test
    void testSpreadEmptyArrays() {
        String script =
                "var empty1 = [];"
                        + "var empty2 = [];"
                        + "var result = [...empty1, ...empty2, 1];"
                        + "result.join(',');";
        Utils.assertWithAllModes_ES6("1", script);
    }

    @Test
    void testSingleElementSpread() {
        String script =
                "var single = [42];" + "var result = [...single, ...single];" + "result.join(',');";
        Utils.assertWithAllModes_ES6("42,42", script);
    }

    @Test
    void testNestedArraySpread() {
        String script =
                "var arr1 = [1, [2, 3]];"
                        + "var arr2 = [4, 5];"
                        + "var result = [...arr1, ...arr2];"
                        + "result.length + ',' + result[0] + ',' + result[1].join('-') + ',' + result[2] + ',' + result[3];";
        Utils.assertWithAllModes("4,1,2-3,4,5", script);
    }

    @Test
    void testSpreadAtBeginning() {
        String script = "var arr = [1, 2];" + "var result = [...arr, 3, 4];" + "result.join(',');";
        Utils.assertWithAllModes_ES6("1,2,3,4", script);
    }

    @Test
    void testSpreadAtEnd() {
        String script = "var arr = [3, 4];" + "var result = [1, 2, ...arr];" + "result.join(',');";
        Utils.assertWithAllModes_ES6("1,2,3,4", script);
    }

    @Test
    void testSpreadInMiddle() {
        String script = "var arr = [2, 3];" + "var result = [1, ...arr, 4];" + "result.join(',');";
        Utils.assertWithAllModes_ES6("1,2,3,4", script);
    }

    @Test
    void testMultipleConsecutiveSpreads() {
        String script =
                "var arr1 = [1, 2];"
                        + "var arr2 = [3, 4];"
                        + "var arr3 = [5, 6];"
                        + "var result = [...arr1, ...arr2, ...arr3];"
                        + "result.join(',');";
        Utils.assertWithAllModes_ES6("1,2,3,4,5,6", script);
    }

    @Test
    void testSpreadWithUndefinedElements() {
        String script =
                "var sparse = [1, , 3];"
                        + "var result = [...sparse];"
                        + "result.length + ',' + result[0] + ',' + (result[1] === undefined) + ',' + result[2];";
        Utils.assertWithAllModes_ES6("3,1,true,3", script);
    }

    @Test
    void testSpreadLargeArray() {
        String script =
                "var arr = [];"
                        + "for (var i = 0; i < 100; i++) arr.push(i);"
                        + "var result = [999, ...arr, 1000];"
                        + "result.length + ',' + result[0] + ',' + result[1] + ',' + result[100] + ',' + result[101];";
        Utils.assertWithAllModes_ES6("102,999,0,99,1000", script);
    }

    @Test
    void testSpreadArrayLikeObjects() {
        String script =
                "var arrayLike = {0: 'a', 1: 'b', 2: 'c', length: 3};"
                        + "var arr = Array.prototype.slice.call(arrayLike);"
                        + "var result = ['x', ...arr, 'y'];"
                        + "result.join(',');";
        Utils.assertWithAllModes_ES6("x,a,b,c,y", script);
    }

    @Test
    void testSpreadOnlyArray() {
        String script = "var arr = [1, 2, 3];" + "var result = [...arr];" + "result.join(',');";
        Utils.assertWithAllModes_ES6("1,2,3", script);
    }

    @Test
    @Disabled("TODO: needs to be implemented, not passing currently")
    void testSpreadAndSkipIndexes() {
        String script =
                "var arr = [1, 2, 3];" + "var result = [0, ,...arr, , 4];" + "result.join(',');";
        Utils.assertWithAllModes_ES6("0,,1,2,3,,4", script);
    }

    @Test
    @Disabled("TODO: needs to be implemented, not passing currently")
    void testSpreadSymbolIterator() {
        String script =
                "var obj = {\n"
                        + "  *[Symbol.iterator]() {\n"
                        + "    yield 1;\n"
                        + "    yield 2;\n"
                        + "  }\n"
                        + "};\n"
                        + "var arr = [...obj];\n"
                        + "arr.join(',')\n";
        Utils.assertWithAllModes_ES6("1,2", script);
    }
}
