package org.mozilla.javascript.tests;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class NullishCoalescingOpTest {

    @Test
    public void testNullishColascingBasic() {
        try (Context cx = Context.enter()) {
            Scriptable scope = cx.initStandardObjects();
            cx.setLanguageVersion(Context.VERSION_ES6);

            String script = "null ?? 'default string'";
            Assert.assertEquals(
                    "default string",
                    cx.evaluateString(scope, script, "nullish coalescing basic", 0, null));

            String script2 = "undefined ?? 'default string'";
            Assert.assertEquals(
                    "default string",
                    cx.evaluateString(scope, script2, "nullish coalescing basic", 0, null));
        }
    }

    @Test
    public void testNullishColascingPrecedence() {
        try (Context cx = Context.enter()) {
            Scriptable scope = cx.initStandardObjects();
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1);

            String script1 = "3 == 3 ? 'yes' ?? 'default string' : 'no'";
            Assert.assertEquals(
                    "yes", cx.evaluateString(scope, script1, "nullish coalescing basic", 0, null));

            String script3 = "3 || null ?? 'default string'";
            Assert.assertEquals(
                    3.0, cx.evaluateString(scope, script3, "nullish coalescing basic", 0, null));

            String script2 = "3 && null ?? 'default string'";
            Assert.assertEquals(
                    "default string",
                    cx.evaluateString(scope, script2, "nullish coalescing basic", 0, null));
        }
    }
}
