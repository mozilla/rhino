/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.testutils.Utils;

/*
 * General tests for Function
 */
public class NativeFunctionTest {

    @Test
    public void functionPrototypeLength() {
        final String code =
                "var res = '' + ('length' in Function.prototype);\n"
                        + "var desc = Object.getOwnPropertyDescriptor(Function.prototype, 'length');\n"
                        + "res += ' configurable: ' + desc.configurable;\n"
                        + "res += ' enumerable: ' + desc.enumerable;\n"
                        + "res += ' writable: ' + desc.writable;";
        Utils.assertWithAllModes_ES6(
                "true configurable: true enumerable: false writable: false", code);
        Utils.assertWithAllModes_1_8(
                "true configurable: true enumerable: false writable: false", code);
    }

    @Test
    public void functionLength() {
        final String code =
                "var f=function(){};\n"
                        + "var res = '' + ('length' in f);\n"
                        + "var desc = Object.getOwnPropertyDescriptor(f, 'length');\n"
                        + "res = 'configurable: ' + desc.configurable;\n"
                        + "res += ' enumerable: ' + desc.enumerable;\n"
                        + "res += ' writable: ' + desc.writable;";
        Utils.assertWithAllModes_ES6("configurable: true enumerable: false writable: false", code);
        Utils.assertWithAllModes_1_8("configurable: true enumerable: false writable: false", code);
    }

    @Test
    public void functionPrototypeArity() {
        final String code =
                "var res = '' + ('arity' in Function.prototype);\n"
                        + "res += ' ' + Object.getOwnPropertyDescriptor(Function.prototype, 'arity');\n";
        // todo Utils.assertWithAllModes_ES6("false undefined", code);
        Utils.assertWithAllModes_ES6("true [object Object]", code);
        Utils.assertWithAllModes_1_8("true [object Object]", code);
    }

    @Test
    public void functionArity() {
        final String code =
                "var f=function(){};\n"
                        + "var res = '' + ('arity' in f);\n"
                        + "res += ' ' + Object.getOwnPropertyDescriptor(f, 'arity');\n";
        // todo Utils.assertWithAllModes_ES6("false undefined", code);
        Utils.assertWithAllModes_ES6("true [object Object]", code);
        Utils.assertWithAllModes_1_8("true [object Object]", code);
    }

    @Test
    public void functionPrototypeArguments() {
        final String code =
                "var res = '' + ('arguments' in Function.prototype);\n"
                        + "var desc = Object.getOwnPropertyDescriptor(Function.prototype, 'arguments');\n"
                        + "res += ' configurable: ' + desc.configurable;\n"
                        + "res += ' enumerable: ' + desc.enumerable;\n"
                        + "res += ' writable: ' + desc.writable;";
        Utils.assertWithAllModes_ES6(
                "true configurable: true enumerable: false writable: undefined", code);
        Utils.assertWithAllModes_1_8(
                "true configurable: false enumerable: false writable: true", code);
    }

    @Test
    public void functionPrototypeArgumentsAccess() {
        final String code =
                "var f = function(){};\n"
                        + "let res = '';\n"
                        + "try { res += f.arguments; } catch (e) { res += e.message; };\n"
                        + "res += ' ';\n"
                        + "try { f.arguments = 7; res += f.arguments; } catch (e) { res += e.message; };";
        Utils.assertWithAllModes_ES6("null null", code);
        Utils.assertWithAllModes_1_8("null 7", code);
    }

    @Test
    public void functionArguments() {
        final String code =
                "var f=function(){};\n"
                        + "var res = '' + ('arguments' in f);\n"
                        + "res += ' ' + Object.getOwnPropertyDescriptor(f, 'arguments');\n";
        Utils.assertWithAllModes_ES6("true undefined", code);
        Utils.assertWithAllModes_1_8("true [object Object]", code);
    }

    @Test
    public void functionPrototypeName() {
        final String code =
                "var res = '' + ('name' in Function.prototype);\n"
                        + "var desc = Object.getOwnPropertyDescriptor(Function.prototype, 'name');\n"
                        + "res += ' configurable: ' + desc.configurable;\n"
                        + "res += ' enumerable: ' + desc.enumerable;\n"
                        + "res += ' writable: ' + desc.writable;";
        Utils.assertWithAllModes_ES6(
                "true configurable: true enumerable: false writable: false", code);
        Utils.assertWithAllModes_1_8(
                "true configurable: true enumerable: false writable: false", code);
    }

    @Test
    public void functionName() {
        final String code =
                "var f=function(){};\n"
                        + "var res = '' + ('name' in f);\n"
                        + "var desc = Object.getOwnPropertyDescriptor(f, 'name');\n"
                        + "res += ' configurable: ' + desc.configurable;\n"
                        + "res += ' enumerable: ' + desc.enumerable;\n"
                        + "res += ' writable: ' + desc.writable;";
        Utils.assertWithAllModes_ES6(
                "true configurable: true enumerable: false writable: false", code);
        Utils.assertWithAllModes_1_8(
                "true configurable: true enumerable: false writable: false", code);
    }

    @Test
    public void functionNameJavaObject() throws Exception {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();
                    try {
                        ScriptableObject.defineClass(scope, HelperObject.class);
                    } catch (Exception e) {
                    }

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var f=new HelperObject().foo;\n"
                                            + "var desc = Object.getOwnPropertyDescriptor(f, 'name');\n"
                                            + "var res = 'configurable: ' + desc.configurable;\n"
                                            + "res += ' enumerable: ' + desc.enumerable;\n"
                                            + "res += ' writable: ' + desc.writable;",
                                    "test",
                                    1,
                                    null);
                    assertEquals("configurable: true enumerable: false writable: false", result);

                    return null;
                });
    }

    public static class HelperObject extends ScriptableObject {

        public HelperObject() {}

        @Override
        public String getClassName() {
            return "HelperObject";
        }

        @JSConstructor
        public void jsConstructorMethod() {
            put("initialized", this, Boolean.TRUE);
        }

        @JSFunction("foo")
        public Object foo() {
            return "foo()";
        }
    }
}
