package org.mozilla.javascript;

import static org.junit.Assert.*;

import org.junit.jupiter.api.Test;

public class ScriptRuntimeAdvanceStringIndexTest {
    @Test
    void nonUnicode() {
        assertEquals(2, ScriptRuntime.advanceStringIndex("abc", 1, false));
    }

    @Test
    void unicodeNormalCodePoint() {
        assertEquals(1, ScriptRuntime.advanceStringIndex("abc", 0, true));
    }

    @Test
    void unicodeCodePointSurrogatePair() {
        assertEquals(2, ScriptRuntime.advanceStringIndex("\uD81B\uDF777a", 0, true));
    }

    @Test
    void unicodeAfterEnd() {
        assertEquals(4, ScriptRuntime.advanceStringIndex("abc", 3, true));
    }
}
