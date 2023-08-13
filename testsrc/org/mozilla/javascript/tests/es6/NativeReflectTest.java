package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tests.Utils;

public class NativeReflectTest {

    @Test
    public void testToString() {
        testString("[object Reflect]", "Reflect.toString()");
    }

    @Test
    public void apply() {
        testDouble(1.0, "Reflect.apply(Math.floor, undefined, [1.75])");
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
        testString("1 true 4 arg1 2 undefined null", js);
    }

    @Test
    public void applyMissingArgs() {
        String js =
                "try {\n"
                        + "  Reflect.apply();\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}";
        testString(
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
        testString("TypeError: [object Object] is not a function, it is object.", js);
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
        testString("TypeError: Expected argument of type object, but instead had type symbol", js);
    }

    @Test
    public void construct() {
        String js =
                "var d = Reflect.construct(Date, [1776, 6, 4]);\n"
                        + "'' + (d instanceof Date) + ' ' + d.getFullYear();";
        testString("true 1776", js);
    }

    @Test
    public void constructNoConstructorNumber() {
        String js = "try {\n"
                    + "  Reflect.construct(function() {}, [], 1);\n"
                    + "} catch(e) {\n"
                    + "  '' + e;\n"
                    + "}";
        testString("TypeError: \"number\" is not a constructor.", js);
    }

    @Test
    public void constructNoConstructorNull() {
        String js = "try {\n"
                    + "  Reflect.construct(function() {}, [], null);\n"
                    + "} catch(e) {\n"
                    + "  '' + e;\n"
                    + "}";
        testString("TypeError: \"object\" is not a constructor.", js);
    }

    @Test
    public void constructNoConstructorObject() {
        String js = "try {\n"
                    + "  Reflect.construct(function() {}, [], {});\n"
                    + "} catch(e) {\n"
                    + "  '' + e;\n"
                    + "}";
        testString("TypeError: \"object\" is not a constructor.", js);
    }

    @Test
    public void constructNoConstructorFunction() {
        String js = "try {\n"
                    + "  Reflect.construct(function() {}, [], Date.now);\n"
                    + "} catch(e) {\n"
                    + "  '' + e;\n"
                    + "}";
        // testString("TypeError: \"object\" is not a constructor.", js);
        // found no way to check a function for constructor
    }

    @Test
    public void defineProperty() {
        String js =
                "var o = {};\n" + "'' + Reflect.defineProperty(o, 'p', { value: 42 }) + ' ' + o.p;";
        testString("true 42", js);
    }

    @Test
    public void definePropertyWithoutValue() {
        String js =
                "var o = {};\n"
                        + "'' + Reflect.defineProperty(o, 'p', {})"
                        + "+ ' ' + Reflect.has(o, 'p')"
                        + "+ ' ' + o.p;";
        testString("true true undefined", js);
    }

    @Test
    public void definePropertyFreezed() {
        String js =
                "var o = {};\n"
                        + "Object.freeze(o);\n"
                        + "'' + Reflect.defineProperty(o, 'p', { value: 42 }) + ' ' + o.p;";
        testString("false undefined", js);
    }

    @Test
    public void deleteProperty() {
        String js =
                "var o = { p: 42 };\n"
                        + "'' + Reflect.deleteProperty(o, 'p')"
                        + "+ ' ' + Reflect.has(o, 'p')"
                        + "+ ' ' + o.p;";
        testString("true false undefined", js);
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
        testString("[get,set,enumerable,configurable] false true true true", js);
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

        testString("true false false", js);
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
        testString("p1,p2 length", js);
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
        // FF: 0,6,8,55,773,s1,str,-1,s2,str2
        testString("-1,0,6,8,55,773,s1,str,s2,str2", js);
    }

    @Test
    public void ownKeysEmptyObj() {
        String js = "'' + Reflect.ownKeys({}).length";
        testString("0", js);
    }

    @Test
    public void ownKeysDeleteObj() {
        String js = "var o = { d: 42 };\n" + "delete o.d;\n" + "'' + Reflect.ownKeys(o).length";
        testString("0", js);
    }

    @Test
    public void ownKeysEmptyArray() {
        String js = "'' + Reflect.ownKeys([])";
        testString("length", js);
    }

    @Test
    public void ownKeysArray() {
        String js = "'' + Reflect.ownKeys([, , 2])";
        testString("2,length", js);
    }

    @Test
    public void ownKeysNotEnumerable() {
        String js =
                "var o = {};\n"
                        + "Object.defineProperty(o, 'p1', { value: 42, enumerable: false });\n"
                        + "Object.defineProperty(o, 'p2', { get: function() {}, enumerable: false });\n"
                        + "'' + Reflect.ownKeys(o)";
        testString("p1,p2", js);
    }

    @Test
    public void has() {
        String js =
                "var o1 = { p: 42 }\n"
                        + "'' + Reflect.has(o1, 'p')"
                        + "+ ' ' + Reflect.has(o1, 'p2')"
                        + "+ ' ' + Reflect.has(o1, 'toString')";
        testString("true false true", js);
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
        testString("true false", js);
    }

    @Test
    public void hasProto() {
        String js = "var o1 = { p: 42 }\n" + "'' + typeof Reflect.has.__proto__";
        testString("function", js);
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
        testString("42 true true true", js);
    }

    @Test
    public void getOwnPropertyDescriptorUndefinedProperty() {
        String js =
                "var o = Object.create({p: 1});\n"
                        + "var result = Reflect.getOwnPropertyDescriptor(o, 'p');\n"
                        + "'' + (result === undefined)";
        testString("true", js);
    }

    @Test
    public void getPropertyByInt() {
        String js = "var a = ['zero', 'one']\n" + "Reflect.get(a, 1);";
        testString("one", js);
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

        testString("value 1, undefined, foo, 42, undefined", js);
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
        testString("true true false", js);
    }

    @Test
    public void setPrototypeOfCycle() {
        String js = "var o1 = {};\n" + "'' + Reflect.setPrototypeOf(o1, o1);\n";
        testString("false", js);
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
        testString("true true false", js);
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
        testString("true true true", js);
    }

    private static void testString(String expected, String js) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result = cx.evaluateString(scope, js, "test", 1, null);
                    assertEquals(expected, result);

                    return null;
                });
    }

    private static void testDouble(double expected, String js) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result = cx.evaluateString(scope, js, "test", 1, null);
                    assertEquals(expected, ((Double) result).doubleValue(), 0.00001);

                    return null;
                });
    }
}
