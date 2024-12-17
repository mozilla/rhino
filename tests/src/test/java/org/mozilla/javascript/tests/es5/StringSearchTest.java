package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * @see <a
 *     href="https://github.com/mozilla/rhino/issues/651">https://github.com/mozilla/rhino/issues/651</a>
 */
public class StringSearchTest {
    private Context cx;
    private Scriptable scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_1_8);
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void search() {
        Object result =
                cx.evaluateString(scope, "String.prototype.search(1, 1)", "test.js", 1, null);
        assertEquals(-1, result);
    }
}
