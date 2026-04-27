package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.FileReader;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.StackStyle;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.testutils.TestSource;
import org.mozilla.javascript.testutils.Utils;
import org.mozilla.javascript.tools.shell.Global;

public class StackTraceExtensionV8Test {
    @BeforeAll
    public static void init() {
        RhinoException.setStackStyle(StackStyle.V8);
    }

    @AfterAll
    public static void terminate() {
        RhinoException.setStackStyle(StackStyle.RHINO);
    }

    private void testTraces(boolean interpretedMode) {
        final ContextFactory factory =
                Utils.contextFactoryWithFeatures(Context.FEATURE_LOCATION_INFORMATION_IN_ERROR);

        try (Context cx = factory.enterContext()) {
            cx.setLanguageVersion(Context.VERSION_1_8);
            cx.setInterpretedMode(interpretedMode);
            cx.setGeneratingDebug(true);

            Global global = new Global(cx);
            global.setFileLoadPrefix(TestSource.getPrefix());
            VarScope root = cx.newVarEnv(global);

            try (FileReader rdr =
                    new FileReader(
                            TestSource.resolve("testsrc/jstests/extensions/stack-traces-v8.js"))) {
                cx.evaluateReader(root, rdr, "stack-traces-v8.js", 1, null);
            }
        } catch (IOException ioe) {
            assertFalse(true, "I/O Error: " + ioe);
        }
    }

    @Test
    public void stackTraceInterpreted() {
        testTraces(true);
    }

    @Test
    public void stackTraceCompiled() {
        testTraces(false);
    }
}
