package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.tests.Utils;

class SuperTest {
    @Nested
    class LexerParser {
        @Test
        void superIsNotAKeywordUntilES6() {
            try (Context cx = Context.enter()) {
                cx.setLanguageVersion(Context.VERSION_1_8);
                Script test = cx.compileString("var super = 42;", "test", 1, null);
                assertNotNull(test);
            }
        }

        @Test
        void superIsAKeywordInES6AndCannotBeUsedAsVariableName() {
            Utils.assertEvaluatorExceptionES6("missing variable name (test#1)", "var super = 42;");
        }

        @Test
        void isSyntaxErrorIfHasSuperCall() {
            Utils.assertEvaluatorExceptionES6(
                    "super should be inside a shorthand function (test#1)",
                    "({ method() { super(); }});");
        }

        @Test
        void superCannotBeUsedInAPropertyValue() {
            Utils.assertEvaluatorExceptionES6(
                    "super should be inside a shorthand function (test#1)",
                    "var o = { a: super.b }");
        }

        @Test
        void superCannotBeUsedInAComputedPropertyName() {
            Utils.assertEvaluatorExceptionES6(
                    "super should be inside a shorthand function (test#1)",
                    "var o = { [super.x]: 42 }");
        }

        @Test
        void superCannotBeUsedInANonShorthandMethod() {
            Utils.assertEvaluatorExceptionES6(
                    "super should be inside a shorthand function (test#1)",
                    "var o = { f: function() { super.x } }");
        }

        @Test
        void superCannotHaveOptionalPropertyAccess() {
            Utils.assertEvaluatorExceptionES6(
                    "super is not allowed in an optional chaining expression (test#1)",
                    "var o = { f() { super?.x } }");
        }

        @Test
        void superNestedInAFunctionInsideAMethodIsNotAllowed() {
            Utils.assertEvaluatorExceptionES6(
                    "super should be inside a shorthand function (test#2)",
                    "var o = { f() {\n" + "  (function() { super.x; })() \n" + "} }");
        }
    }

    @Nested
    class PropertyRead {
        @Test
        void byName() {
            String script =
                    ""
                            + "const a = { x: 1 };\n"
                            + "const b = {\n"
                            + "  x: 2,\n"
                            + "  f() {\n"
                            + "    return super.x;\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "b.f();";

            Utils.assertWithAllModes_ES6(1, script);
        }

        @Test
        void byIndex() {
            String script =
                    ""
                            + "const a = { [42]: 1 };\n"
                            + "const b = {\n"
                            + "  f() {\n"
                            + "    return super[42];\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "b.f();";

            Utils.assertWithAllModes_ES6(1, script);
        }

        @Test
        void byIndexNegative() {
            String script =
                    ""
                            + "var a = { [-1]: 1 };"
                            + "var b = {\n"
                            + "  f() {\n"
                            + "    return super[-1];\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "b.f();";

            Utils.assertWithAllModes_ES6(1, script);
        }

        @Test
        void byIndexFractional() {
            String script =
                    ""
                            + "var a = { [0.1]: 1 };"
                            + "var b = {\n"
                            + "  f() {\n"
                            + "    return super[0.1];\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "b.f();";

            Utils.assertWithAllModes_ES6(1, script);
        }

        @Test
        void byElementString() {
            String script =
                    ""
                            + "const a = { x: 1 };\n"
                            + "const b = {\n"
                            + "  x: 2,\n"
                            + "  f() {\n"
                            + "    return super['x'];\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "b.f();";

            Utils.assertWithAllModes_ES6(1, script);
        }

        @Test
        void byElementIndex() {
            String script =
                    ""
                            + "const a = { [42]: 1 };\n"
                            + "const b = {\n"
                            + "  [42]: 2,\n"
                            + "  f() {\n"
                            + "    return super['42'];\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "b.f();";

            Utils.assertWithAllModes_ES6(1, script);
        }

