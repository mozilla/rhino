/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.tests.Utils;

public class PromiseTest {

    @Test
    public void ctorCallableThis() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects(new TopLevel(), false);

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "  var r = '';"
                                            + "  var p = new Promise(function(resolve, reject) {\n"
                                            + "      r += this;\n"
                                            + "    });\n"
                                            + "  r += ' done';\n"
                                            + "  r;",
                                    "test",
                                    1,
                                    null);
                    assertEquals("[object global] done", result);

                    return null;
                });
    }

    @Test
    public void ctorCallableThisStrict() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "'use strict';"
                                            + "  var r = '';"
                                            + "  var p = new Promise(function(resolve, reject) {\n"
                                            + "      r += this === undefined;\n"
                                            + "    });\n"
                                            + "  r += ' done';\n"
                                            + "  r;",
                                    "test",
                                    1,
                                    null);
                    assertEquals("true done", result);

                    return null;
                });
    }
}
