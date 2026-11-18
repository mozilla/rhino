package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.dtoa.DecimalFormatter;
import org.mozilla.javascript.testutils.Utils;

/**
 * Tests for Number.prototype.toPrecision.
 *
 * <p>The expected strings agree with V8 (Node) and JavaScriptCore. Before the fix, values below 0.1
 * whose result is in fixed notation and needs trailing zeros were missing digits at the end,
 * because the zeros between the decimal point and the first significant digit were counted as
 * significant digits. See <a href="https://github.com/mozilla/rhino/issues/2586">issue 2586</a>.
 */
public class ToPrecisionTest {

    // order: expected result, input, precision
    private static final Object[][] TO_PRECISION_TESTS = {
        // The reproductions from the issue
        {"0.06250", 0.0625, 4},
        {"0.06250000000", 0.0625, 10},
        {"0.00195312500", 0.001953125, 9},
        {"-0.0312500000000000000000", -0.03125, 21},
        {"0.5000", 0.5, 4},
        // Exact binary fractions below 0.1 with and without padding
        {"0.06", 0.0625, 1},
        {"0.063", 0.0625, 2},
        {"0.0625", 0.0625, 3},
        {"0.062500", 0.0625, 5},
        {"0.0078125", 0.0078125, 5},
        {"0.00781250", 0.0078125, 6},
        {"0.03125", 0.03125, 4},
        {"0.031250", 0.03125, 5},
        {"0.0156250", 0.015625, 6},
        {"0.01562500", 0.015625, 7},
        {"0.00390625000", 0.00390625, 9},
        {"0.003906250000", 0.00390625, 10},
        {"-0.062500", -0.0625, 5},
        {"-0.001953125000", -0.001953125, 10},
        // The boundary between fixed and exponential notation
        {"0.000001", 1e-6, 1},
        {"0.0000010", 1e-6, 2},
        {"0.0000010000", 1e-6, 5},
        {"1e-7", 1e-7, 1},
        {"1.0e-7", 1e-7, 2},
        {"5e-7", 5e-7, 1},
        {"9e-7", 9e-7, 1},
        {"9.9e-7", 9.9e-7, 2},
        {"2.5e-7", 2.5e-7, 2},
        {"3.5e-7", 3.5e-7, 2},
        {"1.2e-8", 1.25e-8, 2},
        {"1.0e-8", 1.04e-8, 2},
        {"1e-7", 1.5e-7, 1},
        {"0.1", 0.1, 1},
        {"0.10", 0.1, 2},
        // Padding of values that are already exact
        {"0.050000", 0.05, 5},
        {"0.05000000000000000277555756156289135105907917022705078125", 0.05, 55},
        {"0.0500000000000000027755575615628913510590791702270507812500000", 0.05, 60},
        {"1.5000", 1.5, 5},
        {"0.125", 0.125, 3},
        {"0.1250", 0.125, 4},
        {"0.1875", 0.1875, 4},
        // Long expansions and rounding
        {"0.29999999999999999", 0.3, 17},
        {"0.29999999999999998890", 0.3, 20},
        {"0.69999999999999996", 0.7, 17},
        {"0.999999", 0.999999, 6},
        {"1.0", 0.999999, 2},
        {"0.01", 0.0099, 1},
        {"0.1", 0.0999999, 1},
        {"9e-7", 0.0000009, 1},
        // Large values, fixed and exponential
        {"1.00000000000000000000e+21", 1e21, 21},
        {"1000000000000000000000", 1e21, 22},
        {"1.000000000000000000000e+22", 1e22, 22},
        {"123.5", 123.456, 4},
        {"1.2e+2", 123.456, 2},
        // Controls that were already correct before the fix
        {"0.000", 0.0, 4},
        {"-0.5000", -0.5, 4}
    };

    private static Object[][] getToPrecisionParams() {
        return TO_PRECISION_TESTS;
    }

    @ParameterizedTest
    @MethodSource("getToPrecisionParams")
    public void testToPrecision(String expected, double d, int precision) {
        assertEquals(
                expected,
                DecimalFormatter.toPrecision(d, precision),
                "Expected: " + expected + " val = " + d + " precision = " + precision);
        // The same value through the JavaScript API
        Utils.assertWithAllModes_ES6(
                expected, "(" + ScriptRuntime.toString(d) + ").toPrecision(" + precision + ")");
    }

