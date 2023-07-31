package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;
import static org.mozilla.javascript.tests.Evaluator.eval;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;

/**
 * @see <a
 *     href="https://github.com/mozilla/rhino/issues/651">https://github.com/mozilla/rhino/issues/651</a>
 */
public class StringSearchTest {
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
    public void search() {
        NativeObject object = new NativeObject();

        Object result = eval("String.prototype.search(1, 1)", "obj", object);
        assertEquals(-1, result);
    }
}
