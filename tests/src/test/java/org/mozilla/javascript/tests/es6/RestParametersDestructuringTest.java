/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for rest parameters, object rest, array rest, and other destructuring patterns. */
class RestParametersDestructuringTest {

    @Nested
    class RestParameters {
        @Test
        void restAfterDefaultParam() {
            String code =
                    "function foo(a = 1, ...rest) {\n"
                            + "  return a + '-' + rest.length + '-' + rest.join(',');\n"
                            + "}\n"
                            + "foo() + '|' + foo(2) + '|' + foo(2, 3, 4);\n";

            Utils.assertWithAllModes_ES6("1-0-|2-0-|2-2-3,4", code);
        }

        @Test
        void restAfterMultipleDefaultParams() {
            String code =
                    "function foo(a = 1, b = 2, ...rest) {\n"
                            + "  return a + '-' + b + '-' + rest.length;\n"
                            + "}\n"
                            + "foo() + '|' + foo(10) + '|' + foo(10, 20) + '|' + foo(10, 20, 30, 40);\n";

            Utils.assertWithAllModes_ES6("1-2-0|10-2-0|10-20-0|10-20-2", code);
        }

        @Test
        void defaultParamRestAndArguments() {
            String code =
                    "function foo(a = 1, ...rest) {\n"
                            + "  return arguments.length + '-' + rest.length;\n"
                            + "}\n"
                            + "foo() + '|' + foo(2) + '|' + foo(2, 3, 4);\n";

            Utils.assertWithAllModes_ES6("0-0|1-0|3-2", code);
        }

        @Test
        void functionLengthWithDefaultAndRest() {
            String code =
                    "function foo1(a = 1, ...rest) {}\n"
                            + "function foo2(a, b = 2, ...rest) {}\n"
                            + "function foo3(a, b, c = 3, ...rest) {}\n"
                            + "foo1.length + '-' + foo2.length + '-' + foo3.length;\n";

            // Function.length should count only required params before first default
            Utils.assertWithAllModes_ES6("0-1-2", code);
        }

        @Test
        void arrayDestructuringRest() {
            String code =
                    "function foo([a, b], ...rest) {\n"
                            + "  return a + '-' + b + '-' + rest.length;\n"
                            + "}\n"
                            + "foo([1, 2]) + '|' + foo([1, 2], 3, 4);\n";

            Utils.assertWithAllModes_ES6("1-2-0|1-2-2", code);
        }

        @Test
        void objectDestructuringRest() {
            String code =
                    "function foo({x, y}, ...rest) {\n"
                            + "  return x + '-' + y + '-' + rest.length;\n"
                            + "}\n"
                            + "foo({x: 1, y: 2}) + '|' + foo({x: 1, y: 2}, 3, 4);\n";

            Utils.assertWithAllModes_ES6("1-2-0|1-2-2", code);
        }

        @Test
        void nestedDestructuringWithRest() {
            String code =
                    "function foo({a: [b, c]}, ...rest) {\n"
                            + "  return b + '-' + c + '-' + rest.length;\n"
                            + "}\n"
                            + "foo({a: [1, 2]}) + '|' + foo({a: [1, 2]}, 3);\n";

            Utils.assertWithAllModes_ES6("1-2-0|1-2-1", code);
        }

        @Test
        void destructuringWithDefaultsAndRest() {
            String code =
                    "function foo({x = 1, y = 2} = {}, ...rest) {\n"
                            + "  return x + '-' + y + '-' + rest.length;\n"
                            + "}\n"
                            + "foo() + '|' + foo({x: 10}) + '|' + foo({x: 10, y: 20}, 3, 4);\n";

            Utils.assertWithAllModes_ES6("1-2-0|10-2-0|10-20-2", code);
        }

        @Test
        void arrayDestructuringRestInParam() {
            String code =
                    "function foo([first, ...rest]) {\n"
                            + "  return first + '-' + rest.length + '-' + rest.join(',');\n"
                            + "}\n"
                            + "foo([1, 2, 3, 4]);\n";

            Utils.assertWithAllModes_ES6("1-3-2,3,4", code);
        }

