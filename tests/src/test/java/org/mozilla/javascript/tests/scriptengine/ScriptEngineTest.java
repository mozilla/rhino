package org.mozilla.javascript.tests.scriptengine;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.engine.RhinoScriptEngine;
import org.mozilla.javascript.engine.RhinoScriptEngineFactory;

public class ScriptEngineTest {

    private static ScriptEngineManager manager;
    private ScriptEngine engine;
    private Compilable cEngine;

    @BeforeClass
    public static void initManager() {
        manager = new ScriptEngineManager();
        manager.registerEngineName("rhino", new RhinoScriptEngineFactory());
    }

    @Before
    public void init() {
        engine = manager.getEngineByName("rhino");
        cEngine = (Compilable) engine;
    }

    @Test
    public void hello() throws ScriptException {
        Object result = engine.eval("'Hello, World!';");
        assertEquals(result, "Hello, World!");
    }

    @Test
    public void helloInterpreted() throws ScriptException {
        engine.put(RhinoScriptEngine.OPTIMIZATION_LEVEL, -1);
        Object result = engine.eval("'Hello, World!';");
        assertEquals(result, "Hello, World!");
    }

    @Test
    public void helloReader() throws ScriptException {
        String src = "1 + 1;";
        StringReader sr = new StringReader(src);
        Object result = engine.eval(sr);
        assertEquals(result, 2L);
    }

    @Test
    public void genericStatements() throws ScriptException {
        Object result =
                engine.eval(engine.getFactory().getProgram("let x = 1;", "let y = 2", "x + y"));
        assertEquals(3L, result);
    }

    @Test
    public void testThrows() {
        assertThrows(
                ScriptException.class,
                () -> {
                    engine.eval("throw 'This is an error'");
                });
    }

    @Test
    public void engineBindings() throws IOException, ScriptException {
        engine.put("string", "Hello");
        engine.put("integer", 123);
        engine.put("a", "a");
        engine.put("b", "b");
        engine.put("c", "c");
        engine.put("actuallyNull", null);

        // Ensure that stuff we just stuck in bindings made it to a global
        engine.eval(new FileReader("testsrc/assert.js"));
        engine.eval(
                "assertEquals(string, 'Hello');\n"
                        + "assertEquals(integer, 123);\n"
                        + "assertEquals(a, 'a');\n"
                        + "assertEquals(b, 'b');\n"
                        + "assertEquals(c, 'c');\n"
                        + "assertNull(actuallyNull);\n"
                        + "string = 'Goodbye';\n"
                        + "assertEquals(string, 'Goodbye');");
        assertEquals(engine.get("string"), "Goodbye");

        Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        // Make sure that null works the way we expect
        assertTrue(engineBindings.containsKey("actuallyNull"));
        assertNull(engineBindings.get("actuallyNull"));

        // Make sure we can delete
        engineBindings.remove("string");
        // This will throw because string is undefined
        assertThrows(
                ScriptException.class,
                () -> {
                    engine.eval("let failing = string + '123';");
                });
    }

    @Test
    public void engineScope() throws IOException, ScriptException {
        engine.put("string", "Hello");
        engine.put("integer", 123);
        engine.eval(new FileReader("testsrc/assert.js"));
        engine.eval("assertEquals(string, 'Hello');" + "assertEquals(integer, 123);");

        // Additional things added to the context but old stuff still there
        engine.put("second", true);
        engine.put("integer", 99);
        engine.eval(
                "assertEquals(string, 'Hello');"
                        + "assertEquals(integer, 99);"
                        + "assertTrue(second);");
    }

    @Test
    public void scopedBindings() throws IOException, ScriptException {
        ScriptContext sc = new SimpleScriptContext();

        // We treat engine and global scope the same -- if the user actually
        // uses both, then engine scope overrides global scope.
        Bindings eb = new SimpleBindings();
        sc.setBindings(eb, ScriptContext.ENGINE_SCOPE);
        eb.put("engine", Boolean.TRUE);
        eb.put("level", 2);

        Bindings gb = new SimpleBindings();
        sc.setBindings(gb, ScriptContext.GLOBAL_SCOPE);
        gb.put("global", Boolean.TRUE);
        gb.put("level", 0);

        engine.eval(new FileReader("testsrc/assert.js"), sc);
        engine.eval("assertTrue(engine);" + "assertTrue(global);" + "assertEquals(level, 2);", sc);
    }