    /**
     * A result is wrong exactly when the value is in [1e-6, 0.1) so the result is in fixed notation
     * with zeros right after the decimal point, and the exact decimal expansion of the value has
     * fewer significant digits than the requested precision. Sweep a deterministic set of values
     * covering that range and compare against the expected strings derived from the specification.
     */
    @Test
    public void sweepFixedNotationValues() {
        List<double[]> values = new ArrayList<>();
        Random random = new Random(42);
        // Exact binary fractions: (2^-i) / 2 for i in [4, 24) all lie below 0.1
        for (int i = 4; i < 24; i++) {
            values.add(new double[] {1.0 / (1L << i)});
        }
        // Denary-looking values near powers of ten
        for (int e = 1; e <= 5; e++) {
            for (int m = 1; m <= 9; m++) {
                values.add(new double[] {m * Math.pow(10, -e - 1)});
            }
        }
        // Arbitrary doubles in [1e-6, 0.1)
        for (int i = 0; i < 300; i++) {
            double v = random.nextDouble() * 0.1;
            values.add(new double[] {Math.max(v, 1e-6)});
        }

        for (double[] valueAndUnused : values) {
            double v = valueAndUnused[0];
            for (int precision = 1; precision <= 100; precision++) {
                assertEquals(
                        expectedFor(v, precision),
                        DecimalFormatter.toPrecision(v, precision),
                        "val = " + v + " precision = " + precision);
            }
        }
    }

    /**
     * The expected string of the ECMAScript Number::toString specification for toPrecision, derived
     * independently from the implementation under test using the exact expansion of the rounded
     * value.
     */
    private static String expectedFor(double v, int precision) {
        boolean negative = Math.signum(v) < 0;
        var bd = new BigDecimal(Math.abs(v), new MathContext(precision, RoundingMode.HALF_UP));
        String digits = bd.unscaledValue().toString();
        int scale = bd.scale();
        int numDigits = digits.length();
        int exponent = numDigits - scale - 1;

        StringBuilder expect = new StringBuilder();
        if (negative) {
            expect.append('-');
        }
        if (exponent < -6 || exponent >= precision) {
            expect.append(digits.charAt(0));
            if (numDigits > 1 || precision - 1 >= 1) {
                expect.append('.');
                expect.append(digits.substring(1));
                fillZeroes(expect, precision - 1 - (numDigits - 1));
            }
            expect.append('e');
            if (exponent >= 0) {
                expect.append('+');
            }
            expect.append(exponent);
        } else if (scale >= numDigits) {
            // 0.<leading zeroes><digits><padding zeroes>
            expect.append("0.");
            fillZeroes(expect, scale - numDigits);
            expect.append(digits);
            fillZeroes(expect, precision - numDigits);
        } else {
            // <integer digits>.<fraction digits><padding zeroes>
            expect.append(digits, 0, numDigits - scale);
            expect.append('.');
            expect.append(digits.substring(numDigits - scale));
            fillZeroes(expect, precision - numDigits);
        }
        return expect.toString();
    }

    private static void fillZeroes(StringBuilder b, int count) {
        for (int i = 0; i < count; i++) {
            b.append('0');
        }
    }

    @Test
    public void issue2586Repro() {
        Utils.assertWithAllModes_ES6(
                "0.06250\n0.06250000000\n0.00195312500\n-0.0312500000000000000000\n0.5000",
                "(0.0625).toPrecision(4) + '\\n'\n"
                        + "    + (0.0625).toPrecision(10) + '\\n'\n"
                        + "    + (0.001953125).toPrecision(9) + '\\n'\n"
                        + "    + (-0.03125).toPrecision(21) + '\\n'\n"
                        + "    + (0.5).toPrecision(4)");
    }

    @Test
    public void toFixedAndToExponentialAreNotAffected() {
        Utils.assertWithAllModes_ES6("0.06250", "(0.0625).toFixed(5)");
        Utils.assertWithAllModes_ES6("6.2500e-2", "(0.0625).toExponential(4)");
        Utils.assertWithAllModes_ES6("5e-1", "(0.5).toExponential()");
        Utils.assertWithAllModes_ES6("0.00195312500", "(0.001953125).toFixed(11)");
    }
}
