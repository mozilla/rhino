/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

public class IteratorPrototypeEagerTest {

    // ---- toArray ----

    @Test
    public void toArrayFromArrayValues() {
        Utils.assertWithAllModes_ES6("1,2,3", "[1,2,3].values().toArray().join(',');");
    }

    @Test
    public void toArrayFromString() {
        Utils.assertWithAllModes_ES6("a,b,c", "Iterator.from('abc').toArray().join(',');");
    }

    @Test
    public void toArrayFromExhaustedIterator() {
        String code =
                "var it = [1,2].values();\n"
                        + "it.next(); it.next(); it.next();\n"
                        + "it.toArray().length;";
        Utils.assertWithAllModes_ES6(0, code);
    }

    // ---- forEach ----

    @Test
    public void forEachCollectsAndReturnsUndefined() {
        String code =
                "var seen = [];\n"
                        + "var r = [10,20,30].values().forEach(function(v, i){ seen.push(i+':'+v); });\n"
                        + "seen.join(',') + '/' + typeof r;";
        Utils.assertWithAllModes_ES6("0:10,1:20,2:30/undefined", code);
    }

    @Test
    public void forEachCallbackThrowCallsReturnOnSource() {
        String code =
                "var closed = false;\n"
                        + "var src = { next: function(){ return {value: 1, done: false}; },\n"
                        + "            return: function(){ closed = true; return {done: true}; } };\n"
                        + "try {\n"
                        + "  Iterator.from(src).forEach(function(){ throw 'oops'; });\n"
                        + "  'no throw';\n"
                        + "} catch (e) { closed + ',' + e; }\n";
        Utils.assertWithAllModes_ES6("true,oops", code);
    }

    // ---- reduce ----

    @Test
    public void reduceWithInitial() {
        Utils.assertWithAllModes_ES6(
                10, "[1,2,3,4].values().reduce(function(a,b){ return a + b; }, 0);");
    }

    @Test
    public void reduceWithoutInitialUsesFirstValue() {
        Utils.assertWithAllModes_ES6(
                10, "[1,2,3,4].values().reduce(function(a,b){ return a + b; });");
    }

    @Test
    public void reduceWithoutInitialOnEmptyThrows() {
        Utils.assertEcmaErrorES6(
                "TypeError: Reduce of empty array with no initial value",
                "[].values().reduce(function(a,b){ return a + b; });");
    }

    // ---- some ----

    @Test
    public void someShortCircuitsAndClosesSource() {
        String code =
                "var closed = false;\n"
                        + "var i = 0;\n"
                        + "var src = { next: function(){ return {value: i++, done: false}; },\n"
                        + "            return: function(){ closed = true; return {done: true}; } };\n"
                        + "var r = Iterator.from(src).some(function(v){ return v === 2; });\n"
                        + "r + ',' + closed + ',' + i;";
        Utils.assertWithAllModes_ES6("true,true,3", code);
    }

    @Test
    public void someReturnsFalseOnEmpty() {
        Utils.assertWithAllModes_ES6(
                Boolean.FALSE, "[].values().some(function(){ return true; });");
    }

    // ---- every ----

    @Test
    public void everyReturnsTrueOnAllMatch() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE, "[2,4,6].values().every(function(v){ return v % 2 === 0; });");
    }

    @Test
    public void everyShortCircuitsOnFalse() {
        String code =
                "var seen = [];\n"
                        + "var r = [2,4,5,6].values().every(function(v){ seen.push(v); return v % 2 === 0; });\n"
                        + "r + ':' + seen.join(',');";
        Utils.assertWithAllModes_ES6("false:2,4,5", code);
    }

    // ---- find ----

    @Test
    public void findReturnsMatchedValue() {
        Utils.assertWithAllModes_ES6(3, "[1,2,3,4].values().find(function(v){ return v > 2; });");
    }

    @Test
    public void findReturnsUndefinedIfNoMatch() {
        Utils.assertWithAllModes_ES6(
                "undefined", "typeof [1,2].values().find(function(v){ return v > 10; });");
    }

    // ---- receiver/arg validation ----

    @Test
    public void toArrayOnNonIteratorThrows() {
        Utils.assertEcmaErrorES6(
                "TypeError: next is not a function, it is undefined.",
                "Iterator.prototype.toArray.call({});");
    }

    @Test
    public void forEachRequiresCallable() {
        Utils.assertEcmaErrorES6(
                "TypeError: undefined is not a function, it is undefined.",
                "[1].values().forEach();");
    }

    // ---- generators pick up helpers too ----

    @Test
    public void generatorToArray() {
        String code =
                "function* g(){ yield 'a'; yield 'b'; yield 'c'; }\n" + "g().toArray().join(',');";
        Utils.assertWithAllModes_ES6("a,b,c", code);
    }
}
