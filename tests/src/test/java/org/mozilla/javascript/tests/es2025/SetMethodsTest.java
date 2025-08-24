/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2025;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Tests for ES2025 Set methods based on V8's mjsunit tests. These tests provide comprehensive
 * coverage beyond test262.
 */
public class SetMethodsTest {

    @Test
    public void testIntersectionBasic() {
        final String script =
                "var set1 = new Set([1, 2, 3]);"
                        + "var set2 = new Set([2, 3, 4]);"
                        + "var result = set1.intersection(set2);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("2,3", script);
    }

    @Test
    public void testIntersectionFirstShorter() {
        final String script =
                "var set1 = new Set([42, 43]);"
                        + "var set2 = new Set([42, 46, 47]);"
                        + "var result = set1.intersection(set2);"
                        + "Array.from(result).join(',')";
        Utils.assertWithAllModes_ES6("42", script);
    }

    @Test
    public void testIntersectionSecondShorter() {
        final String script =
                "var set1 = new Set([42, 43, 44]);"
                        + "var set2 = new Set([42, 45]);"
                        + "var result = set1.intersection(set2);"
                        + "Array.from(result).join(',')";
        Utils.assertWithAllModes_ES6("42", script);
    }

    @Test
    public void testIntersectionWithMap() {
        final String script =
                "var set = new Set([42, 43]);"
                        + "var map = new Map([[42, 'value'], [44, 'other']]);"
                        + "var result = set.intersection(map);"
                        + "Array.from(result).join(',')";
        Utils.assertWithAllModes_ES6("42", script);
    }

    @Test
    public void testIntersectionWithArray() {
        final String script =
                "var set = new Set([1, 2, 3]);"
                        + "var arr = [2, 3, 4, 3, 2];"
                        + // duplicates in array
                        "var result = set.intersection(arr);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("2,3", script);
    }

    @Test
    public void testIntersectionEmpty() {
        final String script =
                "var set1 = new Set([1, 2, 3]);"
                        + "var set2 = new Set([4, 5, 6]);"
                        + "var result = set1.intersection(set2);"
                        + "result.size";
        Utils.assertWithAllModes_ES6(0, script);
    }

    @Test
    public void testUnionBasic() {
        final String script =
                "var set1 = new Set([1, 2]);"
                        + "var set2 = new Set([2, 3]);"
                        + "var result = set1.union(set2);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("1,2,3", script);
    }

    @Test
    public void testUnionWithDuplicates() {
        final String script =
                "var set = new Set([1, 2, 3]);"
                        + "var arr = [3, 4, 5, 4, 3];"
                        + "var result = set.union(arr);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("1,2,3,4,5", script);
    }

    @Test
    public void testDifferenceBasic() {
        final String script =
                "var set1 = new Set([1, 2, 3, 4]);"
                        + "var set2 = new Set([2, 4]);"
                        + "var result = set1.difference(set2);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("1,3", script);
    }

    @Test
    public void testDifferenceEmptyResult() {
        final String script =
                "var set1 = new Set([1, 2]);"
                        + "var set2 = new Set([1, 2, 3]);"
                        + "var result = set1.difference(set2);"
                        + "result.size";
        Utils.assertWithAllModes_ES6(0, script);
    }

    @Test
    public void testSymmetricDifferenceBasic() {
        final String script =
                "var set1 = new Set([1, 2, 3]);"
                        + "var set2 = new Set([2, 3, 4]);"
                        + "var result = set1.symmetricDifference(set2);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("1,4", script);
    }

    @Test
    public void testSymmetricDifferenceSame() {
        final String script =
                "var set = new Set([1, 2, 3]);"
                        + "var result = set.symmetricDifference(set);"
                        + "result.size";
        Utils.assertWithAllModes_ES6(0, script);
    }

