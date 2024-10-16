package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

/**
 * Test cases for the {@link org.mozilla.javascript.ScriptRuntime}.
 *
 * @author Ronald Brill
 */
public class ScriptRuntimeTest {

    /**
     * Test toNumber(Object) to work with non Scriptable objects that are not supported by the
     * Rhino. There was a bug that in this case the impl stucks in an endless loop.
     */
    @Test
    public void toNumberNotScriptable() {
        assertEquals(Double.NaN, ScriptRuntime.toNumber(Scriptable.NOT_FOUND), 0.00001);
        assertEquals(Double.NaN, ScriptRuntime.toNumber(new Object()), 0.00001);
        assertEquals(Double.NaN, ScriptRuntime.toNumber(new NullPointerException("NPE")), 0.00001);
    }

    /**
     * Test toString(Object) to work with non Scriptable objects that are not supported by the
     * Rhino. There was a bug that in this case the impl stucks in an endless loop.
     */
    @Test
    public void toStringNotScriptable() {
        assertTrue(
                ScriptRuntime.toString(Scriptable.NOT_FOUND)
                        .startsWith("org.mozilla.javascript.UniqueTag@"));
        assertTrue(ScriptRuntime.toString(Scriptable.NOT_FOUND).endsWith("NOT_FOUND"));

        assertTrue(ScriptRuntime.toString(new Object()).startsWith("java.lang.Object@"));

        assertEquals(
                "java.lang.NullPointerException: NPE",
                ScriptRuntime.toString(new NullPointerException("NPE")));
    }
}