        @Test
        void byElementSymbol() {
            String script =
                    ""
                            + "const s = Symbol();\n"
                            + "const a = { [s]: 1 };\n"
                            + "const b = {\n"
                            + "  [s]: 2,\n"
                            + "  f() {\n"
                            + "    return super[s];\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "b.f();";

            Utils.assertWithAllModes_ES6(1, script);
        }

        @Test
        void getter() {
            String script =
                    ""
                            + "const a = { x: 1 };\n"
                            + "const b = {\n"
                            + "  get f() {\n"
                            + "    return super.x;\n"
                            + "  }\n"
                            + "};\n"
                            + "\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "b.f;";

            Utils.assertWithAllModes_ES6(1, script);
        }

        @Test
        void getterWithThis() {
            String script =
                    ""
                            + "const a = { x: 'a', get y() { return this.x + 'x'; } };\n"
                            + "const b = { x: 'b', get y() { return super['y'] + 'y'; } };\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "b.y;";

            Utils.assertWithAllModes_ES6("bxy", script);
        }

        @Test
        void superInDefaultArguments() {
            String script =
                    ""
                            + "const a = { x: 'a'};\n"
                            + "const b = { x: 'b', f(p = super.x) { return p; } };\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "b.f();";

            Utils.assertWithAllModes_ES6("a", script);
        }

        @Test
        void usesHomeObjectAndIgnoresThis1() {
            String script =
                    ""
                            + "const a = { x: 1 };\n"
                            + "const b = {\n"
                            + "  f() {\n"
                            + "    return super.x;\n"
                            + "  }\n"
                            + "};\n"
                            + "\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "var fn = b.f;\n"
                            + "fn()\n";

            Utils.assertWithAllModes_ES6(1, script);
        }

        @Test
        void usesHomeObjectAndIgnoresThis2() {
            String script =
                    ""
                            + "const a = { x: 1 };\n"
                            + "const b = {\n"
                            + "  f() {\n"
                            + "    return super.x;\n"
                            + "  }\n"
                            + "};\n"
                            + "\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "var fn = b.f;\n"
                            + "var c = {x: 42};\n"
                            + "var d = { fn };\n"
                            + "Object.setPrototypeOf(d, c)\n"
                            + "d.fn()\n";

            Utils.assertWithAllModes_ES6(1, script);
        }

        @Test
        void nestedObjects() {
            String script =
                    ""
                            + "const protoX = { x: 'x' };\n"
                            + "const protoY = { y: 'y' };\n"
                            + "\n"
                            + "const obj = {\n"
                            + "  f() {\n"
                            + "    var nested = { g() { return super.x; } };\n"
                            + "    Object.setPrototypeOf(nested, protoX);\n"
                            + "    return nested.g() + super.y;\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(obj, protoY);\n"
                            + "obj.f();";

            Utils.assertWithAllModes_ES6("xy", script);
        }

        @Test
        void nestedObjectsLambda() {
            String script =
                    ""
                            + "const protoX = { x: 'x' };\n"
                            + "const protoY = { y: 'y' };\n"
                            + "\n"
                            + "const obj = {\n"
                            + "  f() {\n"
                            + "    var nested = { g() { return () => super.x; } };\n"
                            + "    Object.setPrototypeOf(nested, protoX);\n"
                            + "    return nested.g()() + super.y;\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(obj, protoY);\n"
                            + "obj.f();";

            Utils.assertWithAllModes_ES6("xy", script);
        }

        @Test
        void getPropNoWarnPropertyFound() {
            String script =
                    ""
                            + "const a = { x: 1 };\n"
                            + "const b = {\n"
                            + "  x: 'a string',\n"
                            + "  f() {\n"
                            + "    return typeof super.x;\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "b.f();";

            Utils.assertWithAllModes_ES6("number", script);
        }

        @Test
        void getPropNoWarnPropertyMissing() {
            String script =
                    ""
                            + "const a = {};\n"
                            + "const b = {\n"
                            + "  x: 'a string',\n"
                            + "  f() {\n"
                            + "    return typeof super.x;\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "b.f();";

            Utils.assertWithAllModes_ES6("undefined", script);
        }

