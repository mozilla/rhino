package org.mozilla.javascript.tests;

import static org.junit.Assert.assertThrows;

import java.io.FileReader;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SymbolKey;

public class LambdaFunctionTest {

    private Context cx;
    private Scriptable root;

    @Before
    public void init() throws IOException {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        root = cx.initStandardObjects();
        try (FileReader rdr = new FileReader("testsrc/assert.js")) {
            cx.evaluateReader(root, rdr, "assert.js", 1, null);
        }
    }

    @After
    public void cleanup() {
        Context.exit();
    }

    private void eval(String source) {
        cx.evaluateString(root, source, "test.js", 1, null);
    }

    @Test
    public void nativeFunction() {
        eval(
                "function foo() { return 'Hello'; }\n"
                        + "assertEquals(foo.name, 'foo');\n"
                        + "assertEquals(foo.length, 0);\n"
                        + "assertEquals(typeof foo, 'function');\n"
                        + "assertEquals(foo(), 'Hello');\n"
                        + "assertTrue(foo.toString().length > 0);\n"
                        + "assertTrue(foo.prototype !== undefined);\n"
                        + "assertTrue(foo.prototype.toString !== undefined);");
    }

    @Test
    public void noArgLambdaFunction() {
        LambdaFunction f =
                new LambdaFunction(
                        root,
                        "foo",
                        0,
                        (Context ctx, Scriptable scope, Scriptable thisObj, Object[] args) -> {
                            return "Hello";
                        });
        ScriptableObject.putProperty(root, "foo", f);
        eval(
                "assertEquals(foo.name, 'foo');\n"
                        + "assertEquals(foo.length, 0);\n"
                        + "assertEquals(typeof foo, 'function');\n"
                        + "assertEquals(foo(), 'Hello');\n"
                        + "assertTrue(foo.toString().length > 0);\n"
                        + "assertTrue(foo.prototype.toString !== undefined);");
    }

    @Test
    public void constructLambdaClass() {
        TestClass.init(root);
        eval(
                "let tc = new TestClass('foo');\n"
                        + "assertEquals(tc.value, 'foo');\n"
                        + "tc.value = 'bar';\n"
                        + "assertEquals(tc.value, 'bar');\n"
                        + "tc.anotherValue = 123;\n"
                        + "assertEquals(tc.anotherValue, 123);\n"
                        + "assertEquals(TestClass.name, 'TestClass');\n"
                        + "assertEquals(TestClass.length, 1);\n"
                        + "assertEquals(typeof TestClass, 'function');\n"
                        + "assertTrue(tc instanceof TestClass);\n");
    }

    @Test
    public void nativePrototypeFunctions() {
        eval(
                "function TestClass(v) { this.value = v; }\n"
                        + "TestClass.prototype.appendToValue = function(x) { return this.value + x; }\n"
                        + "let tc = new TestClass('foo');\n"
                        + "assertEquals(tc.value, 'foo');\n"
                        + "assertEquals(tc.appendToValue('bar'), 'foobar');\n"
                        + "tc.value = 'x';\n"
                        + "assertEquals(tc.appendToValue('x'), 'xx');\n"
                        + "assertEquals(TestClass.prototype.appendToValue.length, 1);\n"
                        + "assertEquals(typeof TestClass.prototype.appendToValue, 'function');");
    }

    @Test
    public void lambdaPrototypeFunctions() {
        TestClass.init(root);
        eval(
                "let tc = new TestClass('foo');\n"
                        + "assertEquals(typeof TestClass.prototype.appendToValue, 'function');\n"
                        + "assertEquals(tc.value, 'foo');\n"
                        + "assertEquals(tc.appendToValue('bar', 'baz'), 'foobarbaz');\n"
                        + "tc.value = 'x';\n"
                        + "assertEquals(tc.appendToValue('x'), 'xx');\n"
                        + "assertEquals(TestClass.prototype.appendToValue.length, 1);\n");
    }

    @Test
    public void lambdaPrototypeFunctionNotFound() {
        TestClass.init(root);
        assertThrows(
                RhinoException.class,
                () -> {
                    eval("let tc = new TestClass('foo');\n" + "tc.notFound();");
                });
    }

    @Test
    public void lambdaPrototypeFunctionInvalidThis() {
        TestClass.init(root);
        eval(
                "let tc = new TestClass();\n"
                        + "assertThrows(function() { tc.appendToValue.call(null, 'invalid'); }, TypeError);\n"
                        + "assertThrows(function() { tc.appendToValue.call(undefined, 'invalid'); }, TypeError);\n"
                        + "assertThrows(function() { tc.appendToValue.call({}, 'invalid'); }, TypeError);\n");
    }

