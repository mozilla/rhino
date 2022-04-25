/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/** Tests the ConsString class to ensure it properly supports String, StringBuffer and StringBuilder. */
public class Issue1206Test {
    @Test
    public void testConsStringUsingString() {
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects(null);
        scope.put("var1", scope, "hello");
        cx.evaluateString(scope, "var1 = var1 + ' world'", "test", 1, null);
    }

    @Test
    public void testConsStringUsingStringBuffer() {
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects(null);
        scope.put("var1", scope, new StringBuffer("hello"));
        cx.evaluateString(scope, "var1 = var1 + ' world'", "test", 1, null);
    }

    @Test
    public void testConsStringUsingStringBuilder() {
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects(null);
        scope.put("var1", scope, new StringBuilder("hello"));
        cx.evaluateString(scope, "var1 = var1 + ' world'", "test", 1, null);
    }
}
