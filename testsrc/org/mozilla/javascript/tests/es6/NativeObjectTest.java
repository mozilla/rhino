/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop), Object.keys, Object.values and Object.entries method
 */
package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tests.Utils;

/** Test for NativeObject. */
public class NativeObjectTest {

    @Test
    public void assignPropertyGetter() {
        evaluateAndAssert(
                "var obj = Object.defineProperty({}, 'propA', {\n"
                        + "                 enumerable: true,\n"
                        + "                 get: function () {\n"
                        + "                        Object.defineProperty(this, 'propB', {\n"
                        + "                                 value: 3,\n"
                        + "                                 enumerable: false\n"
                        + "                        });\n"
                        + "                      }\n"
                        + "          });\n"
                        + "Object.assign(obj, {propB: 2});\n"
                        + "Object.assign({propB: 1}, obj);\n"
                        + "'obj.propB = ' + obj.propB + ', enumerable = ' + Object.getOwnPropertyDescriptor(obj, 'propB').enumerable",
                "obj.propB = 3, enumerable = false");
    }

    @Test
    public void assignOneParameter() {
        evaluateAndAssert(
                "var obj = {};" + "res = Object.assign(obj);" + "res === obj;", Boolean.TRUE);
    }

    @Test
    public void assignMissingParameters() {
        evaluateAndAssert(
                "try { " + "  Object.assign();" + "} catch (e) { e.message }",
                "Cannot convert undefined to an object.");
    }

    @Test
    public void assignNumericPropertyGetter() {
        evaluateAndAssert(
                "var obj = Object.defineProperty({}, 1, {\n"
                        + "                 enumerable: true,\n"
                        + "                 get: function () {\n"
                        + "                        Object.defineProperty(this, 2, {\n"
                        + "                                 value: 3,\n"
                        + "                                 enumerable: false\n"
                        + "                        });\n"
                        + "                      }\n"
                        + "          });\n"
                        + "Object.assign(obj, {2: 2});\n"
                        + "Object.assign({2: 1}, obj);\n"
                        + "'obj[2] = ' + obj[2] + ', enumerable = ' + Object.getOwnPropertyDescriptor(obj, 2).enumerable",
                "obj[2] = 3, enumerable = false");
    }

    @Test
    public void assignUndefined() {
        evaluateAndAssert("Object.keys(Object.assign({a:undefined}, {b:undefined})).join()", "a,b");
    }

    @Test
    public void assignInextensible() {
        evaluateAndAssert(
                "var obj = Object.freeze({});\n"
                        + "try {\n"
                        + "  Object.assign(obj, { a: 1 });\n"
                        + "  'success';\n"
                        + "} catch(e) {\n"
                        + "  'error';\n"
                        + "}",
                "error");
        evaluateAndAssert(
                "var obj = Object.freeze({});\n"
                        + "try {\n"
                        + "  Object.assign(obj, {});\n"
                        + "  'success';\n"
                        + "} catch(e) {\n"
                        + "  'error';\n"
                        + "}",
                "success");
    }

    @Test
    public void assignUnwritable() {
        evaluateAndAssert(
                "var src = Object.defineProperty({}, 1, {\n"
                        + "  enumerable: true,\n"
                        + "  value: 'v'\n"
                        + "});\n"
                        + "var dest = Object.defineProperty({}, 1, {\n"
                        + "  writable: false,\n"
                        + "  value: 'v'\n"
                        + "});\n"
                        + "try {\n"
                        + "  Object.assign(dest, src);\n"
                        + "  'success';\n"
                        + "} catch(e) {\n"
                        + "  'error';\n"
                        + "}",
                "error");
        evaluateAndAssert(
                "var src = Object.defineProperty({}, 1, {\n"
                        + "  enumerable: false,\n"
                        + "  value: 'v'\n"
                        + "});\n"
                        + "var dest = Object.defineProperty({}, 1, {\n"
                        + "  writable: false,\n"
                        + "  value: 'v'\n"
                        + "});\n"
                        + "try {\n"
                        + "  Object.assign(dest, src);\n"
                        + "  'success';\n"
                        + "} catch(e) {\n"
                        + "  'error';\n"
                        + "}",
                "success");
        evaluateAndAssert(
                "var src = Object.defineProperty({}, 1, {\n"
                        + "  enumerable: true,\n"
                        + "  value: 'v'\n"
                        + "});\n"
                        + "var destProto = Object.defineProperty({}, 1, {\n"
                        + "  writable: false,\n"
                        + "  value: 'v'\n"
                        + "});\n"
                        + "var dest = Object.create(destProto);\n"
                        + "try {\n"
                        + "  Object.assign(dest, src);\n"
                        + "  'success';\n"
                        + "} catch(e) {\n"
                        + "  'error';\n"
                        + "}",
                "error");
    }