        @Test
        void getPropMissingWithContextFeatureStrictMode() {
            ContextFactory factory =
                    new ContextFactory() {
                        @Override
                        protected boolean hasFeature(Context cx, int featureIndex) {
                            if (featureIndex == Context.FEATURE_STRICT_MODE) {
                                return true;
                            }
                            return super.hasFeature(cx, featureIndex);
                        }
                    };

            Utils.runWithAllModes(
                    factory,
                    cx -> {
                        AtomicBoolean warningReported = new AtomicBoolean(false);
                        cx.setErrorReporter(
                                new ErrorReporter() {
                                    @Override
                                    public void warning(
                                            String message,
                                            String sourceName,
                                            int line,
                                            String lineSource,
                                            int lineOffset) {
                                        assertEquals(
                                                "Reference to undefined property \"x\"", message);
                                        warningReported.set(true);
                                    }

                                    @Override
                                    public void error(
                                            String message,
                                            String sourceName,
                                            int line,
                                            String lineSource,
                                            int lineOffset) {
                                        fail("should not have been called");
                                    }

                                    @Override
                                    public EvaluatorException runtimeError(
                                            String message,
                                            String sourceName,
                                            int line,
                                            String lineSource,
                                            int lineOffset) {
                                        fail("should not have been called");
                                        return null;
                                    }
                                });

                        cx.setLanguageVersion(Context.VERSION_ES6);
                        String script =
                                ""
                                        + "const a = {};\n"
                                        + "const b = {\n"
                                        + "  x: 'a string',\n"
                                        + "  f() {\n"
                                        + "    return super.x;\n"
                                        + "  }\n"
                                        + "};\n"
                                        + "Object.setPrototypeOf(b, a);\n"
                                        + "b.f();";
                        Object result =
                                cx.evaluateString(
                                        cx.initStandardObjects(), script, "test", 1, null);
                        assertEquals(Undefined.instance, result);

                        assertTrue(warningReported.get());
                        return null;
                    });
        }

        @Test
        void propertyNotFoundInSuper() {
            // super is implicitly Object.prototype here
            String script =
                    ""
                            + "const o = {\n"
                            + "  f() {\n"
                            + "    return super.x;\n"
                            + "  },"
                            + "   x: 42\n"
                            + "};\n"
                            + "o.f();";

            Utils.assertWithAllModes_ES6(Undefined.instance, script);
        }

        @Test
        void propertyNotFoundInSuperByElement() {
            // super is implicitly Object.prototype here
            String script =
                    ""
                            + "const xAsString = 'x';\n"
                            + "const o = {\n"
                            + "  f() {\n"
                            + "    return super[xAsString];\n"
                            + "  }"
                            + "};\n"
                            + "o.f();";

            Utils.assertWithAllModes_ES6(Undefined.instance, script);
        }

        @Test
        void propertyNotFoundInSuperByIndex() {
            // super is implicitly Object.prototype here
            String script =
                    ""
                            + "const o = {\n"
                            + "  f() {\n"
                            + "    return super[2];\n"
                            + "  }"
                            + "};\n"
                            + "o.f();";

            Utils.assertWithAllModes_ES6(Undefined.instance, script);
        }

        @Test
        void prototypeIsNull() {
            String script =
                    ""
                            + "var obj = {\n"
                            + "  method() {\n"
                            + "      super.x;\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(obj, null);\n"
                            + "obj.method();";

            Utils.assertEcmaErrorES6(
                    "TypeError: Cannot read property \"x\" from null (test#3)", script);
        }
    }

    @Nested
    class PropertyMutate {
        @Test
        void byName() {
            String script =
                    ""
                            + "var proto = { x: 'proto' };"
                            + "var object = {\n"
                            + "  x: 'obj',\n"
                            + "  f() { super.x = 'new'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();"
                            + "object.x + ':' + proto.x";

            Utils.assertWithAllModes_ES6("new:proto", script);
        }

