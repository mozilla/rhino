/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class PromiseTest {

    @Test
    public void ctorCallableThis() {
        final String script =
                "  var r = '';"
                        + "  var p = new Promise(function(resolve, reject) {\n"
                        + "      r += this;\n"
                        + "    });\n"
                        + "  r += ' done';\n"
                        + "  r;";
        Utils.assertWithAllModesTopLevelScope_ES6("[object global] done", script);
    }

    @Test
    public void ctorCallableThisStrict() {
        final String script =
                "'use strict';"
                        + "  var r = '';"
                        + "  var p = new Promise(function(resolve, reject) {\n"
                        + "      r += this === undefined;\n"
                        + "    });\n"
                        + "  r += ' done';\n"
                        + "  r;";
        Utils.assertWithAllModes_ES6("true done", script);
    }
}