        @Test
        void objectDestructuringRestInParam() {
            String code =
                    "function foo({a, ...rest}) {\n"
                            + "  return a + '-' + Object.keys(rest).sort().join(',') + '-' + rest.b + '-' + rest.c;\n"
                            + "}\n"
                            + "foo({a: 1, b: 2, c: 3});\n";

            Utils.assertWithAllModes_ES6("1-b,c-2-3", code);
        }

        @Test
        void mixedDestructuringDefaultsAndRest() {
            String code =
                    "function foo(a, {x = 5} = {}, b = 10, ...rest) {\n"
                            + "  return a + '-' + x + '-' + b + '-' + rest.length;\n"
                            + "}\n"
                            + "foo(1) + '|' + foo(1, {x: 2}) + '|' + foo(1, {x: 2}, 3, 4, 5);\n";

            Utils.assertWithAllModes_ES6("1-5-10-0|1-2-10-0|1-2-3-2", code);
        }

        @Test
        void arrowFunctionWithRest() {
            String code =
                    "const foo = (...rest) => rest.length;\n"
                            + "foo() + '-' + foo(1) + '-' + foo(1, 2, 3);\n";

            Utils.assertWithAllModes_ES6("0-1-3", code);
        }

        @Test
        void arrowFunctionWithParamAndRest() {
            String code =
                    "const foo = (a, ...rest) => a + '-' + rest.length;\n"
                            + "foo(1) + '|' + foo(1, 2, 3);\n";

            Utils.assertWithAllModes_ES6("1-0|1-2", code);
        }

        @Test
        void arrowFunctionDefaultAndRest() {
            String code =
                    "const foo = (a = 1, ...rest) => a + '-' + rest.length;\n"
                            + "foo() + '|' + foo(2) + '|' + foo(2, 3, 4);\n";

            Utils.assertWithAllModes_ES6("1-0|2-0|2-2", code);
        }

        @Test
        void arrowFunctionDestructuringAndRest() {
            String code =
                    "const foo = ({x}, ...rest) => x + '-' + rest.length;\n"
                            + "foo({x: 1}) + '|' + foo({x: 1}, 2, 3);\n";

            Utils.assertWithAllModes_ES6("1-0|1-2", code);
        }
    }

    @Nested
    class ArrayRestDestructuring {
        @Test
        void simpleArrayRestInVariableDeclaration() {
            String code =
                    "let [a, ...rest] = [1, 2, 3, 4];\n"
                            + "a + '-' + rest.length + '-' + rest.join(',');";

            Utils.assertWithAllModes_ES6("1-3-2,3,4", code);
        }

        @Test
        void arrayRestWithConstDeclaration() {
            String code =
                    "const [first, ...remaining] = ['a', 'b', 'c', 'd'];\n"
                            + "first + '-' + remaining.length + '-' + remaining.join('');";

            Utils.assertWithAllModes_ES6("a-3-bcd", code);
        }

        @Test
        void emptyArrayRest() {
            String code = "let [...empty] = [];\n" + "empty.length + '-' + Array.isArray(empty);";

            Utils.assertWithAllModes_ES6("0-true", code);
        }

        @Test
        void arrayRestWithSingleElement() {
            String code = "let [...rest] = [1];\n" + "rest.length + '-' + rest[0];";

            Utils.assertWithAllModes_ES6("1-1", code);
        }

        @Test
        void arrayRestNoElementsBeforeRest() {
            String code = "let [...rest] = [1, 2, 3];\n" + "rest.length + '-' + rest.join(',');";

            Utils.assertWithAllModes_ES6("3-1,2,3", code);
        }

        @Test
        void arrayDestructuringWithDefaultBeforeRest() {
            String code =
                    "function foo([a = 1, ...rest]) {\n"
                            + "  return a + '-' + rest.length + '-' + rest.join(',');\n"
                            + "}\n"
                            + "foo([undefined, 2, 3]) + '|' + foo([5, 6, 7]);";

            Utils.assertWithAllModes_ES6("1-2-2,3|5-2-6,7", code);
        }

