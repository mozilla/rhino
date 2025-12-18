/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.testutils.Utils;

/** Tests for Arguments support. */
public class ArgumentsTest {

    @Test
    public void argumentsSymbolIterator() {
        String code =
                "function foo() {"
                        + "  return arguments[Symbol.iterator] === Array.prototype.values;"
                        + "}"
                        + "foo()";

        Utils.assertWithAllModes_ES6(true, code);
    }

    @Test
    public void argumentsSymbolIterator2() {
        String code =
                "function foo() {"
                        + "  return arguments[Symbol.iterator] === [][Symbol.iterator];"
                        + "}"
                        + "foo()";

        Utils.assertWithAllModes_ES6(true, code);
    }

    @Test
    public void argumentsForOf() {
        String code =
                "function foo() {"
                        + "  var res = '';"
                        + "  for (arg of arguments) {"
                        + "    res += arg;"
                        + "  }"
                        + "  return res;"
                        + "}"
                        + "foo(1, 2, 3, 5)";

        Utils.assertWithAllModes_ES6("1235", code);
    }

    @Test
    public void argumentsNestedLambdas() {
        String code =
                "var foo = (function foo() {\n"
                        + "    return () => arguments[0];\n"
                        + "})(1);\n"
                        + "foo()";

        Utils.assertWithAllModes_ES6(1, code);
        Utils.assertWithAllModes_1_8(1, code);
    }

    @Test
    public void argumentsNestedNestedLambdas() {
        String code =
                "var foo = (function foo() {\n"
                        + "   return () => {"
                        + "       return () => arguments[0];\n"
                        + "   }\n"
                        + "})(1);\n"
                        + "foo()()";

        Utils.assertWithAllModes_ES6(1, code);
        Utils.assertWithAllModes_1_8(1, code);
    }

