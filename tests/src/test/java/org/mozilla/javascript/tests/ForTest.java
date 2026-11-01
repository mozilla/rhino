/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.testutils.Utils;

public class ForTest {

    /** Test for issue #645. There was a NPE. */
    @Test
    public void forInit() {
        Utils.assertWithAllModes(Undefined.instance, "for(({});;){ break; }");
    }

    /** Test for issue #1394. */
    @Test
    public void forInSkipsPropertiesShadowedByNonEnumerableOnes() {
        Utils.assertWithAllModes_ES6(
                "p1",
                "var proto = { p1: 1, p2: 2 };\n"
                        + "var o = Object.create(proto, { p2: { value: 3, enumerable: false } });\n"
                        + "var keys = [];\n"
                        + "for (var k in o) keys.push(k);\n"
                        + "keys.join()");
    }

    @Test
    public void forInSkipsPropertiesShadowedByObjectWithoutEnumerables() {
        Utils.assertWithAllModes_ES6(
                "y",
                "var a = { x: 1, y: 2 };\n"
                        + "var b = Object.create(a, { x: { value: 3, enumerable: false } });\n"
                        + "var c = Object.create(b);\n"
                        + "var keys = [];\n"
                        + "for (var k in c) keys.push(k);\n"
                        + "keys.join()");
    }

    @Test
    public void forInSkipsBuiltinNonEnumerableShadowing() {
        Utils.assertWithAllModes_ES6(
                "",
                "Object.prototype.length = 42;\n"
                        + "var keys = [];\n"
                        + "for (var k in Function) keys.push(k);\n"
                        + "delete Object.prototype.length;\n"
                        + "keys.join()");
    }

    @Test
    public void forInArrayLengthShadowsPrototype() {
        Utils.assertWithAllModes_ES6(
                "0,1",
                "Object.prototype.length = 42;\n"
                        + "var keys = [];\n"
                        + "for (var k in [5, 6]) keys.push(k);\n"
                        + "delete Object.prototype.length;\n"
                        + "keys.join()");
    }

    @Test
    public void forInPropertyAddedDuringLoopDoesNotShadow() {
        Utils.assertWithAllModes_ES6(
                "x,a,y",
                "var proto = { a: 1, y: 2 };\n"
                        + "var o = Object.create(proto);\n"
                        + "o.x = 0;\n"
                        + "var keys = [];\n"
                        + "for (var k in o) {\n"
                        + "  if (k === 'x') o.y = 3;\n"
                        + "  keys.push(k);\n"
                        + "}\n"
                        + "keys.join()");
    }

    @Test
    public void forInCallsProxyOwnKeysOnce() {
        Utils.assertWithAllModes_ES6(
                "a,z 1",
                "var calls = 0;\n"
                        + "var p = new Proxy({ a: 1 }, {\n"
                        + "  ownKeys: function(t) { calls++; return Reflect.ownKeys(t); }\n"
                        + "});\n"
                        + "Object.prototype.z = 1;\n"
                        + "var keys = [];\n"
                        + "for (var k in p) keys.push(k);\n"
                        + "delete Object.prototype.z;\n"
                        + "keys.join() + ' ' + calls");
    }
}
