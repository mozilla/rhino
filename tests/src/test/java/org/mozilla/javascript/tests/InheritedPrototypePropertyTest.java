/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * A function without an own "prototype" property must still find one inherited through its
 * prototype chain, in compiled mode as well as in the interpreter.
 */
class InheritedPrototypePropertyTest {

    @Test
    void methodInheritsFromFunctionPrototype() {
        Utils.assertWithAllModes_ES6(
                "inherited",
                "Function.prototype.prototype = 'inherited'; ({ m() {} }).m.prototype");
    }

    @Test
    void arrowFunctionInheritsFromFunctionPrototype() {
        Utils.assertWithAllModes_ES6(
                "inherited", "Function.prototype.prototype = 'inherited'; (() => 1).prototype");
    }

    @Test
    void oneCallSiteSeesFunctionsWithAndWithoutOwnPrototype() {
        Utils.assertWithAllModes_ES6(
                "object,inherited,object,inherited",
                "Function.prototype.prototype = 'inherited';"
                        + "function f() {}"
                        + "var m = ({ m() {} }).m;"
                        + "function read(fn) { var p = fn.prototype; return typeof p === 'object' ? 'object' : p; }"
                        + "[read(f), read(m), read(f), read(m)].join(',')");
    }

    @Test
    void boundFunctionInheritsFromFunctionPrototype() {
        Utils.assertWithAllModes_ES6(
                "inherited",
                "Function.prototype.prototype = 'inherited';"
                        + "function f() {} f.bind(null).prototype");
    }

    @Test
    void methodInheritsFromCustomPrototype() {
        Utils.assertWithAllModes_ES6(
                "custom",
                "var m = ({ m() {} }).m;"
                        + "Object.setPrototypeOf(m, { prototype: 'custom' }); m.prototype");
    }

    @Test
    void ownPrototypeShadowsInherited() {
        Utils.assertWithAllModes_ES6(
                "object",
                "Function.prototype.prototype = 'inherited';"
                        + "function f() {} typeof f.prototype");
        Utils.assertWithAllModes_ES6(
                "undefined",
                "Function.prototype.prototype = 'inherited';"
                        + "function f() {} f.prototype = undefined; typeof f.prototype");
    }

    @Test
    void noPrototypeAnywhereIsUndefined() {
        Utils.assertWithAllModes_ES6("undefined", "typeof ({ m() {} }).m.prototype");
    }
}
