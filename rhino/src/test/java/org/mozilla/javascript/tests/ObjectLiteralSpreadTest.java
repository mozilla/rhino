package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.testutils.Utils;

public class ObjectLiteralSpreadTest {
    @Test
    public void testObjectLiteralSpread() {
        String script =
                "var x = { a: 'a', 3: 'd' };\n"
                        + "var y = { ...x, b: 'b' };\n"
                        + "y.a + y.b + y[3.0]";
        Utils.assertWithAllModes_ES6("abd", script);
    }

    @Test
    public void testObjectLiteral() {
        // language=JavaScript
        String script =
                "function e() { return 2; }"
                        + "var x = { a: 'a', "
                        + "          1: 'b', "
                        + "          false: 'c', "
                        + "          3.14: 'd',"
                        + "          [e()]: 'e',"
                        + "            f() { return 'f';}, "
                        + "          get g() { return 'g'}}; "
                        + "x.a + x[1] + x[false] + x[3.14] + x[2] + x.f() + x.g\n";
        Utils.assertWithAllModes_ES6("abcdefg", script);
    }

    @Test
    public void testObjectSpreadBasic() {
        String script =
                "var obj1 = { a: 1, b: 2 };\n"
                        + "var obj2 = { ...obj1, c: 3 };\n"
                        + "obj2.a + obj2.b + obj2.c";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadBasicGenerator() {
        String script =
                "var obj1 = { a: 1, b: 2 };\n"
                        + "function *gen() {\n"
                        + "    yield { ...obj1, c: 3 }\n"
                        + "}\n"
                        + "var g = gen();\n"
                        + "var obj = g.next().value;\n"
                        + "obj.a + obj.b + obj.c;\n";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadOverride() {
        String script =
                "var obj1 = { a: 1, b: 2 };\n"
                        + "var obj2 = { a: 3, ...obj1 };\n"
                        + "obj2.a + obj2.b";
        Utils.assertWithAllModes_ES6(3, script);
    }

    @Test
    public void testObjectSpreadMultiple() {
        String script =
                "var obj1 = { a: 1 };\n"
                        + "var obj2 = { b: 2 };\n"
                        + "var obj3 = { c: 3 };\n"
                        + "var result = { ...obj1, ...obj2, ...obj3 };\n"
                        + "result.a + result.b + result.c";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadWithNullUndefined() {
        String script = "var obj = { ...null, ...undefined, a: 1 };\n" + "obj.a";
        Utils.assertWithAllModes_ES6(1, script);
    }

    @Test
    public void testObjectSpreadWithGetter() {
        String script =
                "var obj1 = { get x() { return 1; } };\n" + "var obj2 = { ...obj1 };\n" + "obj2.x";
        Utils.assertWithAllModes_ES6(1, script);
    }

    @Test
    public void testObjectSpreadWithSymbols() {
        String script =
                "var sym = Symbol('test');\n"
                        + "var obj1 = { [sym]: 'value' };\n"
                        + "var obj2 = { ...obj1 };\n"
                        + "obj2[sym] === 'value' && obj2[sym] === obj1[sym]";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testObjectSpreadWithEnumerable() {
        String script =
                "var x = {};\n"
                        + "Object.defineProperty(x, 'd', { value: 4, enumerable: false})\n"
                        + "var y = { ...x};\n"
                        + "y.d";
        Utils.assertWithAllModes_ES6(Undefined.instance, script);
    }

    @Test
    public void testObjectSpreadInFunctionCall() {
        String script =
                "var obj = { a: 1, b: 2 };\n"
                        + "function test(arg) { return arg.a + arg.b + arg.c; }\n"
                        + "test({ ...obj, c: 3 });";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadInMethodCall() {
        String script =
                "var obj = { a: 1, b: 2 };\n"
                        + "var target = {\n"
                        + "  test: function(arg) { return arg.a + arg.b + arg.c; }\n"
                        + "};\n"
                        + "target.test({ ...obj, c: 3 });";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadInConstructorCall() {
        String script =
                "var obj = { a: 1, b: 2 };\n"
                        + "function TestConstructor(arg) {\n"
                        + "  this.result = arg.a + arg.b + arg.c;\n"
                        + "}\n"
                        + "var instance = new TestConstructor({ ...obj, c: 3 });\n"
                        + "instance.result;";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadInGetterContext() {
        String script =
                "var obj = { a: 1, b: 2 };\n"
                        + "var target = {\n"
                        + "  _value: null,\n"
                        + "  get testGetter() {\n"
                        + "    return this._value.a + this._value.b + this._value.c;\n"
                        + "  },\n"
                        + "  setValue: function(val) { this._value = val; }\n"
                        + "};\n"
                        + "target.setValue({ ...obj, c: 3 });\n"
                        + "target.testGetter;";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadInSetterContext() {
        String script =
                "var obj = { a: 1, b: 2 };\n"
                        + "var target = {\n"
                        + "  _result: 0,\n"
                        + "  set testSetter(val) {\n"
                        + "    this._result = val.a + val.b + val.c;\n"
                        + "  },\n"
                        + "  get result() { return this._result; }\n"
                        + "};\n"
                        + "target.testSetter = { ...obj, c: 3 };\n"
                        + "target.result;";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadInChainedMethodCalls() {
        String script =
                "var obj = { a: 1, b: 2 };\n"
                        + "var target = {\n"
                        + "  process: function(arg) {\n"
                        + "    this.value = arg.a + arg.b + arg.c;\n"
                        + "    return this;\n"
                        + "  },\n"
                        + "  getValue: function() { return this.value; }\n"
                        + "};\n"
                        + "target.process({ ...obj, c: 3 }).getValue();";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadInNestedFunctionCalls() {
        String script =
                "var obj = { a: 1, b: 2 };\n"
                        + "function outer(arg) {\n"
                        + "  function inner(val) {\n"
                        + "    return val.a + val.b + val.c;\n"
                        + "  }\n"
                        + "  return inner(arg);\n"
                        + "}\n"
                        + "outer({ ...obj, c: 3 });";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadInArrayMethodCall() {
        String script =
                "var obj = { a: 1, b: 2 };\n"
                        + "var arr = [{ ...obj, c: 3 }];\n"
                        + "arr.map(function(item) { return item.a + item.b + item.c; })[0];";
        Utils.assertWithAllModes_ES6(6, script);
    }
}
