package org.mozilla.javascript.tests;

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
        Utils.assertWithAllModes_ES6(32, script + "\nfoo(32)");
        Utils.assertWithAllModes_ES6(2, script + "\nfoo()");
        Utils.assertWithAllModes_ES6(2, script + "\nfoo(undefined)");
    }

    @Test
    public void functionDefaultArgsBasicCall() throws Exception {
        final String script = "function b() { return 2; }; function foo(a = b()) { return a; }";
        Utils.assertWithAllModes_ES6(32, script + "\nfoo(32)");
        Utils.assertWithAllModes_ES6(2, script + "\nfoo()");
        Utils.assertWithAllModes_ES6(2, script + "\nfoo(undefined)");
    }

    @Test
    public void functionDefaultArgsBasicArrow() throws Exception {
        final String script = "((a = 2, b) => { return a; })";
        Utils.assertWithAllModes_ES6(32, script + "(32, 12)");
        Utils.assertWithAllModes_ES6(12, script + "(12)");
        Utils.assertWithAllModes_ES6(2, script + "()");
    }

    @Test
    public void functionDefaultArgsArrayArrow() throws Exception {
        final String script = "(([a = 2, b = 1] = [1, 2]) => { return a + b; })";
        Utils.assertWithAllModes_ES6(3, script + "()");
        Utils.assertWithAllModes_ES6(5, script + "([4,])");
        Utils.assertWithAllModes_ES6(6, script + "([,4])");
    }

    @Test
    public void functionDefaultArgsMulti() throws Exception {
        final String script = "function foo(a = 2, b = 23) { return a + b; }";
        Utils.assertWithAllModes_ES6(55, script + "\nfoo(32)");
        Utils.assertWithAllModes_ES6(25, script + "\nfoo()");
        Utils.assertWithAllModes_ES6(34, script + "\nfoo(32, 2)");
        Utils.assertWithAllModes_ES6(25, script + "\nfoo(undefined, undefined)");
    }

    @Test
    public void functionDefaultArgsUsage() throws Exception {
        final String script = "function foo(a = 2, b = a * 2) { return a + b; }";
        Utils.assertWithAllModes_ES6(96, script + "\nfoo(32)");
        Utils.assertWithAllModes_ES6(6, script + "\nfoo()");
        Utils.assertWithAllModes_ES6(34, script + "\nfoo(32, 2)");
    }

    @Test
    public void ObjIdInitSimpleStrictExpr() throws Exception {
        final String script =
                "(function () { \n " + "'use strict'; \n " + "(0, { eval = 0 } = {}) })()";
        Utils.assertEvaluatorExceptionES6("syntax error", script);
    }

    @Test
    public void ObjIdInitSimpleStrictForOf() throws Exception {
        final String script = "for ({ eval = 0 } of [{}]) ;";
        Utils.assertEvaluatorExceptionES6("syntax error", script);
    }

    @Test
    public void CoverInitName() throws Exception {
        final String script = "({ a = 1 });";
        Utils.assertEvaluatorExceptionES6("syntax error", script);
    }

    @Test
    public void functionDefaultArgsObjectArrow() throws Exception {
        final String script = "(({x = 1} = {x: 2}) => {\n  return x;\n})";

        Utils.assertWithAllModes_ES6(1, script + "({})");
        Utils.assertWithAllModes_ES6(2, script + "()");
        Utils.assertWithAllModes_ES6(3, script + "({x: 3})");
    }

    @Test
    @Ignore("destructuring-not-supported-in-for-let-expressions")
    public void letExprDestructuring() throws Exception {
        // JavaScript
        final String script =
                "var a = 12; (function() { "
                        + "            for (let {x = a} = {}; ; ) { "
                        + "                return x; "
                        + "            }"
                        + "        })()";
        Utils.assertWithAllModes_ES6(12, script);
    }

    @Test
    public void normObjectLiteralDestructuringFunCall() throws Exception {
        // JavaScript
        final String script = "function a() { return 2;};  let {x = a()} = {x: 12}; x";

        final String script2 = "function a() { return 2;};  let {x = 12} = {x: a()}; x";
        Utils.assertWithAllModes_ES6(12, script);
        Utils.assertWithAllModes_ES6(2, script2);
    }

    @Test
    public void normDefaultParametersObjectDestructuringFunCall() throws Exception {
        // JavaScript
        final String script =
                "function a() { return 12;};  function b({x = a()} = {x: 1}) { return x }; b()";
        final String script2 =
                "function a() { return 12;};  function b({x = a()} = {}) { return x }; b()";
        final String script3 =
                "var a = { p1: { p2: 121}}; function b({x = a.p1.p2} = {}) { return x }; b()";
        final String script4 =
                "function a() { return 12;};  function b({x = 1} = {x: a()}) { return x }; b()\n";

        Utils.assertWithAllModes_ES6(1, script);
        Utils.assertWithAllModes_ES6(12, script2);
        Utils.assertWithAllModes_ES6(121, script3);
        Utils.assertWithAllModes_ES6(12, script4);
    }

    @Test
    public void normDefaultParametersArrayDestructuringFunCall() throws Exception {
        // JavaScript
        final String script =
                "function a() { return 12;};  function b([x = a()] = [1]) { return x }; b()";
        final String script2 =
                "function a() { return 12;};  function b([x = a()] = []) { return x }; b()";
        final String script3 =
                "var a = { p1: { p2: 121}}; function b([x = a.p1.p2] = []) { return x }; b()";
        final String script4 =
                "function a() { return 12;};  function b([x = 1] = [a()]) { return x }; b()\n";

        Utils.assertWithAllModes_ES6(1, script);
        Utils.assertWithAllModes_ES6(12, script2);
        Utils.assertWithAllModes_ES6(121, script3);
        Utils.assertWithAllModes_ES6(12, script4);
    }

    @Test
    public void normDefaultParametersFunCall() throws Exception {
        // JavaScript
        final String script = "function a() { return 12;};  function b(x = a()) { return x }; b()";
        Utils.assertWithAllModes_ES6(12, script);
    }

    @Test
    @Ignore("destructuring-not-supported-in-for-let-expressions")
    public void letExprDestructuringFunCall() throws Exception {
        // JavaScript
        final String script =
                "function a() { return 4; }; (function() { "
                        + "            for (let {x = a()} = {}; ; ) { "
                        + "                return x; "
                        + "            }"
                        + "        })()";
        Utils.assertWithAllModes_ES6(4, script);
    }

    @Test
    public void letExprUnresolvableRefDestructuring() throws Exception {
        // JavaScript
        final String script =
                "  (function() { for (let [ x = unresolvableReference ] = []; ; ) {\n"
                        + "    return 3;\n"
                        + "  }})()";
        Utils.assertEcmaErrorES6(
                "ReferenceError: \"unresolvableReference\" is not defined.", script);
    }

    @Test
    public void destructuringNestedArray() throws Exception {
        // JavaScript
        final String script = "let [[y], x] = [[4], 3]; x + y";
        Utils.assertWithAllModes_ES6(7, script);
    }

    @Test
    public void letExprUnresolvableRefObjDestructuring() throws Exception {
        // JavaScript
        final String script = "var f = function({ x = unresolvableReference } = {}) {}; f()";
        Utils.assertEcmaErrorES6(
                "ReferenceError: \"unresolvableReference\" is not defined.", script);
    }

    @Test
    public void getIntPropArg() throws Exception {
        final String script =
                "function foo([gen = function () { return 2; }, xGen = function* x() { yield 2; }] = []) {\n"
                        + " return gen() + xGen().next().value; }";
        Utils.assertWithAllModes_ES6(4, script + "; foo()");
    }

    @Test
    public void getIntErrPropArg() throws Exception {
        final String script =
                "var e = 0; var b = 'hello'; var { f: y = ++e } = { f: { get: function() {}}}; "
                        + "Object.keys(y).includes('get') && Object.keys(y).length == 1";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void getIntPropArgParenExpr() throws Exception {
        final String script =
                "const [cover = (function () {}), xCover = (0, function() {})] = [];\n"
                        + "cover.name == 'cover' && xCover.name == 'xCover' ? 4 : -1";
        Utils.assertWithAllModes_ES6(-1, script);
    }

    @Test
    public void getIntProp() throws Exception {
        final String script =
                "const { gen = function () { return 2;}, xGen = function* () { yield 2;} } = {};\n"
                        + "gen() + xGen().next().value";
        Utils.assertWithAllModes_ES6(4, script);
    }

    @Test
    public void getIntPropExhausted() throws Exception {
        final String script = "const [x = 23] = []; x";
        Utils.assertWithAllModes_ES6(23, script);
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
        Utils.assertWithAllModes_ES6(24, script + "\nf(24)");
        Utils.assertEcmaErrorES6(
                "ReferenceError: \"go\" is not defined.", "function f() { go() }; var f1 = f()");
        Utils.assertEcmaErrorES6("ReferenceError: \"go\" is not defined.", script + "\nf()");
    }

    @Test
    @Ignore("temporal-dead-zone")
    public void functionDefaultArgsMultiReferEarlier() throws Exception {
        final String script = "var f = function(a = b * 2, b = 3) { return a * b; }\n";
        Utils.assertEcmaErrorES6("ReferenceError: \"b\" is not defined.", script + "\nf()");
    }

    @Test
    public void functionConstructor() throws Exception {
        final String script = "const f = new Function('a=2', 'b=a', 'return a + b');";
        Utils.assertWithAllModes_ES6(4, script + "f()");
        Utils.assertWithAllModes_ES6(6, script + "f(3)");
        Utils.assertWithAllModes_ES6(16, script + "f(3, 13)");
    }

    @Test
    public void destructuringAssigmentDefaultArray() throws Exception {
        final String script =
                "function f([x = 1, y = 2] = [], [z = 1] = [4]) {\n"
                        + "  return x + y + z;\n"
                        + "}";

        Utils.assertWithAllModes_ES6(7, script + "f()");
        Utils.assertWithAllModes_ES6(7, script + "f([])");
        Utils.assertWithAllModes_ES6(4, script + "f([], [])");
        Utils.assertWithAllModes_ES6(8, script + "f([2])");
        Utils.assertWithAllModes_ES6(5, script + "f([2], [])");
        Utils.assertWithAllModes_ES6(8, script + "f([], [5])");
        Utils.assertWithAllModes_ES6(6, script + "f([2, 3], [])");
        Utils.assertWithAllModes_ES6(9, script + "f([2, 3], [4])");
        Utils.assertWithAllModes_ES6(7, script + "f([2], [3])");
        Utils.assertWithAllModes_ES6(9, script + "f([2, 3])");
    }

    @Test
    @Ignore("needs-checking-for-iterator")
    public void destructuringAssigmentInFunctionsWithObjectDefaults() throws Exception {
        final String script = "function f([x = 1, y = 2] = {x: 3, y: 4}) {\n return x + y;\n }";

        Utils.assertEcmaErrorES6("TypeError", script + "f()"); // TODO: returns 3
        Utils.assertEcmaErrorES6(
                "TypeError", script + "f(2)"); // TODO: returns 3, should be throwing TypeError
    }

    @Test
    public void destructuringTest() throws Exception {
        final String script = "function f([x]) { return x; }; f([1]);";
        Utils.assertWithAllModes_ES6(1, script);
    }

    @Test
    public void destructuringAssignmentDefaultArray() throws Exception {
        final String script = "var [a = 10] = []; a";
        final String script2 = "var [a = 10] = [1]; a";
        Utils.assertWithAllModes_ES6(10, script);
        Utils.assertWithAllModes_ES6(1, script2);
    }

    @Test
    public void destructuringAssignmentDefaultObject() throws Exception {
        final String script1 = "var {a: b = 10} = {hello: 3}; b+a";
        final String script3 = "var a = 20; var {a: b = 10} = {hello: 3}; b+a";
        final String script4 = "var a = 30; var {a: b = 10} = {}; b+a";
        Utils.assertEcmaErrorES6("ReferenceError", script1);
        Utils.assertWithAllModes_ES6(30, script3);
        Utils.assertWithAllModes_ES6(40, script4);
    }

    @Test
    public void destructuringHookTest() throws Exception {
        final String script = "function f([x]) { return x == undefined ? 2 : x; }; f([1]);";
        Utils.assertWithAllModes_ES6(1, script);
    }

    @Test
    public void destructuringAssigmentRealRealBasicArray() throws Exception {
        final String script = "function f([x] = [1]) {\n return x;\n }";
        Utils.assertWithAllModes_ES6(1, script + "f()");
        Utils.assertWithAllModes_ES6(2, script + "f([2])");
        Utils.assertWithAllModes_ES6(42, script + "f([]) == undefined ? 42 : 0");
    }

    @Test
    public void destructuringAssigmentRealBasicArray() throws Exception {
        final String script = "function f([x = 1]) {\n return x;\n }";
        Utils.assertWithAllModes_ES6(1, script + "f([])");
        Utils.assertWithAllModes_ES6(3, script + "f([3])");
        Utils.assertEcmaErrorES6("TypeError", script + "f()");
    }

    @Test
    public void destructuringAssigmentBasicArray() throws Exception {
        final String script = "function f([x = 1] = [2]) {\n return x;\n }";
        Utils.assertWithAllModes_ES6(1, script + "f([])");
        Utils.assertWithAllModes_ES6(2, script + "f()");
        Utils.assertWithAllModes_ES6(3, script + "f([3])");
    }

    @Test
    public void destructuringAssigmentBasicObject() throws Exception {
        final String script = "function f({x = 1} = {x: 2}) {\n return x;\n }";

        Utils.assertWithAllModes_ES6(1, script + "f({})");
        Utils.assertWithAllModes_ES6(2, script + "f()");
        Utils.assertWithAllModes_ES6(3, script + "f({x: 3})");
    }

    @Test
    public void destructuringAssigmentRealBasicObject() throws Exception {
        final String script = "function f({x = 1}) {\n return x;\n }";

        Utils.assertWithAllModes_ES6(1, script + "f({})");
        Utils.assertEcmaErrorES6("TypeError", script + "f()");
        Utils.assertWithAllModes_ES6(3, script + "f({x: 3})");
    }

    @Test
    public void destructuringAssigmentDefaultObject() throws Exception {
        final String script = "function f({ z = 3, x = 2 } = {}) {\n return z;\n}\n";
        Utils.assertWithAllModes_ES6(3, script + "f()");
        Utils.assertWithAllModes_ES6(3, script + "f({})");
        Utils.assertWithAllModes_ES6(2, script + "f({z: 2})");
    }

    @Test
    public void destructuringAssigmentDefaultObjectWithDefaults() throws Exception {
        final String script = "function f({ z = 3, x = 2 } = {z: 4, x: 5}) {\n return z;\n}\n";
        Utils.assertWithAllModes_ES6(4, script + "f()");
        Utils.assertWithAllModes_ES6(3, script + "f({})");
        Utils.assertWithAllModes_ES6(2, script + "f({z: 2})");
    }

    @Test
    public void deeplyNestedObjectLiteral() throws Exception {
        final String script =
                "let { d: { b }, d, a} = { \n"
                        + "                          a: \"world\",\n"
                        + "                          d: {\n"
                        + "                            b: \"hello\"\n"
                        + "                          }\n"
                        + "                        }\n"
                        + "                        \n";
        Utils.assertWithAllModes_ES6("hello", script + "b");
        Utils.assertWithAllModes_ES6("world", script + "a");
        Utils.assertWithAllModes_ES6("hello", script + "d.b");
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
        Utils.assertWithAllModes_ES6(10, script + "f(10)");
        Utils.assertWithAllModes_ES6(55, script + "f()");
        Utils.assertWithAllModes_ES6(10, script + "g(10)");
        Utils.assertWithAllModes_ES6(Undefined.instance, script + "g()");
        Utils.assertWithAllModes_ES6(0, script + "h()");
        Utils.assertWithAllModes_ES6(1, script + "h(10)");
    }

    @Test
    public void functionDefaultArgsArray() throws Exception {
        final String script =
                "function append(value, array = []) {\n"
                        + "  array.push(value);\n"
                        + "  return array;\n"
                        + "}\n"
                        + "\n";
        Utils.assertWithAllModes_ES6(1, script + "append(1)[0]");
        Utils.assertWithAllModes_ES6(2, script + "append(2)[0]");
    }

    @Test
    public void functionDefaultArgsObject() throws Exception {
        final String script =
                "function append(key, value, obj = {}) {\n"
                        + "  obj[key]=value;\n"
                        + "  return obj;\n"
                        + "}\n"
                        + "\n";

        Utils.assertWithAllModes_ES6(1, script + "append('a', 1)['a']");
        Utils.assertWithAllModes_ES6(2, script + "append('a', 2)['a']");
    }

    @Test
    public void parserThrowsOnLowerLanguageLevelBasic() throws Exception {
        final String script = "function f(x = 1) {\n return x;\n }";
        Utils.assertEvaluatorException_1_8(
                "Default values are only supported in version >= 200", script + "f()");
    }

    @Test
    public void parserThrowsOnLowerLanguageLevelObjLit() throws Exception {
        final String script = "function f({ z = 3, x = 2 } = {}) {\n return z;\n}\n";
        Utils.assertEvaluatorException_1_8(
                "Default values are only supported in version >= 200", script + "f()");
    }

    @Test
    public void parserThrowsOnLowerLanguageLevelArrowBasic() throws Exception {
        final String script = "((x = 1) => {\n return x;\n })";
        Utils.assertEvaluatorException_1_8(
                "Default values are only supported in version >= 200", script + "()");
    }
}