        @Test
        void byIndex() {
            String script =
                    ""
                            + "var proto = { [42]: 'proto' };"
                            + "var object = {\n"
                            + "  [42]: 'obj',\n"
                            + "  f() { super[42] = 'new'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();"
                            + "object[42] + ':' + proto[42]";

            Utils.assertWithAllModes_ES6("new:proto", script);
        }

        @Test
        void byIndexNegative() {
            String script =
                    ""
                            + "var proto = { [-1]: 'proto' };"
                            + "var object = {\n"
                            + "  [-1]: 'obj',\n"
                            + "  f() { super[-1] = 'new'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();"
                            + "object[-1] + ':' + proto[-1]";

            Utils.assertWithAllModes_ES6("new:proto", script);
        }

        @Test
        void byIndexFractional() {
            String script =
                    ""
                            + "var proto = { [0.1]: 'proto' };"
                            + "var object = {\n"
                            + "  [0.1]: 'obj',\n"
                            + "  f() { super[0.1] = 'new'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();"
                            + "object[0.1] + ':' + proto[0.1]";

            Utils.assertWithAllModes_ES6("new:proto", script);
        }

        @Test
        void byElementString() {
            String script =
                    ""
                            + "var proto = { x: 'proto' };"
                            + "var object = {\n"
                            + "  x: 'obj',\n"
                            + "  f() { super['x'] = 'new'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();"
                            + "object.x+ ':' + proto.x";

            Utils.assertWithAllModes_ES6("new:proto", script);
        }

        @Test
        void byElementIndex() {
            String script =
                    ""
                            + "var proto = { [42]: 'proto' };"
                            + "var object = {\n"
                            + "  [42]: 'obj',\n"
                            + "  f() { super['42'] = 'new'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();"
                            + "object[42] + ':' + proto[42]";

            Utils.assertWithAllModes_ES6("new:proto", script);
        }

        @Test
        void byElementSymbol() {
            String script =
                    ""
                            + "const s = Symbol();"
                            + "var proto = { [s]: 'proto' };"
                            + "var object = {\n"
                            + "  [s]: 'obj',\n"
                            + "  f() { super[s] = 'new'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();"
                            + "object[s] + ':' + proto[s]";

            Utils.assertWithAllModes_ES6("new:proto", script);
        }

        @Test
        void setter() {
            String script =
                    ""
                            + "var proto = {\n"
                            + "  x: 0\n"
                            + "};\n"
                            + "var object = {\n"
                            + "  set f(v) {\n"
                            + "    super.x = v;\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f = 1;\n"
                            + "object.x + ':' + proto.x";

            Utils.assertWithAllModes_ES6("1:0", script);
        }

        @Test
        void setterWithThis() {
            String script =
                    ""
                            + "var proto = {\n"
                            + "  _x: 0,\n"
                            + "  set x(v) {\n"
                            + "    return this._x = v;\n"
                            + "  }\n"
                            + "};\n"
                            + "var object = {\n"
                            + "  set x(v) {\n"
                            + "    super.x = v;\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.x = 1;\n"
                            + "object._x + ':' + proto._x";

            Utils.assertWithAllModes_ES6("1:0", script);
        }

        @Test
        void propertyNotFoundInSuper() {
            // super is implicitly Object.prototype here
            String script =
                    ""
                            + "const o = {\n"
                            + "  f() {\n"
                            + "    super.x = 1;\n"
                            + "  },"
                            + "   x: 42\n"
                            + "};\n"
                            + "o.f();\n"
                            + "o.x + ':' + Object.prototype.x";

            Utils.assertWithAllModes_ES6("1:undefined", script);
        }

        @Test
        void superPropertyNotWritableIgnoredSilentlyInNonStrictMode() {
            String script =
                    ""
                            + "var proto = { x: 'proto' };\n"
                            + "var object = {\n"
                            + "  x: 'obj',\n"
                            + "  f() { super.x = 'new'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "Object.defineProperty(proto, 'x', {writable: false});\n"
                            + "object.f();\n"
                            + "object.x + ':' + proto.x";

            Utils.assertWithAllModes_ES6("obj:proto", script);
        }