    @Test
    public void assignVariousKeyTypes() {
        evaluateAndAssert(
                "var strKeys = Object.assign({}, {a: 1, b: 2});\n"
                        + "var arrayKeys = Object.assign({}, ['a', 'b']);\n"
                        + "var res = 'strKeys: ' + JSON.stringify(strKeys) + "
                        + "'  arrayKeys:' + JSON.stringify(arrayKeys);\n"
                        + "res",
                "strKeys: {\"a\":1,\"b\":2}  arrayKeys:{\"0\":\"a\",\"1\":\"b\"}");
    }

    @Test
    public void assignWithPrototypeStringKeys() {
        final String script =
                "var targetProto = { a: 1, b: 2 };\n"
                        + "var target = Object.create(targetProto);\n"
                        + "var source = { b: 4, c: 5 };\n"
                        + "var assigned = Object.assign(target, source);\n"
                        + "var res = 'targetProto: ' + JSON.stringify(targetProto) + "
                        + "'  target: ' + JSON.stringify(target) + "
                        + "'  assigned: ' + JSON.stringify(assigned);\n"
                        + "res";

        evaluateAndAssert(
                script,
                "targetProto: {\"a\":1,\"b\":2}  target: {\"b\":4,\"c\":5}  assigned: {\"b\":4,\"c\":5}");
    }

    @Test
    public void assignWithPrototypeNumericKeys() {
        final String script =
                "var targetProto = {0: 'a', 1: 'b'};\n"
                        + "var target = Object.create(targetProto);\n"
                        + "var source = {1: 'c', 2: 'd'};\n"
                        + "var assigned = Object.assign(target, source);\n"
                        + "var res = 'targetProto: ' + JSON.stringify(targetProto) + "
                        + "'  target: ' + JSON.stringify(target) + "
                        + "'  assigned: ' + JSON.stringify(assigned);\n"
                        + "res";

        evaluateAndAssert(
                script,
                "targetProto: {\"0\":\"a\",\"1\":\"b\"}  target: {\"1\":\"c\",\"2\":\"d\"}  assigned: {\"1\":\"c\",\"2\":\"d\"}");
    }

    @Test
    public void assignWithPrototypeWithGettersAndSetters() {
        final String script =
                "var targetProto = {"
                        + "_a: 1, get a() { return this._a }, set a(val) { this._a = val+10 },"
                        + " _b: 2, get b() { return this._b }, set b(val) { this._b = val+10 }"
                        + "};\n"
                        + "var target = Object.create(targetProto);\n"
                        + "var source = {"
                        + "_b: 4, get b() { return this._b * 4 }, set b(val) { this._b = val+10 },"
                        + " _c: 5, get c() { return this._c }, set c(val) { this._c = val+10 }};\n"
                        + "var assigned = Object.assign(target, source);\n"
                        + "var res = 'targetProto: ' + JSON.stringify(targetProto) + "
                        + "'  target: ' + JSON.stringify(target) + "
                        + "'  assigned: ' + JSON.stringify(assigned) + "
                        + "'  assigned.a: ' + assigned.a + "
                        + "'  assigned.b: ' + assigned.b + "
                        + "'  assigned.c: ' + assigned.c;\n"
                        + "res";

        evaluateAndAssert(
                script,
                "targetProto: {\"_a\":1,\"a\":1,\"_b\":2,\"b\":2}  target: {\"_b\":26,\"_c\":5,\"c\":5}  assigned: {\"_b\":26,\"_c\":5,\"c\":5}  assigned.a: 1  assigned.b: 26  assigned.c: 5");
    }

    @Test
    public void getOwnPropertyDescriptorSetPropertyIsAlwaysDefined() {
        evaluateAndAssert(
                "var obj = Object.defineProperty({}, 'prop', {\n"
                        + "                 get: function() {}\n"
                        + "          });\n"
                        + "var desc = Object.getOwnPropertyDescriptor(obj, 'prop');\n"
                        + "'' + obj.prop"
                        + "+ ' ' + desc.get"
                        + "+ ' ' + desc.set"
                        + "+ ' [' + Object.getOwnPropertyNames(desc) + ']'",
                "undefined \nfunction () {\n}\n undefined [get,set,enumerable,configurable]");
    }

    @Test
    public void setPrototypeOfNull() {
        evaluateAndAssert(
                "try { "
                        + "  Object.setPrototypeOf(null, new Object());"
                        + "} catch (e) { e.message }",
                "Object.prototype.setPrototypeOf method called on null or undefined");
    }

    @Test
    public void setPrototypeOfUndefined() {
        evaluateAndAssert(
                "try { "
                        + "  Object.setPrototypeOf(undefined, new Object());"
                        + "} catch (e) { e.message }",
                "Object.prototype.setPrototypeOf method called on null or undefined");
    }

