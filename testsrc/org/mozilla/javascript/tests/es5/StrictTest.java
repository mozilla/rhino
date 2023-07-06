package org.mozilla.javascript.tests.es5;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tests.Utils;

/**
 * @see <a
 *     href="https://github.com/mozilla/rhino/issues/651">https://github.com/mozilla/rhino/issues/651</a>
 */
public class StrictTest {
    private Context cx;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void functionCtor() {
        Utils.runWithAllOptimizationLevels(
                ctx -> {
                    cx.evaluateString(
                            cx.initSafeStandardObjects(),
                            "(function() {"
                                    + "'use strict';"
                                    + "Function('with(this) {  }')();"
                                    + "})()",
                            "test.js",
                            1,
                            null);
                    return null;
                });
    }

    @Test
    public void newFunction() {
        Utils.runWithAllOptimizationLevels(
                ctx -> {
                    cx.evaluateString(
                            cx.initSafeStandardObjects(),
                            "(function() {"
                                    + "'use strict';"
                                    + "new Function('with(this) {  }')();"
                                    + "})()",
                            "test.js",
                            1,
                            null);
                    return null;
                });
    }
}
