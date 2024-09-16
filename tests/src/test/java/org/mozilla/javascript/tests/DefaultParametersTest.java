package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mozilla.javascript.*;

/*
   Many of these are taken from examples at developer.mozilla.org
*/
public class DefaultParametersTest {
    @Test
    public void functionDefaultArgsBasic() throws Exception {
        final String script = "function foo(a = 2) { return a; }";
        assertIntEvaluates(32, script + "\nfoo(32)");
        assertIntEvaluates(2, script + "\nfoo()");
        assertIntEvaluates(2, script + "\nfoo(undefined)");
    }

    @Test
    public void functionDefaultArgsBasicCall() throws Exception {
        final String script = "function b() { return 2; }; function foo(a = b()) { return a; }";
        assertIntEvaluates(32, script + "\nfoo(32)");
        assertIntEvaluates(2, script + "\nfoo()");
        assertIntEvaluates(2, script + "\nfoo(undefined)");
    }

    @Test
    public void functionDefaultArgsBasicArrow() throws Exception {
        final String script = "((a = 2, b) => { return a; })";
        assertIntEvaluates(32, script + "(32, 12)");
        assertIntEvaluates(12, script + "(12)");
        assertIntEvaluates(2, script + "()");
    }

    @Test
    public void functionDefaultArgsArrayArrow() throws Exception {
        final String script = "(([a = 2, b = 1] = [1, 2]) => { return a + b; })";
        assertIntEvaluates(3, script + "()");
        assertIntEvaluates(5, script + "([4,])");
        assertIntEvaluates(6, script + "([,4])");
    }

    @Test
    public void functionDefaultArgsMulti() throws Exception {
        final String script = "function foo(a = 2, b = 23) { return a + b; }";
        assertIntEvaluates(55, script + "\nfoo(32)");
        assertIntEvaluates(25, script + "\nfoo()");
        assertIntEvaluates(34, script + "\nfoo(32, 2)");
        assertIntEvaluates(25, script + "\nfoo(undefined, undefined)");
    }

    @Test
    public void functionDefaultArgsUsage() throws Exception {
        final String script = "function foo(a = 2, b = a * 2) { return a + b; }";
        assertIntEvaluates(96, script + "\nfoo(32)");
        assertIntEvaluates(6, script + "\nfoo()");
        assertIntEvaluates(34, script + "\nfoo(32, 2)");
    }

    @Test
    public void ObjIdInitSimpleStrictExpr() throws Exception {
        final String script =
                "(function () { \n " + "'use strict'; \n " + "(0, { eval = 0 } = {}) })()";
        assertThrows("syntax error", script);
    }

    @Test
    public void ObjIdInitSimpleStrictForOf() throws Exception {
        final String script = "for ({ eval = 0 } of [{}]) ;";
        assertThrows("syntax error", script);
    }

    @Test
    public void CoverInitName() throws Exception {
        final String script = "({ a = 1 });";
        assertThrows("syntax error", script);
    }

    @Test
    public void functionDefaultArgsObjectArrow() throws Exception {
        final String script = "(({x = 1} = {x: 2}) => {\n  return x;\n})";

        assertIntEvaluates(1, script + "({})");
        assertIntEvaluates(2, script + "()");
        assertIntEvaluates(3, script + "({x: 3})");
    }

    @Test
    @Ignore("defaults-not-supported-in-let-destructuring")
    public void letExprDestructuring() throws Exception {
        // JavaScript
        final String script =
                "function a() {}; (function() { "
                        + "            for (let {x = a()} = {}; ; ) { "
                        + "                return 3; "
                        + "            }"
                        + "        })()";
        assertIntEvaluates(3, script);
    }

    @Test
    public void letExprUnresolvableRefDestructuring() throws Exception {
        // JavaScript
        final String script =
                "  (function() { for (let [ x = unresolvableReference ] = []; ; ) {\n"
                        + "    return 3;\n"
                        + "  }})()";
        assertThrows("ReferenceError: \"unresolvableReference\" is not defined.", script);
    }