    @Test
    public void reservedBindings() throws ScriptException {
        engine.put(ScriptEngine.ENGINE, "engine");
        engine.put(ScriptEngine.ENGINE_VERSION, "123");
        engine.put(ScriptEngine.LANGUAGE, "foo");
        engine.put(ScriptEngine.NAME, "nothing");

        // Can't actually test for those invalid property names -- but
        // at least they didn't break the script.
        assertEquals(engine.eval("'success'"), "success");
    }

    @Test
    public void compiled() throws ScriptException, IOException {
        CompiledScript asserts = cEngine.compile(new FileReader("testsrc/assert.js"));
        CompiledScript tests = cEngine.compile("assertEquals(compiled, true);");

        // Fails because asserts have not been loaded
        assertThrows(ScriptException.class, tests::eval);

        asserts.eval();
        // Fails because value has not been set
        assertThrows(ScriptException.class, tests::eval);

        engine.put("compiled", Boolean.TRUE);
        tests.eval();
    }

    @Test
    public void compiled2() throws ScriptException, IOException {
        CompiledScript asserts = cEngine.compile(new FileReader("testsrc/assert.js"));
        CompiledScript init = cEngine.compile("value = 0;");
        CompiledScript tests =
                cEngine.compile("assertEquals(value, expectedValue);" + "value += 1;");

        asserts.eval();
        init.eval();
        for (int i = 0; i <= 10; i++) {
            engine.put("expectedValue", i);
            tests.eval();
        }
    }

    @Test
    public void compiledThrows() throws ScriptException {
        engine.put(ScriptEngine.FILENAME, "throws1.js");
        CompiledScript throw1 = cEngine.compile("throw 'one';");
        engine.put(ScriptEngine.FILENAME, "throws2.js");
        CompiledScript throw2 = cEngine.compile("throw 'two';");

        try {
            throw1.eval();
            fail("Expected a throw");
        } catch (ScriptException se) {
            assertTrue(se.getMessage().startsWith("one"));
            assertEquals("throws1.js", se.getFileName());
            assertEquals(1, se.getLineNumber());
        }

        try {
            throw2.eval();
            fail("Expected a throw");
        } catch (ScriptException se) {
            assertTrue(se.getMessage().startsWith("two"));
            assertEquals("throws2.js", se.getFileName());
            assertEquals(1, se.getLineNumber());
        }
    }

    @Test
    public void cantCompile() {
        assertThrows(
                ScriptException.class,
                () -> {
                    cEngine.compile("This is not JavaScript at all!");
                });
    }

    @Test
    public void languageVersion() throws ScriptException {
        // Default language version is modernish
        ScriptEngine newEngine = manager.getEngineByName("rhino");
        assertEquals(newEngine.eval("Symbol() == Symbol()"), Boolean.FALSE);

        // Older language versions
        ScriptEngine oldEngine = manager.getEngineByName("rhino");
        oldEngine.put(ScriptEngine.LANGUAGE_VERSION, 120);
        assertThrows(
                ScriptException.class,
                () -> {
                    oldEngine.eval("Symbol() == Symbol()");
                });

        // The same with a string
        ScriptEngine olderEngine = manager.getEngineByName("rhino");
        olderEngine.put(ScriptEngine.LANGUAGE_VERSION, "100");
        assertThrows(
                ScriptException.class,
                () -> {
                    olderEngine.eval("Symbol() == Symbol()");
                });
    }

    @Test
    public void badLanguageVersion() {
        assertThrows(
                ScriptException.class,
                () -> {
                    engine.put(ScriptEngine.LANGUAGE_VERSION, "Not a number");
                    engine.eval("print('Hi!');");
                });
        assertThrows(
                ScriptException.class,
                () -> {
                    engine.put(ScriptEngine.LANGUAGE_VERSION, 3.14);
                    engine.eval("print('Hi!');");
                });
    }

    @Test
    public void filename() {
        engine.put(ScriptEngine.FILENAME, "test.js");
        try {
            engine.eval("throw 'This is an exception';");
        } catch (ScriptException se) {
            assertEquals(se.getFileName(), "test.js");
        }
    }

    @Test
    public void javaObject() throws ScriptException {
        File f = new File("testsrc/assert.js");
        String absVal = f.getAbsolutePath();
        engine.put("file", f);
        Object result = engine.eval("file.getAbsolutePath();");
        assertEquals(absVal, result);
    }
}
