package org.mozilla.javascript.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class Issue594Test {
    private static Context context;
    private static ScriptableObject scope;

    private final double expectedValue;
    private final String script;

    @Parameterized.Parameters(name="{0} === {1}")
    public static Collection arguments() {
        return asList(new Object[][]{{"parseInt(\"009\")", 9.0}, {"parseInt(\"-090\")", -90.0},
                {"parseInt(\"-008\")", -8.0}});
    }

    @BeforeClass
    public static void setupClass() {
        context = Context.enter();
        scope = context.initStandardObjects();
    }

    public Issue594Test(String script, double expectedValue) {
        this.script = script;
        this.expectedValue = expectedValue;
    }

    @Test
    public void parseIntParsesLeadingZeros() {
        double value = ((Number) context.evaluateString(scope, script, "testsrc", 1, null)).doubleValue();
        assertEquals(expectedValue, value, 1e-12);
    }
}
