package org.mozilla.javascript.tests.scriptengine;

import static org.junit.Assert.*;

import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.junit.Assert;
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
    public void printStdout() throws Exception {
        // only way to test these are redirections and then prayers
        // this was not true earlier
        final String content = "Hello, World!";
        final String tmpFile = "out.txt" ;
        final Path filePath = Paths.get(tmpFile);
        if ( Files.exists( filePath )){
            Files.delete(filePath);
        }
        PrintStream o = new PrintStream(tmpFile);
        PrintStream console = System.out;
        System.setOut(o);
        engine.eval("print('" + content + "');");
        System.setOut(console);
        Assert.assertTrue( Files.exists(filePath));
        // now read the content of the file ?
        String writtenContent = new String(Files. readAllBytes(filePath));
        Assert.assertEquals( content + "\n",  writtenContent);
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
