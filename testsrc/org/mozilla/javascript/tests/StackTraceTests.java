package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.StackStyle;

import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.*;

public class StackTraceTests
{
    private void testTraces(int opt)
    {
        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_1_8);
            cx.setOptimizationLevel(opt);
            cx.setGeneratingDebug(true);
            Scriptable global = cx.initStandardObjects();
            FileReader rdr = new FileReader("testsrc/jstests/extensions/stack-traces.js");

            try {
                RhinoException.setStackStyle(StackStyle.V8);
                cx.evaluateReader(global, rdr, "stack-traces.js", 1, null);
            } finally {
                RhinoException.setStackStyle(StackStyle.RHINO);
                rdr.close();
            }
        } catch (IOException ioe) {
            assertFalse("I/O Error: " + ioe, true);
        } finally {
            Context.exit();
        }
    }

    @Test
    public void testStackTrace0()
    {
        testTraces(0);
    }

    @Test
    public void testStackTrace9()
    {
        testTraces(9);
    }

    @Test
    public void testStackTraceInt()
    {
        testTraces(-1);
    }
}
