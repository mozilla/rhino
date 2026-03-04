/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.testutils.Utils;

public class DirectConstructorCallTest {
    /**
     * Test that calling Function.construct() directly from Java works even if no top-level call is
     * active. This was a regression where JSFunction.construct() did not set up a top-level call,
     * causing an assertion failure in the interpreter.
     */
    @Test
    public void testDirectConstructInterpreted() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    Function f =
                            cx.compileFunction(
                                    scope, "function f(a) { this.x = a; }", "test.js", 1, null);

                    // This used to fail with IllegalStateException: FAILED ASSERTION
                    Scriptable obj = f.construct(cx, scope, new Object[] {42});

                    assertEquals(42, Context.toNumber(ScriptableObject.getProperty(obj, "x")), 0.0);
                    return null;
                });
    }
}