        @Test
        void arrayRestWithDefaultInVariableDeclaration() {
            String code =
                    "let [x = 10, ...rest] = [undefined, 2, 3];\n" + "x + '-' + rest.join(',');";

            Utils.assertWithAllModes_ES6("10-2,3", code);
        }

        @Test
        void multipleElementsBeforeArrayRest() {
            String code =
                    "const [a, b, c, ...rest] = [1, 2, 3, 4, 5, 6];\n"
                            + "a + '-' + b + '-' + c + '-' + rest.join(',');";

            Utils.assertWithAllModes_ES6("1-2-3-4,5,6", code);
        }

        @Test
        void restWithArrayPattern() {
            String code =
                    "var callCount = 0;\n"
                            + "var f = ([...[x, y, z]]) => {\n"
                            + "  callCount++;\n"
                            + "  return x + '|' + y + '|' + z;\n"
                            + "};\n"
                            + "f([3, 4, 5]);";

            Utils.assertWithAllModes_ES6("3|4|5", code);
        }

        @Test
        void restWithObjectPattern() {
            String code =
                    "var callCount = 0;\n"
                            + "var f = ([...{ length }]) => {\n"
                            + "  callCount++;\n"
                            + "  return length;\n"
                            + "};\n"
                            + "f([1, 2, 3]);";

            Utils.assertWithAllModes_ES6(3.0, code);
        }

        @Test
        void restWithArrayPatternInFunctionParameter() {
            String code =
                    "function f([...[x, y]]) {\n" + "  return x * 10 + y;\n" + "}\n" + "f([5, 7]);";

            Utils.assertWithAllModes_ES6(57.0, code);
        }

        @Test
        void restWithObjectPatternInFunctionParameter() {
            String code =
                    "function f([...{length}]) {\n"
                            + "  return length * 2;\n"
                            + "}\n"
                            + "f([10, 20, 30, 40]);";

            Utils.assertWithAllModes_ES6(8.0, code);
        }

        @Test
        void restWithNestedArrayPattern() {
            String code =
                    "var f = ([...[a, [b, c]]]) => {\n"
                            + "  return a + '|' + b + '|' + c;\n"
                            + "};\n"
                            + "f([1, [2, 3]]);";

            Utils.assertWithAllModes_ES6("1|2|3", code);
        }

        @Test
        void restWithObjectPatternMultipleProps() {
            String code =
                    "var f = ([...{length, 0: first, 2: third}]) => {\n"
                            + "  return first + '|' + third + '|' + length;\n"
                            + "};\n"
                            + "f(['a', 'b', 'c']);";

            Utils.assertWithAllModes_ES6("a|c|3", code);
        }

        @Test
        void restWithArrayPatternInDestructuringAssignment() {
            String code =
                    "var x, y, z;\n"
                            + "([...[x, y, z]] = [10, 20, 30]);\n"
                            + "x + '|' + y + '|' + z;";

            Utils.assertWithAllModes_ES6("10|20|30", code);
        }

        @Test
        void restWithObjectPatternInDestructuringAssignment() {
            String code = "var len;\n" + "([...{length: len}] = [1, 2, 3, 4, 5]);\n" + "len;";

            Utils.assertWithAllModes_ES6(5.0, code);
        }
    }

    @Nested
    class ObjectRestDestructuring {
        @Test
        void objectRestExcludesExtractedProperty() {
            String code =
                    "const obj = {a: 1, b: 2, c: 3};\n"
                            + "const {a, ...rest} = obj;\n"
                            + "JSON.stringify(rest);";

            Utils.assertWithAllModes_ES6("{\"b\":2,\"c\":3}", code);
        }

        @Test
        void objectRestExcludesMultipleProperties() {
            String code =
                    "const obj = {x: 10, y: 20, z: 30, w: 40};\n"
                            + "const {x, y, ...rest} = obj;\n"
                            + "JSON.stringify(rest);";

            Utils.assertWithAllModes_ES6("{\"z\":30,\"w\":40}", code);
        }

        @Test
        void objectRestWithNoExtraction() {
            String code =
                    "const obj = {a: 1, b: 2, c: 3};\n"
                            + "const {...rest} = obj;\n"
                            + "JSON.stringify(rest);";

            Utils.assertWithAllModes_ES6("{\"a\":1,\"b\":2,\"c\":3}", code);
        }

