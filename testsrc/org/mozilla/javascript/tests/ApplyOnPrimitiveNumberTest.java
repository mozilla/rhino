/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptableObject;

import junit.framework.TestCase;

/**
 * Primitive numbers are not wrapped before calling apply.
 * Test for bug <a href="https://bugzilla.mozilla.org/show_bug.cgi?id=466661">466661</a>.
 * @author Marc Guillemot
 */
public class ApplyOnPrimitiveNumberTest extends TestCase
{
    public void testIt()
    {
        final String script = "var fn = function() { return this; }\n"
            + "fn.apply(1)";

        Utils.runWithAllOptimizationLevels(_cx -> {
            final ScriptableObject scope = _cx.initStandardObjects();
            final Object result = _cx.evaluateString(scope, script, "test script", 0, null);
            assertEquals("object", ScriptRuntime.typeof(result));
            assertEquals("1", Context.toString(result));
            return null;
        });
    }
}
