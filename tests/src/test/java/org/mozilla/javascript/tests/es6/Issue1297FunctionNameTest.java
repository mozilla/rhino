/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Test that we can redefine a function's name. */
public class Issue1297FunctionNameTest {

    @Test
    public void canSetFunctionName() {
        final String code =
                "'use strict';"
                        + "function X() {};\n"
                        + "Object.defineProperty(X, 'name', {value: 'y', configurable: true, writable: true});"
                        + "X.name";

        Utils.assertWithAllModes("y", code);
    }
}
