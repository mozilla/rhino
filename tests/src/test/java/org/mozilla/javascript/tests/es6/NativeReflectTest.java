package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
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

    @Test
    public void setWithoutReceiver() {
        // Basic sanity: no receiver argument, value lands on target.
        String js = "var o = { p: 0 };\n" + "Reflect.set(o, 'p', 42);\n" + "'' + o.p;";
        Utils.assertWithAllModes_ES6("42", js);
    }

    @Test
    public void setWithReceiverSameAsTarget() {
        // receiver === target: ordinary path, value lands on the object.
        String js = "var o = { p: 0 };\n" + "'' + Reflect.set(o, 'p', 99, o) + ' ' + o.p;";
        Utils.assertWithAllModes_ES6("true 99", js);
    }

    @Test
    public void setWithReceiverDifferentFromTarget_writableDataProp() {
        // OrdinarySetWithOwnDescriptor step 1.d.iii:
        // target has writable data property, receiver is a different object.
        // Value must be written to the receiver via [[DefineOwnProperty]], not [[Set]].
        String js =
                "var target = { p: 0 };\n"
                        + "var receiver = { p: 10 };\n"
                        + "var result = Reflect.set(target, 'p', 42, receiver);\n"
                        + "'' + result + ' ' + target.p + ' ' + receiver.p;";
        // target unchanged, receiver updated, returns true
        Utils.assertWithAllModes_ES6("true 0 42", js);
    }

    @Test
    public void setWithReceiverDifferentFromTarget_newPropOnReceiver() {
        // OrdinarySetWithOwnDescriptor step 1.e (CreateDataProperty):
        // target has writable data property, receiver does NOT own that property yet.
        String js =
                "var target = { p: 0 };\n"
                        + "var receiver = {};\n"
                        + "var result = Reflect.set(target, 'p', 42, receiver);\n"
                        + "'' + result + ' ' + target.p + ' ' + receiver.p;";
        // target unchanged, new property created on receiver
        Utils.assertWithAllModes_ES6("true 0 42", js);
    }

    @Test
    public void setWithReceiverDifferentFromTarget_nonWritableOwnDescOnTarget() {
        // OrdinarySetWithOwnDescriptor step 1.a: target property is non-writable → false.
        String js =
                "var target = {};\n"
                        + "Object.defineProperty(target, 'p', { value: 0, writable: false, configurable: true });\n"
                        + "var receiver = {};\n"
                        + "'' + Reflect.set(target, 'p', 42, receiver);";
        Utils.assertWithAllModes_ES6("false", js);
    }

    @Test
    public void setWithReceiverDifferentFromTarget_nonWritableOwnDescOnReceiver() {
        // OrdinarySetWithOwnDescriptor step 1.d.ii:
        // receiver already owns the property but it is non-writable → false.
        String js =
                "var target = { p: 0 };\n"
                        + "var receiver = {};\n"
                        + "Object.defineProperty(receiver, 'p', { value: 1, writable: false, configurable: true });\n"
                        + "'' + Reflect.set(target, 'p', 42, receiver);";
        Utils.assertWithAllModes_ES6("false", js);
    }

    @Test
    public void setWithReceiverDifferentFromTarget_accessorDescOnReceiverReturnsFalse() {
        // OrdinarySetWithOwnDescriptor step 1.d.i:
        // receiver owns an accessor for that property → false.
        String js =
                "var target = { p: 0 };\n"
                        + "var receiver = {};\n"
                        + "Object.defineProperty(receiver, 'p', { get() { return 1; }, configurable: true });\n"
                        + "'' + Reflect.set(target, 'p', 42, receiver);";
        Utils.assertWithAllModes_ES6("false", js);
    }

    @Test
    public void setWithReceiverDifferentFromTarget_accessorWithSetter() {
        // OrdinarySetWithOwnDescriptor step 3: target has accessor with setter,
        // setter must be called with receiver as 'this'.
        String js =
                "var log = [];\n"
                        + "var target = {};\n"
                        + "Object.defineProperty(target, 'p', {\n"
                        + "  get() { return 0; },\n"
                        + "  set(v) { log.push(this === receiver ? 'receiver' : 'other', v); },\n"
                        + "  configurable: true\n"
                        + "});\n"
                        + "var receiver = {};\n"
                        + "var result = Reflect.set(target, 'p', 7, receiver);\n"
                        + "'' + result + ' ' + log;";
        Utils.assertWithAllModes_ES6("true receiver,7", js);
    }

    @Test
    public void setWithReceiverDifferentFromTarget_accessorNoSetterReturnsFalse() {
        // OrdinarySetWithOwnDescriptor step 4: target has accessor with no setter → false.
        String js =
                "var target = {};\n"
                        + "Object.defineProperty(target, 'p', { get() { return 0; }, configurable: true });\n"
                        + "var receiver = {};\n"
                        + "'' + Reflect.set(target, 'p', 7, receiver);";
        Utils.assertWithAllModes_ES6("false", js);
    }

    @Test
    public void setWithReceiverPreservesExistingAttributesOnReceiver() {
        // When receiver already owns the (writable) property, [[DefineOwnProperty]]
        // with {[[Value]]: V} must preserve writable/enumerable/configurable.
        String js =
                "var target = { p: 0 };\n"
                        + "var receiver = {};\n"
                        + "Object.defineProperty(receiver, 'p', {\n"
                        + "  value: 1, writable: true, enumerable: false, configurable: false\n"
                        + "});\n"
                        + "Reflect.set(target, 'p', 42, receiver);\n"
                        + "var d = Object.getOwnPropertyDescriptor(receiver, 'p');\n"
                        + "'' + d.value + ' ' + d.writable + ' ' + d.enumerable + ' ' + d.configurable;";
        // value updated, other attributes unchanged
        Utils.assertWithAllModes_ES6("42 true false false", js);
    }

    @Test
    public void setTrapReturnsTrueForConfigurableNonWritableProperty() {
        // Reflect.set(proxy, "attr", "foo") must return true when the
        // set trap returns true and the target property is configurable (even though
        // it is non-writable). The OrdinarySetWithOwnDescriptor non-writable check
        // must NOT run when the target itself is the proxy — the trap result is final.
        // See: ECMAScript [[Set]] 10.5.9 step 11 — invariant only applies when
        // targetDesc.[[Configurable]] is false.
        String js =
                "var target = {};\n"
                        + "var handler = {\n"
                        + "  set: function(t, prop, value, receiver) {\n"
                        + "    return true;\n"
                        + "  }\n"
                        + "};\n"
                        + "var p = new Proxy(target, handler);\n"
                        + "Object.defineProperty(target, 'attr', {\n"
                        + "  configurable: true,\n"
                        + "  writable: false,\n"
                        + "  value: 'foo'\n"
                        + "});\n"
                        + "'' + Reflect.set(p, 'attr', 'foo');";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void applyThisArgumentMatchesCall_number() {
        // Reflect.apply must not add extra boxing on top of what the call
        // machinery does. Whatever typeof this fn.call(42) gives,
        // Reflect.apply(fn, 42, []) must give the same result.
        String js =
                "function getThis() { 'use strict'; return typeof this; }\n"
                        + "var viaCall    = getThis.call(42);\n"
                        + "var viaReflect = Reflect.apply(getThis, 42, []);\n"
                        + "'' + (viaCall === viaReflect);";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void applyThisArgumentMatchesCall_string() {
        String js =
                "function getThis() { 'use strict'; return typeof this; }\n"
                        + "var viaCall    = getThis.call('hello');\n"
                        + "var viaReflect = Reflect.apply(getThis, 'hello', []);\n"
                        + "'' + (viaCall === viaReflect);";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void applyThisArgumentMatchesCall_null() {
        String js =
                "function getThis() { 'use strict'; return this === null; }\n"
                        + "var viaCall    = getThis.call(null);\n"
                        + "var viaReflect = Reflect.apply(getThis, null, []);\n"
                        + "'' + (viaCall === viaReflect);";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void applyThisArgumentMatchesCall_undefined() {
        String js =
                "function getThis() { 'use strict'; return this === undefined; }\n"
                        + "var viaCall    = getThis.call(undefined);\n"
                        + "var viaReflect = Reflect.apply(getThis, undefined, []);\n"
                        + "'' + (viaCall === viaReflect);";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void applyThisArgumentMatchesCall_object() {
        // Object thisArg: must be the exact same reference in both cases.
        String js =
                "var obj = {};\n"
                        + "function getThis() { 'use strict'; return this === obj; }\n"
                        + "var viaCall    = getThis.call(obj);\n"
                        + "var viaReflect = Reflect.apply(getThis, obj, []);\n"
                        + "'' + viaCall + ' ' + viaReflect;";
        Utils.assertWithAllModes_ES6("true true", js);
    }

    @Test
    public void definePropertyReturnsFalseWhenTrapReturnsFalse() {
        // On a proxy whose defineProperty trap returns false, Reflect.defineProperty
        // must return false — this is the legitimate false path, not an error.
        String js =
                "var p = new Proxy({}, {\n"
                        + "  defineProperty() { return false; }\n"
                        + "});\n"
                        + "'' + Reflect.defineProperty(p, 'x', { value: 1 });";
        Utils.assertWithAllModes_ES6("false", js);
    }

    @Test
    public void definePropertyReturnsTrueOnSuccess() {
        String js =
                "var o = {};\n"
                        + "'' + Reflect.defineProperty(o, 'p', { value: 42, configurable: true });";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void getReceiverDefaultsToTarget() {
        // When no receiver is supplied, getter sees target as 'this'.
        String js =
                "var o = {};\n"
                        + "Object.defineProperty(o, 'p', {\n"
                        + "  get() { return this === o; }\n"
                        + "});\n"
                        + "'' + Reflect.get(o, 'p');";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void getReceiverPassedToGetter() {
        // When a receiver is supplied, getter sees receiver as 'this', not target.
        String js =
                "var target = {};\n"
                        + "var receiver = {};\n"
                        + "Object.defineProperty(target, 'p', {\n"
                        + "  get() { return this === receiver; }\n"
                        + "});\n"
                        + "'' + Reflect.get(target, 'p', receiver);";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void getReceiverPassedToGetterOnPrototype() {
        // Getter is on the prototype, receiver is the leaf object — 'this' must
        // still be the receiver, not the prototype where the getter lives.
        String js =
                "var receiver = {};\n"
                        + "var proto = {};\n"
                        + "Object.defineProperty(proto, 'p', {\n"
                        + "  get() { return this === receiver; }\n"
                        + "});\n"
                        + "var target = Object.create(proto);\n"
                        + "'' + Reflect.get(target, 'p', receiver);";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void getReceiverDoesNotAffectDataProperty() {
        // Receiver has no effect on plain data property reads — value comes from
        // the prototype chain of target, not receiver.
        String js =
                "var target = { p: 42 };\n"
                        + "var receiver = { p: 99 };\n"
                        + "'' + Reflect.get(target, 'p', receiver);";
        Utils.assertWithAllModes_ES6("42", js);
    }

    @Test
    public void getReceiverWithSymbolKey() {
        // Receiver must also be forwarded for Symbol-keyed getters.
        String js =
                "var sym = Symbol('test');\n"
                        + "var target = {};\n"
                        + "var receiver = {};\n"
                        + "Object.defineProperty(target, sym, {\n"
                        + "  get() { return this === receiver; }\n"
                        + "});\n"
                        + "'' + Reflect.get(target, sym, receiver);";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void setPlainFallbackConsultsTargetChainNotReceiverChain() {
        // target has no own 'p'. 'p' lives on target's prototype (writable).
        // receiver has no own 'p'. receiver's prototype has a non-writable 'p'.
        //
        // Spec: target.[[Set]]('p', 42, receiver)
        //   → no own desc on target → walk target's proto chain
        //   → finds writable 'p' on targetProto → OrdinarySetWithOwnDescriptor
        //   → receiver has no own 'p' → CreateDataProperty(receiver, 'p', 42) ← own write
        String js =
                "var targetProto = { p: 1 };\n"
                        + "var target = Object.create(targetProto);\n"
                        + "var receiverProto = {};\n"
                        + "Object.defineProperty(receiverProto, 'p', {\n"
                        + "  value: 0, writable: false, configurable: true\n"
                        + "});\n"
                        + "var receiver = Object.create(receiverProto);\n"
                        + "var result = Reflect.set(target, 'p', 42, receiver);\n"
                        + "'' + result + ' ' + receiver.hasOwnProperty('p') + ' ' + receiver.p;";
        Utils.assertWithAllModes_ES6("true true 42", js);
    }

    @Test
    public void setPlainFallbackNoOwnPropCreatesOwnOnReceiver() {
        // Neither target nor receiver has own 'p'. No proto chain interference.
        // Spec: target.[[Set]] → no own on target (or proto)
        // → CreateDataProperty(receiver, 'p', 42).
        // receiver must end up with its own 'p'; target must be untouched.
        String js =
                "var target = {};\n"
                        + "var receiver = {};\n"
                        + "var result = Reflect.set(target, 'p', 42, receiver);\n"
                        + "'' + result\n"
                        + "+ ' ' + receiver.hasOwnProperty('p')\n"
                        + "+ ' ' + receiver.p\n"
                        + "+ ' ' + target.hasOwnProperty('p');";
        Utils.assertWithAllModes_ES6("true true 42 false", js);
    }

    @Test
    public void setPlainFallbackIndexKeyConsultsTargetChain() {
        // Same as setPlainFallbackConsultsTargetChainNotReceiverChain but with integer key.
        String js =
                "var targetProto = [, , 99];\n"
                        + "var target = Object.create(targetProto);\n"
                        + "var receiverProto = [];\n"
                        + "Object.defineProperty(receiverProto, '2', {\n"
                        + "  value: 0, writable: false, configurable: true\n"
                        + "});\n"
                        + "var receiver = Object.create(receiverProto);\n"
                        + "var result = Reflect.set(target, 2, 42, receiver);\n"
                        + "'' + result + ' ' + receiver.hasOwnProperty('2') + ' ' + receiver[2];";
        Utils.assertWithAllModes_ES6("true true 42", js);
    }

    @Test
    public void setPlainFallbackSymbolKeyConsultsTargetChain() {
        // Same scenario for Symbol keys.
        String js =
                "var sym = Symbol('p');\n"
                        + "var targetProto = {};\n"
                        + "targetProto[sym] = 1;\n"
                        + "var target = Object.create(targetProto);\n"
                        + "var receiverProto = {};\n"
                        + "Object.defineProperty(receiverProto, sym, {\n"
                        + "  value: 0, writable: false, configurable: true\n"
                        + "});\n"
                        + "var receiver = Object.create(receiverProto);\n"
                        + "var result = Reflect.set(target, sym, 42, receiver);\n"
                        + "'' + result + ' ' + receiver.hasOwnProperty(sym) + ' ' + receiver[sym];";
        Utils.assertWithAllModes_ES6("true true 42", js);
    }

    @Test
    public void setFallbackShouldCallSetterFromTargetProtoNotCreateOwnOnReceiver() {
        // target has no own 'p'. target's prototype has an accessor (setter) for 'p'.
        // receiver has no own 'p' and is extensible.
        //
        // Spec: target.[[Set]]('p', 42, receiver)
        //   → no own desc on target → walk target's proto chain
        //   → finds accessor on targetProto → call setter with receiver as 'this'
        //   → return true, receiver gets NO own 'p' (setter was called instead)
        String js =
                "var log = [];\n"
                        + "var targetProto = {};\n"
                        + "Object.defineProperty(targetProto, 'p', {\n"
                        + "  set: function(v) { log.push('setter:' + v); },\n"
                        + "  configurable: true\n"
                        + "});\n"
                        + "var target = Object.create(targetProto);\n"
                        + "var receiver = {};\n"
                        + "var result = Reflect.set(target, 'p', 42, receiver);\n"
                        // setter must be called, receiver must NOT get an own 'p'
                        + "'' + result + ' ' + log + ' ' + receiver.hasOwnProperty('p');";
        Utils.assertWithAllModes_ES6("true setter:42 false", js);
    }

    @Test
    public void setFallbackShouldReturnFalseForNonWritableOnTargetProto() {
        // target has no own 'p'. target's prototype has a NON-WRITABLE 'p'.
        // receiver has no own 'p'.
        //
        // Spec: target.[[Set]]('p', 42, receiver)
        //   → no own on target → walk target's proto chain
        //   → finds non-writable data 'p' on targetProto
        //   → OrdinarySetWithOwnDescriptor: writable=false → return false
        //   → receiver gets NO own 'p'
        String js =
                "var targetProto = {};\n"
                        + "Object.defineProperty(targetProto, 'p', {\n"
                        + "  value: 1, writable: false, configurable: true\n"
                        + "});\n"
                        + "var target = Object.create(targetProto);\n"
                        + "var receiver = {};\n"
                        + "var result = Reflect.set(target, 'p', 42, receiver);\n"
                        + "'' + result + ' ' + receiver.hasOwnProperty('p');";
        Utils.assertWithAllModes_ES6("false false", js);
    }

    @Test
    public void setMissingValueArgumentTreatedAsUndefined() {
        // Reflect.set(target, key) — value argument is omitted.
        // Spec: missing arguments are undefined, so this is equivalent to
        // Reflect.set(target, 'p', undefined).
        String js =
                "var target = {};\n"
                        + "var result = Reflect.set(target, 'p');\n"
                        + "'' + result + ' ' + target.p + ' ' + (target.p === undefined);";
        Utils.assertWithAllModes_ES6("true undefined true", js);
    }

    @Test
    public void setMissingValueWithReceiverTreatedAsUndefined() {
        // Reflect.set(target, key, <missing>, receiver) is not reachable from JS
        // (you cannot pass a 4th arg without a 3rd), but the missing-value case
        // with an explicit receiver via Reflect.set(target, key) still applies.
        // Verifies the set trap also receives undefined as the value.
        String js =
                "var log = [];\n"
                        + "var proxy = new Proxy({}, {\n"
                        + "  set: function(t, prop, value, receiver) {\n"
                        + "    log.push(value === undefined);\n"
                        + "    return true;\n"
                        + "  }\n"
                        + "});\n"
                        + "Reflect.set(proxy, 'p');\n"
                        + "'' + log;";
        Utils.assertWithAllModes_ES6("true", js);
    }

    @Test
    public void setTrapCalledWhenReceiverDiffersFromProxy_stringKey() {
        // Reflect.set(proxy, 'p', 42, otherReceiver) — receiver != proxy.
        // The proxy's set trap MUST still be invoked, with otherReceiver as
        // the 4th trap argument.
        String js =
                "var log = [];\n"
                        + "var target = {};\n"
                        + "var otherReceiver = {};\n"
                        + "var proxy = new Proxy(target, {\n"
                        + "  set: function(t, prop, value, receiver) {\n"
                        + "    log.push('trap:' + prop + ':' + value\n"
                        + "             + ':' + (receiver === otherReceiver));\n"
                        + "    return true;\n"
                        + "  }\n"
                        + "});\n"
                        + "Reflect.set(proxy, 'p', 42, otherReceiver);\n"
                        + "'' + log;";
        Utils.assertWithAllModes_ES6("trap:p:42:true", js);
    }

    @Test
    public void setTrapCalledWhenReceiverDiffersFromProxy_indexKey() {
        // Same as above but with an integer key.
        String js =
                "var log = [];\n"
                        + "var target = [];\n"
                        + "var otherReceiver = [];\n"
                        + "var proxy = new Proxy(target, {\n"
                        + "  set: function(t, prop, value, receiver) {\n"
                        + "    log.push('trap:' + prop + ':' + value\n"
                        + "             + ':' + (receiver === otherReceiver));\n"
                        + "    return true;\n"
                        + "  }\n"
                        + "});\n"
                        + "Reflect.set(proxy, 0, 99, otherReceiver);\n"
                        + "'' + log;";
        Utils.assertWithAllModes_ES6("trap:0:99:true", js);
    }

    @Test
    public void setTrapCalledWhenReceiverDiffersFromProxy_symbolKey() {
        // Same as above but with a Symbol key.
        String js =
                "var log = [];\n"
                        + "var sym = Symbol('s');\n"
                        + "var target = {};\n"
                        + "var otherReceiver = {};\n"
                        + "var proxy = new Proxy(target, {\n"
                        + "  set: function(t, prop, value, receiver) {\n"
                        + "    if (prop === sym) {\n"
                        + "      log.push('trap:' + value\n"
                        + "               + ':' + (receiver === otherReceiver));\n"
                        + "    }\n"
                        + "    return true;\n"
                        + "  }\n"
                        + "});\n"
                        + "Reflect.set(proxy, sym, 42, otherReceiver);\n"
                        + "'' + log;";
        Utils.assertWithAllModes_ES6("trap:42:true", js);
    }

    @Test
    public void setTrapReturnFalseHonouredWhenReceiverDiffers() {
        // When the set trap returns false, Reflect.set must return false —
        // even when receiver != proxy.
        String js =
                "var target = {};\n"
                        + "var otherReceiver = {};\n"
                        + "var proxy = new Proxy(target, {\n"
                        + "  set: function(t, prop, value, receiver) {\n"
                        + "    return false;\n"
                        + "  }\n"
                        + "});\n"
                        + "'' + Reflect.set(proxy, 'p', 42, otherReceiver);";
        Utils.assertWithAllModes_ES6("false", js);
    }

    @Test
    public void setTrapNotCalledWhenNoTrapAndReceiverDiffers() {
        // No set trap defined — the value must still end up written correctly.
        String js =
                "var target = { p: 1 };\n"
                        + "var otherReceiver = {};\n"
                        + "var proxy = new Proxy(target, {});\n"
                        + "var result = Reflect.set(proxy, 'p', 42, otherReceiver);\n"
                        + "'' + result\n"
                        + "+ ' target=' + target.p\n"
                        + "+ ' receiver=' + otherReceiver.p;";
        Utils.assertWithAllModes_ES6("true target=1 receiver=42", js);
    }

    @Test
    public void setReturnsFlaseNotThrowWhenReceiverDefinePropertyWouldThrow_nonExtensible() {
        // target has own writable 'p'. receiver is non-extensible and does NOT
        // have own 'p'. OrdinarySetWithOwnDescriptor step 1.e calls
        // CreateDataProperty(receiver, 'p', 42) → Receiver.[[DefineOwnProperty]].
        // On a non-extensible receiver that has no own 'p', [[DefineOwnProperty]]
        // must return false per spec. Rhino throws an EcmaError instead, so
        // Reflect.set must catch it and return false rather than propagating the throw.
        String js =
                "var target = { p: 1 };\n"
                        + "var receiver = Object.preventExtensions({});\n"
                        + "try {\n"
                        + "  var result = Reflect.set(target, 'p', 42, receiver);\n"
                        + "  '' + result + ' ' + receiver.hasOwnProperty('p');\n"
                        + "} catch(e) {\n"
                        + "  'threw: ' + e;\n"
                        + "}";
        Utils.assertWithAllModes_ES6("false false", js);
    }

    @Test
    public void setReturnsFalseNotThrowWhenReceiverDefinePropertyWouldThrow_nonConfigurable() {
        // target has own writable 'p'. receiver has own non-writable non-configurable 'p'.
        // OrdinarySetWithOwnDescriptor step 1.d.iii calls [[DefineOwnProperty]] with
        // { [[Value]]: 42 } on receiver. The existing slot is non-configurable and
        // non-writable so [[DefineOwnProperty]] must return false. Rhino throws instead.
        String js =
                "var target = { p: 1 };\n"
                        + "var receiver = {};\n"
                        + "Object.defineProperty(receiver, 'p', {\n"
                        + "  value: 0, writable: false, configurable: false\n"
                        + "});\n"
                        + "try {\n"
                        + "  var result = Reflect.set(target, 'p', 42, receiver);\n"
                        + "  '' + result + ' ' + receiver.p;\n"
                        + "} catch(e) {\n"
                        + "  'threw: ' + e;\n"
                        + "}";
        Utils.assertWithAllModes_ES6("false 0", js);
    }

    @Test
    public void setReturnsFalseNotThrowWhenCreateDataPropertyFailsOnNonExtensibleReceiver() {
        // target has own writable 'p'. receiver is non-extensible with NO own 'p'.
        //
        // OrdinarySetWithOwnDescriptor flow:
        //   - ownDesc (from target) is writable data         → step 1.a passes
        //   - receiver is a ScriptableObject                 → step 1.b passes
        //   - existingDesc on receiver is null (no own 'p')  → step 1.d skipped
        //   - step 1.e: CreateDataProperty(receiver, 'p', 42)
        //     → Receiver.[[DefineOwnProperty]] on a non-extensible object
        //     → spec: returns false
        //     → Rhino: throws EcmaError
        //   → Reflect.set must return false, not propagate the throw.
        String js =
                "var target = { p: 1 };\n"
                        + "var receiver = Object.preventExtensions({});\n"
                        + "try {\n"
                        + "  var result = Reflect.set(target, 'p', 42, receiver);\n"
                        + "  '' + result + ' ' + receiver.hasOwnProperty('p');\n"
                        + "} catch(e) {\n"
                        + "  'threw: ' + e;\n"
                        + "}";
        Utils.assertWithAllModes_ES6("false false", js);
    }
}
