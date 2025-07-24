/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Test for NativeObject __proto__ property. Spec:
 * https://tc39.es/ecma262/multipage/fundamental-objects.html#sec-object.prototype.__proto__ Hints:
 * https://exploringjs.com/es6/ch_oop-besides-classes.html#sec_proto
 *
 * <p>Test are written with asserts for older language versions, because the old code has some
 * switches for the used versions. This should help to maintain backward compatibility.
 */
public class ProtoProperty2Test {

    @Test
    public void protoGet() {
        String script =
                "var a = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += a.__proto__ === Object.getPrototypeOf(a);"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === a.__proto__;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === Object.getPrototypeOf(a);"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === Object.getPrototypeOf(a);";

        Utils.assertWithAllModes_1_5("true / false / false / false", script);
        Utils.assertWithAllModes_1_8("true / false / false / false", script);
        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoGetDebug() {
        String script =
                "var res = '';"
                        + "var desc = Object.getOwnPropertyDescriptor(Object.prototype, '__proto__');\n"
                        + "if (desc) {"
                        + "  var get = desc.get;\n"
                        + "  var proto = {};\n"
                        + "  var withCustomProto = Object.create(proto);\n"
                        + "  var withNullProto = Object.create(null);\n"
                        + "  res += get.call({}) == Object.prototype;\n"
                        + "  res += get.call(withCustomProto) === proto;\n"
                        + "  res += get.call(withNullProto) === null;"
                        + "} else { res += 'desc undefined'; }";

        Utils.assertWithAllModes_1_5("desc undefined", script);
        Utils.assertWithAllModes_1_8("desc undefined", script);
        Utils.assertWithAllModes_ES6("truetruetrue", script);
    }

    @Test
    public void protoFunctionGet() {
        String script =
                "var a = function (o) { return o; };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += a.__proto__ === Object.getPrototypeOf(a);"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === a.__proto__;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === Object.getPrototypeOf(a);"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === Object.getPrototypeOf(a);";

        Utils.assertWithAllModes_1_5("true / false / false / false", script);
        Utils.assertWithAllModes_1_8("true / false / false / false", script);
        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoSymbolGet() {
        String script =
                "var a = Symbol('sym');"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += a.__proto__ === Object.getPrototypeOf(a);"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === a.__proto__;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === Object.getPrototypeOf(a);"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === Object.getPrototypeOf(a);";

        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoNullGet() {
        String script =
                "var a = null;"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "try { res += a.__proto__; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += ' / ';"
                        + "try { res += a['__proto__']; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += ' / ';"
                        + "try { res += a[s1 + s2 + s1]; } catch(e) { res += 'e-' + (e instanceof TypeError); }";

        Utils.assertWithAllModes_1_5("e-true / e-true / e-true", script);
        Utils.assertWithAllModes_1_8("e-true / e-true / e-true", script);
        Utils.assertWithAllModes_ES6("e-true / e-true / e-true", script);
    }

    @Test
    public void protoUndefinedGet() {
        String script =
                "var a = undefined;"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "try { res += a.__proto__; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += ' / ';"
                        + "try { res += a['__proto__']; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += ' / ';"
                        + "try { res += a[s1 + s2 + s1]; } catch(e) { res += 'e-' + (e instanceof TypeError); }";

        Utils.assertWithAllModes_1_5("e-true / e-true / e-true", script);
        Utils.assertWithAllModes_1_8("e-true / e-true / e-true", script);
        Utils.assertWithAllModes_ES6("e-true / e-true / e-true", script);
    }

    @Test
    public void protoNullPrototypeObjectsGet() {
        String script =
                "var a = Object.create(null);"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += Object.getPrototypeOf(a);"
                        + "res += ' / ';"
                        + "res += a.__proto__;"
                        + "res += ' / ';"
                        + "try { res += a.__proto__; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += ' / ';"
                        + "try { res += a['__proto__']; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += ' / ';"
                        + "try { res += a[s1 + s2 + s1]; } catch(e) { res += 'e-' + (e instanceof TypeError); }";

        Utils.assertWithAllModes_1_5("null / null / null / undefined / undefined", script);
        Utils.assertWithAllModes_1_8("null / null / null / undefined / undefined", script);
        Utils.assertWithAllModes_ES6(
                "null / undefined / undefined / undefined / undefined", script);
    }

    @Test
    public void protoNumberGet() {
        String script =
                "var a = 4711;"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += a.__proto__ === Number.prototype;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === a.__proto__;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === Number.prototype;"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === Number.prototype;";

        Utils.assertWithAllModes_1_5("true / false / false / false", script);
        Utils.assertWithAllModes_1_8("true / false / false / false", script);
        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoStringGet() {
        String script =
                "var a = '4711';"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += a.__proto__ === String.prototype;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === a.__proto__;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === String.prototype;"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === String.prototype;";

        Utils.assertWithAllModes_1_5("true / false / false / false", script);
        Utils.assertWithAllModes_1_8("true / false / false / false", script);
        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoBoolGet() {
        String script =
                "var a = false;"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += a.__proto__ === Boolean.prototype;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === a.__proto__;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === Boolean.prototype;"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === Boolean.prototype;";

        Utils.assertWithAllModes_1_5("true / false / false / false", script);
        Utils.assertWithAllModes_1_8("true / false / false / false", script);
        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoSuperGet() {
        String script =
                "var res = '';"
                        + "var a = {x: 'a'};"
                        + "var b = {x: 'b'};"
                        + "var c = {x: 'c', "
                        + "  f() {"
                        + "    res +=  this.__proto__ === b;"
                        + "    res +=  super.__proto__ === b;"
                        + "    res +=  super['__proto__'] === b;"
                        + "    res +=  super.__proto__.x;"
                        + "  }};"
                        + "Object.setPrototypeOf(c, b);"
                        + "Object.setPrototypeOf(b, a);"
                        + "c.f();"
                        + "res";

        Utils.assertWithAllModes_ES6("truetruetrueb", script);
    }

    @Test
    public void protoSet() {
        String script =
                "var a = {};"
                        + "var p = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += (a.__proto__ = p) === p;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "res += (a['__proto__'] = p) === p;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "res += (a[s1 + s2 + s1] = p) === p;"
                        + "res += '-';"
                        + ""
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_1_5("true-truefalse / true-falsetrue / true-falsetrue", script);
        Utils.assertWithAllModes_1_8("true-truefalse / true-falsetrue / true-falsetrue", script);
        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoFunctionSet() {
        String script =
                "var a = function (o) { return o; };"
                        + "var p = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += (a.__proto__ = p) === p;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = function (o) { return o; };"
                        + "res += (a['__proto__'] = p) === p;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = function (o) { return o; };"
                        + "res += (a[s1 + s2 + s1] = p) === p;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_1_5("true-truefalse / true-falsetrue / true-falsetrue", script);
        Utils.assertWithAllModes_1_8("true-truefalse / true-falsetrue / true-falsetrue", script);
        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoSymbolSet() {
        String script =
                "var a = Symbol('sym');"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var newP = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += (a.__proto__ = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = Symbol('sym');"
                        + "res += (a['__proto__'] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = Symbol('sym');"
                        + "res += (a[s1 + s2 + s1] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoNullSet() {
        String script =
                "var a = null;"
                        + "var newP = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "try { res += a.__proto__ = newP; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += ' / ';"
                        + "a = null;"
                        + "try { res += a['__proto__'] = newP; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += ' / ';"
                        + "a = null;"
                        + "try { res += a[s1 + s2 + s1] = newP; } catch(e) { res += 'e-' + (e instanceof TypeError); }";

        Utils.assertWithAllModes_1_5("e-true / e-true / e-true", script);
        Utils.assertWithAllModes_1_8("e-true / e-true / e-true", script);
        Utils.assertWithAllModes_ES6("e-true / e-true / e-true", script);
    }

    @Test
    public void protoCallSetter() {
        String script =
                "var newP = {};"
                        + "var res = '';"
                        + "if (Object.getOwnPropertyDescriptor(Object.prototype, '__proto__')) {"
                        + "var set = Object.getOwnPropertyDescriptor(Object.prototype, '__proto__').set;"
                        + "  try { res += set.call(undefined); } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "  res += ' / ';"
                        + "  try { res += set.call(null); } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "  res += ' / ';"
                        + "  var a = {};"
                        + "  var p = {};"
                        + "  set.call(a, p);"
                        + "  res += a.__proto__ === p"
                        + "} else { res += 'no __proto__' }";

        Utils.assertWithAllModes_1_5("no __proto__", script);
        Utils.assertWithAllModes_1_8("no __proto__", script);
        Utils.assertWithAllModes_ES6("e-true / e-true / true", script);
    }

    @Test
    public void protoUndefinedSet() {
        String script =
                "var a = undefined;"
                        + "var newP = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "try { res += a.__proto__ = newP; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += ' / ';"
                        + "a = null;"
                        + "try { res += a['__proto__'] = newP; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += ' / ';"
                        + "a = null;"
                        + "try { res += a[s1 + s2 + s1] = newP; } catch(e) { res += 'e-' + (e instanceof TypeError); }";

        Utils.assertWithAllModes_1_5("e-true / e-true / e-true", script);
        Utils.assertWithAllModes_1_8("e-true / e-true / e-true", script);
        Utils.assertWithAllModes_ES6("e-true / e-true / e-true", script);
    }

    @Test
    public void protoNullPrototypeObjectsSet() {
        String script =
                "var a = Object.create(null);"
                        + "var newP = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += (a.__proto__ = newP) === newP;"
                        + "res += Object.getPrototypeOf(a);"
                        + "res += '-';"
                        + "res += a.__proto__ === newP;"
                        + "res += a['__proto__'] === newP;"
                        + "res += ' / ';"
                        + "a = Object.create(null);"
                        + "res += (a['__proto__'] = newP) === newP;"
                        + "res += Object.getPrototypeOf(a);"
                        + "res += '-';"
                        + "res += a.__proto__ === newP;"
                        + "res += a['__proto__'] === newP;"
                        + "res += ' / ';"
                        + "a = Object.create(null);"
                        + "res += (a[s1 + s2 + s1] = newP) === newP;"
                        + "res += Object.getPrototypeOf(a);"
                        + "res += '-';"
                        + "res += a.__proto__ === newP;"
                        + "res += a['__proto__'] === newP;";

        Utils.assertWithAllModes_1_5(
                "true[object Object]-truefalse / truenull-falsetrue / truenull-falsetrue", script);
        Utils.assertWithAllModes_1_8(
                "true[object Object]-truefalse / truenull-falsetrue / truenull-falsetrue", script);
        Utils.assertWithAllModes_ES6(
                "truenull-truetrue / truenull-truetrue / truenull-truetrue", script);
    }

    @Test
    public void protoNumberSet() {
        String script =
                "var a = 4711;"
                        + "var newP = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += (a.__proto__ = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === Number.prototype;"
                        + "res += a['__proto__'] === Number.prototype;"
                        + "res += ' / ';"
                        + "a = 4711;"
                        + "res += (a['__proto__'] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === Number.prototype;"
                        + "res += a['__proto__'] === Number.prototype;"
                        + "res += ' / ';"
                        + "a = 4711;"
                        + "res += (a[s1 + s2 + s1] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === Number.prototype;"
                        + "res += a['__proto__'] === Number.prototype;";

        Utils.assertWithAllModes_1_5("true-truefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_1_8("true-truefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoStringSet() {
        String script =
                "var a = '4711';"
                        + "var newP = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += (a.__proto__ = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === String.prototype;"
                        + "res += a['__proto__'] === String.prototype;"
                        + "res += ' / ';"
                        + "a = '4711';"
                        + "res += (a['__proto__'] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === String.prototype;"
                        + "res += a['__proto__'] === String.prototype;"
                        + "res += ' / ';"
                        + "a = '4711';"
                        + "res += (a[s1 + s2 + s1] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === String.prototype;"
                        + "res += a['__proto__'] === String.prototype;";

        Utils.assertWithAllModes_1_5("true-truefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_1_8("true-truefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoBoolSet() {
        String script =
                "var a = false;"
                        + "var newP = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += (a.__proto__ = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === Boolean.prototype;"
                        + "res += a['__proto__'] === Boolean.prototype;"
                        + "res += ' / ';"
                        + "a = false;"
                        + "res += (a['__proto__'] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === Boolean.prototype;"
                        + "res += a['__proto__'] === Boolean.prototype;"
                        + "res += ' / ';"
                        + "a = false;"
                        + "res += (a[s1 + s2 + s1] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === Boolean.prototype;"
                        + "res += a['__proto__'] === Boolean.prototype;";

        Utils.assertWithAllModes_1_5("true-truefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_1_8("true-truefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoSuperSet() {
        String script =
                "var res = '';"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var newP = {};"
                        + "var a = {x: 'a'};"
                        + "var b = {x: 'b'};"
                        + "var c = {x: 'c', "
                        + "  f() {"
                        + "    res += (super.__proto__ = newP) === newP;"
                        + "    res += Object.getPrototypeOf(this) === newP;"
                        + "    res += '-';"
                        + "    res += this.__proto__ === newP;"
                        + "    res += this['__proto__'] === newP;"
                        + "  }};"
                        + "Object.setPrototypeOf(c, b);"
                        + "Object.setPrototypeOf(b, a);"
                        + "c.f();"
                        + "res += ' / ';"
                        + "a = {x: 'a'};"
                        + "b = {x: 'b'};"
                        + "c = {x: 'c', "
                        + "  f() {"
                        + "    res += (super['__proto__'] = newP) === newP;"
                        + "    res += Object.getPrototypeOf(this) === newP;"
                        + "    res += '-';"
                        + "    res += this.__proto__ === newP;"
                        + "    res += this['__proto__'] === newP;"
                        + "  }};"
                        + "Object.setPrototypeOf(c, b);"
                        + "Object.setPrototypeOf(b, a);"
                        + "c.f();"
                        + "res += ' / ';"
                        + "a = {x: 'a'};"
                        + "b = {x: 'b'};"
                        + "c = {x: 'c', "
                        + "  f() {"
                        + "    res += (super[s1 + s2 + s1] = newP) === newP;"
                        + "    res += Object.getPrototypeOf(this) === newP;"
                        + "    res += '-';"
                        + "    res += this.__proto__ === newP;"
                        + "    res += this['__proto__'] === newP;"
                        + "  }};"
                        + "Object.setPrototypeOf(c, b);"
                        + "Object.setPrototypeOf(b, a);"
                        + "c.f();"
                        + "res;";

        Utils.assertWithAllModes_ES6(
                "truetrue-truetrue / truetrue-truetrue / truetrue-truetrue", script);
    }

    @Test
    public void protoSetNull() {
        String script =
                "var a = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "res += (a.__proto__ = null) === null;"
                        + "res += '-';"
                        + "res += a.__proto__ === undefined;"
                        + "res += a['__proto__'] === undefined;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "res += (a['__proto__'] = null) === null;"
                        + "res += '-';"
                        + "res += a.__proto__ === undefined;"
                        + "res += a['__proto__'] === undefined;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "res += (a[s1 + s2 + s1] = null) === null;"
                        + "res += '-';"
                        + "res += a.__proto__ === undefined;"
                        + "res += a['__proto__'] === undefined;";

        Utils.assertWithAllModes_1_5("true-falsetrue / true-falsefalse / true-falsefalse", script);
        Utils.assertWithAllModes_1_8("true-falsetrue / true-falsefalse / true-falsefalse", script);
        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoFunctionSetNull() {
        String script =
                "var a = function (o) { return o; };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "res += (a.__proto__ = null) === null;"
                        + "res += '-';"
                        + "res += a.__proto__ === undefined;"
                        + "res += a['__proto__'] === undefined;"
                        + "res += ' / ';"
                        + "a = function (o) { return o; };"
                        + "res += (a['__proto__'] = null) === null;"
                        + "res += '-';"
                        + "res += a.__proto__ === undefined;"
                        + "res += a['__proto__'] === undefined;"
                        + "res += ' / ';"
                        + "a = function (o) { return o; };"
                        + "res += (a[s1 + s2 + s1] = null) === null;"
                        + "res += '-';"
                        + "res += a.__proto__ === undefined;"
                        + "res += a['__proto__'] === undefined;";

        Utils.assertWithAllModes_1_5("true-falsetrue / true-falsefalse / true-falsefalse", script);
        Utils.assertWithAllModes_1_8("true-falsetrue / true-falsefalse / true-falsefalse", script);
        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoSymbolSetNull() {
        String script =
                "var a = Symbol('sym');"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "res += (a.__proto__ = null) === null;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = Symbol('sym');"
                        + "res += (a['__proto__'] = null) === null;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = Symbol('sym');"
                        + "res += (a[s1 + s2 + s1] = null) === null;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoSetUndefined() {
        String script =
                "var a = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "res += (a.__proto__ = undefined) === undefined;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "res += (a['__proto__'] = undefined) === undefined;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "res += (a[s1 + s2 + s1] = undefined) === undefined;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_1_5("false-falsefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_1_8("false-falsefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoFunctionSetUndefined() {
        String script =
                "var a = function (o) { return o; };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "res += (a.__proto__ = undefined) === undefined;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = function (o) { return o; };"
                        + "res += (a['__proto__'] = undefined) === undefined;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = function (o) { return o; };"
                        + "res += (a[s1 + s2 + s1] = undefined) === undefined;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_1_5("false-falsefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_1_8("false-falsefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoSymbolSetUndefined() {
        String script =
                "var a = Symbol('sym');"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "res += (a.__proto__ = undefined) === undefined;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = a = Symbol('sym');"
                        + "res += (a['__proto__'] = undefined) === undefined;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = a = Symbol('sym');"
                        + "res += (a[s1 + s2 + s1] = undefined) === undefined;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoSetNumber() {
        String script =
                "var a = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "res += (a.__proto__ = 4711) === 4711;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "res += (a['__proto__'] = 4711) === 4711;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "res += (a[s1 + s2 + s1] = 4711) === 4711;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_1_5("false-falsefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_1_8("false-falsefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoFunctionSetNumber() {
        String script =
                "var a = function (o) { return o; };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "res += (a.__proto__ = 4711) === 4711;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = function (o) { return o; };"
                        + "res += (a['__proto__'] = 4711) === 4711;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = function (o) { return o; };"
                        + "res += (a[s1 + s2 + s1] = 4711) === 4711;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_1_5("false-falsefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_1_8("false-falsefalse / true-truefalse / true-truefalse", script);
        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoSymbolSetNumber() {
        String script =
                "var a = Symbol('sym');"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "res += (a.__proto__ = 4711) === 4711;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = Symbol('sym');"
                        + "res += (a['__proto__'] = 4711) === 4711;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = Symbol('sym');"
                        + "res += (a[s1 + s2 + s1] = 4711) === 4711;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoSetSymbol() {
        String script =
                "var a = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var newP = Symbol('sym');"
                        + "var res = '';"
                        + "res += (a.__proto__ = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "res += (a['__proto__'] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "res += (a[s1 + s2 + s1] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoFunctionSetSymbol() {
        String script =
                "var a = function (o) { return o; };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var newP = Symbol('sym');"
                        + "var res = '';"
                        + "res += (a.__proto__ = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = function (o) { return o; };"
                        + "res += (a['__proto__'] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = function (o) { return o; };"
                        + "res += (a[s1 + s2 + s1] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoSymbolSetSymbol() {
        String script =
                "var a = Symbol('sy');"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var newP = Symbol('sym');"
                        + "var res = '';"
                        + "res += (a.__proto__ = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = Symbol('sy');"
                        + "res += (a['__proto__'] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = Symbol('sy');"
                        + "res += (a[s1 + s2 + s1] = newP) === newP;"
                        + "res += '-';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_ES6("true-truetrue / true-truetrue / true-truetrue", script);
    }

    @Test
    public void protoInit() {
        String script =
                "var proto = {};"
                        + "var a = { __proto__: proto };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += Object.getPrototypeOf(a) === proto;"
                        + "res += ' / ';"
                        + "res += a.__proto__ === proto;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === proto;"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === proto;";

        Utils.assertWithAllModes_1_5("true / true / false / false", script);
        Utils.assertWithAllModes_1_8("true / true / false / false", script);
        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoInitFunction() {
        String script =
                "var proto = function(){};"
                        + "var a = { __proto__: proto };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += Object.getPrototypeOf(a) === a.__proto__;"
                        + "res += Object.getPrototypeOf(a) === proto;"
                        + "res += ' / ';"
                        + "res += a.__proto__ === proto;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === proto;"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === proto;";

        Utils.assertWithAllModes_1_5("truetrue / true / false / false", script);
        Utils.assertWithAllModes_1_8("truetrue / true / false / false", script);
        Utils.assertWithAllModes_ES6("truetrue / true / true / true", script);
    }

    @Test
    public void protoInitFunctionShorthand() {
        String script =
                "var a = { __proto__(){} };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += Object.getPrototypeOf(a) === Object.__proto__;"
                        + "res += ' / ';"
                        + "res += typeof a.__proto__;"
                        + "res += ' / ';"
                        + "res += Object.getOwnPropertyDescriptor(a, '__proto__');";

        Utils.assertWithAllModes_1_5("false / function / undefined", script);
        Utils.assertWithAllModes_1_8("false / function / undefined", script);
        Utils.assertWithAllModes_ES6("false / function / [object Object]", script);
    }

    @Test
    public void protoInitUndefined() {
        String script =
                "var proto = undefined;"
                        + "var a = { __proto__: proto };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += Object.getPrototypeOf(a) === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a.__proto__ === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === {}.__proto__;";

        Utils.assertWithAllModes_1_5("false / false / false / false", script);
        Utils.assertWithAllModes_1_8("false / false / false / false", script);
        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoInitNull() {
        String script =
                "var proto = null;"
                        + "var a = { __proto__: proto };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += Object.getPrototypeOf(a) === proto;"
                        + "res += ' / ';"
                        + "res += a.__proto__ === undefined;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === undefined;"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === undefined;";

        Utils.assertWithAllModes_1_5("true / false / true / true", script);
        Utils.assertWithAllModes_1_8("true / false / true / true", script);
        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoInitNumber() {
        String script =
                "var proto = 4711;"
                        + "var a = { __proto__: proto };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += Object.getPrototypeOf(a) === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a.__proto__ === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === {}.__proto__;";

        Utils.assertWithAllModes_1_5("false / false / false / false", script);
        Utils.assertWithAllModes_1_8("false / false / false / false", script);
        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoInitBool() {
        String script =
                "var proto = false;"
                        + "var a = { __proto__: proto };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += Object.getPrototypeOf(a) === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a.__proto__ === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === {}.__proto__;";

        Utils.assertWithAllModes_1_5("false / false / false / false", script);
        Utils.assertWithAllModes_1_8("false / false / false / false", script);
        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoInitString() {
        String script =
                "var proto = 'abcd';"
                        + "var a = { __proto__: proto };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += Object.getPrototypeOf(a) === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a.__proto__ === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === {}.__proto__;";

        Utils.assertWithAllModes_1_5("false / false / false / false", script);
        Utils.assertWithAllModes_1_8("false / false / false / false", script);
        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoInitSymbol() {
        String script =
                "var proto = Symbol('abcd');"
                        + "var a = { __proto__: proto };"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "res += Object.getPrototypeOf(a) === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a.__proto__ === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === {}.__proto__;"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === {}.__proto__;";

        Utils.assertWithAllModes_ES6("true / true / true / true", script);
    }

    @Test
    public void protoSetThis() {
        String script =
                "var a = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "try { a.__proto__ = a; } catch (e) { res += 'ex-'; }"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "try { a['__proto__'] = a; } catch (e) { res += 'ex-'; }"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "try { a[s1 + s2 + s1] = a; } catch (e) { res += 'ex-'; }"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_1_5("ex-truefalse / truefalse / truefalse", script);
        Utils.assertWithAllModes_1_8("ex-truefalse / truefalse / truefalse", script);
        Utils.assertWithAllModes_ES6("ex-truetrue / ex-truetrue / ex-truetrue", script);
    }

    @Test
    public void protoSetChild() {
        String script =
                "var a = {};"
                        + "var b = Object.create(a);"
                        + "var c = Object.create(b);"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "try { a.__proto__ = c; } catch (e) { res += 'ex-'; }"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "var b = Object.create(a);"
                        + "var c = Object.create(b);"
                        + "try { a['__proto__'] = c; } catch (e) { res += 'ex-'; }"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "var b = Object.create(a);"
                        + "var c = Object.create(b);"
                        + "try { a[s1 + s2 + s1] = c; } catch (e) { res += 'ex-'; }"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;";

        Utils.assertWithAllModes_1_5("ex-truefalse / truefalse / truefalse", script);
        Utils.assertWithAllModes_1_8("ex-truefalse / truefalse / truefalse", script);
        Utils.assertWithAllModes_ES6("ex-truetrue / ex-truetrue / ex-truetrue", script);
    }

    @Test
    public void protoIn() {
        String script = "'__proto__' in {}";

        Utils.assertWithAllModes_1_5(false, script);
        Utils.assertWithAllModes_1_8(false, script);
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void protoRedefine() {
        String script =
                "var a = {};"
                        + "var s1 = '__'; var s2 = 'proto';"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "Object.defineProperty(a, '__proto__', { value: 42, writable: false });"
                        + "res += a.__proto__;"
                        + "res += '#' + a['__proto__'];";

        Utils.assertWithAllModes_1_5("truefalse / [object Object]#42", script);
        Utils.assertWithAllModes_1_8("truefalse / [object Object]#42", script);
        Utils.assertWithAllModes_ES6("truetrue / 42#42", script);
    }

    @Test
    public void protoRedefineOnObjectPrototype() {
        String script =
                "var a = {};"
                        + "var p = Object.getPrototypeOf(a);"
                        + "var res = '';"
                        + "res += a.__proto__ === p;"
                        + "res += a['__proto__'] === p;"
                        + "res += ' / ';"
                        + "a = {};"
                        + "Object.defineProperty(Object.prototype, '__proto__', { value: 42, writable: false });"
                        + "res += a.__proto__;"
                        + "res += '#' + a['__proto__'];";

        Utils.assertWithAllModes_1_5("truefalse / [object Object]#42", script);
        Utils.assertWithAllModes_1_8("truefalse / [object Object]#42", script);
        Utils.assertWithAllModes_ES6("truetrue / 42#42", script);
    }

    @Test
    public void protoInitOverwriteGetter() {
        String script =
                "var s1 = '__'; var s2 = 'proto';"
                        + "var res = '';"
                        + "Object.defineProperty(Object.prototype, '__proto__', { get: function(v) { return 'get called'; }});"
                        + "var p = {};"
                        + "var a = { __proto__: p };"
                        + "res += !a.hasOwnProperty('__proto__');"
                        + "res += a.__proto__ === 'get called';"
                        + "res += ' / ';"
                        + "res += a['__proto__'] === 'get called';"
                        + "res += ' / ';"
                        + "res += a[s1 + s2 + s1] === 'get called';";

        Utils.assertWithAllModes_1_5("truefalse / true / true", script);
        Utils.assertWithAllModes_1_8("truefalse / true / true", script);
        Utils.assertWithAllModes_ES6("truetrue / true / true", script);
    }

    @Test
    public void protoInitOverwriteSetter() {
        String script =
                "var res = '';"
                        + "Object.defineProperty(Object.prototype, '__proto__', { set: function(v) { res += 'get called ' + v; }});"
                        + "var desc = Object.getOwnPropertyDescriptor(Object.prototype, '__proto__');"
                        + "if (desc) {"
                        + "  res += desc.configurable;"
                        + "  res += ' / ';"
                        + "  res += desc.enumerable;"
                        + "  res += ' / ';"
                        + "  res += desc.writable;"
                        + "  res += ' / ';"
                        + "  res += (desc.get) ? desc.get.name : desc.get;"
                        + "  res += ' / ';"
                        + "  res += desc.set.name;"
                        + "} else { res += desc }"
                        + "res += ' # ';"
                        + "var p = {};"
                        + "var a = { __proto__: p };"
                        + "res += !a.hasOwnProperty('__proto__');"
                        + "res += a.__proto__ === p;"
                        + "res += ' / ';"
                        + "res += a.__proto__ = 'new';";

        Utils.assertWithAllModes_1_5(
                "false / false / undefined / undefined /  # truetrue / new", script);
        Utils.assertWithAllModes_1_8(
                "false / false / undefined / undefined /  # truetrue / new", script);
        // Utils.assertWithAllModes_ES6("true / false / undefined / get __proto__ / set # truetrue /
        // new", script);
        Utils.assertWithAllModes_ES6(
                "true / false / undefined / get __proto__ /  # truetrue / new", script);
    }

    @Test
    public void protoPreventExtensions() {
        String script =
                "var p = {};"
                        + "var a = Object.create(p);"
                        + "Object.preventExtensions(a);"
                        + "var res = '';"
                        + "res += p === Object.getPrototypeOf(a);"
                        + "res += ' / ';"
                        + "try { a.__proto__ = {}; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += ' / ';"
                        + "res += a.__proto__ === p;"
                        + "res += ' / ';"
                        + "try { (a.__proto__ = p) === p; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += ' / ';"
                        + "res += a.__proto__ === p;";

        Utils.assertWithAllModes_1_5("true /  / false /  / true", script);
        Utils.assertWithAllModes_1_8("true / e-true / true / e-true / true", script);
        Utils.assertWithAllModes_ES6("true / e-true / true /  / true", script);
    }

    @Test
    public void protoObject() {
        String script =
                "var res = '';"
                        + "try { Object.prototype.__proto__ = null; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += Object.prototype.__proto__ === null;"
                        + "res += ' / ';"
                        + "try { Object.prototype.__proto__ = {}; } catch(e) { res += 'e-' + (e instanceof TypeError); }"
                        + "res += Object.prototype.__proto__ === null;";

        Utils.assertWithAllModes_1_5("true / e-falsetrue", script);
        Utils.assertWithAllModes_1_8("true / e-falsetrue", script);
        Utils.assertWithAllModes_ES6("true / e-truetrue", script);
    }

    @Test
    public void protoOwnPropertyDescriptor() {
        String script =
                "var res = '';"
                        + "var desc = Object.getOwnPropertyDescriptor(Object.prototype, '__proto__');"
                        + "if (desc) {"
                        + "  res += desc.configurable;"
                        + "  res += ' / ';"
                        + "  res += desc.enumerable;"
                        + "  res += ' / ';"
                        + "  res += desc.writable;"
                        + "  res += ' / ';"
                        + "  res += desc.get.name;"
                        + "  res += ' / ';"
                        + "  res += desc.set.name;"
                        + "} else { res += desc }";

        Utils.assertWithAllModes_1_5("undefined", script);
        Utils.assertWithAllModes_1_8("undefined", script);
        Utils.assertWithAllModes_ES6(
                "true / false / undefined / get __proto__ / set __proto__", script);
    }
}
