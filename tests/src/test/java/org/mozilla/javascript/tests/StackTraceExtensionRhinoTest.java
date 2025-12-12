package org.mozilla.javascript.tests;

import static org.junit.Assert.assertFalse;

import java.io.FileReader;
import java.io.IOException;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.testutils.Utils;
import org.mozilla.javascript.tools.shell.Global;

public class StackTraceExtensionRhinoTest {
    private void testTraces(boolean interpretedMode, boolean debug) {
        final ContextFactory factory =
                debug
                        ? Utils.contextFactoryWithFeatures(
                                Context.FEATURE_LOCATION_INFORMATION_IN_ERROR)
                        : new ContextFactory();

        try (Context cx = factory.enterContext()) {
            cx.setLanguageVersion(Context.VERSION_1_8);
            cx.setInterpretedMode(interpretedMode);
            cx.setGeneratingDebug(debug);

            Global global = new Global(cx);
            Scriptable root = cx.newObject(global);
            root.put("ExpectFileNames", global, interpretedMode || debug);

            try (FileReader rdr =
                    new FileReader("testsrc/jstests/extensions/stack-traces-rhino.js")) {
                cx.evaluateReader(root, rdr, "stack-traces-rhino.js", 1, null);
            }
        } catch (IOException ioe) {
            assertFalse("I/O Error: " + ioe, true);
        }
    }

    @Test
    public void stackTraceInterpreted() {
        testTraces(true, true);
    }

    @Test
    public void stackTraceInterpretedNoDebug() {
        testTraces(true, false);
    }

    @Test
    public void stackTraceCompiled() {
        testTraces(false, true);
    }

    @Test
    public void stackTraceCompiledNoDebug() {
        testTraces(false, false);
    }
}
