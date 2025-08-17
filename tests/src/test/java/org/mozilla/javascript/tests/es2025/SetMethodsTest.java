/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2025;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class SetMethodsTest {

    @Test
    public void intersectionBasic() {
        final String script =
                "var s1 = new Set([1, 2, 3]);"
                        + "var s2 = new Set([2, 3, 4]);"
                        + "var result = s1.intersection(s2);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("2,3", script);
    }

    @Test
    public void unionBasic() {
        final String script =
                "var s1 = new Set([1, 2, 3]);"
                        + "var s2 = new Set([3, 4, 5]);"
                        + "var result = s1.union(s2);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("1,2,3,4,5", script);
    }

    @Test
    public void differenceBasic() {
        final String script =
                "var s1 = new Set([1, 2, 3, 4]);"
                        + "var s2 = new Set([2, 4]);"
                        + "var result = s1.difference(s2);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("1,3", script);
    }

    @Test
    public void symmetricDifferenceBasic() {
        final String script =
                "var s1 = new Set([1, 2, 3]);"
                        + "var s2 = new Set([2, 3, 4]);"
                        + "var result = s1.symmetricDifference(s2);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("1,4", script);
    }

    @Test
    public void isSubsetOfTrue() {
        final String script =
                "var s1 = new Set([1, 2]);" + "var s2 = new Set([1, 2, 3]);" + "s1.isSubsetOf(s2)";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void isSubsetOfFalse() {
        final String script =
                "var s1 = new Set([1, 2, 4]);"
                        + "var s2 = new Set([1, 2, 3]);"
                        + "s1.isSubsetOf(s2)";
        Utils.assertWithAllModes_ES6(false, script);
    }

    @Test
    public void isSupersetOfTrue() {
        final String script =
                "var s1 = new Set([1, 2, 3, 4]);"
                        + "var s2 = new Set([2, 3]);"
                        + "s1.isSupersetOf(s2)";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void isSupersetOfFalse() {
        final String script =
                "var s1 = new Set([1, 2]);" + "var s2 = new Set([2, 3]);" + "s1.isSupersetOf(s2)";
        Utils.assertWithAllModes_ES6(false, script);
    }

    @Test
    public void isDisjointFromTrue() {
        final String script =
                "var s1 = new Set([1, 2]);" + "var s2 = new Set([3, 4]);" + "s1.isDisjointFrom(s2)";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void isDisjointFromFalse() {
        final String script =
                "var s1 = new Set([1, 2, 3]);"
                        + "var s2 = new Set([3, 4, 5]);"
                        + "s1.isDisjointFrom(s2)";
        Utils.assertWithAllModes_ES6(false, script);
    }

    @Test
    public void intersectionEmptySet() {
        final String script =
                "var s1 = new Set([1, 2, 3]);"
                        + "var s2 = new Set();"
                        + "var result = s1.intersection(s2);"
                        + "result.size";
        Utils.assertWithAllModes_ES6(0, script);
    }

    @Test
    public void unionEmptySet() {
        final String script =
                "var s1 = new Set([1, 2, 3]);"
                        + "var s2 = new Set();"
                        + "var result = s1.union(s2);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("1,2,3", script);
    }

    @Test
    public void setMethodsExist() {
        final String script =
                "typeof Set.prototype.intersection === 'function' && "
                        + "typeof Set.prototype.union === 'function' && "
                        + "typeof Set.prototype.difference === 'function' && "
                        + "typeof Set.prototype.symmetricDifference === 'function' && "
                        + "typeof Set.prototype.isSubsetOf === 'function' && "
                        + "typeof Set.prototype.isSupersetOf === 'function' && "
                        + "typeof Set.prototype.isDisjointFrom === 'function'";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void intersectionWithSetLikeObject() {
        final String script =
                "var s1 = new Set([1, 2, 3, 4]);"
                        + "var setLike = {"
                        + "  size: 3,"
                        + "  has: function(v) { return v === 2 || v === 3 || v === 5; },"
                        + "  keys: function() { return [2, 3, 5].values(); }"
                        + "};"
                        + "var result = s1.intersection(setLike);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("2,3", script);
    }

    @Test
    public void differenceWithSetLikeObject() {
        final String script =
                "var s1 = new Set([1, 2, 3, 4]);"
                        + "var setLike = {"
                        + "  size: 2,"
                        + "  has: function(v) { return v === 2 || v === 4; },"
                        + "  keys: function() { return [2, 4].values(); }"
                        + "};"
                        + "var result = s1.difference(setLike);"
                        + "Array.from(result).sort().join(',')";
        Utils.assertWithAllModes_ES6("1,3", script);
    }

    @Test
    public void methodsWithStrings() {
        final String script =
                "var s1 = new Set(['a', 'b', 'c']);"
                        + "var s2 = new Set(['b', 'c', 'd']);"
                        + "var intersection = s1.intersection(s2);"
                        + "var union = s1.union(s2);"
                        + "Array.from(intersection).sort().join(',') + '|' + Array.from(union).sort().join(',')";
        Utils.assertWithAllModes_ES6("b,c|a,b,c,d", script);
    }

    @Test
    public void methodsWithMixedTypes() {
        final String script =
                "var s1 = new Set([1, '2', true, null]);"
                        + "var s2 = new Set(['2', null, false, 3]);"
                        + "var result = s1.intersection(s2);"
                        + "result.size + ',' + result.has('2') + ',' + result.has(null)";
        Utils.assertWithAllModes_ES6("2,true,true", script);
    }
}
