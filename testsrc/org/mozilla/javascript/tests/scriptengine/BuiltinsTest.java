package org.mozilla.javascript.tests.scriptengine;

import static org.junit.Assert.*;

import java.io.StringWriter;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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
    public void testPrintStdout() throws ScriptException {
        engine.eval("print('Hello, World!');");
    }

    @Test
    public void testPrintWriter() throws ScriptException {
        StringWriter sw = new StringWriter();
        ScriptContext sc = new SimpleScriptContext();
        sc.setWriter(sw);
        engine.eval("print('one', 2, true);", sc);
        assertEquals(sw.toString(), "one2true\n");
    }

    @Test
    public void testPrintWriterGeneric() throws ScriptException {
        StringWriter sw = new StringWriter();
        engine.getContext().setWriter(sw);
        engine.eval(engine.getFactory().getOutputStatement("Display This!"));
        assertEquals(sw.toString(), "Display This!\n");
    }
}
