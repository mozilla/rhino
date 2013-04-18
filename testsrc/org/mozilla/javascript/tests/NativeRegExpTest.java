/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.regexp.NativeRegExp;

public class NativeRegExpTest extends TestCase {

    public void testOpenBrace() {
        final String script = "/0{0/";
        final ContextAction action = new ContextAction() {
            public Object run(final Context _cx) {
                final ScriptableObject scope = _cx.initStandardObjects();
                final Object result = _cx.evaluateString(scope, script, "test script", 0, null);
                assertEquals(script, Context.toString(result));
                return null;
            }
        };

        Utils.runWithAllOptimizationLevels(action);
    }
}