    @Test
    public void destructuringNestedArray() throws Exception {
        // JavaScript
        final String script = "let [[y], x] = [[4], 3]; x + y";
        assertIntEvaluates(7, script);
    }

    @Test
    public void letExprUnresolvableRefObjDestructuring() throws Exception {
        // JavaScript
        final String script = "var f = function({ x = unresolvableReference } = {}) {}; f()";
        assertThrows("ReferenceError: \"unresolvableReference\" is not defined.", script);
    }

    @Test
    public void getIntPropArg() throws Exception {
        final String script =
                "function foo([gen = function () { return 2; }, xGen = function* x() { yield 2; }] = []) {\n"
                        + " return gen() + xGen().next().value; }";
        assertIntEvaluates(4, script + "; foo()");
    }

    @Test
    public void getIntErrPropArg() throws Exception {
        final String script =
                "var e = 0; var b = 'hello'; var { f: y = ++e } = { f: { get: function() {}}}; "
                        + "Object.keys(y).includes('get') && Object.keys(y).length == 1";
        assertEvaluates(true, script);
    }

    @Test
    public void getIntPropArgParenExpr() throws Exception {
        final String script =
                "const [cover = (function () {}), xCover = (0, function() {})] = [];\n"
                        + "cover.name == 'cover' && xCover.name == 'xCover' ? 4 : -1";
        assertIntEvaluates(-1, script);
    }

    @Test
    public void getIntProp() throws Exception {
        final String script =
                "const { gen = function () { return 2;}, xGen = function* () { yield 2;} } = {};\n"
                        + "gen() + xGen().next().value";
        assertIntEvaluates(4, script);
    }

    @Test
    public void getIntPropExhausted() throws Exception {
        final String script = "const [x = 23] = []; x";
        assertIntEvaluates(23, script);
    }

    @Test
    @Ignore("temporal-dead-zone")
    public void functionDefaultArgsMultiFollowUsage() throws Exception {
        final String script =
                "function f(a = go()) {\n"
                        + "  function go() {\n"
                        + "    return \":P\";\n"
                        + "  }\n"
                        + " return a; "
                        + "}\n"
                        + "\n";
        assertIntEvaluates(24, script + "\nf(24)");
        assertThrows(
                "ReferenceError: \"go\" is not defined.", "function f() { go() }; var f1 = f()");
        assertThrows("ReferenceError: \"go\" is not defined.", script + "\nf()");
    }

    @Test
    @Ignore("temporal-dead-zone")
    public void functionDefaultArgsMultiReferEarlier() throws Exception {
        final String script = "var f = function(a = b * 2, b = 3) { return a * b; }\n";
        assertThrows("ReferenceError: \"b\" is not defined.", script + "\nf()");
    }

    @Test
    public void functionConstructor() throws Exception {
        final String script = "const f = new Function('a=2', 'b=a', 'return a + b');";
        assertIntEvaluates(4, script + "f()");
        assertIntEvaluates(6, script + "f(3)");
        assertIntEvaluates(16, script + "f(3, 13)");
    }

    @Test
    public void destructuringAssigmentDefaultArray() throws Exception {
        final String script =
                "function f([x = 1, y = 2] = [], [z = 1] = [4]) {\n"
                        + "  return x + y + z;\n"
                        + "}";

        assertIntEvaluates(7, script + "f()");
        assertIntEvaluates(7, script + "f([])");
        assertIntEvaluates(4, script + "f([], [])");
        assertIntEvaluates(8, script + "f([2])");
        assertIntEvaluates(5, script + "f([2], [])");
        assertIntEvaluates(8, script + "f([], [5])");
        assertIntEvaluates(6, script + "f([2, 3], [])");
        assertIntEvaluates(9, script + "f([2, 3], [4])");
        assertIntEvaluates(7, script + "f([2], [3])");
        assertIntEvaluates(9, script + "f([2, 3])");
    }

