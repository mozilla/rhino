/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Array methods set {@code length} with Set(O, "length", len, true), so a failed write throws a
 * TypeError in non-strict code as well.
 */
public class ArrayLengthSetThrowsTest {

    private static final String TRY =
            "function t(f) { try { f(); return 'no error'; } catch (e) { return e.name; } }\n";

    @Test
    public void frozenArray() {
        Utils.assertWithAllModes_ES6(
                "TypeError TypeError TypeError TypeError",
                TRY
                        + "var a = Object.freeze([]);\n"
                        + "[t(function () { a.pop(); }), t(function () { a.push(); }),"
                        + " t(function () { a.shift(); }), t(function () { a.unshift(); })].join(' ')");
    }

    @Test
    public void nonWritableLength() {
        Utils.assertWithAllModes_ES6(
                "TypeError TypeError TypeError TypeError TypeError 0",
                TRY
                        + "var a = [];\n"
                        + "Object.defineProperty(a, 'length', { writable: false });\n"
                        + "[t(function () { a.push(); }),"
                        + " t(function () { a.pop(); }), t(function () { a.shift(); }),"
                        + " t(function () { a.unshift(); }), t(function () { a.splice(0, 0); }),"
                        + " a.length].join(' ')");
    }

    @Test
    public void nonWritableLengthOnArrayLike() {
        Utils.assertWithAllModes_ES6(
                "TypeError 3",
                TRY
                        + "var o = { length: 3 };\n"
                        + "Object.defineProperty(o, 'length', { writable: false });\n"
                        + "[t(function () { Array.prototype.splice.call(o, 0, 0); }), o.length].join(' ')");
    }

    @Test
    public void nonExtensibleArrayLike() {
        Utils.assertWithAllModes_ES6(
                "TypeError",
                TRY
                        + "var o = Object.preventExtensions({});\n"
                        + "t(function () { Array.prototype.push.call(o); })");
    }

    @Test
    public void writableLengthStillWorks() {
        Utils.assertWithAllModes_ES6(
                "2 2 3 1",
                "var o = { length: 0 };\n"
                        + "Array.prototype.push.call(o, 'x');\n"
                        + "[[1].push(2), [1, 2].pop(), [1, 2].unshift(0), o.length].join(' ')");
    }

    @Test
    public void callerStrictnessIsRestored() {
        Utils.assertWithAllModes_ES6(
                true,
                "var a = [1];\n"
                        + "a.push(2);\n"
                        + "(function () { undeclared = 1; return true; })()");
    }
}
