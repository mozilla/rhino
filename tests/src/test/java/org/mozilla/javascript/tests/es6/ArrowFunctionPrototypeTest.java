/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class ArrowFunctionPrototypeTest {

    @Test
    void noPrototypeProperty() {
        Utils.assertWithAllModes_ES6("undefined", "var f = () => 1; typeof f.prototype");
        Utils.assertWithAllModes_ES6("undefined", "typeof (() => 1).prototype");
        Utils.assertWithAllModes_ES6(
                "undefined",
                "var f = () => 1; typeof Object.getOwnPropertyDescriptor(f, 'prototype')");
        Utils.assertWithAllModes_ES6(false, "'prototype' in (() => 1)");
    }

    @Test
    void noPrototypePropertyInFunction() {
        Utils.assertWithAllModes_ES6(
                "undefined", "function g() { var f = () => this; return typeof f.prototype; } g()");
    }

    @Test
    void functionExpressionKeepsPrototype() {
        Utils.assertWithAllModes_ES6("object", "var f = function () {}; typeof f.prototype");
        Utils.assertWithAllModes_ES6("object", "function f() {}; typeof f.prototype");
    }
}