    @Test
    @Ignore("needs-checking-for-iterator")
    public void destructuringAssigmentInFunctionsWithObjectDefaults() throws Exception {
        final String script = "function f([x = 1, y = 2] = {x: 3, y: 4}) {\n return x + y;\n }";

        assertThrows("TypeError", script + "f()"); // TODO: returns 3
        assertThrows("TypeError", script + "f(2)"); // TODO: returns 3, should be throwing TypeError
    }

    @Test
    public void destructuringTest() throws Exception {
        final String script = "function f([x]) { return x; }; f([1]);";
        assertIntEvaluates(1, script);
    }

    @Test
    public void destructuringAssignmentDefaultArray() throws Exception {
        final String script = "var [a = 10] = []; a";
        final String script2 = "var [a = 10] = [1]; a";
        assertIntEvaluates(10, script);
        assertIntEvaluates(1, script2);
    }

    @Test
    public void destructuringAssignmentDefaultObject() throws Exception {
        final String script1 = "var {a: b = 10} = {hello: 3}; b+a";
        final String script3 = "var a = 20; var {a: b = 10} = {hello: 3}; b+a";
        final String script4 = "var a = 30; var {a: b = 10} = {}; b+a";
        assertThrows("ReferenceError", script1);
        assertIntEvaluates(30, script3);
        assertIntEvaluates(40, script4);
    }

    @Test
    public void destructuringHookTest() throws Exception {
        final String script = "function f([x]) { return x == undefined ? 2 : x; }; f([1]);";
        assertIntEvaluates(1, script);
    }

    @Test
    public void destructuringAssigmentRealRealBasicArray() throws Exception {
        final String script = "function f([x] = [1]) {\n return x;\n }";
        assertIntEvaluates(1, script + "f()");
        assertIntEvaluates(2, script + "f([2])");
        assertIntEvaluates(42, script + "f([]) == undefined ? 42 : 0");
    }

    @Test
    public void destructuringAssigmentRealBasicArray() throws Exception {
        final String script = "function f([x = 1]) {\n return x;\n }";
        assertIntEvaluates(1, script + "f([])");
        assertIntEvaluates(3, script + "f([3])");
        assertThrows("TypeError", script + "f()");
    }

    @Test
    public void destructuringAssigmentBasicArray() throws Exception {
        final String script = "function f([x = 1] = [2]) {\n return x;\n }";
        assertIntEvaluates(1, script + "f([])");
        assertIntEvaluates(2, script + "f()");
        assertIntEvaluates(3, script + "f([3])");
    }

    @Test
    public void destructuringAssigmentBasicObject() throws Exception {
        final String script = "function f({x = 1} = {x: 2}) {\n return x;\n }";

        assertIntEvaluates(1, script + "f({})");
        assertIntEvaluates(2, script + "f()");
        assertIntEvaluates(3, script + "f({x: 3})");
    }

    @Test
    public void destructuringAssigmentRealBasicObject() throws Exception {
        final String script = "function f({x = 1}) {\n return x;\n }";

        assertIntEvaluates(1, script + "f({})");
        assertThrows("TypeError", script + "f()");
        assertIntEvaluates(3, script + "f({x: 3})");
    }

    @Test
    public void destructuringAssigmentDefaultObject() throws Exception {
        final String script = "function f({ z = 3, x = 2 } = {}) {\n return z;\n}\n";
        assertIntEvaluates(3, script + "f()");
        assertIntEvaluates(3, script + "f({})");
        assertIntEvaluates(2, script + "f({z: 2})");
    }

    @Test
    public void destructuringAssigmentDefaultObjectWithDefaults() throws Exception {
        final String script = "function f({ z = 3, x = 2 } = {z: 4, x: 5}) {\n return z;\n}\n";
        assertIntEvaluates(4, script + "f()");
        assertIntEvaluates(3, script + "f({})");
        assertIntEvaluates(2, script + "f({z: 2})");
    }

