package org.mozilla.javascript.tests.es5;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * @see <a
 *     href="https://github.com/mozilla/rhino/issues/651">https://github.com/mozilla/rhino/issues/651</a>
 */
public class StrictTest {

    @Test
    public void functionCtor() {
        final String script =
                "(function() {"
                        + "'use strict';"
                        + "Function('with(this) {  }')();"
                        + "})();"
                        + "'done'";
        Utils.assertWithAllModes("done", script);
    }

    @Test
    public void newFunction() {
        final String script =
                "(function() {"
                        + "'use strict';"
                        + "new Function('with(this) {  }')();"
                        + "})();"
                        + "'done'";
        Utils.assertWithAllModes("done", script);
    }
}
