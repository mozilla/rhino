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
    public void setsSpeciesResultLength() {
        Utils.assertWithAllModes_ES6(
                0,
                "var a = [1, 2, 3];\n"
                        + "a.constructor = {};\n"
                        + "a.constructor[Symbol.species] = function () { return {}; };\n"
                        + "a.splice().length");
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
    public void normalizesLength() {
        Utils.assertWithAllModes_ES6(
                "number 2",
                "var o = { length: '2' };\n"
                        + "Array.prototype.splice.call(o);\n"
                        + "typeof o.length + ' ' + o.length");
    }

    @Test
    public void clampsNegativeLength() {
        Utils.assertWithAllModes_ES6(
                0, "var o = { length: -5 };\n" + "Array.prototype.splice.call(o);\n" + "o.length");
    }

    @Test
    public void writesLengthOnce() {
        Utils.assertWithAllModes_ES6(
                "[0]",
                "var writes = [];\n"
                        + "Array.prototype.splice.call({\n"
                        + "  get length() { return 0; }, set length(v) { writes.push(v); }\n"
                        + "});\n"
                        + "JSON.stringify(writes)");
    }

    @Test
    public void differsFromUndefinedStart() {
        Utils.assertWithAllModes_ES6(
                "[[],[1,2,3]] [[1,2,3],[]]",
                "var a = [1, 2, 3], b = [1, 2, 3];\n"
                        + "JSON.stringify([a.splice(), a]) + ' ' + JSON.stringify([b.splice(undefined), b])");
    }

    @Test
    public void arrayLikeElementsUntouched() {
        Utils.assertWithAllModes_ES6(
                "[] {\"0\":\"a\",\"2\":\"c\",\"length\":3}",
                "var o = { 0: 'a', 2: 'c', length: 3 };\n"
                        + "JSON.stringify(Array.prototype.splice.call(o)) + ' ' + JSON.stringify(o)");
    }

    @Test
    public void plainArrayIsUnchanged() {
        Utils.assertWithAllModes_ES6(
                "[[],[1,2,3]]",
                "var a = [1, 2, 3];\n" + "var r = a.splice();\n" + "JSON.stringify([r, a])");
    }
}
