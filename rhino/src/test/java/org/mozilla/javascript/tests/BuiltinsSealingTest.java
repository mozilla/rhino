package org.mozilla.javascript.tests;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.testutils.Utils;

class BuiltinsSealingTest {
    @Test
    public void object() {
        assertIsSealed("Object");
    }

    @Test
    public void function() {
        assertIsSealed("Function");
    }

    @Test
    public void errors() {
        assertIsSealed("Error");
        assertIsSealed("AggregateError");
        assertIsSealed("EvalError");
        assertIsSealed("RangeError");
        assertIsSealed("ReferenceError");
        assertIsSealed("SyntaxError");
        assertIsSealed("TypeError");
        assertIsSealed("URIError");
        assertIsSealed("InternalError");
        assertIsSealed("JavaException");
    }

    @Test
    public void array() {
        assertIsSealed("Array");
    }

    @Test
    public void string() {
        assertIsSealed("String");
    }

    @Test
    public void jsBoolean() {
        assertIsSealed("Boolean");
    }

    @Test
    public void number() {
        assertIsSealed("Number");
    }

    @Test
    public void date() {
        assertIsSealed("Date");
    }

    @Test
    public void math() {
        assertIsSealedNoPrototype("Math");
    }

    @Test
    public void json() {
        assertIsSealedNoPrototype("JSON");
    }

    @Test
    void regexp() {
        assertIsSealed("RegExp");
    }

    @Test
    public void rhinoNonStandard() {
        assertIsSealed("With");
        assertIsSealed("Call");
        assertIsSealed("CallSite");
        assertIsSealed("Iterator");
        assertIsSealed("Continuation");
    }

    @Test
    public void typedArrays() {
        assertIsSealed("ArrayBuffer");
        assertIsSealed("Int8Array");
        assertIsSealed("Uint8Array");
        assertIsSealed("Uint8ClampedArray");
        assertIsSealed("Int16Array");
        assertIsSealed("Uint16Array");
        assertIsSealed("Int32Array");
        assertIsSealed("Uint32Array");
        assertIsSealed("BigInt64Array");
        assertIsSealed("BigUint64Array");
        assertIsSealed("Float32Array");
        assertIsSealed("Float64Array");
        assertIsSealed("DataView");
    }

    @Test
    public void map() {
        assertIsSealed("Map");
    }

    @Test
    public void promise() {
        assertIsSealed("Promise");
    }

    @Test
    public void set() {
        assertIsSealed("Set");
    }

    @Test
    public void weakMap() {
        assertIsSealed("WeakMap");
    }

    @Test
    public void weakSet() {
        assertIsSealed("WeakSet");
    }

    @Test
    public void bigInt() {
        assertIsSealed("BigInt");
    }

    @Test
    public void proxy() {
        assertIsSealedNoPrototype("Proxy");
    }

    @Test
    public void reflect() {
        assertIsSealedNoPrototype("Reflect");
    }

    /** Checks both X and X.prototype */
    private static void assertIsSealed(String builtinName) {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ECMASCRIPT);
                    TopLevel scope = new TopLevel();
                    cx.initStandardObjects(scope, true);

                    assertThrows(
                            EvaluatorException.class,
                            () ->
                                    cx.evaluateString(
                                            scope, builtinName + ".a = 'a'", "test.js", 1, null));
                    assertThrows(
                            EvaluatorException.class,
                            () ->
                                    cx.evaluateString(
                                            scope,
                                            builtinName + ".prototype.a = 'a'",
                                            "test.js",
                                            1,
                                            null));
                    return null;
                });
    }

    /** Checks only X, doesn't try to access X.prototype (but checks it's undefined) */
    private static void assertIsSealedNoPrototype(String builtinName) {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ECMASCRIPT);
                    TopLevel scope = new TopLevel();
                    cx.initStandardObjects(scope, true);

                    assertThrows(
                            EvaluatorException.class,
                            () ->
                                    cx.evaluateString(
                                            scope, builtinName + ".a = 'a'", "test.js", 1, null));
                    assertTrue(
                            Undefined.isUndefined(
                                    cx.evaluateString(
                                            scope,
                                            builtinName + ".prototype",
                                            "test.js",
                                            1,
                                            null)));
                    return null;
                });
    }
}
