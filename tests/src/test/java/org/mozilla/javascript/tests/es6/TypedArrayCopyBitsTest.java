/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Copies between typed arrays of the same type keep the bit pattern of every element, NaN payloads
 * included. See https://github.com/mozilla/rhino/issues/2595
 */
public class TypedArrayCopyBitsTest {

    private static final String OPS =
            "function copyWithin(F, w) { new F(w.buffer).copyWithin(1, 0, 1); return w[1]; }\n"
                    + "function slice(F, w, B) {\n"
                    + "  return new B(new F(w.buffer).slice(0, 1).buffer)[0];\n"
                    + "}\n"
                    + "function set(F, w, B) {\n"
                    + "  var src = new F(1); new B(src.buffer)[0] = w[0];\n"
                    + "  new F(w.buffer).set(src, 1); return w[1];\n"
                    + "}\n"
                    + "function ctor(F, w, B) {\n"
                    + "  return new B(new F(new F(w.buffer, 0, 1)).buffer)[0];\n"
                    + "}\n"
                    + "function run(F, B, bits) {\n"
                    + "  return [copyWithin, slice, set, ctor].map(function (op) {\n"
                    + "    var w = new B(2); w[0] = bits; return op(F, w, B).toString(16);\n"
                    + "  }).join(',');\n"
                    + "}\n";

    @Test
    public void float32SignalingNaN() {
        Utils.assertWithAllModes_ES6(
                "7f800001,7f800001,7f800001,7f800001",
                OPS + "run(Float32Array, Uint32Array, 0x7f800001)");
    }

    @Test
    public void float32QuietNaNWithPayload() {
        Utils.assertWithAllModes_ES6(
                "ffc00005,ffc00005,ffc00005,ffc00005",
                OPS + "run(Float32Array, Uint32Array, 0xffc00005)");
    }

    @Test
    public void float64NaN() {
        Utils.assertWithAllModes_ES6(
                "7ff0000000000001,7ff0000000000001,7ff0000000000001,7ff0000000000001",
                OPS + "run(Float64Array, BigUint64Array, 0x7ff0000000000001n)");
    }

    @Test
    public void float16NaN() {
        Utils.assertWithAllModes_ES6(
                "7c01,7c01,7c01,7c01", OPS + "run(Float16Array, Uint16Array, 0x7c01)");
        Utils.assertWithAllModes_ES6(
                "fe05,fe05,fe05,fe05", OPS + "run(Float16Array, Uint16Array, 0xfe05)");
    }

    @Test
    public void overlappingCopyWithin() {
        Utils.assertWithAllModes_ES6(
                "1,1,2,3,4 2,3,4,5,5",
                "var a = new Int16Array([1, 2, 3, 4, 5]); a.copyWithin(1, 0, 4);\n"
                        + "var b = new Int16Array([1, 2, 3, 4, 5]); b.copyWithin(0, 1);\n"
                        + "a.join() + ' ' + b.join()");
    }

    @Test
    public void overlappingSetOnSameBuffer() {
        Utils.assertWithAllModes_ES6(
                "1,1,2,3",
                "var u = new Uint8Array([1, 2, 3, 4]);\n"
                        + "new Uint8Array(u.buffer, 1).set(new Uint8Array(u.buffer, 0, 3));\n"
                        + "u.join()");
    }

    @Test
    public void sliceOfOffsetView() {
        Utils.assertWithAllModes_ES6(
                "7,6", "new Int32Array(new Int32Array([9, 8, 7, 6]).buffer, 4).slice(1, 3).join()");
    }

    @Test
    public void sliceIntoOverlappingSpeciesViewCopiesForward() {
        Utils.assertWithAllModes_ES6(
                "20,20,20,60",
                "var ta = new Int16Array([10, 20, 30, 40, 50, 60]);\n"
                        + "ta.constructor = { [Symbol.species]: function () {\n"
                        + "  return new Int16Array(ta.buffer, 4);\n"
                        + "} };\n"
                        + "ta.slice(1, 4).join()");
    }

    @Test
    public void differentTypesStillConvert() {
        Utils.assertWithAllModes_ES6(
                "-1,7 255,0,2",
                "var f = new Float32Array(2); f.set(new Int8Array([-1, 7]));\n"
                        + "f.join() + ' ' + new Uint8ClampedArray(new Float64Array([300, -5, 1.5])).join()");
    }
}
