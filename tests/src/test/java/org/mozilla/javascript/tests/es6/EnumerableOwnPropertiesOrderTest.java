/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Enumerability is checked when each key is reached, and a proxy answers it with its
 * getOwnPropertyDescriptor trap. See https://github.com/mozilla/rhino/issues/2596
 */
public class EnumerableOwnPropertiesOrderTest {

    // The getter of 'a' makes 'b' enumerable and 'c' non-enumerable.
    private static final String SOURCE =
            "function source() {\n"
                    + "  var o = {};\n"
                    + "  Object.defineProperty(o, 'a', { enumerable: true, configurable: true,\n"
                    + "    get: function () {\n"
                    + "      Object.defineProperty(o, 'b', { enumerable: true });\n"
                    + "      Object.defineProperty(o, 'c', { enumerable: false });\n"
                    + "      return 1;\n"
                    + "    } });\n"
                    + "  Object.defineProperty(o, 'b', { value: 2, enumerable: false, configurable: true });\n"
                    + "  Object.defineProperty(o, 'c', { value: 3, enumerable: true, configurable: true });\n"
                    + "  return o;\n"
                    + "}\n"
                    + "function descriptors() {\n"
                    + "  var o = {};\n"
                    + "  Object.defineProperty(o, 'a', { enumerable: true, configurable: true,\n"
                    + "    get: function () {\n"
                    + "      Object.defineProperty(o, 'b', { enumerable: true });\n"
                    + "      Object.defineProperty(o, 'c', { enumerable: false });\n"
                    + "      return { value: 1, enumerable: true };\n"
                    + "    } });\n"
                    + "  Object.defineProperty(o, 'b', { value: { value: 2, enumerable: true },\n"
                    + "    enumerable: false, configurable: true });\n"
                    + "  Object.defineProperty(o, 'c', { value: { value: 3, enumerable: true },\n"
                    + "    enumerable: true, configurable: true });\n"
                    + "  return o;\n"
                    + "}\n"
                    + "function proxy() {\n"
                    + "  return new Proxy({ a: 1, b: 2 }, {\n"
                    + "    getOwnPropertyDescriptor: function (t, k) {\n"
                    + "      var d = Reflect.getOwnPropertyDescriptor(t, k);\n"
                    + "      if (k === 'b') d.enumerable = false;\n"
                    + "      return d;\n"
                    + "    } });\n"
                    + "}\n";

    private static void check(String expected, String expr) {
        Utils.assertWithAllModes_ES6(expected, SOURCE + "JSON.stringify(" + expr + ")");
    }

    @Test
    public void entries() {
        check("[[\"a\",1],[\"b\",2]]", "Object.entries(source())");
    }

    @Test
    public void values() {
        check("[1,2]", "Object.values(source())");
    }

    @Test
    public void assign() {
        check("{\"a\":1,\"b\":2}", "Object.assign({}, source())");
    }

    @Test
    public void spread() {
        check("{\"a\":1,\"b\":2}", "({ ...source() })");
    }

    @Test
    public void rest() {
        Utils.assertWithAllModes_ES6(
                "{\"a\":1,\"b\":2}", SOURCE + "var { ...rest } = source(); JSON.stringify(rest)");
    }

    @Test
    public void defineProperties() {
        check("{\"a\":1,\"b\":2}", "Object.defineProperties({}, descriptors())");
    }

    @Test
    public void create() {
        check("[\"a\",\"b\"]", "Object.getOwnPropertyNames(Object.create({}, descriptors()))");
    }

    @Test
    public void proxyUsesGetOwnPropertyDescriptorTrap() {
        check("[\"a\"]", "Object.keys(proxy())");
        check("[[\"a\",1]]", "Object.entries(proxy())");
        check("{\"a\":1}", "Object.assign({}, proxy())");
        check("{\"a\":1}", "({ ...proxy() })");
        Utils.assertWithAllModes_ES6("{\"a\":1}", SOURCE + "JSON.stringify(proxy())");
    }

    @Test
    public void proxyWithoutDescriptorTrapHidesNonEnumerable() {
        Utils.assertWithAllModes_ES6(
                "[\"shown\"] shown",
                "var t = {};\n"
                        + "Object.defineProperty(t, 'hidden', { value: 1, enumerable: false });\n"
                        + "t.shown = 2;\n"
                        + "var p = new Proxy(t, { ownKeys: function (it) { return Reflect.ownKeys(it); } });\n"
                        + "var keys = [];\n"
                        + "for (var k in p) keys.push(k);\n"
                        + "JSON.stringify(Object.keys(p)) + ' ' + keys.join()");
    }

    @Test
    public void proxyTrapOrder() {
        Utils.assertWithAllModes_ES6(
                "ownKeys, gopd a, get a, gopd b, get b",
                "var log = [];\n"
                        + "var p = new Proxy({ a: 1, b: 2 }, {\n"
                        + "  ownKeys: function (t) { log.push('ownKeys'); return Reflect.ownKeys(t); },\n"
                        + "  getOwnPropertyDescriptor: function (t, k) {\n"
                        + "    log.push('gopd ' + k); return Reflect.getOwnPropertyDescriptor(t, k);\n"
                        + "  },\n"
                        + "  get: function (t, k) { log.push('get ' + String(k)); return t[k]; }\n"
                        + "});\n"
                        + "Object.entries(p);\n"
                        + "log.join(', ')");
    }

    @Test
    public void ordinaryObjectsUnchanged() {
        Utils.assertWithAllModes_ES6(
                "[[\"0\",1],[\"1\",2]] {\"x\":1} 2 false",
                "var s = Symbol('s'), o = { x: 1 };\n"
                        + "o[s] = 2;\n"
                        + "Object.defineProperty(o, 'y', { value: 3 });\n"
                        + "var c = Object.assign({}, o);\n"
                        + "JSON.stringify(Object.entries([1, 2])) + ' ' + JSON.stringify(c) + ' '\n"
                        + "  + c[s] + ' ' + ('y' in c)");
    }
}