        @Test
        void thisPropertyNotWritableIgnoredSilentlyInNonStrictMode() {
            String script =
                    ""
                            + "var proto = { x: 'proto' };\n"
                            + "var object = {\n"
                            + "  x: 'obj',\n"
                            + "  f() { super.x = 'new'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "Object.defineProperty(object, 'x', {writable: false});\n"
                            + "object.f();\n"
                            + "object.x + ':' + proto.x";

            Utils.assertWithAllModes_ES6("obj:proto", script);
        }

        @Test
        void superPropertyNotWritableInStrictIsError() {
            String script =
                    ""
                            + "'use strict';\n"
                            + "var proto = { x: 'proto' };\n"
                            + "var object = {\n"
                            + "  x: 'obj',\n"
                            + "  f() { super.x = 'new'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "Object.defineProperty(proto, 'x', {writable: false});\n"
                            + "object.f();\n"
                            + "object.x + ':' + proto.x";

            Utils.assertEcmaErrorES6(
                    "TypeError: Cannot modify readonly property: x. (test#5)", script);
        }

        @Test
        void thisPropertyNotWritableInStrictIsError() {
            String script =
                    ""
                            + "'use strict';\n"
                            + "var proto = { x: 'proto' };\n"
                            + "var object = {\n"
                            + "  x: 'obj',\n"
                            + "  f() { super.x = 'new'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "Object.defineProperty(object, 'x', {writable: false});\n"
                            + "object.f();\n"
                            + "object.x + ':' + proto.x";

            Utils.assertEcmaErrorES6(
                    "TypeError: Cannot modify readonly property: x. (test#5)", script);
        }

        @Test
        void prototypeIsNull() {
            String script =
                    ""
                            + "var obj = {\n"
                            + "  method() {\n"
                            + "      super.x = 42;\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(obj, null);\n"
                            + "obj.method();";

            Utils.assertEcmaErrorES6(
                    "TypeError: Cannot set property \"x\" of null to \"42\" (test#3)", script);
        }

        @Test
        void missingPropertyPrototypeSealedCreatesItOnTheThisObject() {
            String script =
                    ""
                            + "var proto = {};\n"
                            + "var object = {\n"
                            + "  f() { super.x = 1; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "Object.seal(proto);\n"
                            + "object.f();\n"
                            + "object.x + ':' + proto.x";

            Utils.assertWithAllModes_ES6("1:undefined", script);
        }

        @Test
        void missingPropertyThisSealedIsIgnoredSilentlyInNonStrictMode() {
            String script =
                    ""
                            + "var proto = {};\n"
                            + "var object = {\n"
                            + "  f() { super.x = 1; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "Object.seal(object);\n"
                            + "object.f();\n"
                            + "object.x + ':' + proto.x";

            Utils.assertWithAllModes_ES6("undefined:undefined", script);
        }

        @Test
        void missingPropertyThisSealedIsErrorInStrictMode() {
            String script =
                    ""
                            + "'use strict';\n"
                            + "var proto = {};\n"
                            + "var object = {\n"
                            + "  f() { super.x = 1; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "Object.seal(object);\n"
                            + "object.f();\n"
                            + "object.x + ':' + proto.x";

            Utils.assertEcmaErrorES6(
                    "TypeError: Cannot add properties to this object because extensible is false. (test#4)",
                    script);
        }

        @Test
        void modifyOperatorByName() {
            // Equivalent to `super.x = super.x + 1`, so reads from super, writes in this
            String script =
                    ""
                            + "var proto = { x: 'proto' };"
                            + "var object = {\n"
                            + "  x: 'obj',\n"
                            + "  f() { super.x += '1'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();"
                            + "object.x + ':' + proto.x";

            Utils.assertWithAllModes_ES6("proto1:proto", script);
        }

