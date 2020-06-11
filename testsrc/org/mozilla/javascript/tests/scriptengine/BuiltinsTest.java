package org.mozilla.javascript.tests.scriptengine;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.engine.RhinoScriptEngineFactory;

import static org.junit.Assert.*;

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

  @Test
  public void testFailingWriter() {
    Writer bogusWriter = new Writer() {
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        throw new IOException("Can't write!");
      }

      @Override
      public void flush() throws IOException {
      }

      @Override
      public void close() throws IOException {
      }
    };

    engine.getContext().setWriter(bogusWriter);
    assertThrows(ScriptException.class, () -> {
      engine.eval("print('This is going to fail.');");
    });
  }
}
