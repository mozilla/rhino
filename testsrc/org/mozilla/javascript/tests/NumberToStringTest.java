package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mozilla.javascript.ScriptRuntime;

@RunWith(Parameterized.class)
public class NumberToStringTest {
    @Parameterized.Parameter(0)
    public String expected;

    @Parameterized.Parameter(1)
    public double value;

    // toString results from V8 compared to their inputs
    private static final Object[][] TO_STRING_TESTS = {
        // order: expected result, source
        {"0", 0.0},
        {"1", 1.0},
        {"-1", -1.0},
        {"100", 100},
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

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return java.util.Arrays.asList(TO_STRING_TESTS);
    }

    @Test
    public void testToString() {
        assertEquals(expected, ScriptRuntime.toString(value));
    }
}
