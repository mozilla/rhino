package org.mozilla.javascript.tests;

import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class OptionalChainingOperatorTest {
    @Test
    public void requiresES6() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    assertThrows(
                            EvaluatorException.class,
                            () -> cx.evaluateString(scope, "a?.b", "test.js", 0, null));
                    return null;
                });
    }

    @Test
    public void simplePropertyAccess() {
        Utils.assertWithAllOptimizationLevelsES6("val", "var a = {b: 'val'}; a?.b");
        Utils.assertWithAllOptimizationLevelsES6("val", "var a = {b: {c: 'val'}}; a?.b?.c");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "var a = null; a?.b");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "var a = undefined; a?.b");
    }

    @Test
    public void specialRef() {
        Utils.assertWithAllOptimizationLevelsES6(
                true, "var a = {}; a?.__proto__ === Object.prototype");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "var a = null; a?.__proto__");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "var a = undefined; a?.__proto__");
    }

    @Test
    public void afterExpression() {
        Utils.assertWithAllOptimizationLevelsES6(1, "var a = {b: 'x'}; a.b?.length");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "var a = {b: 'x'}; a.c?.length");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "var a = [1, 2, 3]; a[42]?.name");
    }

    @Test
    public void expressions() {
        Utils.assertWithAllOptimizationLevelsES6(true, "o = {a: true}; o?.['a']");
        Utils.assertWithAllOptimizationLevelsES6(true, "o = {[42]: true}; o?.[42]");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "o = null; o?.['a']");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "o = undefined; o?.['a']");
    }

    @Test
    public void expressionsAreNotEvaluatedIfNotNecessary() {
        Utils.assertWithAllOptimizationLevelsES6(
                1,
                "var counter = 0;\n"
                        + "function f() { ++counter; return 0; }\n"
                        + "var o = {}\n"
                        + "o?.[f()];\n"
                        + "counter\n");
        Utils.assertWithAllOptimizationLevelsES6(
                0,
                "var counter = 0;\n"
                        + "function f() { ++counter; return 0; }\n"
                        + "null?.[f()];\n"
                        + "counter\n");
        Utils.assertWithAllOptimizationLevelsES6(
                0,
                "var counter = 0;\n"
                        + "function f() { ++counter; return 0; }\n"
                        + "undefined?.[f()];\n"
                        + "counter\n");
    }

    @Test
    public void leftHandSideIsEvaluatedOnlyOnce() {
        Utils.assertWithAllOptimizationLevelsES6(
                1,
                "var counter = 0;\n"
                        + "function f() {\n"
                        + "  ++counter;\n"
                        + "  return 'abc';\n"
                        + "}\n"
                        + "f()?.length;\n"
                        + "counter\n");
    }
}
