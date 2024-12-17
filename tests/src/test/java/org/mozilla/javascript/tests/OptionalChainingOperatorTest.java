package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.Undefined;

public class OptionalChainingOperatorTest {
    @Test
    public void requiresES6() {
        Utils.assertEvaluatorException_1_8("syntax error (test#1)", "a?.b");
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

    @Test
    public void doesNotLeakVariables() {
        Utils.assertWithAllOptimizationLevelsES6(false, "$0 = false; o = {}; o?.x; $0");
    }

    @Test
    public void shortCircuits() {
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = undefined; a?.b.c");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = {}; a.b?.c.d.e");
    }

    @Test
    public void standardFunctionCall() {
        // Various combination of arguments for compiled mode, where we have special cases for 0, 1,
        // and 2 args

        Utils.assertWithAllOptimizationLevelsES6(1, "function f() {return 1;} f?.()");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "f = null; f?.()");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "f = undefined; f?.()");

        Utils.assertWithAllOptimizationLevelsES6(1, "function f(x) {return x;} f?.(1)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "f = null; f?.(1)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "f = undefined; f?.(1)");

        Utils.assertWithAllOptimizationLevelsES6(2, "function f(x, y) {return y;} f?.(1, 2)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "f = null; f?.(1, 2)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "f = undefined; f?.(1, 2)");

        Utils.assertWithAllOptimizationLevelsES6(3, "function f(x, y, z) {return z;} f?.(1, 2, 3)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "f = null; f?.(1, 2, 3)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "f = undefined; f?.(1, 2, 3)");
    }

    @Test
    public void standardFunctionCallWithParentScope() {
        // Needed because there are some special paths in ScriptRuntime when we have a parent scope.
        // A "with" block is the easiest way to get one.
        Utils.assertWithAllOptimizationLevelsES6(
                1, "function f(x) {return x;} x = {}; with (x) { f?.(1) }");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "x = {}; with (x) { f = null; f?.(1) }");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "x = {}; with (x) { f = undefined; f?.(1) }");
    }

    @Test
    public void specialFunctionCall() {
        Utils.assertWithAllOptimizationLevelsES6(
                1, "a = { __parent__: function(x) {return x;} }; a.__parent__?.(1)");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "a = { __parent__: null }; a.__parent__?.(1)");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "a = { __parent__: undefined }; a.__parent__?.(1)");
    }

    @Test
    public void memberFunctionCall() {
        Utils.assertWithAllOptimizationLevelsES6(1, "a = { f: function() {return 1;} }; a.f?.()");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = {f: null}; a.f?.()");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "a = {f: undefined}; a.f?.(1)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = {}; a.f?.()");

        Utils.assertWithAllOptimizationLevelsES6(1, "a = { f: function(x) {return x;} }; a.f?.(1)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = {f: null}; a.f?.(1)");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "a = {f: undefined}; a.f?.(1)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = {}; a.f?.(1)");

        Utils.assertWithAllOptimizationLevelsES6(
                2, "a = { f: function(x, y) {return y;} }; a.f?.(1, 2)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = {f: null}; a.f?.(1, 2)");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "a = {f: undefined}; a.f?.(1, 2)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = {}; a.f?.(1, 2)");

        Utils.assertWithAllOptimizationLevelsES6(
                3, "a = { f: function(x, y, z) {return z;} }; a.f?.(1, 2, 3)");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "a = {f: null}; a.f?.(1, 2, 3)");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "a = {f: undefined}; a.f?.(1, 2, 3)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = {}; a.f?.(1, 2, 3)");
    }

    @Test
    public void expressionFunctionCall() {
        Utils.assertWithAllOptimizationLevelsES6(1, "a = [ function(x) {return x;} ]; a[0]?.(1)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = [null]; a[0]?.(1)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = [undefined]; a[0]?.(1)");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = []; a[0]?.(1)");
    }

    @Test
    public void specialCall() {
        Utils.assertWithAllOptimizationLevelsES6(
                1, "eval = function () { return 1 };\n" + "eval?.()");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "eval = null;\n" + "eval?.()");
        Utils.assertWithAllOptimizationLevelsES6(
                Undefined.instance, "eval = undefined;\n" + "eval?.()");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "delete eval;\n" + "eval?.()");
    }

    @Test
    public void shortCircuitsFunctionCalls() {
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = undefined; a?.b()");
        Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "a = {}; a.b?.c().d()");
    }

    @Test
    public void shortCircuitArgumentEvaluation() {
        Utils.assertWithAllOptimizationLevelsES6(1, "c = 0; f = function(x){}; f?.(c++); c");
        Utils.assertWithAllOptimizationLevelsES6(0, "c = 0; f = undefined; f?.(c++); c");
        Utils.assertWithAllOptimizationLevelsES6(0, "c = 0; f = null; f?.(c++); c");

        Utils.assertWithAllOptimizationLevelsES6(1, "c = 0; a = {f: function() {}}; a.f?.(c++); c");
        Utils.assertWithAllOptimizationLevelsES6(0, "c = 0; a = {f: undefined}; a.f?.(c++); c");
        Utils.assertWithAllOptimizationLevelsES6(0, "c = 0; a = {f: null}; a.f?.(c++); c");
        Utils.assertWithAllOptimizationLevelsES6(0, "c = 0; a = {}; a.f?.(c++); c");

        Utils.assertWithAllOptimizationLevelsES6(1, "c = 0; a = [function() {}]; a[0]?.(c++); c");
        Utils.assertWithAllOptimizationLevelsES6(0, "c = 0; a = [undefined]; a[0]?.(c++); c");
        Utils.assertWithAllOptimizationLevelsES6(0, "c = 0; a = [null]; a[0]?.(c++); c");
        Utils.assertWithAllOptimizationLevelsES6(0, "c = 0; a = []; a[0]?.(c++); c");

        Utils.assertWithAllOptimizationLevelsES6(
                1, "c = 0; a = {__parent__: function() {}}; a.__parent__?.(c++); c");
        Utils.assertWithAllOptimizationLevelsES6(
                0, "c = 0; a = {__parent__: undefined}; a.__parent__?.(c++); c");
        Utils.assertWithAllOptimizationLevelsES6(
                0, "c = 0; a = {__parent__: null}; a.__parent__?.(c++); c");
        Utils.assertWithAllOptimizationLevelsES6(0, "c = 0; a = {}; a.__parent__.f?.(c++); c");
    }

    @Test
    public void toStringOfOptionalChaining() {
        Utils.assertWithAllOptimizationLevelsES6(
                "function f() { a?.b }", "function f() { a?.b } f.toString()");
        Utils.assertWithAllOptimizationLevelsES6(
                "function f() { a?.() }", "function f() { a?.() } f.toString()");
    }

    @Test
    public void optionalChainingOperatorFollowedByDigitsIsAHook() {
        Utils.assertWithAllOptimizationLevelsES6(0.5, "true ?.5 : false");
    }
}
