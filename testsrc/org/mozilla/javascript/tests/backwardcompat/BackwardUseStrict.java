package org.mozilla.javascript.tests.backwardcompat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.tools.shell.Global;

public class BackwardUseStrict {
    private static String source;

    @BeforeClass
    public static void init() throws IOException {
        InputStream is =
                BackwardUseStrict.class.getResourceAsStream(
                        "/jstests/backwardcompat/backward-use-strict.js");
        assertNotNull(is);
        try {
            InputStreamReader rdr = new InputStreamReader(is);
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int r;
            do {
                r = rdr.read(buf);
                if (r > 0) {
                    sb.append(buf, 0, r);
                }
            } while (r > 0);
            rdr.close();
            source = sb.toString();
        } finally {
            is.close();
        }
    }

    private void strictIgnored(int opt) {
        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_1_8);
        cx.setOptimizationLevel(opt);
        try {
            Global root = new Global(cx);
            cx.evaluateString(root, source, "[test]", 1, null);
        } catch (RhinoException re) {
            System.err.println(re.getScriptStackTrace());
            assertTrue("Unexpected code error: " + re, false);
        } finally {
            Context.exit();
        }
    }

    private void strictHonored(int opt) {
        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        cx.setOptimizationLevel(opt);
        try {
            Global root = new Global(cx);
            cx.evaluateString(root, source, "[test]", 1, null);
            assertTrue("Expected a runtime exception", false);
        } catch (RhinoException re) {
            // We expect an error here.
        } finally {
            Context.exit();
        }
    }

    @Test
    public void testStrictIgnored0() {
        strictIgnored(0);
    }

    @Test
    public void testStrictIgnored1() {
        strictIgnored(1);
    }

    @Test
    public void testStrictIgnored9() {
        strictIgnored(9);
    }

    @Test
    public void testStrictHonored0() {
        strictHonored(0);
    }

    @Test
    public void testStrictHonored1() {
        strictHonored(1);
    }

    @Test
    public void testStrictHonored9() {
        strictHonored(9);
    }
}
