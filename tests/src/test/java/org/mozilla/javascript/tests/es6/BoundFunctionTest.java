/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

public class BoundFunctionTest {

    @Test
    public void ctorCallableThis() {
        String code = "function foo() {};\n" + " foo.bind({}).name;";

        Utils.assertWithAllModes_ES6("bound foo", code);
    }
}