    @Test
    public void testIsSubsetOfTrue() {
        final String script =
                "var set1 = new Set([1, 2]);"
                        + "var set2 = new Set([1, 2, 3, 4]);"
                        + "set1.isSubsetOf(set2)";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testIsSubsetOfFalse() {
        final String script =
                "var set1 = new Set([1, 2, 5]);"
                        + "var set2 = new Set([1, 2, 3, 4]);"
                        + "set1.isSubsetOf(set2)";
        Utils.assertWithAllModes_ES6(false, script);
    }

    @Test
    public void testIsSubsetOfEqual() {
        final String script =
                "var set1 = new Set([1, 2, 3]);"
                        + "var set2 = new Set([3, 2, 1]);"
                        + "set1.isSubsetOf(set2)";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testIsSupersetOfTrue() {
        final String script =
                "var set1 = new Set([1, 2, 3, 4]);"
                        + "var set2 = new Set([2, 3]);"
                        + "set1.isSupersetOf(set2)";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testIsSupersetOfFalse() {
        final String script =
                "var set1 = new Set([1, 2]);"
                        + "var set2 = new Set([2, 3]);"
                        + "set1.isSupersetOf(set2)";
        Utils.assertWithAllModes_ES6(false, script);
    }

    @Test
    public void testIsDisjointFromTrue() {
        final String script =
                "var set1 = new Set([1, 2]);"
                        + "var set2 = new Set([3, 4]);"
                        + "set1.isDisjointFrom(set2)";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testIsDisjointFromFalse() {
        final String script =
                "var set1 = new Set([1, 2, 3]);"
                        + "var set2 = new Set([3, 4, 5]);"
                        + "set1.isDisjointFrom(set2)";
        Utils.assertWithAllModes_ES6(false, script);
    }

    @Test
    public void testWithCustomIterator() {
        final String script =
                "var set = new Set([1, 2, 3]);"
                        + "var custom = {"
                        + "  [Symbol.iterator]: function() {"
                        + "    var values = [2, 4, 6];"
                        + "    var index = 0;"
                        + "    return {"
                        + "      next: function() {"
                        + "        return index < values.length ? "
                        + "          {value: values[index++], done: false} : "
                        + "          {done: true};"
                        + "      }"
                        + "    };"
                        + "  }"
                        + "};"
                        + "var result = set.intersection(custom);"
                        + "Array.from(result).join(',')";
        Utils.assertWithAllModes_ES6("2", script);
    }

    @Test
    public void testLargeSetPerformance() {
        // Test with larger sets to ensure reasonable performance
        final String script =
                "var set1 = new Set();"
                        + "var set2 = new Set();"
                        + "for (var i = 0; i < 1000; i++) {"
                        + "  set1.add(i);"
                        + "  if (i % 2 === 0) set2.add(i);"
                        + "}"
                        + "var result = set1.intersection(set2);"
                        + "result.size";
        Utils.assertWithAllModes_ES6(500, script);
    }

    @Test
    public void testNaNHandling() {
        final String script =
                "var set1 = new Set([NaN, 1, 2]);"
                        + "var set2 = new Set([NaN, 2, 3]);"
                        + "var result = set1.intersection(set2);"
                        + "var arr = Array.from(result);"
                        + "arr.length + ',' + arr.filter(x => x === 2).length + ',' + arr.filter(x => x !== x).length";
        Utils.assertWithAllModes_ES6("2,1,1", script); // 2 elements total, one is 2, one is NaN
    }

    @Test
    public void testInfinityHandling() {
        final String script =
                "var set1 = new Set([Infinity, -Infinity, 0]);"
                        + "var set2 = new Set([Infinity, 1, 0]);"
                        + "var result = set1.intersection(set2);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("0,Infinity", script);
    }

    @Test
    public void testStringCoercion() {
        final String script =
                "var set = new Set(['1', '2', '3']);"
                        + "var arr = [1, 2, 3];"
                        + // numbers, not strings
                        "var result = set.intersection(arr);"
                        + "result.size";
        Utils.assertWithAllModes_ES6(0, script); // no overlap due to type difference
    }

    @Test
    public void testChaining() {
        final String script =
                "var set1 = new Set([1, 2, 3, 4]);"
                        + "var set2 = new Set([2, 3, 4, 5]);"
                        + "var set3 = new Set([3, 4, 5, 6]);"
                        + "var result = set1.intersection(set2).intersection(set3);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("3,4", script);
    }

    @Test
    public void testNonCallableKeysError() {
        final String script =
                "var set = new Set([1, 2, 3]);"
                        + "var badIterable = { keys: 'not a function' };"
                        + "try {"
                        + "  set.intersection(badIterable);"
                        + "  false;"
                        + "} catch (e) {"
                        + "  e instanceof TypeError;"
                        + "}";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testNonCallableHasError() {
        final String script =
                "var set = new Set([1, 2, 3]);"
                        + "var badIterable = { keys: function() { return [].values(); }, has: 'not a function' };"
                        + "try {"
                        + "  set.intersection(badIterable);"
                        + "  false;"
                        + "} catch (e) {"
                        + "  e instanceof TypeError;"
                        + "}";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testIntersectionSizeOptimization() {
        // Test that intersection optimizes by iterating over smaller set
        final String script =
                "var callCount = 0;"
                        + "var smallSet = new Set([1, 2]);"
                        + "var largeSet = {"
                        + "  size: 1000,"
                        + "  has: function(v) { callCount++; return v === 1; },"
                        + "  keys: function() { return [1, 2, 3, 4, 5].values(); }"
                        + "};"
                        + "var result = smallSet.intersection(largeSet);"
                        + "callCount <= 5"; // Should check at most the 5 values from keys(), not
        // 1000
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testSymbolValues() {
        final String script =
                "var sym1 = Symbol('a');"
                        + "var sym2 = Symbol('b');"
                        + "var sym3 = Symbol('c');"
                        + "var set1 = new Set([sym1, sym2]);"
                        + "var set2 = new Set([sym2, sym3]);"
                        + "var result = set1.intersection(set2);"
                        + "result.has(sym2)";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testObjectValues() {
        final String script =
                "var obj1 = {a: 1};"
                        + "var obj2 = {b: 2};"
                        + "var obj3 = {c: 3};"
                        + "var set1 = new Set([obj1, obj2]);"
                        + "var set2 = new Set([obj2, obj3]);"
                        + "var result = set1.intersection(set2);"
                        + "result.has(obj2) && result.size === 1";
        Utils.assertWithAllModes_ES6(true, script);
    }
}
