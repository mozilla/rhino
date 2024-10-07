package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class OptionalChainingOperatorTests {

    @Test
    public void testOptionalChainingOperator() {

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    String sourceName = "optionalChainingOperator";
                    Scriptable scope = cx.initStandardObjects();
                    assertEquals(
                            Undefined.instance,
                            cx.evaluateString(
                                    scope, "var nul = null; nul?.a", sourceName, 1, null));

                    String script = " var a = {name: 'val'}; a.outerProp?.innerProp";
                    assertEquals(
                            Undefined.instance,
                            cx.evaluateString(scope, script, sourceName, 1, null));

                    String script2 =
                            " var a = {outerProp: {innerProp: 'val' } }; a.outerProp?.innerProp";
                    assertEquals("val", cx.evaluateString(scope, script2, sourceName, 1, null));

                    String script3 =
                            "var a = {outerProp: {innerProp: { innerInnerProp: {name: 'val' } } } }; "
                                    + "a.outerProp?.innerProp?.missingProp?.name";
                    assertEquals(
                            Undefined.instance,
                            cx.evaluateString(scope, script3, sourceName, 1, null));

                    String script4 =
                            " var a = {outerProp: {innerProp: { innerInnerProp: {name: 'val' } } } }; "
                                    + "a.outerProp?.innerProp?.innerInnerProp?.name";
                    assertEquals("val", cx.evaluateString(scope, script4, sourceName, 1, null));

                    String script5 = " var a = {}; a.someNonExistentMethod?.()";
                    assertEquals(
                            Undefined.instance,
                            cx.evaluateString(scope, script5, sourceName, 1, null));
                    assertEquals(
                            Undefined.instance,
                            cx.evaluateString(
                                    scope,
                                    "function fn3 () {\n"
                                            + "  return () => {\n"
                                            + "    return null;\n"
                                            + "  };\n"
                                            + "}"
                                            + " fn3()()?.a",
                                    sourceName,
                                    1,
                                    null));
                    assertEquals(
                            Undefined.instance,
                            cx.evaluateString(
                                    scope,
                                    " var a = {}; a.someNonExistentMethod?.()",
                                    sourceName,
                                    1,
                                    null));

                    // SpecialRef and Optional Chaining operator
                    assertEquals(
                            Undefined.instance,
                            cx.evaluateString(
                                    scope, " var a = null; a?.__proto__", sourceName, 1, null));
                    assertEquals(
                            Undefined.instance,
                            cx.evaluateString(scope, "a?.__proto__", sourceName, 1, null));

                    assertEquals(
                            Undefined.instance,
                            cx.evaluateString(scope, "a?.__parent__", sourceName, 1, null));

                    var e =
                            Assert.assertThrows(
                                    EvaluatorException.class,
                                    () ->
                                            cx.evaluateString(
                                                    scope,
                                                    "var y = {};\n"
                                                            + "0, { x: y?.z = 42 } = { x: 23 };",
                                                    sourceName,
                                                    1,
                                                    null));
                    Assert.assertTrue(
                            e.getMessage().contains("Invalid left-hand side in assignment"));

                    e =
                            Assert.assertThrows(
                                    EvaluatorException.class,
                                    () ->
                                            cx.evaluateString(
                                                    scope, "y?.z = 42", sourceName, 1, null));
                    Assert.assertTrue(
                            e.getMessage().contains("Invalid left-hand side in assignment"));

                    e =
                            Assert.assertThrows(
                                    EvaluatorException.class,
                                    () ->
                                            cx.evaluateString(
                                                    scope, "y.z?.x = 42", sourceName, 1, null));
                    Assert.assertTrue(
                            e.getMessage().contains("Invalid left-hand side in assignment"));

                    e =
                            Assert.assertThrows(
                                    EvaluatorException.class,
                                    () ->
                                            cx.evaluateString(
                                                    scope, "y?.z.x = 42", sourceName, 1, null));
                    Assert.assertTrue(
                            e.getMessage().contains("Invalid left-hand side in assignment"));

                    return null;
                });
    }

    @Test
    public void expressionsInOptionalChaining() {
        Utils.assertWithAllOptimizationLevelsES6(true, "o = {a: true}; o?.['a']");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "o = null; o?.['a']");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "o = undefined; o?.['a']");
    }

    @Test
    public void expressionsInOptionalChainingAreNotEvaluatedIfUnnecessary() {
        Utils.assertWithAllOptimizationLevelsES6(
                1,
                "c = 0;\n"
                        + "function f() { ++c; return 0; }\n"
                        + "o = {}\n"
                        + "o?.[f()];\n"
                        + "c\n");
        Utils.assertWithAllOptimizationLevelsES6(
                0, "c = 0;\n" + "function f() { ++c; return 0; }\n" + "null?.[f()];\n" + "c\n");
        Utils.assertWithAllOptimizationLevelsES6(
                0,
                "c = 0;\n" + "function f() { ++c; return 0; }\n" + "undefined?.[f()];\n" + "c\n");
    }
}
