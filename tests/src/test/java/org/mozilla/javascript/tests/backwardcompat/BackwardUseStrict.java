package org.mozilla.javascript.tests.backwardcompat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.tests.Utils;
import org.mozilla.javascript.tools.shell.Global;

public class BackwardUseStrict {
    private static String source;

    @BeforeClass
    public static void init() throws IOException {
        try (InputStream is =
                BackwardUseStrict.class.getResourceAsStream(
                        "/backwardcompat/backward-use-strict.js")) {
            assertNotNull(is);

            try (InputStreamReader rdr = new InputStreamReader(is)) {
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
            }
        }
    }

    @Test
    public void strictIgnored() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_1_8);
                    try {
                        Global root = new Global(cx);
                        cx.evaluateString(root, source, "[test]", 1, null);
                    } catch (RhinoException re) {
                        System.err.println(re.getScriptStackTrace());
                        fail("Unexpected code error: " + re);
                    }

                    return null;
                });
    }

    @Test
    public void strictHonored() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    try {
                        Global root = new Global(cx);
                        cx.evaluateString(root, source, "[test]", 1, null);
                        assertTrue("Expected a runtime exception", false);
                    } catch (RhinoException re) {
                        // We expect an error here.
                    }

                    return null;
                });
    }
}
