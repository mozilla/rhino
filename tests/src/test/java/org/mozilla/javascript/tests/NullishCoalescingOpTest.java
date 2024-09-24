package org.mozilla.javascript.tests;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class NullishCoalescingOpTest {
    @Test
    public void testNullishColascingBasic() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
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
                    return null;
                });
    }

    @Test
    public void testNullishColascingShortCircuit() {
        String script = "0 || 0 ?? true";
        Utils.assertEvaluatorExceptionES6("Syntax Error: Unexpected token. (test#1)", script);

        String script2 = "0 && 0 ?? true";
        Utils.assertEvaluatorExceptionES6("Syntax Error: Unexpected token. (test#1)", script2);

        String script3 = "0 ?? 0 && true;";
        Utils.assertEvaluatorExceptionES6("Syntax Error: Unexpected token. (test#1)", script3);

        String script4 = "0 ?? 0 || true;";
        Utils.assertEvaluatorExceptionES6("Syntax Error: Unexpected token. (test#1)", script4);
    }

    @Test
    public void testNullishColascingPrecedence() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);

                    String script1 = "3 == 3 ? 'yes' ?? 'default string' : 'no'";
                    Assert.assertEquals(
                            "yes",
                            cx.evaluateString(scope, script1, "nullish coalescing basic", 0, null));
                    return null;
                });
    }

    @Test
    public void testNullishColascingEvalOnce() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    cx.setLanguageVersion(Context.VERSION_ES6);

                    String script1 =
                            "var runs = 0; \n"
                                    + "function f() { runs++; return 3; } \n"
                                    + "var eval1 = f() ?? 42; \n"
                                    + "runs";
                    Assert.assertEquals(
                            1,
                            cx.evaluateString(scope, script1, "nullish coalescing basic", 0, null));
                    return null;
                });
    }
}
