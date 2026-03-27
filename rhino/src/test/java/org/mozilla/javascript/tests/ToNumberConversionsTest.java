package org.mozilla.javascript.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.testutils.Utils;

/**
 * Test cases for ToNumber conversion applied to a String type.
 *
 * <p>See the #sec-tonumber-applied-to-the-string-type section of ECMA262.
 *
 * <p>This is only enabled with a language version >= ES6 (200).
 */
public class ToNumberConversionsTest {
    private static final Object[][] TESTS = {
        // order: expected result, source string
        // (0) special
        {"-Infinity", "  -Infinity  "},
        {"+Infinity", "  +Infinity  "},

        // (1) bin
        {"4", "  0b100  "},
        {"5", "0B0000101  "},
        // bin, invalid
        {"NaN", "-0b100"},
        {"NaN", "+0b100"},
        {"NaN", "0b100.1"},
        {"NaN", "0b100e1"},
        {"NaN", "0b100e-1"},
        {"NaN", "0b100e+1"},
        {"NaN", "0b2"},
        {"NaN", "0b"},
        {"NaN", "1b1"},

        // (2) oct
        {"8", "  0o10  "},
        {"10", "0O000012  "},
        // oct, invalid
        {"NaN", "-0o10"},
        {"NaN", "+0o10"},
        {"NaN", "0o10.1"},
        {"NaN", "0o10e1"},
        {"NaN", "0o10e-1"},
        {"NaN", "0o10e+1"},
        {"NaN", "0o9"},
        {"NaN", "1o9"},
        {"NaN", "0o"},

        // (3) dec
        {"210", "  210  "},
        {"210", "21e1  "},
        {"-210", "  -0021.00e+1  "},
        {"210", "  +02100.00e-1"},
        // dec, invalid
        {"NaN", "  210d2"},
        {"NaN", "  210 d2"},

        // (4) hex
        {"210", "  0x00d2  "},
        {"210", "  0x00D2  "},
        {"210", "  0X00D2  "},
        {"53985", "0xd2e1"}, // 'an exponent without sign' variant is a valid hex literal
        // hex, invalid
        {"NaN", "-0xd2"},
        {"NaN", "+0xd2"},
        {"NaN", "0xd2.00"},
        {"NaN", "0xd2e+1"},
        {"NaN", "0xd2e-1"},
        {"NaN", "0xd2g"},
        {"NaN", "0xd2 g"},
        {"NaN", "0x7f.0x0.0x0.0x1"},
        {"NaN", "0x"},
        {"NaN", "1xd2"},
        {"NaN", "+1xd2"},
        {"NaN", "-0x"}
    };
    private static final String PRELUDE =
            "function eq(a,b) {" + "if (a != a) return b != b;" + "return a == b;" + "}\n";

    public static Collection<Object[]> data() {
        List<Object[]> cases = new ArrayList<>();

        for (Object[] test : TESTS) {
            cases.add(new Object[] {test[0], test[1]});
        }

        return cases;
    }

    public String expected;
    public String source;

    @SuppressWarnings("ConstantConditions")
    private boolean execute(Context cx, Scriptable scope, String script) {
        return (Boolean) cx.evaluateString(scope, script, "inline", 1, null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "ToNumber(\"{1}\") == {0} (opt={2})")
    public void cumberConstructor(String expected, String source) {
        initToNumberConversionsTest(expected, source);
        String script = String.format("%seq(Number(\"%s\"), %s)", PRELUDE, source, expected);
        Utils.assertWithAllModes_ES6(
                "Number('" + source + "') doesn't produce " + expected, true, script);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "ToNumber(\"{1}\") == {0} (opt={2})")
    public void coercion(String expected, String source) {
        initToNumberConversionsTest(expected, source);
        String script = String.format("%seq(+(\"%s\"), %s)", PRELUDE, source, expected);
        Utils.assertWithAllModes_ES6(
                "+('" + source + "') doesn't produce " + expected, true, script);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "ToNumber(\"{1}\") == {0} (opt={2})")
    public void isNaN(String expected, String source) {
        initToNumberConversionsTest(expected, source);
        String script = String.format("%seq(isNaN(\"%s\"), isNaN(%s))", PRELUDE, source, expected);
        Utils.assertWithAllModes_ES6(
                "isNaN('" + source + "') !== isNaN(" + expected + ")", true, script);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "ToNumber(\"{1}\") == {0} (opt={2})")
    public void isFinite(String expected, String source) {
        initToNumberConversionsTest(expected, source);
        String script =
                String.format("%seq(isFinite(\"%s\"), isFinite(%s))", PRELUDE, source, expected);
        Utils.assertWithAllModes_ES6(
                "isFinite('" + source + "') !== isFinite(" + expected + ")", true, script);
    }

    public void initToNumberConversionsTest(String expected, String source) {
        this.expected = expected;
        this.source = source;
    }
}
