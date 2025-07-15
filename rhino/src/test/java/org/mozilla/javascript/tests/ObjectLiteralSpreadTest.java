package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class ObjectLiteralSpreadTest {
    @Test
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

    @Test
    public void testObjectSpreadBasic() {
        String script =
                "var obj1 = { a: 1, b: 2 };\n"
                        + "var obj2 = { ...obj1, c: 3 };\n"
                        + "obj2.a + obj2.b + obj2.c";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadOverride() {
        String script =
                "var obj1 = { a: 1, b: 2 };\n"
                        + "var obj2 = { a: 3, ...obj1 };\n"
                        + "obj2.a + obj2.b";
        Utils.assertWithAllModes_ES6(3, script);
    }

    @Test
    public void testObjectSpreadMultiple() {
        String script =
                "var obj1 = { a: 1 };\n"
                        + "var obj2 = { b: 2 };\n"
                        + "var obj3 = { c: 3 };\n"
                        + "var result = { ...obj1, ...obj2, ...obj3 };\n"
                        + "result.a + result.b + result.c";
        Utils.assertWithAllModes_ES6(6, script);
    }

    @Test
    public void testObjectSpreadWithNullUndefined() {
        String script = "var obj = { ...null, ...undefined, a: 1 };\n" + "obj.a";
        Utils.assertWithAllModes_ES6(1, script);
    }

    @Test
    public void testObjectSpreadWithGetter() {
        String script =
                "var obj1 = { get x() { return 1; } };\n" + "var obj2 = { ...obj1 };\n" + "obj2.x";
        Utils.assertWithAllModes_ES6(1, script);
    }

    @Test
    // TODO
    public void testObjectSpreadWithSymbols() {
        String script =
                "var sym = Symbol('test');\n"
                        + "var obj1 = { [sym]: 'value' };\n"
                        + "var obj2 = { ...obj1 };\n"
                        + "obj2[sym] === 'value' && obj2[sym] === obj1[sym]";
        Utils.assertWithAllModes_ES6(true, script);
    }
}
