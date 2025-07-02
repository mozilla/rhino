package org.mozilla.javascript.tests;

import org.junit.Ignore;
import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class ObjectLiteralSpreadTest {
    @Test
    @Ignore
    public void testObjectLiteralSpread() {
        String script = "var x = { a: 'a' };\n" + "var y = { ...x, b: 'b' };\n" + "y.a + y.b";
        Utils.assertWithAllModes_ES6("ab", script);
    }

    @Test
    public void testObjectLiteral() {
        // language=JavaScript
        String script =
                "function e() { return 2; }"
                        + "var x = { a: 'a', "
                        + "          1: 'b', "
                        + "          false: 'c', "
                        + "          3.14: 'd',"
                        + "          [e()]: 'e',"
                        + "            f() { return 'f';}, "
                        + "          get g() { return 'g'}}; "
                        + "x.a + x[1] + x[false] + x[3.14] + x[2] + x.f() + x.g\n";
        Utils.assertWithAllModes_ES6("abcdefg", script);
    }
}
