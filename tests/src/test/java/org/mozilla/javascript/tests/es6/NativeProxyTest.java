package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class NativeProxyTest {

    @Test
    public void testToString() {
        Utils.assertWithAllModes_ES6(
                "function Proxy() {\n\t[native code, arity=2]\n}\n", "Proxy.toString()");

        Utils.assertWithAllModes_ES6(
                "[object Object]", "Object.prototype.toString.call(new Proxy({}, {}))");
        Utils.assertWithAllModes_ES6(
                "[object Array]", "Object.prototype.toString.call(new Proxy([], {}))");
        Utils.assertWithAllModes_ES6(
                "[object Array]",
                "Object.prototype.toString.call(new Proxy(new Proxy([], {}), {}))");
    }

    @Test
    public void testToStringRevoke() {
        String js =
                "var rev = Proxy.revocable(%s, {});\n"
                        + "rev.revoke();\n"
                        + "try {"
                        + "  Object.prototype.toString.call(%s);\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}";

        Utils.assertWithAllModes_ES6(
                "TypeError: Illegal operation attempted on a revoked proxy",
                String.format(js, "{}", "rev.proxy"));
        Utils.assertWithAllModes_ES6(
                "TypeError: Illegal operation attempted on a revoked proxy",
                String.format(js, "[]", "rev.proxy"));
    }

    @Test
    public void prototype() {
        Utils.assertWithAllModes_ES6(
                "false", "'' + Object.hasOwnProperty.call(Proxy, 'prototype')");

        Utils.assertWithAllModes_ES6("2", "'' + Proxy.length");
    }

    @Test
    public void ctorMissingArgs() {
        Utils.assertWithAllModes_ES6(
                "TypeError: Proxy.ctor: At least 2 arguments required, but only 0 passed",
                "try { new Proxy() } catch(e) { '' + e }");
        Utils.assertWithAllModes_ES6(
                "TypeError: Proxy.ctor: At least 2 arguments required, but only 1 passed",
                "try { new Proxy({}) } catch(e) { '' + e }");

        Utils.assertWithAllModes_ES6(
                "TypeError: Expected argument of type object, but instead had type undefined",
                "try { new Proxy(undefined, {}) } catch(e) { '' + e }");
        Utils.assertWithAllModes_ES6(
                "TypeError: Expected argument of type object, but instead had type object",
                "try { new Proxy(null, {}) } catch(e) { '' + e }");
    }

    @Test
    public void ctorWrongArgs() {
        Utils.assertWithAllModes_ES6(
                "TypeError: Expected argument of type object, but instead had type symbol",
                "try { new Proxy({}, Symbol()) } catch(e) { '' + e }");
        Utils.assertWithAllModes_ES6(
                "TypeError: Expected argument of type object, but instead had type symbol",
                "try { new Proxy(Symbol(), {}) } catch(e) { '' + e }");
    }

    @Test
    public void ctorAsFunction() {
        Utils.assertWithAllModes_ES6(
                "TypeError: The constructor for Proxy may not be invoked as a function",
                "try { Proxy() } catch(e) { '' + e }");
    }

    @Test
    public void construct() {
        String js =
                "var _target, _handler, _args, _P;\n"
                        + "function Target() {}\n"
                        + "var handler = {\n"
                        + "  construct: function(t, args, newTarget) {\n"
                        + "    _handler = this;\n"
                        + "    _target = t;\n"
                        + "    _args = args;\n"
                        + "    _P = newTarget;\n"
                        + "    return new t(args[0], args[1]);\n"
                        + "  }\n"
                        + "};\n"
                        + "var P = new Proxy(Target, handler);\n"
                        + "new P(1, 4);\n"
                        + "'' + (_handler === handler)\n"
                        + "+ ' ' + (_target === Target)"
                        + "+ ' ' + (_P === P)"
                        + "+ ' ' + _args.length + ' ' + _args[0] + ' ' + _args[1]";

        Utils.assertWithAllModes_ES6("true true true 2 1 4", js);
    }

    @Test
    public void apply() {
        String js =
                "function sum(a, b) {\n"
                        + "  return a + b;\n"
                        + "}\n"
                        + "var res = '';\n"
                        + "var handler = {\n"
                        + "  apply: function (target, thisArg, argumentsList) {\n"
                        + "    res += ' ' + `Calculate sum: ${argumentsList}`;\n"
                        + "    return target(argumentsList[0], argumentsList[1]) * 7;\n"
                        + "  },\n"
                        + "};\n"
                        + "var proxy1 = new Proxy(sum, handler);\n"
                        + "var x = ' ' + proxy1(1, 2);\n"
                        + "res + x";

        Utils.assertWithAllModes_ES6(" Calculate sum: 1,2 21", js);
    }

    @Test
    public void applyParameters() {
        String js =
                "var _target, _args, _handler, _context;\n"
                        + "var target = function() {\n"
                        + "  throw new Error('target should not be called');\n"
                        + "};\n"
                        + "var handler = {\n"
                        + "  apply: function(t, c, args) {\n"
                        + "    _handler = this;\n"
                        + "    _target = t;\n"
                        + "    _context = c;\n"
                        + "    _args = args;\n"
                        + "  }\n"
                        + "};\n"
                        + "var p = new Proxy(target, handler);\n"
                        + "var context = {};\n"
                        + "p.call(context, 1, 4);\n"
                        + "'' + (_handler === handler)\n"
                        + "+ ' ' + (_target === target)"
                        + "+ ' ' + (_context === context)"
                        + "+ ' ' + _args.length + ' ' + _args[0] + ' ' + _args[1]";

        Utils.assertWithAllModes_ES6("true true true 2 1 4", js);
    }

    @Test
    public void applyTrapIsNull() {
        String js =
                "var calls = 0;\n"
                        + "var _context;\n"
                        + "var target = new Proxy(function() {}, {\n"
                        + "  apply: function(_target, context, args) {\n"
                        + "    calls++;\n"
                        + "    _context = context;\n"
                        + "    return args[0] + args[1];\n"
                        + "  }\n"
                        + "})\n"
                        + "var p = new Proxy(target, {\n"
                        + "  apply: null\n"
                        + "});\n"
                        + "var context = {};\n"
                        + "var res = p.call(context, 1, 2);\n"
                        + "'' + calls\n"
                        + "+ ' ' + (_context === context)"
                        + "+ ' ' + res";

        Utils.assertWithAllModes_ES6("1 true 3", js);
    }

    @Test
    public void applyWithoutHandler() {
        String js =
                "function sum(a, b) {\n"
                        + "  return a + b;\n"
                        + "}\n"
                        + "var proxy1 = new Proxy(sum, {});\n"
                        + "proxy1(1, 2);";

        Utils.assertWithAllModes_ES6(3, js);
    }

    @Test
    public void defineProperty() {
        String js =
                "var o = {};\n"
                        + "var res = '';\n"
                        + "var handler = {\n"
                        + "         defineProperty(target, key, desc) {\n"
                        + "           res = res + target + ' ' + key + ' '\n"
                        + "                 + desc.writable + ' ' + desc.configurable + ' ' + desc.enumerable;\n"
                        + "           return true;\n"
                        + "         }\n"
                        + "       };\n"
                        + "var proxy1 = new Proxy(o, handler);\n"
                        + "Object.defineProperty(proxy1, 'p', { value: 42, writable: false });\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("[object Object] p false undefined undefined", js);
    }

    @Test
    public void definePropertyTrapReturnsFalse() {
        String js =
                "var target = {};\n"
                        + "var p = new Proxy(target, {\n"
                        + "  defineProperty: function(t, prop, desc) {\n"
                        + "    return 0;\n"
                        + "  }\n"
                        + "});\n"
                        + "'' + Reflect.defineProperty(p, 'attr', {})"
                        + "+ ' ' + Object.getOwnPropertyDescriptor(target, 'attr')";
        Utils.assertWithAllModes_ES6("false undefined", js);
    }

    @Test
    public void
            definePropertyDescNotConfigurableAndTargetPropertyDescriptorConfigurableAndTrapResultIsTrue() {
        String js =
                "var target = {};\n"
                        + "var p = new Proxy(target, {\n"
                        + "  defineProperty: function(t, prop, desc) {\n"
                        + "    return true;\n"
                        + "  }\n"
                        + "});\n"
                        + "Object.defineProperty(target, \"foo\", {\n"
                        + "  value: 1,\n"
                        + "  configurable: true\n"
                        + "});\n"
                        + "try {\n"
                        + "  Object.defineProperty(p, \"foo\", {\n"
                        + "    value: 1,\n"
                        + "    configurable: false\n"
                        + "  });\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}\n";
        Utils.assertWithAllModes_ES6(
                "TypeError: proxy can't define an incompatible property descriptor", js);
    }

    @Test
    public void definePropertyDescAndTargetPropertyDescriptorNotCompatibleAndTrapResultIsTrue() {
        String js =
                "var target = {};\n"
                        + "var p = new Proxy(target, {\n"
                        + "  defineProperty: function(t, prop, desc) {\n"
                        + "    return true;\n"
                        + "  }\n"
                        + "});\n"
                        + "Object.defineProperty(target, \"foo\", {\n"
                        + "  value: 1\n"
                        + "});\n"
                        + "try {\n"
                        + "  Object.defineProperty(p, \"foo\", {\n"
                        + "    value: 2\n"
                        + "  });\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}\n";
        Utils.assertWithAllModes_ES6(
                "TypeError: proxy can't define an incompatible property descriptor", js);
    }

    @Test
    public void definePropertyDescAndTargetPropertyDescriptorNotCompatibleAndTrapResultIsTrue2() {
        String js =
                "var target = Object.create(null);\n"
                        + "var p = new Proxy(target, {\n"
                        + "  defineProperty: function() {\r\n"
                        + "    return true;\r\n"
                        + "  }\n"
                        + "});\n"
                        + "Object.defineProperty(target, 'prop', {\n"
                        + "  value: 1,\n"
                        + "  configurable: false\n"
                        + "});\n"
                        + "try {\n"
                        + "  Object.defineProperty(p, 'prop', {\n"
                        + "    value: 1,\n"
                        + "    configurable: true\n"
                        + "  });\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}\n";
        Utils.assertWithAllModes_ES6(
                "TypeError: proxy can't define an incompatible property descriptor", js);
    }

    @Test
    public void definePropertyWithoutHandler() {
        String js =
                "var o = {};\n"
                        + "var proxy1 = new Proxy(o, {});\n"
                        + "proxy1.p = 42;\n"
                        + "'' + o.p;";
        Utils.assertWithAllModes_ES6("42", js);
    }

    @Test
    public void definePropertyFreezedWithoutHandler() {
        String js =
                "var o = {};\n"
                        + "Object.freeze(o);\n"
                        + "var proxy1 = new Proxy(o, {});\n"
                        + "try {\n"
                        + "  Object.defineProperty(proxy1, 'p', { value: 42, writable: false });\n"
                        + "  '' + o.p;\n"
                        + "} catch(e) {\n"
                        + "  '' + e;"
                        + "}\n";
        Utils.assertWithAllModes_ES6(
                "TypeError: Cannot add properties to this object because extensible is false.", js);
    }

    @Test
    public void definePropertyHandlerNotFunction() {
        String js =
                "var o = {};\n"
                        + "var proxy1 = new Proxy(o, { defineProperty: 7 });\n"
                        + "try {\n"
                        + "  Object.defineProperty(proxy1, 'p', { value: 42, writable: false });\n"
                        + "  '' + o.p;\n"
                        + "} catch(e) {\n"
                        + "  '' + e;"
                        + "}\n";
        Utils.assertWithAllModes_ES6(
                "TypeError: defineProperty is not a function, it is number.", js);
    }

    @Test
    public void definePropertyHandlerNull() {
        String js =
                "var o = {};\n"
                        + "var proxy1 = new Proxy(o, { defineProperty: null });\n"
                        + "try {\n"
                        + "  Object.defineProperty(proxy1, 'p', { value: 42, writable: false });\n"
                        + "  '' + o.p;\n"
                        + "} catch(e) {\n"
                        + "  '' + e;"
                        + "}\n";
        Utils.assertWithAllModes_ES6("42", js);
    }

    @Test
    public void definePropertyHandlerUndefined() {
        String js =
                "var o = {};\n"
                        + "var proxy1 = new Proxy(o, { defineProperty: undefined });\n"
                        + "try {\n"
                        + "  Object.defineProperty(proxy1, 'p', { value: 42, writable: false });\n"
                        + "  '' + o.p;\n"
                        + "} catch(e) {\n"
                        + "  '' + e;"
                        + "}\n";
        Utils.assertWithAllModes_ES6("42", js);
    }

    @Test
    public void deletePropertyWithoutHandler() {
        String js =
                "var o = { p: 42 };\n"
                        + "var proxy1 = new Proxy(o, {});\n"
                        + "delete proxy1.p;\n"
                        + "'' + o.p;";
        Utils.assertWithAllModes_ES6("undefined", js);
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
                        + "var proxy1 = new Proxy(o1, {\n"
                        + "                     getOwnPropertyDescriptor(target, prop) {\n"
                        + "                         return { configurable: true, enumerable: true, value: 7 };\n"
                        + "                     }});\n"
                        + "var result = Object.getOwnPropertyDescriptor(proxy1, 'p');\n"
                        + "'' + o1.p + ' ' + result.value \n"
                        + "+ ' [' + Object.getOwnPropertyNames(result) + ']' "
                        + "+ ' ' + result.enumerable "
                        + "+ ' ' + result.configurable "
                        + "+ ' ' + result.writable "
                        + "+ ' ' + (result.get === fn) "
                        + "+ ' ' + (result.set === undefined)";
        Utils.assertWithAllModes_ES6(
                "undefined 7 [value,writable,enumerable,configurable] true true false false true",
                js);
    }

    @Test
    public void getOwnPropertyDescriptorResultUndefined() {
        String js =
                "var target = {attr: 1};\n"
                        + "var p = new Proxy(target, {\n"
                        + "            getOwnPropertyDescriptor: function(t, prop) {\n"
                        + "              return;\n"
                        + "            }\n"
                        + "          });\n"
                        + "'' + Object.getOwnPropertyDescriptor(p, 'attr');";
        Utils.assertWithAllModes_ES6("undefined", js);
    }

    @Test
    public void getOwnPropertyDescriptorWithoutHandler() {
        String js =
                "var o1 = {};\n"
                        + "var fn = function() {};\n"
                        + "Object.defineProperty(o1, 'p', {\n"
                        + "  get: fn,\n"
                        + "  configurable: true\n"
                        + "});\n"
                        + "var proxy1 = new Proxy(o1, {});\n"
                        + "var result = Object.getOwnPropertyDescriptor(proxy1, 'p');\n"
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
                "var o = {};\n"
                        + "var res = '';\n"
                        + "var handler = {\n"
                        + "         isExtensible(target) {\n"
                        + "           res += ' a ' + (target == o);\n"
                        + "           return Reflect.isExtensible(target);"
                        + "         },\n"
                        + "         preventExtensions(target) {\n"
                        + "           res += ' o ' + (target == o);\n"
                        + "         }\n"
                        + "       };\n"
                        + "var proxy1 = new Proxy(o, handler);\n"
                        + "var x = Object.isExtensible(proxy1);\n"
                        + "res += ' ' + x;\n"
                        + "res += ' ' + x;\n";
        Utils.assertWithAllModes_ES6(" a true true true", js);
    }

    @Test
    public void isExtensibleWithoutHandler() {
        String js =
                "var o1 = {};\n"
                        + "var proxy1 = new Proxy(o1, {});\n"
                        + "var result = '' + Object.isExtensible(o1) + '-' + Object.isExtensible(proxy1);\n"
                        + "Object.preventExtensions(proxy1);\n"
                        + "result += ' ' + Object.isExtensible(o1) + '-' + Object.isExtensible(proxy1);\n"
                        + "var o2 = Object.seal({});\n"
                        + "var proxy2 = new Proxy(o2, {});\n"
                        + "result += ' ' + Object.isExtensible(o2) + '-' + Object.isExtensible(proxy2);\n";

        Utils.assertWithAllModes_ES6("true-true false-false false-false", js);
    }

    @Test
    public void preventExtensionsTrapReturnsNoBoolean() {
        String js =
                "var target = {};\n"
                        + "var p = new Proxy({}, {\n"
                        + "  preventExtensions: function(t) {\n"
                        + "    return 0;\n"
                        + "  }\n"
                        + "});\n"
                        + "var res = '' + Reflect.preventExtensions(p);\n"
                        + "Object.preventExtensions(target);\n"
                        + "res += ' ' + Reflect.preventExtensions(p);\n";
        Utils.assertWithAllModes_ES6("false false", js);
    }

    @Test
    public void preventExtensionsTrapIsUndefined() {
        String js =
                "var target = {};\n"
                        + "var p = new Proxy(target, {});\n"
                        + "'' + Reflect.preventExtensions(p);";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void ownKeys() {
        String js =
                "var o = { d: 42 };\n"
                        + "var res = '';\n"
                        + "var handler = {\n"
                        + "         ownKeys(target) {\n"
                        + "           res += (target == o);\n"
                        + "           return Reflect.ownKeys(target);"
                        + "         }\n"
                        + "       };\n"
                        + "var proxy1 = new Proxy(o, handler);\n"
                        + "var x = Object.keys(proxy1);\n"
                        + "res += ' ' + x;\n";
        Utils.assertWithAllModes_ES6("true d", js);
    }

    @Test
    public void ownKeysTrapUndefined() {
        String js =
                "var target = {\n"
                        + "  foo: 1,\n"
                        + "  bar: 2\n"
                        + "};\n"
                        + "var p = new Proxy(target, {});\n"
                        + "var keys = Object.getOwnPropertyNames(p);\n"
                        + "'' + keys[0] + ' ' + keys[1] + ' ' + keys.length";
        Utils.assertWithAllModes_ES6("foo bar 2", js);
    }

    @Test
    public void ownKeysArrayInTrapResult() {
        String js =
                "var p = new Proxy({}, {\n"
                        + "  ownKeys: function() {\n"
                        + "    return [ [] ];\n"
                        + "  }\n"
                        + "});\n"
                        + "try { Object.keys(p); } catch(e) { '' + e }\n";
        Utils.assertWithAllModes_ES6(
                "TypeError: proxy [[OwnPropertyKeys]] must return an array with only string and symbol elements",
                js);
    }

    @Test
    public void ownKeysWithoutHandler() {
        String js =
                "var o1 = {\n"
                        + "  p1: 42,\n"
                        + "  p2: 'one'\n"
                        + "};\n"
                        + "var a1 = [];\n"
                        + "var proxy1 = new Proxy(o1, {});\n"
                        + "var proxy2 = new Proxy(a1, {});\n"
                        + "'' + Object.keys(proxy1)"
                        + "+ ' ' + Object.keys(proxy2)";
        Utils.assertWithAllModes_ES6("p1,p2 ", js);
    }

    @Test
    public void ownKeysWithoutHandler2() {
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
                        + "var proxy1 = new Proxy(o1, {});\n"
                        + "'' + Object.keys(proxy1)";
        Utils.assertWithAllModes_ES6("0,6,8,55,773,s1,str,-1,s2,str2", js);
    }

    @Test
    public void ownKeysWithoutHandlerEmptyObj() {
        String js = "var proxy1 = new Proxy({}, {});\n" + "'' + Object.keys(proxy1).length";
        Utils.assertWithAllModes_ES6("0", js);
    }

    @Test
    public void ownKeysWithoutHandlerDeleteObj() {
        String js =
                "var o = { d: 42 };\n"
                        + "delete o.d;\n"
                        + "var proxy1 = new Proxy(o, {});\n"
                        + "'' + Object.keys(proxy1).length";
        Utils.assertWithAllModes_ES6("0", js);
    }

    @Test
    public void ownKeysWithoutHandlerEmptyArray() {
        String js = "var proxy1 = new Proxy([], {});\n" + "'' + Object.keys(proxy1)";
        Utils.assertWithAllModes_ES6("", js);
    }

    @Test
    public void ownKeysWithoutHandlerArray() {
        String js = "var proxy1 = new Proxy([, , 2], {});\n" + "'' + Object.keys(proxy1)";
        Utils.assertWithAllModes_ES6("2", js);
    }

    @Test
    public void ownKeysWithoutHandlerNotEnumerable() {
        String js =
                "var o = {};\n"
                        + "Object.defineProperty(o, 'p1', { value: 42, enumerable: false });\n"
                        + "Object.defineProperty(o, 'p2', { get: function() {}, enumerable: false });\n"
                        + "var proxy1 = new Proxy(o, {});\n"
                        + "'' + Object.keys(proxy1)";
        Utils.assertWithAllModes_ES6("", js);
    }

    @Test
    public void hasTargetNotExtensible() {
        String js =
                "var target = {};\n"
                        + "var handler = {\n"
                        + "  has: function(t, prop) {\n"
                        + "    return 0;\n"
                        + "  }\n"
                        + "};\n"
                        + "var p = new Proxy(target, handler);\n"
                        + "Object.defineProperty(target, 'attr', {\n"
                        + "  configurable: true,\n"
                        + "  extensible: false,\n"
                        + "  value: 1\n"
                        + "});\n"
                        + "Object.preventExtensions(target);\n"
                        + "try { 'attr' in p; } catch(e) { '' + e }\n";

        Utils.assertWithAllModes_ES6(
                "TypeError: proxy can't report an existing own property 'attr' as non-existent on a non-extensible object",
                js);
    }

    @Test
    public void hasHandlerCallsIn() {
        String js =
                "var _handler, _target, _prop;\n"
                        + "var target = {};\n"
                        + "var handler = {\n"
                        + "  has: function(t, prop) {\n"
                        + "    _handler = this;\n"
                        + "    _target = t;\n"
                        + "    _prop = prop;\n"
                        + "    return prop in t;\n"
                        + "  }\n"
                        + "};\n"
                        + "var p = new Proxy(target, handler);\n"
                        + "'' + (_handler === handler)\n"
                        + "+ ' ' + (_target === target)"
                        + "+ ' ' + ('attr' === _prop)"
                        + "+ ' ' + ('attr' in p)";

        Utils.assertWithAllModes_ES6("false false false false", js);
    }

    @Test
    public void hasWithoutHandler() {
        String js =
                "var o1 = { p: 42 }\n"
                        + "var proxy1 = new Proxy(o1, {});\n"
                        + "'' + ('p' in proxy1)"
                        + "+ ' ' + ('p2' in proxy1)"
                        + "+ ' ' + ('toString' in proxy1)";
        Utils.assertWithAllModes_ES6("true false true", js);
    }

    @Test
    public void hasSymbolWithoutHandler() {
        String js =
                "var s1 = Symbol('1');\n"
                        + "var s2 = Symbol('1');\n"
                        + "var o = {};\n"
                        + "o[s1] = 42;\n"
                        + "var proxy1 = new Proxy(o, {});\n"
                        + "'' + (s1 in proxy1)"
                        + "+ ' ' + (2 in proxy1)";
        Utils.assertWithAllModes_ES6("true false", js);
    }

    @Test
    public void getTrapIsNullTargetIsProxy() {
        String js =
                "var stringTarget = new Proxy(new String('str'), {});\n"
                        + "var stringProxy = new Proxy(stringTarget, {\n"
                        + "  get: null,\n"
                        + "});\n"
                        + "'' + stringProxy.length"
                        + " + ' ' + stringProxy[0]"
                        + " + ' ' + stringProxy[4];";
        Utils.assertWithAllModes_ES6("3 s undefined", js);
    }

    @Test
    public void getTrapIsNullTargetIsProxy2() {
        String js =
                "var sym = Symbol();\n"
                        + "var target = new Proxy({}, {\n"
                        + "  get: function(_target, key) {\n"
                        + "    switch (key) {\n"
                        + "      case sym: return 1;\n"
                        + "      case \"10\": return 2;\n"
                        + "      case \"foo\": return 3;\n"
                        + "    }\n"
                        + "  },\n"
                        + "});\n"
                        + "var proxy = new Proxy(target, {\n"
                        + "  get: null,\n"
                        + "});\n"
                        + "'' + proxy[sym]"
                        + " + ' ' + proxy[10]"
                        + " + ' ' + Object.create(proxy).foo"
                        + " + ' ' + proxy.bar;";
        Utils.assertWithAllModes_ES6("1 2 3 undefined", js);
    }

    @Test
    public void setTrap() {
        String js =
                "var res = '';\n"
                        + "var proxy = new Proxy({}, {\n"
                        + "  set(obj, prop, value) {\n"
                        + "    res += value + ' ' + (value instanceof Array);\n"
                        + "    obj[prop] = value;\n"
                        + "    return true;\n"
                        + "  },\n"
                        + "});\n"
                        + "proxy.foo = [1, 2, 3];\n"
                        + "res";

        Utils.assertWithAllModes_ES6("1,2,3 true", js);
    }

    @Test
    public void getPropertyByIntWithoutHandler() {
        String js = "var a = ['zero', 'one'];" + "var proxy1 = new Proxy(a, {});\n" + "proxy1[1];";
        Utils.assertWithAllModes_ES6("one", js);
    }

    @Test
    public void getProperty() {
        String js =
                "var o = {};\n"
                        + "o.p1 = 'value 1';\n"
                        + "var proxy1 = new Proxy(o, { get: function(t, prop) {\n"
                        + "                   return t[prop] + '!';\n"
                        + "               }});\n"
                        + "var result = ''\n;"
                        + "result += proxy1.p1;\n"
                        + "Object.defineProperty(o, 'p3', { get: function() { return 'foo'; } });\n"
                        + "result += ', ' + proxy1.p3;\n"
                        + "var o2 = Object.create({ p: 42 });\n"
                        + "var proxy2 = new Proxy(o2, {});\n"
                        + "result += ', ' + proxy2.p;\n"
                        + "result += ', ' + proxy2.u;\n";

        Utils.assertWithAllModes_ES6("value 1!, foo!, 42, undefined", js);
    }

    @Test
    public void getPropertyParameters() {
        String js =
                "var _target, _handler, _prop, _receiver;\n"
                        + "var target = {\n"
                        + "  attr: 1\n"
                        + "};\n"
                        + "var handler = {\n"
                        + "  get: function(t, prop, receiver) {\n"
                        + "    _handler = this;\n"
                        + "    _target = t;\n"
                        + "    _prop = prop;\n"
                        + "    _receiver = receiver;\n"
                        + "  }\n"
                        + "};\n"
                        + "var p = new Proxy(target, handler);\r\n"
                        + "p.attr;\n"
                        + "var res = '' + (_handler === handler)\n"
                        + "+ ' ' + (_target === target)"
                        + "+ ' ' + (_prop == 'attr')"
                        + "+ ' ' + (_receiver === p);"
                        + "_prop = null;\n"
                        + "p['attr'];\n"
                        + "res + ' ' + (_prop == 'attr')";

        Utils.assertWithAllModes_ES6("true true true true true", js);
    }

    @Test
    public void getPropertyWithoutHandler() {
        String js =
                "var o = {};\n"
                        + "o.p1 = 'value 1';\n"
                        + "var proxy1 = new Proxy(o, {});\n"
                        + "var result = ''\n;"
                        + "result += proxy1.p1;\n"
                        + "Object.defineProperty(o, 'p2', { get: undefined });\n"
                        + "result += ', ' + proxy1.p2;\n"
                        + "Object.defineProperty(o, 'p3', { get: function() { return 'foo'; } });\n"
                        + "result += ', ' + proxy1.p3;\n"
                        + "var o2 = Object.create({ p: 42 });\n"
                        + "var proxy2 = new Proxy(o2, {});\n"
                        + "result += ', ' + proxy2.p;\n"
                        + "result += ', ' + proxy2.u;\n";

        Utils.assertWithAllModes_ES6("value 1, undefined, foo, 42, undefined", js);
    }

    @Test
    public void getPrototypeOfNull() {
        String js =
                "var plainObjectTarget = new Proxy(Object.create(null), {});\n"
                        + "var plainObjectProxy = new Proxy(plainObjectTarget, {\n"
                        + "  getPrototypeOf: null,\n"
                        + "});\n"
                        + "'' + Object.getPrototypeOf(plainObjectProxy);\n";
        Utils.assertWithAllModes_ES6("null", js);
    }

    @Test
    public void setPrototypeOfWithoutHandler() {
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
    public void setPrototypeOfCycleWithoutHandler() {
        String js = "var o1 = {};\n" + "'' + Reflect.setPrototypeOf(o1, o1);\n";
        Utils.assertWithAllModes_ES6("false", js);
    }

    @Test
    public void setPrototypeOfCycleComplexWithoutHandler() {
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
    public void setPrototypeOfSameWithoutHandler() {
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

    @Test
    public void typeof() {
        Utils.assertWithAllModes_ES6("object", "typeof new Proxy({}, {})");
        Utils.assertWithAllModes_ES6("function", "typeof new Proxy(function() {}, {})");
    }

    @Test
    public void typeofRevocable() {
        Utils.assertWithAllModes_ES6(
                "object", "var rev = Proxy.revocable({}, {}); rev.revoke(); typeof rev.proxy");
        Utils.assertWithAllModes_ES6(
                "function",
                "var rev = Proxy.revocable(function() {}, {}); rev.revoke(); typeof rev.proxy");

        String js =
                "var revocableTarget = Proxy.revocable(function() {}, {});\n"
                        + "revocableTarget.revoke();\n"
                        + "var revocable = Proxy.revocable(revocableTarget.proxy, {});\n"
                        + "'' + typeof revocable.proxy;\n";
        Utils.assertWithAllModes_ES6("function", js);
    }

    @Test
    public void revocableFunctionIsAnonymous() {
        String js =
                "var rev = Proxy.revocable({}, {}).revoke;\n"
                        + "var desc = Object.getOwnPropertyDescriptor(rev, 'name');\n"
                        + "'' + desc.name + ' ' + desc.value "
                        + "+ ' ' + desc.writable "
                        + "+ ' ' + desc.enumerable "
                        + "+ ' ' + desc.configurable";
        Utils.assertWithAllModes_ES6("undefined  false false true", js);
    }

    @Test
    public void revocableGetPrototypeOf() {
        Utils.assertWithAllModes_ES6(
                "TypeError: Illegal operation attempted on a revoked proxy",
                "var rev = Proxy.revocable({}, {}); rev.revoke(); "
                        + "try { Object.getPrototypeOf(rev.proxy); } catch(e) { '' + e }");
    }

    @Test
    public void keysFailsWithOwnKeysOverwritten() {
        String js =
                "var target = {};\n"
                        + "Object.defineProperty(target, 'prop', {\n"
                        + "  value: 1,\n"
                        + "  writable: true,\n"
                        + "  enumerable: false,\n"
                        + "  configurable: false,\n"
                        + "});\n"
                        + "var proxy = new Proxy(target, {\n"
                        + "  ownKeys: function() {\n"
                        + "    return [];\n"
                        + "  },\n"
                        + "});\n"
                        + "try {"
                        + "  Object.keys(proxy);\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}";

        Utils.assertWithAllModes_ES6(
                "TypeError: proxy can't skip a non-configurable property 'prop'", js);
    }

    @Test
    public void keysPreventExtensions() {
        String js =
                "var target = {};\n"
                        + "Object.defineProperty(target, 'prop', {\n"
                        + "  value: 3,\n"
                        + "  writable: true,\n"
                        + "  enumerable: false,\n"
                        + "  configurable: true,\n"
                        + "});\n"
                        + "var proxy = new Proxy(target, {\n"
                        + "  ownKeys: function() {\n"
                        + "    return ['prop'];\n"
                        + "  },\n"
                        + "});\n"
                        + "Object.preventExtensions(target);\n"
                        + "try {"
                        + "  Object.keys(proxy).length === 0;\n"
                        + "} catch(e) {\n"
                        + "  '' + e;\n"
                        + "}";

        Utils.assertWithAllModes_ES6(true, js);
    }
}
