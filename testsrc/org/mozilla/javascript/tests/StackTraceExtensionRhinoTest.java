package org.mozilla.javascript.tests;

import static org.junit.Assert.assertFalse;

import java.io.FileReader;
import java.io.IOException;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

public class StackTraceExtensionRhinoTest {
    private void testTraces(int opt, boolean debug) {
        final ContextFactory factory =
                new ContextFactory() {
                    @Override
                    protected boolean hasFeature(Context cx, int featureIndex) {
                        switch (featureIndex) {
                            case Context.FEATURE_LOCATION_INFORMATION_IN_ERROR:
                                return debug;
                            default:
                                return super.hasFeature(cx, featureIndex);
                        }
                    }
                };

        try (Context cx = factory.enterContext()) {
            cx.setLanguageVersion(Context.VERSION_1_8);
            cx.setOptimizationLevel(opt);
            cx.setGeneratingDebug(debug);

            Global global = new Global(cx);
            Scriptable root = cx.newObject(global);
            root.put("ExpectFileNames", global, opt < 0 || debug);

            try (FileReader rdr =
                    new FileReader("testsrc/jstests/extensions/stack-traces-rhino.js")) {
                cx.evaluateReader(root, rdr, "stack-traces-rhino.js", 1, null);
            }
        } catch (IOException ioe) {
            assertFalse("I/O Error: " + ioe, true);
        }
    }

    @Test
    public void testStackTrace0() {
        testTraces(0, true);
    }

    @Test
    public void testStackTrace0NoDebug() {
        testTraces(0, false);
    }

    @Test
    public void testStackTrace9() {
        testTraces(9, true);
    }

    @Test
    public void testStackTrace9NoDebug() {
        testTraces(9, false);
    }

    @Test
    public void testStackTraceInt() {
        testTraces(-1, true);
    }

    @Test
    public void testStackTraceIntNoDebug() {
        testTraces(-1, false);
    }
}
