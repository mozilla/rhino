/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * {@code splice()} without arguments behaves like {@code splice(0, 0)}. See
 * https://github.com/mozilla/rhino/issues/2589
 */
public class ArraySpliceNoArgumentsTest {

    @Test
    public void usesSpecies() {
        Utils.assertWithAllModes_ES6(
                true,
                "var a = [1, 2, 3];\n"
                        + "a.constructor = {};\n"
                        + "a.constructor[Symbol.species] = function () { return { species: true }; };\n"
                        + "a.splice().species");
    }

    @Test
    public void setsLength() {
        Utils.assertWithAllModes_ES6(
                0, "var o = {};\n" + "Array.prototype.splice.call(o);\n" + "o.length");
    }

    @Test
    public void readsLengthOnce() {
        Utils.assertWithAllModes_ES6(
                1,
                "var reads = 0;\n"
                        + "Array.prototype.splice.call({\n"
                        + "  get length() { reads++; return 0; }, set length(v) {}\n"
                        + "});\n"
                        + "reads");
    }

    @Test
    public void frozenArrayThrowsInStrictMode() {
        Utils.assertWithAllModes_ES6(
                "TypeError",
                "(function () {\n"
                        + "  'use strict';\n"
                        + "  try { Object.freeze([1, 2]).splice(); return 'no error'; }\n"
                        + "  catch (e) { return e.name; }\n"
                        + "})()");
    }

    @Test
    public void plainArrayIsUnchanged() {
        Utils.assertWithAllModes_ES6(
                "[[],[1,2,3]]",
                "var a = [1, 2, 3];\n" + "var r = a.splice();\n" + "JSON.stringify([r, a])");
    }
}
