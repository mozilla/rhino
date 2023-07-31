package org.mozilla.javascript.tests.scriptengine;

import static org.junit.Assert.*;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import org.junit.Test;
import org.mozilla.javascript.engine.RhinoScriptEngine;
import org.mozilla.javascript.engine.RhinoScriptEngineFactory;

/*
 * A series of tests that depend on us having our engine registered with the
 * ScriptEngineManager by default.
 */
public class FactoryTest {

    @Test
    public void findRhinoFactory() {
        ScriptEngineManager manager = new ScriptEngineManager();
        for (ScriptEngineFactory factory : manager.getEngineFactories()) {
            if (factory instanceof RhinoScriptEngineFactory) {
                assertEquals("rhino", factory.getEngineName());
                assertEquals("rhino", factory.getParameter(ScriptEngine.ENGINE));
                assertEquals("rhino", factory.getParameter(ScriptEngine.NAME));
                // This could be "unknown" if we're not running from a regular JAR
                assertFalse(factory.getEngineVersion().isEmpty());
                assertEquals("javascript", factory.getLanguageName());
                assertEquals("javascript", factory.getParameter(ScriptEngine.LANGUAGE));
                assertEquals("200", factory.getLanguageVersion());
                assertEquals("200", factory.getParameter(ScriptEngine.LANGUAGE_VERSION));
                assertNull(factory.getParameter("THREADING"));
                assertTrue(factory.getExtensions().contains("js"));
                assertTrue(factory.getMimeTypes().contains("application/javascript"));
                assertTrue(factory.getMimeTypes().contains("application/ecmascript"));
                assertTrue(factory.getMimeTypes().contains("text/javascript"));
                assertTrue(factory.getMimeTypes().contains("text/ecmascript"));
                assertTrue(factory.getNames().contains("rhino"));
                assertTrue(factory.getNames().contains("Rhino"));
                assertTrue(factory.getNames().contains("javascript"));
                assertTrue(factory.getNames().contains("JavaScript"));
                return;
            }
        }
        fail("Expected to find Rhino script engine");
    }

    @Test
    public void rhinoFactory() {
        // This will always uniquely return our engine.
        // In Java 8, other ways to find it may return Nashorn.
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("rhino");
        assertTrue(engine instanceof RhinoScriptEngine);
    }
}