        @Test
        void modifyOperatorByKey() {
            // Equivalent to `super[x] = super[x] + 1`, so reads from super, writes in this
            String script =
                    ""
                            + "const xAsStr = 'x';\n"
                            + "var proto = { x: 'proto' };"
                            + "var object = {\n"
                            + "  x: 'obj',\n"
                            + "  f() { super[xAsStr] += '1'; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();"
                            + "object.x + ':' + proto.x";

            Utils.assertWithAllModes_ES6("proto1:proto", script);
        }

        @Test
        void deleteNotAllowed() {
            String script =
                    ""
                            + "var catchHit = false;\n"
                            + "var getterCalled = false;\n"
                            + "var proto = { get x() { getterCalled = true; } };"
                            + "var object = {\n"
                            + "  f() {\n"
                            + "    try {\n"
                            + "      delete super.x;\n"
                            + "    } catch (err) {\n"
                            + "      catchHit = err instanceof ReferenceError;"
                            + "    }\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();\n"
                            + "catchHit + ':' + getterCalled";

            Utils.assertWithAllModes_ES6("true:false", script);
        }

        @Test
        void deleteSuperFirstEvaluatesPropertyKey() {
            String script =
                    ""
                            + "var catchHit = false;\n"
                            + "var gCalled = false;\n"
                            + "var proto = { x: 1 };\n"
                            + "function g() { gCalled = true; return 'x'; }\n"
                            + " object = {\n"
                            + "  f() {\n"
                            + "    try {\n"
                            + "      delete super[g()];\n"
                            + "    } catch (err) {\n"
                            + "      catchHit = err instanceof ReferenceError;"
                            + "    }\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();\n"
                            + "catchHit + ':' + gCalled";

            Utils.assertWithAllModes_ES6("true:true", script);
        }

        @Test
        void memberIncrementPostfix() {
            String script =
                    ""
                            + "var proto = { x: 1 };"
                            + "var object = {\n"
                            + "  x: 42,\n"
                            + "  f() { return super.x++; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "var f = object.f();"
                            + "f + ':' + object.x + ':' + proto.x";

            Utils.assertWithAllModes_ES6("1:2:1", script);
        }

        @Test
        void memberIncrementPrefix() {
            String script =
                    ""
                            + "var proto = { x: 1 };"
                            + "var object = {\n"
                            + "  x: 42,\n"
                            + "  f() { return ++super.x; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "var f = object.f();"
                            + "f + ':' + object.x + ':' + proto.x";

            Utils.assertWithAllModes_ES6("2:2:1", script);
        }

        @Test
        void elementDecrementPostfix() {
            String script =
                    ""
                            + "var proto = { 0: 1 };"
                            + "var object = {\n"
                            + "  0: 42,\n"
                            + "  f() { return super[0]--; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "var f = object.f();"
                            + "f + ':' + object[0] + ':' + proto[0]";

            Utils.assertWithAllModes_ES6("1:0:1", script);
        }

        @Test
        void elementDecrementPrefix() {
            String script =
                    ""
                            + "var proto = { 0: 1 };"
                            + "var object = {\n"
                            + "  0: 42,\n"
                            + "  f() { return --super[0]; }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "var f = object.f();"
                            + "f + ':' + object[0] + ':' + proto[0]";

            Utils.assertWithAllModes_ES6("0:0:1", script);
        }
    }

    @Nested
    class MethodCall {
        @Test
        void methodsAreResolvedOnSuperObject() {
            final String script =
                    ""
                            + "const proto = {\n"
                            + "  f(x) {\n"
                            + "    return 'prototype' + x;\n"
                            + "  }\n"
                            + "};\n"
                            + "const obj = {\n"
                            + "  f() {\n"
                            + "    return super.f(1);\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(obj, proto);\n"
                            + "obj.f();";

            Utils.assertWithAllModes_ES6("prototype1", script);
        }

        // All the n-arguments variants are necessary because we have optimized code paths in
        // compiled classes
        // for 0, 1, 2, and N arguments

