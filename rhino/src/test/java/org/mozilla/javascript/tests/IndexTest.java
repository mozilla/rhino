package org.mozilla.javascript.tests;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mozilla.javascript.ScriptRuntime;

public class IndexTest {
    private void expectInteger(ScriptRuntime.StringIdOrIndex id, int v) {
        assertEquals(v, id.getIndex());
        assertNull(id.getStringId());
    }

    private void expectString(ScriptRuntime.StringIdOrIndex id, String s) {
        assertEquals(s, id.getStringId());
    }

    @Test
    public void testNumericIndices() {
        // Normal integers
        expectInteger(ScriptRuntime.toStringIdOrIndex(0), 0);
        expectInteger(ScriptRuntime.toStringIdOrIndex(1), 1);
        expectInteger(ScriptRuntime.toStringIdOrIndex(Integer.MAX_VALUE), Integer.MAX_VALUE);
        // Negative integers
        expectString(ScriptRuntime.toStringIdOrIndex(-1), "-1");
        expectString(ScriptRuntime.toStringIdOrIndex(Integer.MIN_VALUE), "-2147483648");
        // Floating-point -- but rounding is weird so just check nullness
        ScriptRuntime.StringIdOrIndex id;
        id = ScriptRuntime.toStringIdOrIndex(1.1f);
        assertNotNull(id.getStringId());
    }

    @Test
    public void testStringIndices() {
        // Normal integers
        expectInteger(ScriptRuntime.toStringIdOrIndex("0"), 0);
        expectInteger(ScriptRuntime.toStringIdOrIndex("1"), 1);
        expectInteger(
                ScriptRuntime.toStringIdOrIndex(String.valueOf(Integer.MAX_VALUE)),
                Integer.MAX_VALUE);
        // Negative integers
        expectString(ScriptRuntime.toStringIdOrIndex("-1"), "-1");
        expectString(
                ScriptRuntime.toStringIdOrIndex(String.valueOf(Integer.MIN_VALUE)), "-2147483648");
        // Floating-point
        expectString(ScriptRuntime.toStringIdOrIndex("3.14"), "3.14");
        expectString(ScriptRuntime.toStringIdOrIndex("1.1"), "1.1");
        // Out of range
        expectString(
                ScriptRuntime.toStringIdOrIndex(String.valueOf(Long.MAX_VALUE)),
                "9223372036854775807");
        // Others
        expectString(ScriptRuntime.toStringIdOrIndex(Double.NaN), "NaN");
        // Junk
        expectString(
                ScriptRuntime.toStringIdOrIndex("This is not an integer"),
                "This is not an integer");
    }
}
