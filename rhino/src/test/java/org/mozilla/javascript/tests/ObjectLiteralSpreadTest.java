package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class ObjectLiteralSpreadTest {
    @Test
    public void testObjectLiteralSpread() {
        String script = "var x = { a: 'a' };\n" + "var y = { ...x, b: 'b' };\n" + "y.a + y.b";
        Utils.assertWithAllModes_ES6("ab", script);
    }
}
