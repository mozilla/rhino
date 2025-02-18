package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class NativeReflectTest {

    @Test
    public void testToString() {
        Utils.assertWithAllModes_ES6("[object Reflect]", "Reflect.toString()");
    }

    @Test
    public void apply() {
        Utils.assertWithAllModes_ES6(1.0, "Reflect.apply(Math.floor, undefined, [1.75])");
        Utils.assertWithAllModes_ES6(
                "hello",
                "Reflect.apply(String.fromCharCode, undefined, [104, 101, 108, 108, 111])");
        Utils.assertWithAllModes_ES6(
                4, "Reflect.apply(RegExp.prototype.exec, /ab/, ['confabulation']).index");
        Utils.assertWithAllModes_ES6("i", "Reflect.apply(''.charAt, 'ponies', [3])");
    }

    @Test
    public void applyString() {
        Utils.assertWithAllModes_ES6("foo", "Reflect.apply(String.prototype.toString, 'foo', [])");
        Utils.assertWithAllModes_ES6("oo", "Reflect.apply(String.prototype.substring, 'foo', [1])");
    }

    @Test
    public void applyNumber() {
        Utils.assertWithAllModes_ES6(
                "1.234567e+3", "Reflect.apply(Number.prototype.toExponential, 1234.567, [])");
        Utils.assertWithAllModes_ES6(
                "1.23e+3", "Reflect.apply(Number.prototype.toExponential, 1234.567, [2])");
    }

    @Test
    public void applyBoolean() {
        Utils.assertWithAllModes_ES6("true", "Reflect.apply(Boolean.prototype.toString, true, [])");
        Utils.assertWithAllModes_ES6(
                "false", "Reflect.apply(Boolean.prototype.toString, false, [])");
    }

    @Test
    public void applyDetails() {
        String js =
                "var o = {};\n"
                        + "var count = 0;\n"
                        + "var results, args;\n"
                        + "function fn() {\n"
                        + "  count++;\n"
                        + "  results = {\n"
                        + "    thisArg: this,\n"
                        + "    args: arguments\n"
                        + "  };\n"
                        + "}\n"
                        + "Reflect.apply(fn, o, ['arg1', 2, , null]);\n"
                        + "'' + count "
                        + "+ ' ' + (results.thisArg === o)"
                        + "+ ' ' + results.args.length"
                        + "+ ' ' + results.args[0]"
                        + "+ ' ' + results.args[1]"
                        + "+ ' ' + results.args[2]"
                        + "+ ' ' + results.args[3]";
        Utils.assertWithAllModes_ES6("1 true 4 arg1 2 undefined null", js);
    }

    @Test
    public void applyMissingArgs() {
        String js = "try {\n" + "  Reflect.apply();\n" + "} catch(e) {\n" + "  '' + e;\n" + "}";
        Utils.assertWithAllModes_ES6(
                "TypeError: Reflect.apply: At least 3 arguments required, but only 0 passed", js);
    }

    @Test
    public void applyTargetNotFunction() {
        String js =
                "try {\n"
                        + "  Reflect.apply({}, undefined, [1.75]);\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}";
        Utils.assertWithAllModes_ES6(
                "TypeError: [object Object] is not a function, it is object.", js);
    }

    @Test
    public void applyArgumentsListNotFunction() {
        String js =
                "var s1 = Symbol('1');"
                        + "try {\n"
                        + "  Reflect.apply(Math.floor, undefined, s1);\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}";
        Utils.assertWithAllModes_ES6(
                "TypeError: Expected argument of type object, but instead had type symbol", js);
    }

    @Test
    public void construct() {
        String js =
                "var d = Reflect.construct(Date, [1776, 6, 4]);\n"
                        + "'' + (d instanceof Date) + ' ' + d.getFullYear();";
        Utils.assertWithAllModes_ES6("true 1776", js);
    }

    @Test
    public void constructNewTarget() {
        String js =
                "var o = {};\n"
                        + "var internPrototype;\n"
                        + "function fn() {\n"
                        + "  this.o = o;\n"
                        + "  internPrototype = Object.getPrototypeOf(this);\n"
                        + "}\n"
                        + "var result = Reflect.construct(fn, [], Array);\n"
                        + "'' + (Object.getPrototypeOf(result) === Array.prototype)"
                        + " + ' ' + (internPrototype === Array.prototype)"
                        + " + ' ' + (result.o === o)";
        Utils.assertWithAllModes_ES6("true true true", js);
    }

    @Test
    public void constructNoConstructorNumber() {
        String js =
                "try {\n"
                        + "  Reflect.construct(function() {}, [], 1);\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}";
        Utils.assertWithAllModes_ES6("TypeError: \"number\" is not a constructor.", js);
    }

    @Test
    public void constructNoConstructorNull() {
        String js =
                "try {\n"
                        + "  Reflect.construct(function() {}, [], null);\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}";
        Utils.assertWithAllModes_ES6("TypeError: \"object\" is not a constructor.", js);
    }

    @Test
    public void constructNoConstructorObject() {
        String js =
                "try {\n"
                        + "  Reflect.construct(function() {}, [], {});\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}";
        Utils.assertWithAllModes_ES6("TypeError: \"object\" is not a constructor.", js);
    }

    @Test
    public void constructNoConstructorFunction() {
        String js =
                "try {\n"
                        + "  Reflect.construct(function() {}, [], Math.abs);\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}";
        Utils.assertWithAllModes_ES6("TypeError: \"function\" is not a constructor.", js);
    }

    @Test
    public void constructorArgs() {
        final String script =
                "  var res = '';\n"
                        + "function foo(a, b) {\n"
                        + "  res += 'foo - ';\n"
                        + "  for (let i = 0; i < arguments.length; i++) {\n"
                        + "    res += arguments[i] + ' ';\n"
                        + "  }\n"
                        + "}\n"
                        + "Reflect.construct(foo, [1, 2]);\n"
                        + "res;";

        Utils.assertWithAllModes_ES6("foo - 1 2 ", script);
    }

    @Test
    public void constructorArgsWithTarget() {
        final String script =
                "  var res = '';\n"
                        + "function foo(a, b) {\n"
                        + "  res += 'foo - ';\n"
                        + "  for (let i = 0; i < arguments.length; i++) {\n"
                        + "    res += arguments[i] + ' ';\n"
                        + "  }\n"
                        + "}\n"
                        + "function bar(a, b) {\n"
                        + "  res += 'bar - ';\n"
                        + "  for (let i = 0; i < arguments.length; i++) {\n"
                        + "    res += arguments[i] + ' ';\n"
                        + "  }\n"
                        + "}\n"
                        + "Reflect.construct(foo, [6, 7, 8], bar);\n"
                        + "res;";

        Utils.assertWithAllModes_ES6("foo - 6 7 8 ", script);
    }

    @Test
    public void defineProperty() {
        String js =
                "var o = {};\n" + "'' + Reflect.defineProperty(o, 'p', { value: 42 }) + ' ' + o.p;";
        Utils.assertWithAllModes_ES6("true 42", js);
    }

    @Test
    public void definePropertyWithoutValue() {
        String js =
                "var o = {};\n"
                        + "'' + Reflect.defineProperty(o, 'p', {})"
                        + "+ ' ' + Reflect.has(o, 'p')"
                        + "+ ' ' + o.p;";
        Utils.assertWithAllModes_ES6("true true undefined", js);
    }

    @Test
    public void definePropertyFreezed() {
        String js =
                "var o = {};\n"
                        + "Object.freeze(o);\n"
                        + "'' + Reflect.defineProperty(o, 'p', { value: 42 }) + ' ' + o.p;";
        Utils.assertWithAllModes_ES6("false undefined", js);
    }

    @Test
    public void deleteProperty() {
        String js =
                "var o = { p: 42 };\n"
                        + "'' + Reflect.deleteProperty(o, 'p')"
                        + "+ ' ' + Reflect.has(o, 'p')"
                        + "+ ' ' + o.p;";
        Utils.assertWithAllModes_ES6("true false undefined", js);
    }

    @Test
    public void getOwnPropertyDescriptor() {
        String js =
                "var o1 = {};\n"
                        + "var fn = function() {};\n"
                        + "Object.defineProperty(o1, 'p', {\n"
                        + "  get: fn,\n"
                        + "  configurable: true\n"
                        + "});\n"
                        + "var result = Reflect.getOwnPropertyDescriptor(o1, 'p');\n"
                        + "'[' + Object.getOwnPropertyNames(result) + ']'"
                        + "+ ' ' + result.enumerable"
                        + "+ ' ' + result.configurable"
                        + "+ ' ' + (result.get === fn)"
                        + "+ ' ' + (result.set === undefined)";
        Utils.assertWithAllModes_ES6("[get,set,enumerable,configurable] false true true true", js);
    }

    @Test
    public void isExtensible() {
        String js =
                "var o1 = {};\n"
                        + "var result = '' + Reflect.isExtensible(o1);\n"
                        + "Reflect.preventExtensions(o1);\n"
                        + "result += ' ' + Reflect.isExtensible(o1);\n"
                        + "var o2 = Object.seal({});\n"
                        + "result += ' ' + Reflect.isExtensible(o2);\n";

        Utils.assertWithAllModes_ES6("true false false", js);
    }

    @Test
    public void ownKeys() {
        String js =
                "var o1 = {\n"
                        + "  p1: 42,\n"
                        + "  p2: 'one'\n"
                        + "};\n"
                        + "var a1 = [];\n"
                        + "'' + Reflect.ownKeys(o1)"
                        + "+ ' ' + Reflect.ownKeys(a1)";
        Utils.assertWithAllModes_ES6("p1,p2 length", js);
    }

    @Test
    public void ownKeys2() {
        String js =
                "let s1 = Symbol.for('foo');\n"
                        + "let s2 = Symbol.for('bar');\n"
                        + "var o1 = {\n"
                        + "  s1: 0,\n"
                        + "  'str': 0,\n"
                        + "  773: 0,\n"
                        + "  '55': 0,\n"
                        + "  0: 0,\n"
                        + "  '-1': 0,\n"
                        + "  8: 0,\n"
                        + "  '6': 8,\n"
                        + "  s2: 0,\n"
                        + "  'str2': 0\n"
                        + "};\n"
                        + "var a1 = [];\n"
                        + "'' + Reflect.ownKeys(o1)";
        Utils.assertWithAllModes_ES6("0,6,8,55,773,s1,str,-1,s2,str2", js);
    }

    @Test
    public void ownKeysEmptyObj() {
        String js = "'' + Reflect.ownKeys({}).length";
        Utils.assertWithAllModes_ES6("0", js);
    }

    @Test
    public void ownKeysDeleteObj() {
        String js = "var o = { d: 42 };\n" + "delete o.d;\n" + "'' + Reflect.ownKeys(o).length";
        Utils.assertWithAllModes_ES6("0", js);
    }

    @Test
    public void ownKeysEmptyArray() {
        String js = "'' + Reflect.ownKeys([])";
        Utils.assertWithAllModes_ES6("length", js);
    }

    @Test
    public void ownKeysArray() {
        String js = "'' + Reflect.ownKeys([, , 2])";
        Utils.assertWithAllModes_ES6("2,length", js);
    }

    @Test
    public void ownKeysNotEnumerable() {
        String js =
                "var o = {};\n"
                        + "Object.defineProperty(o, 'p1', { value: 42, enumerable: false });\n"
                        + "Object.defineProperty(o, 'p2', { get: function() {}, enumerable: false });\n"
                        + "'' + Reflect.ownKeys(o)";
        Utils.assertWithAllModes_ES6("p1,p2", js);
    }

    @Test
    public void has() {
        String js =
                "var o1 = { p: 42 }\n"
                        + "'' + Reflect.has(o1, 'p')"
                        + "+ ' ' + Reflect.has(o1, 'p2')"
                        + "+ ' ' + Reflect.has(o1, 'toString')";
        Utils.assertWithAllModes_ES6("true false true", js);
    }

    @Test
    public void hasSymbol() {
        String js =
                "var s1 = Symbol('1');\n"
                        + "var s2 = Symbol('1');\n"
                        + "var o = {};\n"
                        + "o[s1] = 42;\n"
                        + "'' + Reflect.has(o, s1)"
                        + "+ ' ' + Reflect.has(o, 2)";
        Utils.assertWithAllModes_ES6("true false", js);
    }

    @Test
    public void hasProto() {
        String js = "var o1 = { p: 42 }\n" + "'' + typeof Reflect.has.__proto__";
        Utils.assertWithAllModes_ES6("function", js);
    }

    @Test
    public void getOwnPropertyDescriptorSymbol() {
        String js =
                "var s = Symbol('sym');\n"
                        + "var o = {};\n"
                        + "o[s] = 42;\n"
                        + "var result = Reflect.getOwnPropertyDescriptor(o, s);\n"
                        + "'' + result.value"
                        + "+ ' ' + result.enumerable"
                        + "+ ' ' + result.configurable"
                        + "+ ' ' + result.writable";
        Utils.assertWithAllModes_ES6("42 true true true", js);
    }

    @Test
    public void getOwnPropertyDescriptorUndefinedProperty() {
        String js =
                "var o = Object.create({p: 1});\n"
                        + "var result = Reflect.getOwnPropertyDescriptor(o, 'p');\n"
                        + "'' + (result === undefined)";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void getPropertyByInt() {
        String js = "var a = ['zero', 'one']\n" + "Reflect.get(a, 1);";
        Utils.assertWithAllModes_ES6("one", js);
    }

    @Test
    public void getProperty() {
        String js =
                "var o = {};\n"
                        + "o.p1 = 'value 1';\n"
                        + "var result = '';"
                        + "result += Reflect.get(o, 'p1');\n"
                        + "Object.defineProperty(o, 'p2', { get: undefined });\n"
                        + "result += ', ' + Reflect.get(o, 'p2');\n"
                        + "Object.defineProperty(o, 'p3', { get: function() { return 'foo'; } });\n"
                        + "result += ', ' + Reflect.get(o, 'p3');\n"
                        + "var o2 = Object.create({ p: 42 });\n"
                        + "result += ', ' + Reflect.get(o2, 'p');\n"
                        + "result += ', ' + Reflect.get(o2, 'u');\n";

        Utils.assertWithAllModes_ES6("value 1, undefined, foo, 42, undefined", js);
    }

    @Test
    public void setPrototypeOf() {
        String js =
                "var o1 = {};\n"
                        + "var result = '';\n"
                        + "result += Reflect.setPrototypeOf(o1, Object.prototype);\n"
                        + "result += ' ' + Reflect.setPrototypeOf(o1, null);\n"
                        + "var o2 = {};\n"
                        + "result += ' ' + Reflect.setPrototypeOf(Object.freeze(o2), null);\n";
        Utils.assertWithAllModes_ES6("true true false", js);
    }

    @Test
    public void setPrototypeOfCycle() {
        String js = "var o1 = {};\n" + "'' + Reflect.setPrototypeOf(o1, o1);\n";
        Utils.assertWithAllModes_ES6("false", js);
    }

    @Test
    public void setPrototypeOfCycleComplex() {
        String js =
                "var o1 = {};\n"
                        + "var o2 = {};\n"
                        + "var o3 = {};\n"
                        + "'' + Reflect.setPrototypeOf(o1, o2)"
                        + "+ ' ' + Reflect.setPrototypeOf(o2, o3)"
                        + "+ ' ' + Reflect.setPrototypeOf(o3, o1)";
        Utils.assertWithAllModes_ES6("true true false", js);
    }

    @Test
    public void setPrototypeOfSame() {
        String js =
                "var o1 = {};\n"
                        + "Object.preventExtensions(o1);\n"
                        + "var o2 = Object.create(null);\n"
                        + "Object.preventExtensions(o2);\n"
                        + "var proto = {};\n"
                        + "var o3 = Object.create(proto);\n"
                        + "Object.preventExtensions(o3);\n"
                        + "'' + Reflect.setPrototypeOf(o1, Object.prototype)"
                        + "+ ' ' + Reflect.setPrototypeOf(o2, null)"
                        + "+ ' ' + Reflect.setPrototypeOf(o3, proto)";
        Utils.assertWithAllModes_ES6("true true true", js);
    }
}
