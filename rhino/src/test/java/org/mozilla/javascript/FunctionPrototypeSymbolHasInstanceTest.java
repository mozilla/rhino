package org.mozilla.javascript;

import org.junit.Ignore;
import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class FunctionPrototypeSymbolHasInstanceTest {
    @Test
    public void testSymbolHasInstanceIsPresent() {
        String script =
                ""
                        + "var f = {\n"
                        + "   [Symbol.hasInstance](value) { "
                        + "   }"
                        + "};\n"
                        + "var g = {};\n"
                        + "`${f.hasOwnProperty(Symbol.hasInstance)}:${g.hasOwnProperty(Symbol.hasInstance)}`";
        Utils.assertWithAllModes("true:false", script);
    }

    @Test
    public void testSymbolHasInstanceCanBeCalledLikeAnotherMethod() {
        String script =
                ""
                        + "var f = {\n"
                        + "   [Symbol.hasInstance](value) { "
                        + "       return 42;"
                        + "   }"
                        + "};\n"
                        + "f[Symbol.hasInstance]() == 42";
        Utils.assertWithAllModes(true, script);
    }

    // See: https://tc39.es/ecma262/#sec-function.prototype-%symbol.hasinstance%
    @Test
    public void testFunctionPrototypeSymbolHasInstanceHasAttributes() {
        String script =
                "var a = Object.getOwnPropertyDescriptor(Function.prototype, Symbol.hasInstance);\n"
                        + "a.writable + ':' + a.configurable + ':' + a.enumerable";
        Utils.assertWithAllModes("false:false:false", script);
    }

    // See: https://tc39.es/ecma262/#sec-function.prototype-%symbol.hasinstance%
    @Test
    public void testFunctionPrototypeSymbolHasInstanceHasAttributesStrictMode() {
        String script =
                "'use strict';\n"
                        + "var t = typeof Function.prototype[Symbol.hasInstance];\n"
                        + "var a = Object.getOwnPropertyDescriptor(Function.prototype, Symbol.hasInstance);\n"
                        + "var typeErrorThrown = false;\n"
                        + "try { \n"
                        + "    delete Function.prototype[Symbol.hasInstance] \n"
                        + "} catch (e) { \n"
                        + "    typeErrorThrown = true \n"
                        + "}\n"
                        + "Object.prototype.hasOwnProperty.call(Function.prototype, Symbol.hasInstance) + ':' + typeErrorThrown + ':' + t + ':' + a.writable + ':' + a.configurable + ':' + a.enumerable; \n";
        Utils.assertWithAllModes("true:true:function:false:false:false", script);
    }

    @Test
    @Ignore("name-length-params-prototype-set-incorrectly")
    public void testFunctionPrototypeSymbolHasInstanceHasProperties() {
        String script =
                "var a = Object.getOwnPropertyDescriptor(Function.prototype[Symbol.hasInstance], 'length');\n"
                        + "a.value + ':' + a.writable + ':' + a.configurable + ':' + a.enumerable";

        String script2 =
                "var a = Object.getOwnPropertyDescriptor(Function.prototype[Symbol.hasInstance], 'name');\n"
                        + "a.value + ':' + a.writable + ':' + a.configurable + ':' + a.enumerable";
        Utils.assertWithAllModes("1:false:true:false", script);
        Utils.assertWithAllModes("Symbol(Symbol.hasInstance):false:true:false", script2);
    }

    @Test
    public void testFunctionPrototypeSymbolHasInstance() {
        String script =
                "(Function.prototype[Symbol.hasInstance] instanceof Function) + ':' + "
                        + "Function.prototype[Symbol.hasInstance].call(Function, Object)\n";
        Utils.assertWithAllModes("true:true", script);
    }

    @Test
    public void testFunctionPrototypeSymbolHasInstanceOnObjectReturnsTrue() {
        String script =
                "var f = function() {};\n"
                        + "var o = new f();\n"
                        + "var o2 = Object.create(o);\n"
                        + "(f[Symbol.hasInstance](o)) + ':' + "
                        + "(f[Symbol.hasInstance](o2));\n";
        Utils.assertWithAllModes("true:true", script);
    }

    @Test
    public void testFunctionPrototypeSymbolHasInstanceOnBoundTargetReturnsTrue() {
        String script =
                "var BC = function() {};\n"
                        + "var bc = new BC();\n"
                        + "var bound = BC.bind();\n"
                        + "bound[Symbol.hasInstance](bc);\n";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testFunctionInstanceNullVoidEtc() {
        String script =
                "var f = function() {};\n"
                        + "var x;\n"
                        + "a = (undefined instanceof f) + ':' +\n"
                        + "(x instanceof f) + ':' +\n"
                        + "(null instanceof f) + ':' +\n"
                        + "(void 0 instanceof f)\n"
                        + "a";
        Utils.assertWithAllModes("false:false:false:false", script);
    }

    @Test
    public void testFunctionPrototypeSymbolHasInstanceReturnsFalseOnUndefinedOrProtoypeNotFound() {
        String script =
                "Function.prototype[Symbol.hasInstance].call() + ':' +"
                        + "Function.prototype[Symbol.hasInstance].call({});";
        Utils.assertWithAllModes("false:false", script);
    }

    @Test
    public void testSymbolHasInstanceIsInvokedInInstanceOf() {
        String script =
                ""
                        + "var globalSet = 0;"
                        + "var f = {\n"
                        + "   [Symbol.hasInstance](value) { "
                        + "       globalSet = 1;"
                        + "       return true;"
                        + "   }"
                        + "}\n"
                        + "var g = {}\n"
                        + "Object.setPrototypeOf(g, f);\n"
                        + "g instanceof f;"
                        + "globalSet == 1";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testThrowTypeErrorOnNonObjectIncludingSymbol() {
        String script =
                ""
                        + "var f = function() {}; \n"
                        + "f.prototype = Symbol(); \n"
                        + "f[Symbol.hasInstance]({})";
        Utils.assertEcmaErrorES6(
                "TypeError: 'prototype' property of  is not an object. (test#3)", script);
    }
}