    @Test
    public void lambdaConstructorFunctions() {
        TestClass.init(root);
        eval(
                "assertEquals(TestClass.sayHello('World'), 'Hello, World!');\n"
                        + "assertEquals(TestClass.sayHello.name, 'sayHello');\n"
                        + "assertEquals(TestClass.sayHello.length, 1);\n"
                        + "assertEquals(typeof TestClass.sayHello, 'function');");
    }

    @Test
    public void lambdaConstructorValues() {
        TestClass.init(root);
        eval(
                "let tc = new TestClass();\n"
                        + "assertEquals(tc.protoValue, 123);\n"
                        + "assertEquals(tc[Symbol.species], 456);\n");
    }

    @Test
    public void lambdaConstructorNewOnly() {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        root,
                        "NewOnly",
                        0,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        (Context ctx, Scriptable scope, Object[] args) -> ctx.newObject(scope));
        ScriptableObject.defineProperty(root, "NewOnly", constructor, 0);
        eval(
                "let o = new NewOnly();\n"
                        + "assertEquals('object', typeof o);\n"
                        + "assertThrows(() => { NewOnly(); }, TypeError);");
    }

    @Test
    public void lambdaConstructorFunctionOnly() {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        root,
                        "NewOnly",
                        0,
                        LambdaConstructor.CONSTRUCTOR_FUNCTION,
                        (Context ctx, Scriptable scope, Object[] args) -> ctx.newObject(scope));
        ScriptableObject.defineProperty(root, "NewOnly", constructor, 0);
        eval(
                "let o = NewOnly();\n"
                        + "assertEquals('object', typeof o);\n"
                        + "assertThrows(() => { new NewOnly(); }, TypeError);");
    }

    @Test
    public void lambdaFunctionNoNew() {
        LambdaFunction func =
                new LambdaFunction(
                        root,
                        0,
                        (Context ctx, Scriptable scope, Scriptable thisObj, Object[] args) -> true);
        ScriptableObject.defineProperty(root, "noNewFunc", func, 0);
        eval(
                "let o = noNewFunc();\n"
                        + "assertEquals(true, o);\n"
                        + "assertThrows(() => { new noNewFunc(); }, TypeError)");
    }

    private static class TestClass extends ScriptableObject {

        private String instanceVal;

        public static void init(Scriptable scope) {
            LambdaConstructor constructor =
                    new LambdaConstructor(
                            scope,
                            "TestClass",
                            1,
                            (Context cx, Scriptable s, Object[] args) -> {
                                TestClass tc = new TestClass();
                                if (args.length > 0) {
                                    tc.instanceVal = ScriptRuntime.toString(args[0]);
                                }
                                return tc;
                            });
            constructor.defineConstructorMethod(
                    scope,
                    "sayHello",
                    1,
                    (Context cx, Scriptable s, Scriptable thisObj, Object[] args) ->
                            TestClass.sayHello(args),
                    0);
            constructor.definePrototypeMethod(
                    scope,
                    "appendToValue",
                    1,
                    (Context cx, Scriptable s, Scriptable thisObj, Object[] args) -> {
                        TestClass self =
                                LambdaConstructor.convertThisObject(thisObj, TestClass.class);
                        return self.appendToValue(args);
                    });
            constructor.definePrototypeProperty("protoValue", 123, 0);
            constructor.definePrototypeProperty(SymbolKey.SPECIES, 456, 0);
            ScriptableObject.defineProperty(scope, "TestClass", constructor, PERMANENT);
        }

        @Override
        public String getClassName() {
            return "TestClass";
        }

        @Override
        public Object get(String name, Scriptable start) {
            if ("value".equals(name)) {
                return instanceVal;
            }
            return super.get(name, start);
        }

        @Override
        public boolean has(String name, Scriptable start) {
            if ("value".equals(name)) {
                return true;
            }
            return super.has(name, start);
        }

        @Override
        public void put(String name, Scriptable start, Object value) {
            if ("value".equals(name)) {
                instanceVal = ScriptRuntime.toString(value);
            } else {
                super.put(name, start, value);
            }
        }

        private Object appendToValue(Object[] args) {
            StringBuilder sb = new StringBuilder(instanceVal);
            for (Object arg : args) {
                sb.append(ScriptRuntime.toString(arg));
            }
            return sb.toString();
        }

        private static Object sayHello(Object[] args) {
            if (args.length != 1) {
                throw ScriptRuntime.typeError("Expected an argument");
            }
            return "Hello, " + ScriptRuntime.toString(args[0]) + '!';
        }
    }
}