        @Test
        void objectRestExtractsValues() {
            String code =
                    "const obj = {name: 'Alice', age: 30, city: 'NYC'};\n"
                            + "const {name, age, ...address} = obj;\n"
                            + "name + '|' + age + '|' + JSON.stringify(address);";

            Utils.assertWithAllModes_ES6("Alice|30|{\"city\":\"NYC\"}", code);
        }

        @Test
        void objectRestInFunctionParameter() {
            String code =
                    "function getAddress({name, age, ...address}) {\n"
                            + "  return JSON.stringify(address);\n"
                            + "}\n"
                            + "getAddress({name: 'Bob', age: 25, city: 'LA', state: 'CA'});";

            Utils.assertWithAllModes_ES6("{\"city\":\"LA\",\"state\":\"CA\"}", code);
        }

        @Test
        void objectRestWithComputedProperty() {
            String code =
                    "var key = 'b';\n"
                            + "var {a, [key]: val, ...rest} = {a: 1, b: 2, c: 3};\n"
                            + "a + '|' + val + '|' + rest.c + '|' + rest.b;";

            Utils.assertWithAllModes_ES6("1|2|3|undefined", code);
        }

        @Test
        void objectRestWithMultipleComputedProperties() {
            String code =
                    "var k1 = 'x', k2 = 'y';\n"
                            + "var {[k1]: v1, [k2]: v2, ...rest} = {x: 1, y: 2, z: 3};\n"
                            + "v1 + '|' + v2 + '|' + JSON.stringify(rest);";

            Utils.assertWithAllModes_ES6("1|2|{\"z\":3}", code);
        }

        @Test
        void objectRestComputedPropertyExcludesCorrectly() {
            String code =
                    "var key = 'prop';\n"
                            + "var obj = {a: 1, prop: 2, b: 3};\n"
                            + "var {[key]: extracted, ...rest} = obj;\n"
                            + "extracted + '|' + ('prop' in rest) + '|' + rest.a + '|' + rest.b;";

            Utils.assertWithAllModes_ES6("2|false|1|3", code);
        }

        @Test
        void objectRestComputedPropertyEvaluatedOnce() {
            // Verify that computed properties are evaluated exactly once
            String code =
                    "var log = [];\n"
                            + "function getKey(val) { log.push(val); return val; }\n"
                            + "var obj = {a: 1, b: 2, c: 3};\n"
                            + "var {[getKey('a')]: x, [getKey('b')]: y, ...rest} = obj;\n"
                            + "log.join(',') + '|' + x + '|' + y + '|' + rest.c;";

            Utils.assertWithAllModes_ES6("a,b|1|2|3", code);
        }

        @Test
        void objectRestComputedPropertySideEffects() {
            // Verify that computed property expressions with side effects work correctly
            String code =
                    "var counter = 0;\n"
                            + "var obj = {x: 10, y: 20, z: 30};\n"
                            + "var {[(() => { counter++; return 'x'; })()]: val, ...rest} = obj;\n"
                            + "counter + '|' + val + '|' + rest.y + '|' + rest.z;";

            Utils.assertWithAllModes_ES6("1|10|20|30", code);
        }

        @Test
        void objectRestComputedPropertyEvaluationOrder() {
            // Verify that computed properties are evaluated in declaration order
            String code =
                    "var log = [];\n"
                            + "function getKey(val) { log.push(val); return val; }\n"
                            + "var {[getKey('first')]: a, [getKey('second')]: b, [getKey('third')]: c, ...rest} = \n"
                            + "    {first: 1, second: 2, third: 3, fourth: 4};\n"
                            + "log.join(',') + '|' + a + '|' + b + '|' + c;";

            Utils.assertWithAllModes_ES6("first,second,third|1|2|3", code);
        }

        @Test
        void restWithStringValue() {
            String code =
                    "var {...rest} = 'foo';\n"
                            + "rest[0] + '|' + rest[1] + '|' + rest[2] + '|' + (rest instanceof Object);";

            Utils.assertWithAllModes_ES6("f|o|o|true", code);
        }