    @Test
    public void defaultParametersWithArgumentsObject() throws Exception {
        final String script =
                "function f(a = 55) {\n"
                        + "  arguments[0] = 99; // updating arguments[0] does not also update a\n"
                        + "  return a;\n"
                        + "}\n"
                        + "\n"
                        + "function g(a = 55) {\n"
                        + "  a = 99; // updating a does not also update arguments[0]\n"
                        + "  return arguments[0];\n"
                        + "}\n"
                        + "\n"
                        + "// An untracked default parameter\n"
                        + "function h(a = 55) {\n"
                        + "  return arguments.length;\n"
                        + "}\n";
        assertIntEvaluates(10, script + "f(10)");
        assertIntEvaluates(55, script + "f()");
        assertIntEvaluates(10, script + "g(10)");
        assertEvaluates(Undefined.instance, script + "g()");
        assertIntEvaluates(0, script + "h()");
        assertIntEvaluates(1, script + "h(10)");
    }

    @Test
    public void functionDefaultArgsArray() throws Exception {
        final String script =
                "function append(value, array = []) {\n"
                        + "  array.push(value);\n"
                        + "  return array;\n"
                        + "}\n"
                        + "\n";
        assertIntEvaluates(1, script + "append(1)[0]");
        assertIntEvaluates(2, script + "append(2)[0]");
    }

    @Test
    public void functionDefaultArgsObject() throws Exception {
        final String script =
                "function append(key, value, obj = {}) {\n"
                        + "  obj[key]=value;\n"
                        + "  return obj;\n"
                        + "}\n"
                        + "\n";
        assertIntEvaluates(1, script + "append('a', 1)['a']");
        assertIntEvaluates(2, script + "append('a', 2)['a']");
    }

    private static void assertThrows(final String expected, final String source) {
        assertThrowsWithLanguageLevel(expected, source, Context.VERSION_ES6);
    }

    private static void assertThrowsWithLanguageLevel(
            String expected, final String source, int languageLevel) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    int oldVersion = cx.getLanguageVersion();
                    cx.setLanguageVersion(languageLevel);
                    final Scriptable scope = cx.initStandardObjects();
                    var error =
                            Assert.assertThrows(
                                    RhinoException.class,
                                    () -> cx.evaluateString(scope, source, "test.js", 0, null));
                    assertTrue(error.getMessage().startsWith(expected));
                    return null;
                });
    }

    private static void assertIntEvaluates(final Object expected, final String source) {
        assertIntEvaluatesWithLanguageLevel(expected, source, Context.VERSION_ES6);
    }

    private static void assertEvaluates(final Object expected, final String source) {
        assertIntEvaluatesWithLanguageLevel(expected, source, Context.VERSION_ES6);
    }

    @Test
    public void parserThrowsOnLowerLanguageLevelBasic() throws Exception {
        final String script = "function f(x = 1) {\n return x;\n }";
        assertThrowsWithLanguageLevel(
                "Default values are only supported in version >= 200",
                script + "f()",
                Context.VERSION_1_8);
    }

    @Test
    public void parserThrowsOnLowerLanguageLevelObjLit() throws Exception {
        final String script = "function f({ z = 3, x = 2 } = {}) {\n return z;\n}\n";
        assertThrowsWithLanguageLevel(
                "Default values are only supported in version >= 200",
                script + "f()",
                Context.VERSION_1_8);
    }

    @Test
    public void parserThrowsOnLowerLanguageLevelArrowBasic() throws Exception {
        final String script = "((x = 1) => {\n return x;\n })";
        assertThrowsWithLanguageLevel(
                "Default values are only supported in version >= 200",
                script + "()",
                Context.VERSION_1_8);
    }

    private static void assertIntEvaluatesWithLanguageLevel(
            final Object expected, final String source, int languageLevel) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    int oldVersion = cx.getLanguageVersion();
                    cx.setLanguageVersion(languageLevel);
                    final Scriptable scope = cx.initStandardObjects();
                    final Object rep = cx.evaluateString(scope, source, "test.js", 0, null);
                    if (rep instanceof Double)
                        assertEquals((int) expected, ((Double) rep).intValue());
                    else assertEquals(expected, rep);
                    return null;
                });
    }
}
