package org.mozilla.javascript.tests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Test cases for a legacy ToNumber conversion applied to a String type.
 *
 * <p>The differences with the current specification (section
 * #sec-tonumber-applied-to-the-string-type of ECMA262):
 *
 * <p>1. only a valid prefix of HexIntegerLiteral is considered (similar to 'parseInt()')
 * ToNumber('0x10 something') => 16 2. signed HexIntegralLiteral is allowed ToNumber('-0x10') => -16
 * 3. neither BinaryIntegerLiteral nor OctalIntegerLiteral are supported ToNumber('0b1') => NaN
 * ToNumber('0o5') => NaN
 *
 * <p>This is only enabled with a language version < ES6 (200).
 */
@RunWith(Parameterized.class)
public class ToNumberLegacyConversionsTest {
    private static final int[] OPT_LEVELS = {-1, 0, 9};
    private static final Object[][] TESTS = {
        // order: expected result, source string
        // (0) special
        {"-Infinity", "  -Infinity  "},
        {"+Infinity", "  +Infinity  "},

        // (1) bin (unsupported)
        {"NaN", "  0b100  "},
        {"NaN", "0B0000101  "},
        {"NaN", "-0b100"},
        {"NaN", "+0b100"},
        {"NaN", "0b100.1"},
        {"NaN", "0b100e1"},
        {"NaN", "0b100e-1"},
        {"NaN", "0b100e+1"},
        {"NaN", "0b2"},
        {"NaN", "0b"},
        {"NaN", "1b1"},

        // (2) oct (unsupported)
        {"NaN", "  0o10  "},
        {"NaN", "0O000012  "},
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
        {"-210", "-0xd2"},
        {"210", "+0xd2"},
        // hex, when only a valid prefix is considered
        {"210", "0xd2.00"},
        {"3374", "0xd2e+1"},
        {"3374", "0xd2e-1"},
        {"210", "0xd2g"},
        {"210", "0xd2 g"},
        {"127", "0x7f.0x0.0x0.0x1"},
        // hex, invalid
        {"NaN", "0x"},
        {"NaN", "1xd2"},
        {"NaN", "+1xd2"},
        {"NaN", "-0x"}
    };
    private static final String PRELUDE =
            "function eq(a,b) {" + "if (a != a) return b != b;" + "return a == b;" + "}\n";

    @Parameterized.Parameters(name = "ToNumber(\"{1}\") == {0} (opt={2})")
    public static Collection<Object[]> data() {
        List<Object[]> cases = new ArrayList<>();

        for (int optLevel : OPT_LEVELS) {
            for (Object[] test : TESTS) {
                cases.add(new Object[] {test[0], test[1], optLevel});
            }
        }

        return cases;
    }

    @Parameterized.Parameter(0)
    public String expected;

    @Parameterized.Parameter(1)
    public String source;

    @Parameterized.Parameter(2)
    public int optLevel;

    @SuppressWarnings("ConstantConditions")
    private boolean execute(Context cx, Scriptable scope, String script) {
        return (Boolean) cx.evaluateString(scope, script, "inline", 1, null);
    }

    public Context cx;
    public Scriptable scope;

    @Before
    public void setup() {
        cx = Context.enter();
        cx.setOptimizationLevel(optLevel);
        cx.setLanguageVersion(Context.VERSION_1_8);
        scope = cx.initSafeStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void numberConstructor() {
        String script = String.format("%seq(Number(\"%s\"), %s)", PRELUDE, source, expected);
        assertTrue(
                "Number('" + source + "') doesn't produce " + expected, execute(cx, scope, script));
    }

    @Test
    public void coercion() {
        String script = String.format("%seq(+(\"%s\"), %s)", PRELUDE, source, expected);
        assertTrue("+('" + source + "') doesn't produce " + expected, execute(cx, scope, script));
    }

    @Test
    public void isNaN() {
        String script = String.format("%seq(isNaN(\"%s\"), isNaN(%s))", PRELUDE, source, expected);
        assertTrue(
                "isNaN('" + source + "') !== isNaN(" + expected + ")", execute(cx, scope, script));
    }

    @Test
    public void isFinite() {
        String script =
                String.format("%seq(isFinite(\"%s\"), isFinite(%s))", PRELUDE, source, expected);
        assertTrue(
                "isFinite('" + source + "') !== isFinite(" + expected + ")",
                execute(cx, scope, script));
    }
}