        @Test
        void restWithStringValueInAssignment() {
            String code =
                    "var rest;\n"
                            + "var result = ({...rest} = 'foo');\n"
                            + "rest[0] + '|' + rest[1] + '|' + rest[2] + '|' + (result === 'foo');";

            Utils.assertWithAllModes_ES6("f|o|o|true", code);
        }

        @Test
        void objectAssignWithString() {
            String code =
                    "var rest = Object.assign({}, 'foo'); rest[0] + '|' + rest[1] + '|' + rest[2];";
            Utils.assertWithAllModes_ES6("f|o|o", code);
        }

        @Test
        void objectRestShouldCallGetter() {
            String code =
                    "var count = 0;\n"
                            + "var f = ({...x}) => {\n"
                            + "  return {count: count, value: x.v};\n"
                            + "};\n"
                            + "var result = f({ get v() { count++; return 2; } });\n"
                            + "result.count + '|' + result.value;";

            Utils.assertWithAllModes_ES6("1|2", code);
        }

        @Test
        void objectRestWithMultipleGetters() {
            String code =
                    "var count = 0;\n"
                            + "var obj = {\n"
                            + "  a: 1,\n"
                            + "  get b() { count++; return 2; },\n"
                            + "  get c() { count++; return 3; }\n"
                            + "};\n"
                            + "var {a, ...rest} = obj;\n"
                            + "count + '|' + rest.b + '|' + rest.c;";

            Utils.assertWithAllModes_ES6("2|2|3", code);
        }

        @Test
        void objectRestGetterInFunctionParam() {
            String code =
                    "var count = 0;\n"
                            + "function f({...x}) {\n"
                            + "  return count + '|' + x.v;\n"
                            + "}\n"
                            + "f({ get v() { count++; return 42; } });";

            Utils.assertWithAllModes_ES6("1|42", code);
        }

        @Test
        void objectRestSkipsNonEnumerable() {
            String code =
                    "var o = {a: 3, b: 4};\n"
                            + "Object.defineProperty(o, 'x', { value: 4, enumerable: false });\n"
                            + "var {...rest} = o;\n"
                            + "rest.x + '|' + rest.a + '|' + rest.b;";

            Utils.assertWithAllModes_ES6("undefined|3|4", code);
        }

        @Test
        void objectRestWithMixedEnumerability() {
            String code =
                    "var o = {a: 1};\n"
                            + "Object.defineProperty(o, 'b', { value: 2, enumerable: false });\n"
                            + "Object.defineProperty(o, 'c', { value: 3, enumerable: true });\n"
                            + "Object.defineProperty(o, 'd', { value: 4, enumerable: false });\n"
                            + "var {...rest} = o;\n"
                            + "('a' in rest) + '|' + ('b' in rest) + '|' + ('c' in rest) + '|' + ('d' in rest);";

            Utils.assertWithAllModes_ES6("true|false|true|false", code);
        }

        @Test
        void objectRestWithExclusionAndNonEnumerable() {
            String code =
                    "var o = {a: 1, b: 2, c: 3};\n"
                            + "Object.defineProperty(o, 'x', { value: 99, enumerable: false });\n"
                            + "var {a, ...rest} = o;\n"
                            + "('a' in rest) + '|' + ('b' in rest) + '|' + ('c' in rest) + '|' + ('x' in rest);";

            Utils.assertWithAllModes_ES6("false|true|true|false", code);
        }

        @Test
        void simpleObjectDestructuringWithDefault() {
            String code = "var f = ({a} = {a: 1}) => a;\n" + "f();";
            Utils.assertWithAllModes_ES6(1.0, code);
        }

        @Test
        void objectRestWithDefaultParameter() {
            String code = "var f = ({...x} = {v: 2}) => x.v;\n" + "f();";
            Utils.assertWithAllModes_ES6(2.0, code);
        }

        @Test
        void objectRestWithExplicitArgument() {
            String code = "var f = ({...x} = {v: 2}) => x.v;\n" + "f({v: 5});";
            Utils.assertWithAllModes_ES6(5.0, code);
        }

