package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Test that default parameter evaluation happens _before_ generator object is created. Ref: Ecma
 * 2026, 10.2.11 FunctionDeclarationInstantiation
 */
public class GeneratorDefaultParamsTest {

    @Test
    public void testGeneratorCreatedAfterDeclInst() {
        // Default parameter evaluation modifies g.prototype to null
        // Generator instance should be created with the old prototype value
        Utils.assertWithAllModes_ES6(
                Boolean.FALSE,
                "var g = function*(a = (g.prototype = null)) {};"
                        + "var oldPrototype = g.prototype;"
                        + "var it = g();"
                        + "Object.getPrototypeOf(it) === oldPrototype;");
    }

    @Test
    public void testArgumentsCaptureWithShadowing() {
        // The 'arguments' object should be captured in default params
        // even when shadowed by a function declaration
        Utils.assertWithAllModes_ES6(
                0,
                "var args;"
                        + "var g = function* (x = args = arguments) {"
                        + "  function arguments() {}"
                        + "};"
                        + "g().next();"
                        + "args.length;");
    }

    @Test
    public void testDefaultParamException() {
        // Exceptions in default parameter evaluation should propagate
        Utils.assertJavaScriptException_ES6(
                "Error:",
                "var g = function*(_ = (function() { throw new Error('test'); }())) {};" + "g();");
    }

    @Test
    public void testDestructuringParamExceptionFunction() {
        // Destructuring of generator parameters must happen _before_ the
        // generator object is created, so iterator failures propagate at
        // call time rather than on the first .next().
        Utils.assertJavaScriptException_ES6(
                "Error:",
                "var iter = {};"
                        + "iter[Symbol.iterator] = function() {"
                        + "  throw new Error('should fail at call');"
                        + "};"
                        + "var g = function*([x]) {};"
                        + "g(iter);");
    }

    @Test
    public void testDestructuringParamExceptionMethod() {
        // Same as above, but for a generator defined as a class method.
        Utils.assertJavaScriptException_ES6(
                "Error:",
                "var iter = {};"
                        + "iter[Symbol.iterator] = function() {"
                        + "  throw new Error('should fail at call');"
                        + "};"
                        + "class C { *method([x]) {} };"
                        + "var method = C.prototype.method;"
                        + "method(iter);");
    }

    @Test
    public void testDestructuringParamObjectException() {
        // Destructuring an undefined argument against an object pattern
        // throws a TypeError at call time, before the generator object
        // is created.
        Utils.assertEcmaErrorES6(
                "TypeError: Cannot read property \"x\" from undefined",
                "var g = function*({x}) {}; g();");
    }
}