    @Test
    public void simple() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += test.arguments.length;\n"
                        + "  res += ' ' + test1.arguments;\n"
                        + "  test1('hi');\n"
                        + "}\n"
                        + "function test1(a) {\n"
                        + "  res += ' ' + test.arguments.length;\n"
                        + "  res += ' ' + test1.arguments.length;\n"
                        + "  res += ' ' + typeof arguments.callee;\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6("0 null 0 1 function", code);
        Utils.assertWithAllModes_1_8("0 null 0 1 function", code);
    }

    @Test
    public void prototype() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.protoype === {}.prototype;\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6("true", code);
        Utils.assertWithAllModes_1_8("true", code);
    }

    @Test
    public void argumentsLengthProperty() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  let desc = Object.getOwnPropertyDescriptor(arguments, 'length');\n"
                        + "  res += typeof desc.get + '/' + desc.get;\n"
                        + "  res += ' ' + typeof desc.set + '/' + desc.set;\n"
                        + "  res += ' W-' + desc.writable;\n"
                        + "  res += ' C-' + desc.configurable;\n"
                        + "  res += ' E-' + desc.enumerable;\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6(
                "undefined/undefined undefined/undefined W-true C-true E-false", code);
        Utils.assertWithAllModes_1_8(
                "undefined/undefined undefined/undefined W-true C-true E-false", code);
    }

    @Test
    public void argumentsLengthPropertyStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  let desc = Object.getOwnPropertyDescriptor(arguments, 'length');\n"
                        + "  res += typeof desc.get + '/' + desc.get;\n"
                        + "  res += ' ' + typeof desc.set + '/' + desc.set;\n"
                        + "  res += ' W-' + desc.writable;\n"
                        + "  res += ' C-' + desc.configurable;\n"
                        + "  res += ' E-' + desc.enumerable;\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6(
                "undefined/undefined undefined/undefined W-true C-true E-false", code);
        Utils.assertWithAllModes_1_8(
                "undefined/undefined undefined/undefined W-true C-true E-false", code);
    }

    @Test
    public void argumentsCalleeProperty() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  let desc = Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                        + "  res += typeof desc.get + '/' + desc.get;\n"
                        + "  res += ' ' + typeof desc.set + '/' + desc.set;\n"
                        + "  res += ' W-' + desc.writable;\n"
                        + "  res += ' C-' + desc.configurable;\n"
                        + "  res += ' E-' + desc.enumerable;\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6(
                "undefined/undefined undefined/undefined W-true C-true E-false", code);
        Utils.assertWithAllModes_1_8(
                "undefined/undefined undefined/undefined W-true C-true E-false", code);
    }

    @Test
    public void argumentsCalleePropertyStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  let desc = Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                        + "  res += typeof desc.get + '/' + desc.get;\n"
                        + "  res += ' ' + typeof desc.set + '/' + desc.set;\n"
                        + "  res += ' W-' + desc.writable;\n"
                        + "  res += ' C-' + desc.configurable;\n"
                        + "  res += ' E-' + desc.enumerable;\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6(
                "function/function () {\n\t[native code]\n}\n"
                        + " function/function () {\n\t[native code]\n}\n W-undefined C-false E-false",
                code);
        Utils.assertWithAllModes_1_8(
                "function/function () {\n\t[native code]\n}\n"
                        + " function/function () {\n\t[native code]\n}\n W-undefined C-false E-false",
                code);
    }

    @Test
    public void argumentsShouldBeNullOutsideFunction() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += ' ' + arguments;\n"
                        + "  res += ' ' + test.arguments;\n"
                        + "}\n"
                        + "res += test.arguments;\n"
                        + "test();\n"
                        + "res += ' ' + test.arguments;\n"
                        + "res";

        Utils.assertWithAllModes_ES6("null [object Arguments] [object Arguments] null", code);
        Utils.assertWithAllModes_1_8("null [object Arguments] [object Arguments] null", code);
    }

    @Test
    public void argumentsShouldBeNullOutsideFunctionStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  res += ' ' + arguments;\n"
                        + "  try {\n"
                        + "    res += ' ' + test.arguments;\n"
                        + "  } catch(e) { res += ' ex'; }"
                        + "}\n"
                        + "try {\n"
                        + "  res += test.arguments;\n"
                        + "} catch(e) { res += 'ex'; }"
                        + "test();\n"
                        + "try {\n"
                        + "  res += ' ' + test.arguments;\n"
                        + "} catch(e) { res += ' ex'; }\n"
                        + "res";

        // Utils.assertWithAllModes_ES6("ex [object Arguments] ex ex", code);
        Utils.assertWithAllModes_ES6("null [object Arguments] ex null", code);
        Utils.assertWithAllModes_1_8("null [object Arguments] null null", code);
    }

    @Test
    public void argumentsOutsideFunction() {
        String code =
                "let res = '';\n"
                        + "function test() { }\n"
                        + "  let desc = Object.getOwnPropertyDescriptor(test, 'arguments');\n"
                        + "  res += desc\n"
                        + "  let p = Object.getOwnPropertyNames(test);\n"
                        + "  p.sort();\n"
                        + "  res += ' ' + p;\n"
                        + "res";

        // Utils.assertWithAllModes_ES6("undefined length,name,prototype", code);
        Utils.assertWithAllModes_ES6("undefined arity,length,name,prototype", code);
        Utils.assertWithAllModes_1_8("[object Object] arguments,arity,length,name,prototype", code);
    }

    @Test
    public void argumentsOutsideFunctionStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() { }\n"
                        + "  let desc = Object.getOwnPropertyDescriptor(test, 'arguments');\n"
                        + "  res += desc;\n"
                        + "  let p = Object.getOwnPropertyNames(test);\n"
                        + "  p.sort();\n"
                        + "  res += ' ' + p;\n"
                        + "res";

        // Utils.assertWithAllModes_ES6("undefined length,name,prototype", code);
        Utils.assertWithAllModes_ES6("undefined arity,length,name,prototype", code);
        Utils.assertWithAllModes_1_8("undefined length,name,prototype", code);
    }

    @Test
    public void argumentsPropertyNames() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  let p = Object.getOwnPropertyNames(arguments);\n"
                        + "  p.sort();\n"
                        + "  res += p;\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6("callee,length", code);
        Utils.assertWithAllModes_1_8("callee,length", code);
    }

    @Test
    public void argumentsPropertyNamesStrict() {
        String code =
                "'use strict';\n"
                        + "var res = '';\n"
                        + "function test() {\n"
                        + "  var p = Object.getOwnPropertyNames(arguments);\n"
                        + "  p.sort();\n"
                        + "  res += p;\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6("callee,length", code);
        Utils.assertWithAllModes_1_8("callee,caller,length", code);
        Utils.assertWithAllModes(Context.VERSION_1_3, null, "callee,caller,length", code);
    }

    @Test
    public void passedCountDifferentFromDeclared() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + test.arguments.length;\n"
                        + "}\n"
                        + "test('hi', 'there');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2 2", code);
        Utils.assertWithAllModes_1_8("2 2", code);
    }

    @Test
    public void passedCountDifferentFromDeclaredStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.length;\n"
                        + "  try {\n"
                        + "    res += ' ' + test.arguments;\n"
                        + "  } catch(e) { res += ' ex'; }"
                        + "}\n"
                        + "test('hi', 'there');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2 ex", code);
        Utils.assertWithAllModes_1_8("2 null", code);
    }

    @Test
    public void descWhenAccessedThroughFunction() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  let desc = Object.getOwnPropertyDescriptor(test.arguments, '0');\n"
                        + "  res += typeof desc.get + '/' + desc.get;\n"
                        + "  res += ' ' + typeof desc.set + '/' + desc.set;\n"
                        + "  res += ' W-' + desc.writable;\n"
                        + "  res += ' C-' + desc.configurable;\n"
                        + "  res += ' E-' + desc.enumerable;\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6(
                "undefined/undefined undefined/undefined W-true C-true E-true", code);
        Utils.assertWithAllModes_1_8(
                "undefined/undefined undefined/undefined W-true C-true E-true", code);
    }

    public void readOnlyWhenAccessedThroughFunction() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  test.arguments[1] = 'hi';\n"
                        + "  test.arguments[3] = 'you';\n"
                        + "  res += test.arguments.length;\n"
                        + "  res += ' ' + test.arguments[0];\n"
                        + "  res += ' ' + test.arguments[1];\n"
                        + "  res += ' ' + test.arguments[2];\n"
                        + "  res += ' ' + test.arguments[3];\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2 hello world undefined undefined", code);
        Utils.assertWithAllModes_1_8("2 hello hi undefined you", code);
    }

    @Test
    public void writableWithinFunction() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  arguments[1] = 'hi';\n"
                        + "  arguments[3] = 'you';\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "  res += ' ' + arguments[3];\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2 hello hi undefined you", code);
        Utils.assertWithAllModes_1_8("2 hello hi undefined you", code);
    }

    @Test
    public void writableWithinFunctionStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  arguments[1] = 'hi';\n"
                        + "  arguments[3] = 'you';\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "  res += ' ' + arguments[3];\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2 hello hi undefined you", code);
        Utils.assertWithAllModes_1_8("2 hello hi undefined you", code);
    }

    @Test
    public void writableWithinFunctionAdjustsArgument() {
        String code =
                "let res = '';\n"
                        + "function test(a, b, c) {\n"
                        + "  arguments[1] = 'hi';\n"
                        + "  arguments[3] = 'you';\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "  res += ' ' + arguments[3];\n"
                        + "  res += ' ' + a;\n"
                        + "  res += ' ' + b;\n"
                        + "  res += ' ' + c;\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2 hello hi undefined you hello hi undefined", code);
        Utils.assertWithAllModes_1_8("2 hello hi undefined you hello hi undefined", code);
    }

    @Test
    public void writableWithinFunctionAdjustsArgumentStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test(a, b, c) {\n"
                        + "  arguments[1] = 'hi';\n"
                        + "  arguments[3] = 'you';\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "  res += ' ' + arguments[3];\n"
                        + "  res += ' ' + a;\n"
                        + "  res += ' ' + b;\n"
                        + "  res += ' ' + c;\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2 hello hi undefined you hello world undefined", code);
        Utils.assertWithAllModes_1_8("2 hello hi undefined you hello world undefined", code);
    }

    @Test
    public void writableWithinFunctionRestAdjustsArgument() {
        String code =
                "let res = '';\n"
                        + "function test(a, ...b) {\n"
                        + "  arguments[1] = 'hi';\n"
                        + "  arguments[3] = 'you';\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "  res += ' ' + arguments[3];\n"
                        + "  res += ' ' + a;\n"
                        + "  res += ' ' + b;\n"
                        + "}\n"
                        + "test('hello', 'whole', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("3 hello hi world you hello whole,world", code);
        Utils.assertWithAllModes_1_8("3 hello hi world you hello whole,world", code);
    }

    @Test
    public void writableWithinFunctionRestAdjustsArgumentStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test(a, ...b) {\n"
                        + "  arguments[1] = 'hi';\n"
                        + "  arguments[3] = 'you';\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "  res += ' ' + arguments[3];\n"
                        + "  res += ' ' + a;\n"
                        + "  res += ' ' + b;\n"
                        + "}\n"
                        + "test('hello', 'whole', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("3 hello hi world you hello whole,world", code);
        Utils.assertWithAllModes_1_8("3 hello hi world you hello whole,world", code);
    }

    @Test
    public void writableWithinFunctionDefaultAdjustsArgument() {
        String code =
                "let res = '';\n"
                        + "function test(a, b='default') {\n"
                        + "  arguments[1] = 'hi';\n"
                        + "  arguments[3] = 'you';\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "  res += ' ' + arguments[3];\n"
                        + "  res += ' ' + a;\n"
                        + "  res += ' ' + b;\n"
                        + "}\n"
                        + "test('hello');\n"
                        + "test('hello', 'world');\n"
                        + "res";

        String expected =
                "1 hello hi undefined you hello default" + "2 hello hi undefined you hello world";
        Utils.assertWithAllModes_ES6(expected, code);
    }

    @Test
    public void writableWithinFunctionDefaultAdjustsArgumentStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test(a, b='default') {\n"
                        + "  arguments[1] = 'hi';\n"
                        + "  arguments[3] = 'you';\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "  res += ' ' + arguments[3];\n"
                        + "  res += ' ' + a;\n"
                        + "  res += ' ' + b;\n"
                        + "}\n"
                        + "test('hello');\n"
                        + "test('hello', 'world');\n"
                        + "res";

        String expected =
                "1 hello hi undefined you hello default" + "2 hello hi undefined you hello world";
        Utils.assertWithAllModes_ES6(expected, code);
    }

    @Test
    public void writableWithinFunctionDestructAdjustsArgument() {
        String code =
                "let res = '';\n"
                        + "function test({a, b}) {\n"
                        + "  arguments[1] = 'hi';\n"
                        + "  arguments[3] = 'you';\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "  res += ' ' + arguments[3];\n"
                        + "  res += ' ' + a;\n"
                        + "  res += ' ' + b;\n"
                        + "}\n"
                        + "test({ a: 'hello', b: 'world'});\n"
                        + "res";

        Utils.assertWithAllModes_ES6("1 [object Object] hi undefined you hello world", code);
        Utils.assertWithAllModes_1_8("1 [object Object] hi undefined you hello world", code);
    }

    @Test
    public void writableWithinFunctionDestructAdjustsArgumentStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test({a, b}) {\n"
                        + "  arguments[1] = 'hi';\n"
                        + "  arguments[3] = 'you';\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "  res += ' ' + arguments[3];\n"
                        + "  res += ' ' + a;\n"
                        + "  res += ' ' + b;\n"
                        + "}\n"
                        + "test({ a: 'hello', b: 'world'});\n"
                        + "res";

        Utils.assertWithAllModes_ES6("1 [object Object] hi undefined you hello world", code);
        Utils.assertWithAllModes_1_8("1 [object Object] hi undefined you hello world", code);
    }

    @Test
    public void argumentsEqualsFnArguments() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += ' ' + (arguments == test.arguments);\n"
                        + "  res += ' ' + (arguments === test.arguments);\n"
                        + "}\n"
                        + "res += test.arguments;\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("null false false", code);
        Utils.assertWithAllModes_1_8("null true true", code);
    }

    @Test
    public void argumentsEqualsFnArgumentsStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  try {\n"
                        + "    res += ' ' + (arguments == test.arguments);\n"
                        + "  } catch(e) { res += ' ex'; }"
                        + "  try {\n"
                        + "    res += ' ' + (arguments === test.arguments);\n"
                        + "  } catch(e) { res += ' ex'; }"
                        + "}\n"
                        + "res += test.arguments;\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("null ex ex", code);
        Utils.assertWithAllModes_1_8("null false false", code);
    }

    @Test
    public void argumentsAsParameter() {
        String code =
                "let res = '';\n"
                        + "function test(arguments) {\n"
                        + "  res += arguments;\n"
                        + "}\n"
                        + "test('hi');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("hi", code);
    }

    @Test
    public void argumentsAsParameterStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test(arguments) {\n"
                        + "  res += arguments;\n"
                        + "}\n"
                        + "test('hi');\n"
                        + "res";

        Utils.assertEvaluatorExceptionES6(
                "\"arguments\" is not a valid identifier for this use in strict mode.", code);
        Utils.assertEvaluatorException_1_8(
                "\"arguments\" is not a valid identifier for this use in strict mode.", code);
    }

    @Test
    public void argumentsCallee() {
        String code =
                "let res = '';\n"
                        + "function calleeFoo() { foo(); }\n"
                        + "function foo() {\n"
                        + "  res += typeof arguments.callee;\n"
                        + "  res += ' ' + (foo === arguments.callee);\n"
                        + "  let desc = Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                        + "  res += ' ' + typeof desc.get + '/' + desc.get;\n"
                        + "  res += ' ' + typeof desc.set + '/' + desc.set;\n"
                        + "  res += ' ' + (desc.get === desc.set);\n"
                        + "}\n"
                        + "calleeFoo();\n"
                        + "res";

        Utils.assertWithAllModes_ES6(
                "function true undefined/undefined undefined/undefined true", code);
        Utils.assertWithAllModes_1_8(
                "function true undefined/undefined undefined/undefined true", code);
    }

    @Test
    public void argumentsCalleeStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function calleeFoo() { foo(); }\n"
                        + "function foo() {\n"
                        + "  try {\n"
                        + "    arguments.callee;\n"
                        + "  } catch(e) { res += 'ex'; }"
                        + "  let desc = Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                        + "  res += ' ' + typeof desc.get + '/' + desc.get;\n"
                        + "  res += ' ' + typeof desc.set + '/' + desc.set;\n"
                        + "  res += ' ' + (desc.get === desc.set);\n"
                        + "}\n"
                        + "calleeFoo();\n"
                        + "res";

        Utils.assertWithAllModes_ES6(
                "ex function/function () {\n\t[native code]\n}\n "
                        + "function/function () {\n\t[native code]\n}\n true",
                code);
        Utils.assertWithAllModes_1_8(
                "ex function/function () {\n\t[native code]\n}\n "
                        + "function/function () {\n\t[native code]\n}\n true",
                code);
    }

    @Test
    public void argumentsCalleeDifferentFunctions() {
        String code =
                "let res = '';\n"
                        + "function foo1() {\n"
                        + "  return Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                        + "}\n"
                        + "function foo2() {\n"
                        + "  return Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                        + "}\n"
                        + "let desc1 = foo1();\n"
                        + "let desc2 = foo2();\n"
                        + "res += (desc1.get === desc2.get);\n"
                        + "res += ' ' + (desc1.set === desc2.set);\n"
                        + "res";

        Utils.assertWithAllModes_ES6("true true", code);
        Utils.assertWithAllModes_1_8("true true", code);
    }

    @Test
    public void argumentsCalleeDifferentFunctionsStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function foo1() {\n"
                        + "  return Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                        + "}\n"
                        + "function foo2() {\n"
                        + "  return Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                        + "}\n"
                        + "let desc1 = foo1();\n"
                        + "let desc2 = foo2();\n"
                        + "res += (desc1.get === desc2.get);\n"
                        + "res += ' ' + (desc1.set === desc2.set);\n"
                        + "res";

        Utils.assertWithAllModes_ES6("true true", code);
        Utils.assertWithAllModes_1_8("true true", code);
    }

    @Test
    public void argumentsCaller() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.caller;\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6("undefined", code);
        Utils.assertWithAllModes_1_8("undefined", code);
    }

    @Test
    public void argumentsCallerStrict() {
        String code =
                "'use strict';\n"
                        + "var res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.caller;\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6("undefined", code);
        Utils.assertException(
                Context.VERSION_1_8,
                EcmaError.class,
                "TypeError: This operation is not allowed.",
                code);
        Utils.assertException(
                Context.VERSION_1_3,
                EcmaError.class,
                "TypeError: This operation is not allowed.",
                code);
    }

    @Test
    public void length() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.length;\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6("0", code);
        Utils.assertWithAllModes_1_8("0", code);
    }

    @Test
    public void lengthStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.length;\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6("0", code);
        Utils.assertWithAllModes_1_8("0", code);
    }

    @Test
    public void argumentsWithUndefined() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "}\n"
                        + "test(undefined);\n"
                        + "res";

        Utils.assertWithAllModes_ES6("1 undefined", code);
        Utils.assertWithAllModes_1_8("1 undefined", code);
    }

    @Test
    public void argumentsWithNull() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "}\n"
                        + "test(null);\n"
                        + "res";

        Utils.assertWithAllModes_ES6("1 null", code);
        Utils.assertWithAllModes_1_8("1 null", code);
    }

    @Test
    public void deleteArgumentsElement() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + delete arguments[0];\n"
                        + "  res += ' ' + arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2 true 2 undefined world", code);
        Utils.assertWithAllModes_1_8("2 true 2 undefined world", code);
    }

    @Test
    public void deleteArgumentsElementStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + delete arguments[0];\n"
                        + "  res += ' ' + arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2 true 2 undefined world", code);
        Utils.assertWithAllModes_1_8("2 true 2 undefined world", code);
    }

    @Test
    public void argumentsLengthWritable() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.length;\n"
                        + "  arguments.length = 5;\n"
                        + "  res += ' ' + arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2 5 hello world undefined", code);
        Utils.assertWithAllModes_1_8("2 5 hello world undefined", code);
    }

    @Test
    public void argumentsLengthWritableStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments.length;\n"
                        + "  arguments.length = 5;\n"
                        + "  res += ' ' + arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2 5 hello world undefined", code);
        Utils.assertWithAllModes_1_8("2 5 hello world undefined", code);
    }

    @Test
    public void argumentsInArrowFunction() {
        String code =
                "let res = '';\n"
                        + "const test = () => {\n"
                        + "  try {\n"
                        + "    res += arguments.length;\n"
                        + "  } catch(e) { res += 'ex'; }"
                        + "}\n"
                        + "test('hello');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("ex", code);
        Utils.assertWithAllModes_1_8("ex", code);
    }

    @Test
    public void argumentsInArrowFunctionStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "const test = () => {\n"
                        + "  try {\n"
                        + "    res += arguments.length;\n"
                        + "  } catch(e) { res += 'ex'; }"
                        + "}\n"
                        + "test('hello');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("ex", code);
        Utils.assertWithAllModes_1_8("ex", code);
    }

    @Test
    public void argumentsArrayMethods() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += Array.prototype.join.call(arguments, ',');\n"
                        + "  let result = Array.prototype.map.call(arguments, x => x.toUpperCase());"
                        + "  res += ' ' + result;\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("hello,world HELLO,WORLD", code);
        Utils.assertWithAllModes_1_8("hello,world HELLO,WORLD", code);
    }

    @Test
    public void argumentsArrayMethodsStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  res += Array.prototype.join.call(arguments, ',');\n"
                        + "  let result = Array.prototype.map.call(arguments, x => x.toUpperCase());"
                        + "  res += ' ' + result;\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("hello,world HELLO,WORLD", code);
        Utils.assertWithAllModes_1_8("hello,world HELLO,WORLD", code);
    }

    @Test
    public void argumentsNotArrayInstance() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments instanceof Array;\n"
                        + "  res += ' ' + (arguments instanceof Object)\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6("false true", code);
        Utils.assertWithAllModes_1_8("false true", code);
    }

    @Test
    public void argumentsNotArrayInstanceStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  res += arguments instanceof Array;\n"
                        + "  res += ' ' + (arguments instanceof Object)\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6("false true", code);
        Utils.assertWithAllModes_1_8("false true", code);
    }

    @Test
    public void argumentsIterator() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  let arr = [];\n"
                        + "  for (let arg of arguments) {\n"
                        + "    arr.push(arg);\n"
                        + "  }\n"
                        + "  res += arr;\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("hello,world", code);
    }

    @Test
    public void argumentsIteratorStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  let arr = [];\n"
                        + "  for (let arg of arguments) {\n"
                        + "    arr.push(arg);\n"
                        + "  }\n"
                        + "  res += arr;\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("hello,world", code);
    }

    @Test
    public void argumentsSpreadOperator() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  let arr = [...arguments];\n"
                        + "  res += arr;\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        // spreading arguments works correctly with Symbol.iterator support in ES6
        Utils.assertWithAllModes_ES6("hello,world", code);
        // not available in version 1.8
        Utils.assertEvaluatorException_1_8("syntax error", code);
    }

    @Test
    public void argumentsSpreadOperatorStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  let arr = [...arguments];\n"
                        + "  res += arr;\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        // spreading arguments works correctly with Symbol.iterator support in ES6
        Utils.assertWithAllModes_ES6("hello,world", code);
        // not available in version 1.8
        Utils.assertEvaluatorException_1_8("syntax error", code);
    }

    @Test
    public void argumentsWithRestParameters() {
        String code =
                "let res = '';\n"
                        + "function test(first, ...rest) {\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "}\n"
                        + "test('hello', 'world', '!');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("3 hello world !", code);
        Utils.assertWithAllModes_1_8("3 hello world !", code);
    }

    @Test
    public void argumentsWithRestParametersStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test(first, ...rest) {\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "  res += ' ' + arguments[2];\n"
                        + "}\n"
                        + "test('hello', 'world', '!');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("3 hello world !", code);
        Utils.assertWithAllModes_1_8("3 hello world !", code);
    }

    @Test
    public void argumentsWithDefaultParameters() {
        String code =
                "let res = '';\n"
                        + "function test(x, y = 'default') {\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "}\n"
                        + "test('hello');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("1 hello undefined", code);
    }

    @Test
    public void argumentsWithDefaultParametersStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test(x, y = 'default') {\n"
                        + "  res += arguments.length;\n"
                        + "  res += ' ' + arguments[0];\n"
                        + "  res += ' ' + arguments[1];\n"
                        + "}\n"
                        + "test('hello');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("1 hello undefined", code);
    }

    @Test
    public void argumentsInEval() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  eval('res += arguments.length');\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2", code);
        Utils.assertWithAllModes_1_8("2", code);
    }

    @Test
    public void argumentsInEvalStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  eval('res += arguments.length');\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2", code);
        Utils.assertWithAllModes_1_8("2", code);
    }

    @Test
    public void argumentsObjectKeys() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += Object.keys(arguments);\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("0,1", code);
        Utils.assertWithAllModes_1_8("0,1", code);
    }

    @Test
    public void argumentsObjectKeysStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  res += Object.keys(arguments);\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("0,1", code);
        Utils.assertWithAllModes_1_8("0,1", code);
    }

    @Test
    public void argumentsJSONStringify() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += JSON.stringify(arguments);\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("{\"0\":\"hello\",\"1\":\"world\"}", code);
        Utils.assertWithAllModes_1_8("{\"0\":\"hello\",\"1\":\"world\"}", code);
    }

    @Test
    public void argumentsJSONStringifyStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  res += JSON.stringify(arguments);\n"
                        + "}\n"
                        + "test('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("{\"0\":\"hello\",\"1\":\"world\"}", code);
        Utils.assertWithAllModes_1_8("{\"0\":\"hello\",\"1\":\"world\"}", code);
    }

    @Test
    public void argumentsInConstructor() {
        String code =
                "let res = '';\n"
                        + "function MyClass() {\n"
                        + "  res += arguments.length;\n"
                        + "}\n"
                        + "new MyClass('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2", code);
        Utils.assertWithAllModes_1_8("2", code);
    }

    @Test
    public void argumentsInConstructorStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function MyClass() {\n"
                        + "  res += arguments.length;\n"
                        + "}\n"
                        + "new MyClass('hello', 'world');\n"
                        + "res";

        Utils.assertWithAllModes_ES6("2", code);
        Utils.assertWithAllModes_1_8("2", code);
    }

    @Test
    public void argumentsToString() {
        String code =
                "let res = '';\n"
                        + "function test() {\n"
                        + "  res += Object.prototype.toString.call(arguments);\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6("[object Arguments]", code);
        Utils.assertWithAllModes_1_8("[object Arguments]", code);
    }

    @Test
    public void argumentsToStringStrict() {
        String code =
                "'use strict';\n"
                        + "let res = '';\n"
                        + "function test() {\n"
                        + "  res += Object.prototype.toString.call(arguments);\n"
                        + "}\n"
                        + "test();\n"
                        + "res";

        Utils.assertWithAllModes_ES6("[object Arguments]", code);
        Utils.assertWithAllModes_1_8("[object Arguments]", code);
    }

    @Test
    public void sealFunction() {
        String code = "'use strict';\n" + "'' + typeof Object.seal(new Function());";

        Utils.assertWithAllModes_ES6("function", code);
        Utils.assertWithAllModes_1_8("function", code);
    }
}
