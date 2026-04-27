package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.FileReader;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.testutils.TestSource;
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
            global.setFileLoadPrefix(TestSource.getPrefix());
            VarScope root = cx.newVarEnv(global);
            root.put("ExpectFileNames", global, interpretedMode || debug);

            try (FileReader rdr =
                    new FileReader(
                            TestSource.resolve(
                                    "testsrc/jstests/extensions/stack-traces-rhino.js"))) {
                cx.evaluateReader(root, rdr, "stack-traces-rhino.js", 1, null);
            }
        } catch (IOException ioe) {
            assertFalse(true, "I/O Error: " + ioe);
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
