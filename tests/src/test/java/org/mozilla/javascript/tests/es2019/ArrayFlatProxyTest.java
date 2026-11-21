/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2019;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** {@code flat} and {@code flatMap} on proxies follow FlattenIntoArray and ArraySpeciesCreate. */
public class ArrayFlatProxyTest {

    private static final String SETUP =
            "var calls = [];\n"
                    + "var handler = {\n"
                    + "  get: function (t, p, r) { calls.push('get ' + String(p)); return Reflect.get(t, p, r); },\n"
                    + "  has: function (t, p) { calls.push('has ' + String(p)); return Reflect.has(t, p); }\n"
                    + "};\n";

    @Test
    public void flatMapAccessOrder() {
        Utils.assertWithAllModes_ES6(
                "1,2,3 | get length,get constructor,has 0,get 0,has 1,get 1",
                SETUP
                        + "var p = new Proxy([1, [2, 3]], handler);\n"
                        + "var r = Array.prototype.flatMap.call(p, function (x) { return x; });\n"
                        + "r.join() + ' | ' + calls.join()");
    }

    @Test
    public void flatFlattensProxiedArrays() {
        Utils.assertWithAllModes_ES6(
                "1,2,3 | get length,has 0,get 0,has 1,get 1",
                SETUP
                        + "var p = new Proxy([2, 3], handler);\n"
                        + "var r = [1, p].flat();\n"
                        + "r.join() + ' | ' + calls.join()");
    }

    @Test
    public void flatSkipsHolesOfProxiedArrays() {
        Utils.assertWithAllModes_ES6(
                "1,3 2",
                "var p = new Proxy([1, , 3], {});\n"
                        + "var r = [p].flat();\n"
                        + "r.join() + ' ' + r.length");
    }

    @Test
    public void speciesConstructorOfProxiedArray() {
        Utils.assertWithAllModes_ES6(
                true,
                "function MyArray() { return Reflect.construct(Array, arguments, MyArray); }\n"
                        + "MyArray.prototype = Object.create(Array.prototype);\n"
                        + "var a = [1, 2]; a.constructor = {}; a.constructor[Symbol.species] = MyArray;\n"
                        + "Array.prototype.flat.call(new Proxy(a, {})) instanceof MyArray");
    }

    @Test
    public void flatDoesNotSetLength() {
        Utils.assertWithAllModes_ES6(
                "0 2",
                "var sets = 0;\n"
                        + "function C() { return new Proxy({}, { set: function (t, k, v) { if (k === 'length') sets++; t[k] = v; return true; } }); }\n"
                        + "var a = [1, 2]; a.constructor = {}; a.constructor[Symbol.species] = C;\n"
                        + "var r = a.flat();\n"
                        + "sets + ' ' + Object.keys(r).length");
    }
}
