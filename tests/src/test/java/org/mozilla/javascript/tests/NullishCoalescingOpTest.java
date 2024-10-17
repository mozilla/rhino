package org.mozilla.javascript.tests;

import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;

public class NullishCoalescingOpTest {
    @Test
    public void testNullishCoalescingOperatorRequiresES6() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    assertThrows(
                            EvaluatorException.class,
                            () -> cx.evaluateString(scope, "true ?? false", "test.js", 0, null));
                    return null;
                });
    }

    @Test
    public void testNullishCoalescingBasic() {
        Utils.assertWithAllOptimizationLevelsES6("val", "'val' ?? 'default string'");
        Utils.assertWithAllOptimizationLevelsES6("default string", "null ?? 'default string'");
        Utils.assertWithAllOptimizationLevelsES6("default string", "undefined ?? 'default string'");
    }

    @Test
    public void testNullishCoalescingShortCircuit() {
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
    public void testNullishCoalescingPrecedence() {
        Utils.assertWithAllOptimizationLevelsES6(
                "yes", "3 == 3 ? 'yes' ?? 'default string' : 'no'");
    }

    @Test
    public void testNullishCoalescingEvalOnce() {
        String script =
                "var runs = 0; \n"
                        + "function f() { runs++; return 3; } \n"
                        + "var eval1 = f() ?? 42; \n"
                        + "runs";
        Utils.assertWithAllOptimizationLevelsES6(1, script);
    }

    @Test
    public void testNullishCoalescingDoesNotEvaluateRightHandSideIfNotNecessary() {
        String script =
                "var runs = 0; \n"
                        + "function f() { runs++; return 3; } \n"
                        + "var eval1 = 42 ?? f(); \n"
                        + "runs";
        Utils.assertWithAllOptimizationLevelsES6(0, script);
    }

    @Test
    public void testNullishCoalescingDoesNotLeakVariables() {
        String script = "$0 = false; true ?? true; $0";
        Utils.assertWithAllOptimizationLevelsES6(false, script);
    }
}