        @Test
        void arrayRestWithDefaultParameter() {
            String code = "var f = ([...x] = [1, 2, 3]) => x[0];\n" + "f();";
            Utils.assertWithAllModes_ES6(1.0, code);
        }

        @Test
        void objectRestWithDefaultParameterBasic() {
            String code =
                    "var f = ({...x} = { v: 2 }) => {\n" + "  return x.v;\n" + "};\n" + "f();";
            Utils.assertWithAllModes_ES6(2.0, code);
        }

        @Test
        void objectRestGetterWithDefaultParameter() {
            String code =
                    "var count = 0;\n"
                            + "var f = ({...x} = { get v() { count++; return 2; } }) => {\n"
                            + "  return count + '|' + x.v;\n"
                            + "};\n"
                            + "f();";

            Utils.assertWithAllModes_ES6("1|2", code);
        }

        @Test
        void objectRestGetterWithDefaultParameterRegularFunction() {
            String code =
                    "var count = 0;\n"
                            + "function f({...x} = { get v() { count++; return 42; } }) {\n"
                            + "  return count + '|' + x.v;\n"
                            + "}\n"
                            + "f();";

            Utils.assertWithAllModes_ES6("1|42", code);
        }

        @Test
        void restNotLastInObjectPattern() {
            String code = "var {...rest, b} = {a: 1, b: 2};";
            Utils.assertEvaluatorExceptionES6("Rest element must be last", code);
        }

        @Test
        void restNotLastInFunctionParameter() {
            String code = "var f = ({...rest, b}) => rest;";
            Utils.assertEvaluatorExceptionES6("Rest element must be last", code);
        }

        @Test
        void restInLastPositionIsValid() {
            String code = "var {a, ...rest} = {a: 1, b: 2, c: 3}; a + '|' + rest.b + '|' + rest.c;";
            Utils.assertWithAllModes_ES6("1|2|3", code);
        }
    }

    @Nested
    class ComputedPropertyErrors {
        @Test
        void computedPropertyInForLoopBasic() {
            String code =
                    "var results = [];\n"
                            + "for (let { ['x']: x } = {x: 42}; x > 40; x--) {\n"
                            + "  results.push(x);\n"
                            + "}\n"
                            + "results.join(',');";

            Utils.assertWithAllModes_ES6("42,41", code);
        }

        @Test
        void computedPropertyThrowsInForLoop() {
            String code =
                    "function thrower() { throw new Error('test error'); }\n"
                            + "try {\n"
                            + "  for (let { [thrower()]: x } = {}; ; ) {\n"
                            + "    break;\n"
                            + "  }\n"
                            + "  'no error';\n"
                            + "} catch (e) {\n"
                            + "  e.message;\n"
                            + "}";

            Utils.assertWithAllModes_ES6("test error", code);
        }

        @Test
        void computedPropertyThrowsInVariableDeclaration() {
            String code =
                    "function thrower() { throw new Error('test error'); }\n"
                            + "try {\n"
                            + "  let { [thrower()]: x } = {};\n"
                            + "  'no error';\n"
                            + "} catch (e) {\n"
                            + "  e.message;\n"
                            + "}";

            Utils.assertWithAllModes_ES6("test error", code);
        }

        @Test
        void computedPropertyThrowsInFunctionParameter() {
            String code =
                    "function thrower() { throw new Error('test error'); }\n"
                            + "try {\n"
                            + "  (function({ [thrower()]: x }) {})({});\n"
                            + "  'no error';\n"
                            + "} catch (e) {\n"
                            + "  e.message;\n"
                            + "}";

            Utils.assertWithAllModes_ES6("test error", code);
        }

        @Test
        void computedPropertyThrowsInAssignment() {
            String code =
                    "function thrower() { throw new Error('test error'); }\n"
                            + "try {\n"
                            + "  var x;\n"
                            + "  ({ [thrower()]: x } = {});\n"
                            + "  'no error';\n"
                            + "} catch (e) {\n"
                            + "  e.message;\n"
                            + "}";

            Utils.assertWithAllModes_ES6("test error", code);
        }
    }
}
