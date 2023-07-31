package org.mozilla.javascript.tests.scriptengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.io.FileReader;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.engine.RhinoScriptEngineFactory;

public class InvocableTest {

    private static ScriptEngineManager manager;

    private ScriptEngine engine;
    private Invocable iEngine;

    @BeforeClass
    public static void init() {
        manager = new ScriptEngineManager();
        manager.registerEngineName("rhino", new RhinoScriptEngineFactory());
    }

    @Before
    public void setup() {
        engine = manager.getEngineByName("rhino");
        iEngine = (Invocable) engine;
    }

    @Test
    public void invokeFunctionTest() throws ScriptException, NoSuchMethodException {
        engine.eval("function foo(a, b) { return a + b; }");
        Object result = iEngine.invokeFunction("foo", 2, 2);
        assertEquals(result, 4L);
    }

    @Test
    public void invokeScriptFunctionTest() throws ScriptException, NoSuchMethodException {
        Object scriptObj =
                engine.eval("let o = {};\n" + "o.test = function(x) { return x + 2; }\n" + "o;");
        assertEquals(4L, iEngine.invokeMethod(scriptObj, "test", 2));
    }

    @Test
    public void invokeGenericFunctionTest() throws ScriptException, NoSuchMethodException {
        engine.eval("let o = {};\n" + "o.test = function(x) { return x + 2; }\n");
        Object result = engine.eval(engine.getFactory().getMethodCallSyntax("o", "test", "1"));
        assertEquals(3L, result);
    }

    @Test
    public void invokeGenericFunctionTest2() throws ScriptException, NoSuchMethodException {
        engine.eval("let o = {};\n" + "o.test = function(x, y) { return x + y; }\n");
        Object result = engine.eval(engine.getFactory().getMethodCallSyntax("o", "test", "1", "7"));
        assertEquals(8L, result);
    }

    @Test
    public void invokeMethodTest() throws Exception {
        try (FileReader reader = new FileReader("testsrc/assert.js")) {
            engine.eval(reader);
            engine.eval(
                    "function FooObj() { this.x = 0; }\n"
                            + "FooObj.prototype.set = function(a, b) { this.x = a + b; }");
            engine.eval(
                    "let f = new FooObj();\n"
                            + "assertEquals(f.x, 0);\n"
                            + "f.set(2, 2);\n"
                            + "assertEquals(f.x, 4);");

            Object fooObj = engine.eval("let y = new FooObj(); y");
            assertNotNull(fooObj);
            iEngine.invokeMethod(fooObj, "set", 3, 3);
            Object result = engine.eval("y.x");
            assertEquals(result, 6L);
        }
    }

    @Test
    public void interfaceFunctionTest() throws Exception {
        try (FileReader reader = new FileReader("testsrc/assert.js")) {
            engine.eval(reader);

            engine.eval(
                    "var foo = 'initialized';\n"
                            + "function setFoo(v) { foo = v; }\n"
                            + "function getFoo() { return foo; }\n"
                            + "function addItUp(a, b) { return a + b; }");
            I tester = iEngine.getInterface(I.class);
            assertEquals(tester.getFoo(), "initialized");
            tester.setFoo("tested");
            assertEquals(tester.getFoo(), "tested");
            assertEquals(tester.addItUp(100, 1), 101);
        }
    }

    @Test
    public void interfaceMethodTest() throws Exception {
        try (FileReader reader = new FileReader("testsrc/assert.js")) {
            engine.eval(reader);

            Object foo =
                    engine.eval(
                            "function Foo() { this.foo = 'initialized' }\n"
                                    + "Foo.prototype.setFoo = function(v) { this.foo = v; };\n"
                                    + "Foo.prototype.getFoo = function() { return this.foo; };\n"
                                    + "Foo.prototype.addItUp = function(a, b) { return a + b; };\n"
                                    + "new Foo();");
            I tester = iEngine.getInterface(foo, I.class);
            assertEquals(tester.getFoo(), "initialized");
            tester.setFoo("tested");
            assertEquals(tester.getFoo(), "tested");
            assertEquals(tester.addItUp(100, 1), 101);
        }
    }

    @Test
    public void interfaceFunctionMissingTest() {
        I tester = iEngine.getInterface(I.class);
        assertNull(tester);
    }

    @Test
    public void interfaceMethodMissingTest() throws ScriptException {
        // Functions defined, but not on the right object
        Object foo =
                engine.eval(
                        "var foo = 'initialized';\n"
                                + "function setFoo(v) { foo = v; }\n"
                                + "function getFoo() { return foo; }\n"
                                + "function addItUp(a, b) { return a + b; }\n"
                                + "function Foo() {}\n"
                                + "new Foo();");
        I tester = iEngine.getInterface(foo, I.class);
        assertNull(tester);
    }

    @Test
    public void invokeNotFoundTest() {
        assertThrows(
                NoSuchMethodException.class,
                () -> {
                    iEngine.invokeFunction("foo", 2, 2);
                });
    }

    @Test
    public void invokeNotFunctionTest() {
        assertThrows(
                ScriptException.class,
                () -> {
                    engine.eval("foo = 'bar';");
                    iEngine.invokeFunction("foo", 2, 2);
                });
    }

    interface I {

        void setFoo(String v);

        String getFoo();

        int addItUp(int a, int b);
    }
}