        @Test
        void thisIsSetCorrectly0Args() {
            String script =
                    ""
                            + "var proto = {\n"
                            + "  x: 'proto',\n"
                            + "  f() {\n"
                            + "    return this.x;\n"
                            + "  }\n"
                            + "};\n"
                            + "var object = {\n"
                            + "   x: 'object',\n"
                            + "   g() {\n"
                            + "    return super.f();\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.g();";

            Utils.assertWithAllModes_ES6("object", script);
        }

        @Test
        void thisIsSetCorrectly1Arg() {
            String script =
                    ""
                            + "var proto = {\n"
                            + "  x: 'proto',\n"
                            + "  f(a) {\n"
                            + "    return this.x + ':' + a;\n"
                            + "  }\n"
                            + "};\n"
                            + "var object = {\n"
                            + "   x: 'object',\n"
                            + "   g() {\n"
                            + "    return super.f('a');\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.g();";

            Utils.assertWithAllModes_ES6("object:a", script);
        }

        @Test
        void thisIsSetCorrectly2Args() {
            String script =
                    ""
                            + "var proto = {\n"
                            + "  x: 'proto',\n"
                            + "  f(a, b) {\n"
                            + "    return this.x + ':' + a + ':' + b;\n"
                            + "  }\n"
                            + "};\n"
                            + "var object = {\n"
                            + "   x: 'object',\n"
                            + "   g() {\n"
                            + "    return super.f('a', 'b');\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.g();";

            Utils.assertWithAllModes_ES6("object:a:b", script);
        }

        @Test
        void thisIsSetCorrectlyNArgs() {
            String script =
                    ""
                            + "var proto = {\n"
                            + "  x: 'proto',\n"
                            + "  f(a, b, c) {\n"
                            + "    return this.x + ':' + a + ':' + b + ':' + c;\n"
                            + "  }\n"
                            + "};\n"
                            + "var object = {\n"
                            + "   x: 'object',\n"
                            + "   g() {\n"
                            + "    return super.f('a', 'b', 'c');\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.g();";

            Utils.assertWithAllModes_ES6("object:a:b:c", script);
        }

        @Test
        void lastScratchScriptableIsCleanedUpProperly() {
            String script =
                    ""
                            + "function f1() { return 'f1'; }\n"
                            + "var proto = {\n"
                            + "  f2() {\n"
                            + "    return 'f2';\n"
                            + "  }\n"
                            + "};\n"
                            + "var object = {\n"
                            + "   f3() {\n"
                            + "    return super.f2() + f1();\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f3();";

            Utils.assertWithAllModes_ES6("f2f1", script);
        }

        @Test
        void thisIsSetCorrectlyForTemplateLiteralCall() {
            String script =
                    ""
                            + "var proto = {\n"
                            + "  x: 'proto',\n"
                            + "  f() {\n"
                            + "    return this.x;\n"
                            + "  }\n"
                            + "};\n"
                            + "var object = {\n"
                            + "   x: 'object',\n"
                            + "   f() {\n"
                            + "    return super.f`some ignored string`;\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();";

            Utils.assertWithAllModes_ES6("object", script);
        }

        @Test
        void nestedLambdaCaptureSuper() {
            String script =
                    ""
                            + "var proto = {\n"
                            + "  x: 'proto',\n"
                            + "  f() {\n"
                            + "    return this.x;\n"
                            + "  }\n"
                            + "};\n"
                            + "var object = {\n"
                            + "   x: 'object',\n"
                            + "   f() {\n"
                            + "    return () => { return super.f(); };\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f()();";

            Utils.assertWithAllModes_ES6("object", script);
        }

        @Test
        void doublyNestedLambdaCaptureSuper() {
            String script =
                    ""
                            + "var proto = {\n"
                            + "  x: 'proto',\n"
                            + "  f() {\n"
                            + "    return this.x;\n"
                            + "  }\n"
                            + "};\n"
                            + "var object = {\n"
                            + "   x: 'object',\n"
                            + "   f() {\n"
                            + "    return () => { return () => { return super.f(); } };\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f()()();";

            Utils.assertWithAllModes_ES6("object", script);
        }
    }

