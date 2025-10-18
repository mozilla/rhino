package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.ScriptRuntime;

public class NumberToStringTest {

    // toString results from V8 compared to their inputs
    private static final Object[][] TO_STRING_TESTS = {
        // order: expected result, source
        {"0", 0.0},
        {"1", 1.0},
        {"-1", -1.0},
        {"100", 100, 0},
        {"0.000001", 0.000001},
        {"0.9999", 0.9999},
        {"123.456", 123.456},
        {"-123.456", -123.456},
        {"1e+23", 1E23},
        {"1.0000000000000001e+23", 100000000000000000000001.0},
        {"3.14", 3.14},
        {"1000000000", 1E9},
        {"1e+31", 1E31},
        // Make sure we have various high and low ranges
        {"3.141592653589793", Math.PI},
        {"314159265358.9793", Math.PI * 100000000000.0},
        {"3.141592653589793e-11", Math.PI / 100000000000.0},
        {"3141592653589793000", Math.PI * 1000000000000000.0 * 1000.0},
        {"3.1415926535897934e-14", 3.1415926535897934E-14},
        {"3.141592653589793e+23", Math.PI * 1000000000000000.0 * 100000000.0},
        {"9.352546905515962e+198", 9.3525469055159624998555744E198},
        // Boundaries
        {"1e-7", 1E-7},
        {"1e+21", 1E21},
        {"1e+21", 1.00000000000000001E21},
        {"1e+21", 0.99999999999999999E21},
        {"1.0000000000000001e+21", 1.0000000000000001E21},
        {"999999999999999900000", 0.9999999999999999E21},
        {"1e-7", 1.00000000000000001E-7},
        {"1.0000000000000001e-7", 1.0000000000000001E-7},
        {"1e-7", 0.9999999999999999E-7},
        {"9.99999999999999e-8", 0.999999999999999E-7},
        // Denormals
        {"5.88e-39", 5.88E-39},
        {"4.47118444e-314", 4.47118444E-314},
        // Specific values from the JDK test suite
        {"282879384806159000", 2.82879384806159E17},
        {"1387364135037754000", 1.387364135037754E18},
        {"145800632428665000", 1.45800632428665E17},
        {"1.6e-322", 1.6E-322},
        {"6.3e-322", 6.3E-322},
        {"738790000000000000000", 7.3879E20},
        {"2e+23", 2.0E23},
        {"7e+22", 7.0E22},
        {"9.2e+22", 9.2E22},
        {"9.5e+21", 9.5E21},
        {"3.1e+22", 3.1E22},
        {"5.63e+21", 5.63E21},
        {"8.41e+21", 8.41E21},
        {"1.9400994884341945e+25", 1.9400994884341945E25},
        {"3.6131332396758635e+25", 3.6131332396758635E25},
        {"2.5138990223946153e+25", 2.5138990223946153E25},
        // These are controversial -- Java gives a more rounded-
        // off result but the original Schubfach code does not.
        {"9.9e-324", 9.9E-324},
        {"9.9e-323", 9.9E-323}
    };

    private static Object[][] getToStringParams() {
        return TO_STRING_TESTS;
    }

    @ParameterizedTest
    @MethodSource("getToStringParams")
    public void testToString(String expected, double d) {
        assertEquals(expected, ScriptRuntime.toString(d), "Expected: " + expected + " val = " + d);
    }

    @ParameterizedTest
    @MethodSource("getRepresentativeDoubles")
    public void testArbitraryDouble(double orig) {
        // Turn the value into a string using our code and Java's built-in code
        String expected = Double.toString(orig);
        String result = ScriptRuntime.toString(orig);
        // Both should result in the same value once parsed
        double expectedDouble = Double.parseDouble(expected);
        double resultDouble = Double.parseDouble(result);
        assertEquals(expectedDouble, resultDouble);
    }

