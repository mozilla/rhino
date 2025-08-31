/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/** Verifies template literals work correctly with Android optimization level. */
public class AndroidJitTemplateLiteralTest {

    @Test
    public void testTemplateLiteralWithAndroidOptimization() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1); // Android uses -1
            Scriptable scope = cx.initStandardObjects();

            // Basic template literal
            Object result = cx.evaluateString(scope, "`Hello ${'World'}`", "test", 1, null);
            assertEquals("Hello World", result);

            // Tagged template literal
            String script =
                    "function tag(strings, ...values) {"
                            + "  return strings[0] + values[0] + strings[1];"
                            + "}"
                            + "tag`Count: ${42}!`";
            result = cx.evaluateString(scope, script, "test", 1, null);
            assertEquals("Count: 42!", result);
        }
    }

    @Test
    public void testCallSiteObjectFrozen() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1);
            Scriptable scope = cx.initStandardObjects();

            String script =
                    "let site;"
                            + "function tag(strings) { site = strings; }"
                            + "tag`test`;"
                            + "Object.isFrozen(site) && Object.isFrozen(site.raw)";

            Object result = cx.evaluateString(scope, script, "test", 1, null);
            assertEquals(true, result);
        }
    }
}