    /**
     * Test cases related to the special handling of REF in Rhino, which includes the special
     * properties __proto__ and __parent__. It also includes XML stuff, but we do not support it for
     * super.
     */
    @Nested
    class Ref {
        @Test
        void propertyGet() {
            String script =
                    ""
                            + "var a = {x: 'a'};\n"
                            + "var b = {x: 'b'};\n"
                            + "var c = {x: 'c',\n"
                            + "  f() {\n"
                            + "    return super.__proto__.x;\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(c, b);\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "c.f();";

            Utils.assertWithAllModes_ES6("b", script);
        }

        @Test
        void propertyGetter() {
            String script =
                    ""
                            + "var a = {get x() { return 'a' + this.y }, y: 'a' };\n"
                            + "var b = {get x() { return 'b' + this.y }, y: 'b' };\n"
                            + "var c = {\n"
                            + "  get x() { return 'c' + this.y }, y: 'c',\n"
                            + "  f() { return super.__proto__.x },"
                            + "};\n"
                            + "Object.setPrototypeOf(c, b);\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "c.f();";
            Utils.assertWithAllModes_ES6("bb", script);
        }

        @Test
        void propertySet() {
            String script =
                    ""
                            + "var a = {x: 'a'};\n"
                            + "var b = {x: 'b'};\n"
                            + "var c = {x: 'c',\n"
                            + "  f() {\n"
                            + "    super.__proto__ = a;\n"
                            + "    return Object.getPrototypeOf(this).x;\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(c, b);\n"
                            + "Object.setPrototypeOf(b, a);\n"
                            + "c.f();";

            Utils.assertWithAllModes_ES6("a", script);
        }
    }

    @Nested
    class Eval {
        @Test
        void evalInsideMethodCanAccessSuper() {
            String script =
                    ""
                            + "var proto = {\n"
                            + "  x: 'proto'\n"
                            + "};\n"
                            + "var object = {\n"
                            + "   x: 'object',\n"
                            + "   f() {\n"
                            + "    return eval('super.x');\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f();";

            Utils.assertWithAllModes_ES6("proto", script);
        }

        @Test
        void evalFromLambdaInMethodCanAccessSuper() {
            String script =
                    ""
                            + "var proto = {\n"
                            + "  x: 'proto'\n"
                            + "};\n"
                            + "var object = {\n"
                            + "   x: 'object',\n"
                            + "   f() {\n"
                            + "    return () => eval('super.x');\n"
                            + "  }\n"
                            + "};\n"
                            + "Object.setPrototypeOf(object, proto);\n"
                            + "object.f()();";

            Utils.assertWithAllModes_ES6("proto", script);
        }

        @Test
        void superCannotBeUsedAsMethodInEval() {
            String script =
                    ""
                            + "o = {\n"
                            + "  f() {\n"
                            + "    eval('super(42)')"
                            + "  }\n"
                            + "};"
                            + "o.f();";

            Utils.assertEcmaErrorES6(
                    "SyntaxError: super should be inside a shorthand function (test#3(eval)#1)",
                    script);
        }

        @Test
        void evalOutsideMethodCannotAccessSuper() {
            Utils.assertEcmaErrorES6(
                    "SyntaxError: super should be inside a shorthand function (test#1(eval)#1)",
                    "eval('super.x')");
        }

        @Test
        void evalInFunctionInsideMethodDoesNotAllowSuper() {
            String script =
                    ""
                            + "o = {\n"
                            + "  f() {\n"
                            + "    (function() { eval('super(42)') })();"
                            + "  }\n"
                            + "};"
                            + "o.f();";
            Utils.assertEcmaErrorES6(
                    "SyntaxError: super should be inside a shorthand function (test#3(eval)#1)",
                    script);
        }
    }
}
