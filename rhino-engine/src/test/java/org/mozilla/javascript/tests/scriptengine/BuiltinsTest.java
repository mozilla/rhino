package org.mozilla.javascript.tests.scriptengine;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import org.junit.*;
import org.mozilla.javascript.engine.RhinoScriptEngineFactory;

public class BuiltinsTest {

    private static ScriptEngineManager manager;

    private ScriptEngine engine;

    @BeforeClass
    public static void init() {
        manager = new ScriptEngineManager();
        manager.registerEngineName("rhino", new RhinoScriptEngineFactory());
    }

    @Before
    public void setup() {
        engine = manager.getEngineByName("rhino");
    }

    @Test
    public void printStdout() throws ScriptException {
        engine.eval("print('Hello, World!');");
    }

    @Test
    public void printStdoutAndCheckItPrints() throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            PrintStream original = System.out;
            try (PrintStream modified = new PrintStream(bos, false)) {
                System.setOut(modified);
                try {
                    // Now Get A SimpleContext
                    ScriptContext sc = new SimpleScriptContext();

                    // this was a broken test
                    engine.eval("print('Hello, World!');", sc);

                    // this has been hard work https://github.com/mozilla/rhino/issues/1356
                    Assert.assertEquals("Hello, World!\n", bos.toString());
                } finally {
                    // revert the sys out
                    System.setOut(original);
                }
            }
        }
    }

    @Test
    public void printWriter() throws ScriptException {
        StringWriter sw = new StringWriter();
        ScriptContext sc = new SimpleScriptContext();
        sc.setWriter(sw);
        engine.eval("print('one', 2, true);", sc);
        assertEquals(sw.toString(), "one2true\n");
    }

    @Test
    public void printWriterGeneric() throws ScriptException {
        StringWriter sw = new StringWriter();
        engine.getContext().setWriter(sw);
        engine.eval(engine.getFactory().getOutputStatement("Display This!"));
        assertEquals(sw.toString(), "Display This!\n");
    }
}
