/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.testutils.Utils;

public class ObjectLiteralSpreadTest {
    @Test
    public void testObjectSpreadBasic() {
        String script =
                "var obj1 = { a: 1, b: 2 };\n"
                        + "var obj2 = { ...obj1, c: 3 };\n"
                        + "obj2.a + obj2.b + obj2.c";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadOverride() {
        String script =
                "var obj1 = { a: 1, b: 2 };\n"
                        + "var obj2 = { a: 3, ...obj1, b: 4 };\n"
                        + "obj2.a + obj2.b";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void testObjectSpreadMultiple() {
        String script =
                "var obj1 = { a: 1 };\n"
                        + "var obj2 = { b: 2 };\n"
                        + "var obj3 = { c: 3 };\n"
                        + "var result = { ...obj1, ...obj2, ...obj3 };\n"
                        + "result.a + result.b + result.c";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadWithNullUndefined() {
        String script =
                "var obj = { ...null, ...undefined, a: 1 };\n"
                        + "obj.a + ':' + Object.keys(obj).length";
        Utils.assertWithAllModes_ES6("1:1", script);
    }

    @Test
    public void testObjectSpreadNumericKeys() {
        String script =
                "var x = { 1: 'a', 3.14: 'c' };\n"
                        + "var y = { ...x, 2: 'b', 4.56: 'd' };\n"
                        + "y[1] + y[2] + y[3.14] + y['4.56']";
        Utils.assertWithAllModes_ES6("abcd", script);
    }

    @Test
    public void testObjectSpreadSymbolsKeys() {
        String script =
                "var sym = Symbol('test');\n"
                        + "var obj1 = { [sym]: 'value' };\n"
                        + "var obj2 = { ...obj1 };\n"
                        + "obj2[sym] === 'value' && obj2[sym] === obj1[sym]";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testObjectSpreadGetterKeys() {
        String script =
                "var obj1 = { get foo() { return 1; } };\n"
                        + "var obj2 = { ...obj1 };\n"
                        + "obj2.foo";
        Utils.assertWithAllModes_ES6(1, script);
    }

    @Test
    public void testObjectSpreadNotEnumerableKeys() {
        String script =
                "var x = {};\n"
                        + "Object.defineProperty(x, 'd', { value: 4, enumerable: false})\n"
                        + "var y = { ...x};\n"
                        + "y.d";
        Utils.assertWithAllModes_ES6(Undefined.instance, script);
    }

    @Test
    public void testObjectSpreadInsideGenerator() {
        String script =
                "var obj1 = { a: 1, b: 2 };\n"
                        + "function *gen() {\n"
                        + "    yield { ...obj1, c: 3 }\n"
                        + "}\n"
                        + "var g = gen();\n"
                        + "var obj = g.next().value;\n"
                        + "obj.a + obj.b + obj.c;\n";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadNonObjects() {
        String script =
                "var result = { ...1, ...false, ...Symbol.match };\n"
                        + "Object.keys(result).length";
        Utils.assertWithAllModes_ES6(0, script);
    }

    @Test
    public void testObjectSpreadArray() {
        String script =
                "var result = { ...['a', 'b'] };\n"
                        + "result[0] + result[1] + ':' + Object.keys(result).length";
        Utils.assertWithAllModes_ES6("ab:2", script);
    }

    @Test
    public void testObjectSpreadString() {
        String script =
                "var result = { ...'abc' };\n"
                        + "result[0] + result[1] + result[2] + ':' + Object.keys(result).length";
        Utils.assertWithAllModes_ES6("abc:3", script);
    }
}
