/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class BoundFunctionTest {

    @Test
    public void ctorCallableThis() {
        String code = "function foo() {};\n" + " foo.bind({}).name;";

        Utils.assertWithAllModes_ES6("bound foo", code);
    }

    @Test
    public void invokeBoundCallManyArgs() {
        /* This test is a little fiddly. The call to bind causes the
        max stack size to be high enough that it could mask the
        bug, so we have to make the call to the bound in function
        in another function which has a smaller stack. */
        String code =
                "function f() { return 'Hello!'; };\n"
                        + "var b = f.call.bind(f, 1, 2, 3);\n"
                        + "(function(){ return b(); })();";

        Utils.assertWithAllModes_ES6("Hello!", code);
    }
}
