package org.mozilla.javascript.interpreterv2;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Exercises the arity-specialized {@code New} instructions ({@code New0}..{@code New3}) and the
 * generic {@code NewN} fallback. As with calls, the risk in specializing is getting argument
 * positions or left-to-right evaluation order wrong, so the multi-arg cases use distinct,
 * side-effecting arguments.
 */
class SpecializedNewTest {

    private static final String CTOR =
            "var log = '';\n"
                    + "function id(x) { log += x; return x; }\n"
                    + "function C() { for (var i = 0; i < arguments.length; i++) this[i] = arguments[i]; }\n";

    @Test
    void zeroArgs() {
        Utils.assertWithAllModes_ES6(true, CTOR + "(new C()) instanceof C;");
    }

    @Test
    void oneArg() {
        Utils.assertWithAllModes_ES6("a:a", CTOR + "var o = new C(id('a')); o[0] + ':' + log;");
    }

    @Test
    void twoArgs() {
        Utils.assertWithAllModes_ES6(
                "ab:ab", CTOR + "var o = new C(id('a'), id('b')); o[0] + o[1] + ':' + log;");
    }

    @Test
    void threeArgs() {
        Utils.assertWithAllModes_ES6(
                "abc:abc",
                CTOR + "var o = new C(id('a'), id('b'), id('c')); o[0] + o[1] + o[2] + ':' + log;");
    }

    @Test
    void fourArgsUsesGenericPath() {
        Utils.assertWithAllModes_ES6(
                "abcd:abcd",
                CTOR
                        + "var o = new C(id('a'), id('b'), id('c'), id('d'));"
                        + " o[0] + o[1] + o[2] + o[3] + ':' + log;");
    }

    @Test
    void argumentsPositionedCorrectly() {
        Utils.assertWithAllModes_ES6(
                123.0,
                "function P(a, b, c) { this.v = a * 100 + b * 10 + c; }\n" + "new P(1, 2, 3).v;");
    }
}