    private static Object[][] getRepresentativeDoubles() {
        // Come up with many interesting values. Thank you gpt-5 for this.
        java.util.List<Object[]> out = new java.util.ArrayList<>();
        // 1) Specials and simple constants
        double[] constants = {
            0.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.MIN_VALUE, // smallest positive subnormal
            Double.MIN_NORMAL, // smallest positive normal
            Double.MAX_VALUE,
            1.0,
            -1.0,
            2.0,
            10.0,
            1e-7,
            1e-6,
            1e-21,
            1e21,
            1e23,
            1e308,
            1e-308
        };
        for (double v : constants) out.add(new Object[] {v});

        // 2) small integers and powers of 10/powers of 2
        for (long i = 0; i <= 20; i++) out.add(new Object[] {(double) i});
        for (int e = -10; e <= 30; e += 5) out.add(new Object[] {Math.pow(10.0, e)});
        for (int e = -20; e <= 100; e += 10) out.add(new Object[] {Math.pow(2.0, e)});

        // 3) neighbors around interesting values (nextUp/nextDown)
        double[] interesting = {1.0, Math.PI, 1e-7, 1e21, Double.MIN_NORMAL, Double.MAX_VALUE / 2};
        for (double v : interesting) {
            out.add(new Object[] {Math.nextUp(v)});
            out.add(new Object[] {Math.nextDown(v)});
            out.add(new Object[] {v});
        }

        // 4) systematic mantissa patterns across a few exponents
        for (int exp : new int[] {0, 1, 10, 100, 500, 1000}) {
            int biased = exp + 1023;
            if (biased <= 0 || biased >= 0x7ff) continue;
            long expBits = ((long) biased) << 52;
            long[] mantissas = {
                0L,
                (1L << 52) - 1, // all ones
                1L, // least significant bit set
                1L << 51, // most-significant mantissa bit
                0x9249249249249L & ((1L << 52) - 1) // alternating-ish pattern
            };
            for (long m : mantissas) {
                long bits = expBits | m;
                out.add(new Object[] {Double.longBitsToDouble(bits)});
                out.add(new Object[] {-Double.longBitsToDouble(bits)});
            }
        }

        // 5) log-uniform deterministic sampling across exponent range using raw bits
        java.util.Random rnd = new java.util.Random(0); // deterministic
        final int SAMPLE_LOG = 30;
        for (int i = 0; i < SAMPLE_LOG; i++) {
            // pick exponent uniformly in full range [-1074 .. 1023] in biased form [0..0x7fe]
            int unbiasedExp = -1074 + (int) ((i * 1L * (1023 + 1074) / SAMPLE_LOG));
            int biased = unbiasedExp + 1023;
            if (biased < 0) biased = 0;
            if (biased > 0x7fe) biased = 0x7fe;
            long expBits = ((long) biased) << 52;
            long mantissa = rnd.nextLong() & ((1L << 52) - 1);
            long sign = (i % 2 == 0) ? 0L : (1L << 63);
            out.add(new Object[] {Double.longBitsToDouble(sign | expBits | mantissa)});
        }

        // 6) random raw 64-bit patterns (includes NaNs, denormals, infinities)
        rnd.setSeed(1);
        for (int i = 0; i < 40; i++) {
            long bits = rnd.nextLong();
            out.add(new Object[] {Double.longBitsToDouble(bits)});
        }

        // convert to Object[][] for ParameterizedTest
        Object[][] ret = new Object[out.size()][];
        return out.toArray(ret);
    }

    /**
     * Read a list of 10,000 doubles and their toString results as parsed in node.js, and compare
     * that our results match.
     */
    @Test
    public void testManyValues() throws IOException {
        try (var is =
                NumberToStringTest.class.getResourceAsStream(
                        "/org/mozilla/javascript/tests/to-string-numbers.csv")) {
            assertNotNull(is);
            var rdr = new BufferedReader(new InputStreamReader(is));
            int lineNum = 1;
            for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
                String[] fields = line.split(",", 2);
                // If not two fields, must be a comment
                if (fields.length != 2) {
                    continue;
                }
                double val = Double.parseDouble(fields[0].trim());
                String expected = fields[1].trim();
                assertEquals(expected, ScriptRuntime.toString(val));
                lineNum++;
            }
            assertTrue(lineNum > 1, "Expected at least one data line");
        }
    }

    private String trimTrailingZeroes(String s) {
        int len = s.length();
        while (len > 0 && s.charAt(len - 1) == '0') {
            len--;
        }
        if (len != s.length()) {
            return s.substring(0, len);
        }
        return s;
    }
}