    @Test
    public void setPrototypeOfMissingParameters() {
        evaluateAndAssert(
                "try { " + "  Object.setPrototypeOf();" + "} catch (e) { e.message }",
                "Object.setPrototypeOf: At least 2 arguments required, but only 0 passed");

        evaluateAndAssert(
                "try { " + "  Object.setPrototypeOf({});" + "} catch (e) { e.message }",
                "Object.setPrototypeOf: At least 2 arguments required, but only 1 passed");
    }

    @Test
    public void keysMissingParameter() {
        evaluateAndAssert(
                "try { " + "  Object.keys();" + "} catch (e) { e.message }",
                "Cannot convert undefined to an object.");
    }

    @Test
    public void keysOnObjectParameter() {
        evaluateAndAssert("Object.keys({'foo':'bar', 2: 'y', 1: 'x'}).join()", "1,2,foo");
    }

    @Test
    public void keysOnArray() {
        evaluateAndAssert("Object.keys(['x','y','z']).join()", "0,1,2");
    }

    @Test
    public void keysOnArrayWithProp() {
        evaluateAndAssert(
                "var arr = ['x','y','z'];\n" + "arr['foo'] = 'bar';\n" + "Object.keys(arr).join()",
                "0,1,2,foo");
    }

    @Test
    public void valuesMissingParameter() {
        evaluateAndAssert(
                "try { " + "  Object.values();" + "} catch (e) { e.message }",
                "Cannot convert undefined to an object.");
    }

    @Test
    public void valuesOnObjectParameter() {
        evaluateAndAssert("Object.values({'foo':'bar', 2: 'y', 1: 'x'}).join()", "x,y,bar");
    }

    @Test
    public void valuesOnArray() {
        evaluateAndAssert("Object.values(['x','y','z']).join()", "x,y,z");
    }

    @Test
    public void valuesOnArrayWithProp() {
        evaluateAndAssert(
                "var arr = [3,4,5];\n" + "arr['foo'] = 'bar';\n" + "Object.values(arr).join();",
                "3,4,5,bar");
    }

    @Test
    public void entriesMissingParameter() {
        evaluateAndAssert(
                "try { " + "  Object.entries();" + "} catch (e) { e.message }",
                "Cannot convert undefined to an object.");
    }

    @Test
    public void entriesOnObjectParameter() {
        evaluateAndAssert(
                "Object.entries({'foo':'bar', 2: 'y', 1: 'x'}).join()", "1,x,2,y,foo,bar");
    }

    @Test
    public void entriesOnArray() {
        evaluateAndAssert("Object.entries(['x','y','z']).join()", "0,x,1,y,2,z");
    }

    @Test
    public void entriesOnArrayWithProp() {
        evaluateAndAssert(
                "var arr = [3,4,5];\n" + "arr['foo'] = 'bar';\n" + "Object.entries(arr).join()",
                "0,3,1,4,2,5,foo,bar");
    }

    @Test
    public void fromEntriesMissingParameter() {
        evaluateAndAssert(
                "try { " + "  Object.fromEntries();" + "} catch (e) { e.message }",
                "Cannot convert undefined to an object.");
    }

    @Test
    public void fromEntriesOnArray() {
        Map<Object, Object> map = new HashMap<>();
        map.put("0", "x");
        map.put("1", "y");
        map.put("2", "z");
        evaluateAndAssert("Object.fromEntries(Object.entries(['x','y','z']))", map);
    }

    @Test
    public void issue943() {
        evaluateAndAssert(
                "var foo = function e() {}\n"
                        + "var fooProto = foo.prototype;\n"
                        + "var descProp = Object.getOwnPropertyDescriptor(fooProto, 'constructor');"
                        + "descProp.hasOwnProperty('value');\n",
                Boolean.TRUE);
    }

    @Test
    public void issue943Realm() {
        final String script =
                "realm.Object.getOwnPropertyDescriptor(realm.Object, 'getOwnPropertyDescriptor').__proto__ === ({}).__proto__;";

        String[] prefixes = {"", "'use strict;'\n"};
        for (final String prefix : prefixes) {
            Utils.runWithAllOptimizationLevels(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_ES6);
                        ScriptableObject scope = cx.initStandardObjects();

                        Scriptable realm = cx.initStandardObjects();
                        scope.put("realm", scope, realm);

                        Object result = cx.evaluateString(scope, prefix + script, "test", 1, null);
                        assertEquals(Boolean.FALSE, result);
                        return null;
                    });
        }
    }

    private static void evaluateAndAssert(final String script, final Object expected) {
        String[] prefixes = {"", "'use strict;'\n"};
        for (final String prefix : prefixes) {
            Utils.runWithAllOptimizationLevels(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_ES6);
                        ScriptableObject scope = cx.initStandardObjects();
                        Object result = cx.evaluateString(scope, prefix + script, "test", 1, null);
                        assertEquals(expected, result);
                        return null;
                    });
        }
    }
}
