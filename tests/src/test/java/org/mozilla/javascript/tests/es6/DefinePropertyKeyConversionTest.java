/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * {@code Object.defineProperty} and {@code Object.getOwnPropertyDescriptor} convert the key with
 * ToPropertyKey once. See https://github.com/mozilla/rhino/issues/2600
 */
public class DefinePropertyKeyConversionTest {

    private static final String REDEFINE =
            "function redefine(key) {\n"
                    + "  var a = [1];\n"
                    + "  Object.defineProperty(a, key, { value: 2, enumerable: true });\n"
                    + "  return JSON.stringify([a[0], Object.keys(a)]);\n"
                    + "}\n"
                    + "var index = 0;\n";

    @Test
    public void stringLiteralKey() {
        Utils.assertWithAllModes_ES6("[2,[\"0\"]]", REDEFINE + "redefine('0')");
    }

    @Test
    public void concatenatedKey() {
        Utils.assertWithAllModes_ES6("[2,[\"0\"]]", REDEFINE + "redefine(index + '')");
    }

    @Test
    public void templateLiteralKey() {
        Utils.assertWithAllModes_ES6("[2,[\"0\"]]", REDEFINE + "redefine(`${index}`)");
    }

    @Test
    public void objectKey() {
        Utils.assertWithAllModes_ES6(
                "[2,[\"0\"]]", REDEFINE + "redefine({ toString: function () { return '0'; } })");
    }

    @Test
    public void descriptorWithConcatenatedKey() {
        Utils.assertWithAllModes_ES6(
                "{\"value\":1,\"writable\":true,\"enumerable\":true,\"configurable\":true}",
                "var index = 0;\n"
                        + "JSON.stringify(Object.getOwnPropertyDescriptor([1], index + ''))");
    }

    @Test
    public void proxyTrapGetsStringKeyConvertedOnce() {
        Utils.assertWithAllModes_ES6(
                "string 1",
                "var calls = 0, types = [];\n"
                        + "var key = { toString: function () { calls++; return 'a'; } };\n"
                        + "var p = new Proxy({}, { defineProperty: function (t, k, d) {\n"
                        + "  types.push(typeof k); return Reflect.defineProperty(t, k, d);\n"
                        + "} });\n"
                        + "Object.defineProperty(p, key, { value: 1 });\n"
                        + "types.join(',') + ' ' + calls");
    }

    @Test
    public void symbolKeyIsKept() {
        Utils.assertWithAllModes_ES6(
                2,
                "var s = Symbol('k'), o = {};\n"
                        + "Object.defineProperty(o, s, { value: 2 });\n"
                        + "Object.getOwnPropertyDescriptor(o, s).value");
    }
}
