/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

public class IteratorPrototypeLazyTest {

    // ---- map ----

    @Test
    public void mapBasic() {
        Utils.assertWithAllModes_ES6(
                "2,4,6", "[1,2,3].values().map(function(v){ return v * 2; }).toArray().join(',');");
    }

    @Test
    public void mapPassesIndex() {
        Utils.assertWithAllModes_ES6(
                "a0,b1,c2",
                "[\"a\",\"b\",\"c\"].values().map(function(v, i){ return v + i; }).toArray().join(',');");
    }

    @Test
    public void mapIsLazy() {
        String code =
                "var calls = 0;\n"
                        + "var m = [1,2,3].values().map(function(v){ calls++; return v; });\n"
                        + "calls;";
        Utils.assertWithAllModes_ES6(0, code);
    }

    @Test
    public void mapCallbackThrowClosesSource() {
        String code =
                "var closed = false;\n"
                        + "var src = { next: function(){ return {value: 1, done: false}; },\n"
                        + "            return: function(){ closed = true; return {done: true}; } };\n"
                        + "try {\n"
                        + "  Iterator.from(src).map(function(){ throw 'oops'; }).next();\n"
                        + "  'no throw';\n"
                        + "} catch (e) { closed + ':' + e; }\n";
        Utils.assertWithAllModes_ES6("true:oops", code);
    }

    // ---- filter ----

    @Test
    public void filterBasic() {
        Utils.assertWithAllModes_ES6(
                "2,4",
                "[1,2,3,4,5].values().filter(function(v){ return v % 2 === 0; }).toArray().join(',');");
    }

    @Test
    public void filterEmpty() {
        Utils.assertWithAllModes_ES6(
                0, "[].values().filter(function(){ return true; }).toArray().length;");
    }

    // ---- take ----

    @Test
    public void takeLimitsCount() {
        Utils.assertWithAllModes_ES6("1,2", "[1,2,3,4].values().take(2).toArray().join(',');");
    }

    @Test
    public void takeZeroYieldsEmpty() {
        Utils.assertWithAllModes_ES6(0, "[1,2,3].values().take(0).toArray().length;");
    }

    @Test
    public void takeBeyondSourceLength() {
        Utils.assertWithAllModes_ES6("1,2", "[1,2].values().take(10).toArray().join(',');");
    }

    @Test
    public void takeInfinityYieldsAll() {
        Utils.assertWithAllModes_ES6(
                "1,2,3", "[1,2,3].values().take(Infinity).toArray().join(',');");
    }

    @Test
    public void takeNegativeThrows() {
        Utils.assertEcmaErrorES6(
                "RangeError: Iterator helper limit argument must be a non-negative integer.",
                "[1].values().take(-1);");
    }

    @Test
    public void takeClosesSourceOnLimitReached() {
        String code =
                "var closed = false;\n"
                        + "var i = 0;\n"
                        + "var src = { next: function(){ return {value: i++, done: false}; },\n"
                        + "            return: function(){ closed = true; return {done: true}; } };\n"
                        + "Iterator.from(src).take(2).toArray();\n"
                        + "closed + ',' + i;";
        Utils.assertWithAllModes_ES6("true,2", code);
    }

    // ---- drop ----

    @Test
    public void dropSkipsFirstN() {
        Utils.assertWithAllModes_ES6("3,4,5", "[1,2,3,4,5].values().drop(2).toArray().join(',');");
    }

    @Test
    public void dropMoreThanSourceYieldsEmpty() {
        Utils.assertWithAllModes_ES6(0, "[1,2].values().drop(5).toArray().length;");
    }

    @Test
    public void dropNegativeThrows() {
        Utils.assertEcmaErrorES6(
                "RangeError: Iterator helper limit argument must be a non-negative integer.",
                "[1].values().drop(-1);");
    }

    // ---- flatMap ----

    @Test
    public void flatMapBasicArrayReturn() {
        Utils.assertWithAllModes_ES6(
                "1,10,2,20,3,30",
                "[1,2,3].values().flatMap(function(v){ return [v, v*10]; }).toArray().join(',');");
    }

    @Test
    public void flatMapWithIteratorReturn() {
        Utils.assertWithAllModes_ES6(
                "a,b,c,d",
                "[[\"a\",\"b\"], [\"c\",\"d\"]].values().flatMap(function(x){ return x.values(); }).toArray().join(',');");
    }

    @Test
    public void flatMapWithStrings() {
        Utils.assertWithAllModes_ES6(
                "a,b,c,d",
                "[\"ab\", \"cd\"].values().flatMap(function(s){ return s; }).toArray().join(',');");
    }

    @Test
    public void flatMapNonIterableReturnThrows() {
        Utils.assertEcmaErrorES6(
                "TypeError: 42 is not iterable",
                "[1].values().flatMap(function(){ return 42; }).next();");
    }

    // ---- chaining ----

    @Test
    public void chainingMapFilterTake() {
        String code =
                "[1,2,3,4,5,6,7,8,9,10].values()\n"
                        + "  .map(function(v){ return v * v; })\n"
                        + "  .filter(function(v){ return v % 2 === 0; })\n"
                        + "  .take(3)\n"
                        + "  .toArray().join(',');";
        Utils.assertWithAllModes_ES6("4,16,36", code);
    }

    @Test
    public void helpersInheritIteratorPrototype() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE,
                "Iterator.prototype.isPrototypeOf([1].values().map(function(x){ return x; }));");
    }

    @Test
    public void helperIsIterable() {
        String code =
                "var h = [1,2].values().map(function(x){ return x; });\n"
                        + "h[Symbol.iterator]() === h;";
        Utils.assertWithAllModes_ES6(Boolean.TRUE, code);
    }

    // ---- receiver/arg validation ----

    @Test
    public void mapRequiresCallable() {
        Utils.assertEcmaErrorES6(
                "TypeError: undefined is not a function, it is undefined.", "[1].values().map();");
    }

    @Test
    public void takeOnNonIteratorThrows() {
        Utils.assertEcmaErrorES6(
                "TypeError: next is not a function, it is undefined.",
                "Iterator.prototype.take.call({}, 1);");
    }
}
