package org.mozilla.javascript.interpreterv2;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Exercises the arity-specialized {@code Call} instructions ({@code Call0}..{@code Call3}) and the
 * generic {@code CallN} fallback. The key risk in specializing is getting argument positions or
 * left-to-right evaluation order wrong, so each case uses distinct, side-effecting arguments.
 */
class SpecializedCallTest {

    private static final String ID = "var log = '';\n" + "function id(x) { log += x; return x; }\n";

    @Test
    void zeroArgs() {
        Utils.assertWithAllModes_ES6("ok", "function f() { return 'ok'; } f();");
    }

    @Test
    void oneArg() {
        Utils.assertWithAllModes_ES6(
                "a:a", ID + "function f(a) { return a; } f(id('a')) + ':' + log;");
    }

    @Test
    void twoArgs() {
        Utils.assertWithAllModes_ES6(
                "ab:ab",
                ID + "function f(a, b) { return a + b; } f(id('a'), id('b')) + ':' + log;");
    }

    @Test
    void threeArgs() {
        Utils.assertWithAllModes_ES6(
                "abc:abc",
                ID
                        + "function f(a, b, c) { return a + b + c; }\n"
                        + "f(id('a'), id('b'), id('c')) + ':' + log;");
    }

    @Test
    void fourArgsUsesGenericPath() {
        Utils.assertWithAllModes_ES6(
                "abcd:abcd",
                ID
                        + "function f(a, b, c, d) { return a + b + c + d; }\n"
                        + "f(id('a'), id('b'), id('c'), id('d')) + ':' + log;");
    }

    @Test
    void argumentsPositionedCorrectly() {
        // a different value in each position catches positional/ordering mistakes.
        Utils.assertWithAllModes_ES6(
                123.0, "function f(a, b, c) { return a * 100 + b * 10 + c; } f(1, 2, 3);");
    }
}
