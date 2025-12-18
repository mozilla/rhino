/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class FunctionArityTest {

    @Test
    void simple() {
        // todo Utils.assertWithAllModes_ES6("undefined", "function f(a,b) {}; '' + f.arity");
        // todo Utils.assertWithAllModes_ES6("undefined", "function f() {}; '' + f.arity");
        Utils.assertWithAllModes_ES6("2", "function f(a,b) {}; '' + f.arity");
        Utils.assertWithAllModes_ES6("0", "function f() {}; '' + f.arity");

        Utils.assertWithAllModes_1_8("2", "function f(a,b) {}; '' + f.arity");
        Utils.assertWithAllModes_1_8("0", "function f() {}; '' + f.arity");
    }

    @Test
    void arrow() {
        // todo Utils.assertWithAllModes_ES6("undefined", "'' + ((a,b) => {}).arity");
        // todo Utils.assertWithAllModes_ES6("undefined", "'' + (() => {}).arity");
        Utils.assertWithAllModes_ES6("2", "'' + ((a,b) => {}).arity");
        Utils.assertWithAllModes_ES6("0", "'' + (() => {}).arity");

        Utils.assertWithAllModes_1_8("2", "'' + ((a,b) => {}).arity");
        Utils.assertWithAllModes_1_8("0", "'' + (() => {}).arity");
    }
}
