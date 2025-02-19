package org.mozilla.javascript.tools.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.testutils.Utils;
import org.mozilla.javascript.tools.shell.Global;

public class SanityTest {
    /** Make sure that we can load and execute Global in a module environment. */
    @Test
    public void sanityCheckGlobal() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    var result =
                            cx.evaluateString(
                                    g,
                                    "const v = version();\n"
                                            + "if (typeof v !== 'number') throw 'version() returned ' + v;"
                                            + "v",
                                    "test.js",
                                    1,
                                    null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(Context.VERSION_ES6, ((Number) result).intValue());
                    return null;
                });
    }

    /** Verify that the compiler class can at least be instantiated. */
    @Test
    public void sanityCheckCompiler() {
        new org.mozilla.javascript.tools.jsc.Main();
    }
}
