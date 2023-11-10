/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Issue1402Test {
    @Test
    public void codeCanBeRunWithoutRaisingErrorInLegacyMode() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_1_0);

                    Scriptable scope = cx.initStandardObjects(null);
                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var Base = function() {};\n"
                                            + "var Extending = function() {};\n"
                                            + "Extending.prototype = Base;\n"
                                            + "var x = new Extending();\n"
                                            + "!!x\n",
                                    "test",
                                    1,
                                    null);
                    assertEquals(true, result);
                    return null;
                });
    }

    @Test
    public void emptyArraysCoerceToFalseInV10() {
        assertEmptyArrayCoercesTo(Context.VERSION_1_0, false);
    }

    @Test
    public void emptyArraysCoerceToFalseInV11() {
        assertEmptyArrayCoercesTo(Context.VERSION_1_1, false);
    }

    @Test
    public void emptyArraysCoerceToTrueInV12() {
        assertEmptyArrayCoercesTo(Context.VERSION_1_2, true);
    }

    @Test
    public void emptyArraysCoerceToTrueInNormalVersions() {
        assertEmptyArrayCoercesTo(Context.VERSION_1_3, true);
    }

    private static void assertEmptyArrayCoercesTo(int version, boolean expected) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(version);

                    Scriptable scope = cx.initStandardObjects(null);
                    Object result = cx.evaluateString(scope, "!![]", "test", 1, null);
                    assertEquals(expected, result);
                    return null;
                });
    }
}
