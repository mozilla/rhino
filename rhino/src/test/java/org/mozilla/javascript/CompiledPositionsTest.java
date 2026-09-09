/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * The position table used by the JVM bytecode backend, which addresses positions by the line number
 * a stack frame reports and hands out synthetic numbers when a real line is already taken.
 */
class CompiledPositionsTest {

    private static final String M = "_c_f_1";

    @Test
    void aLineWithOnePositionEmitsItself() {
        var b = CompiledPositions.builder();
        assertEquals(7, b.record(M, 7, 12, null));
        CompiledPositions p = b.build();
        assertEquals(7, p.getLine(M, 7));
        assertEquals(12, p.getColumn(M, 7));
    }

    @Test
    void aSecondPositionOnALineGetsASyntheticNumberThatMapsBack() {
        var b = CompiledPositions.builder();
        assertEquals(7, b.record(M, 7, 12, null));
        int synthetic = b.record(M, 7, 30, null);
        assertNotEquals(7, synthetic, "must not reuse a number that identifies another position");

        CompiledPositions p = b.build();
        assertEquals(7, p.getLine(M, 7));
        assertEquals(12, p.getColumn(M, 7));
        assertEquals(7, p.getLine(M, synthetic), "synthetic resolves to the real line");
        assertEquals(30, p.getColumn(M, synthetic));
    }

    @Test
    void manyPositionsOnOneLineAreAllExact() {
        var b = CompiledPositions.builder();
        int[] columns = {3, 11, 20, 34, 41};
        int[] emitted = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            emitted[i] = b.record(M, 4, columns[i], null);
        }
        CompiledPositions p = b.build();
        for (int i = 0; i < columns.length; i++) {
            assertEquals(4, p.getLine(M, emitted[i]));
            assertEquals(columns[i], p.getColumn(M, emitted[i]));
        }
    }

    @Test
    void repeatingAPositionReusesItsNumber() {
        var b = CompiledPositions.builder();
        assertEquals(7, b.record(M, 7, 12, null));
        int second = b.record(M, 7, 30, null);
        assertEquals(7, b.record(M, 7, 12, null), "the line's own position");
        assertEquals(second, b.record(M, 7, 30, null), "an already allocated synthetic");
    }

    /** A synthetic entry such as the script's end line carries no column and claims nothing. */
    @Test
    void aPositionWithoutAColumnTakesNoSyntheticNumber() {
        var b = CompiledPositions.builder();
        assertEquals(7, b.record(M, 7, 12, null));
        assertEquals(7, b.record(M, 7, 0, null));
        CompiledPositions p = b.build();
        assertEquals(12, p.getColumn(M, 7), "the real position survives");
    }

    @Test
    void differingSourcePathsOnOneLineAreBothExact() {
        var b = CompiledPositions.builder();
        int first = b.record(M, 7, 12, "src/a.ts");
        int second = b.record(M, 7, 12, "src/b.ts");
        assertNotEquals(first, second);
        CompiledPositions p = b.build();
        assertEquals("src/a.ts", p.getSourceName(M, first));
        assertEquals("src/b.ts", p.getSourceName(M, second));
        assertEquals(7, p.getLine(M, second));
    }

    @Test
    void methodsAreKeyedSeparatelyAndDoNotCollide() {
        var b = CompiledPositions.builder();
        assertEquals(7, b.record(M, 7, 12, null));
        assertEquals(7, b.record("_c_g_2", 7, 44, null), "a different method may reuse the line");
        CompiledPositions p = b.build();
        assertEquals(12, p.getColumn(M, 7));
        assertEquals(44, p.getColumn("_c_g_2", 7));
    }

    @Test
    void unknownLinesAndMethodsReportUnknown() {
        var b = CompiledPositions.builder();
        b.record(M, 7, 12, null);
        CompiledPositions p = b.build();
        assertEquals(0, p.getColumn(M, 99));
        assertEquals(99, p.getLine(M, 99), "an unrecorded line passes through");
        assertEquals(0, p.getColumn("_c_other_9", 7));
        assertNull(p.getSourceName(M, 99));
    }

    /**
     * Synthetic numbers come from the top of the 16-bit range. A file large enough to reach them
     * falls back to reporting the column as unknown rather than emitting a misleading number.
     */
    @Test
    void exhaustingTheNumberSpaceDegradesToUnknown() {
        var b = CompiledPositions.builder();
        b.record(M, 0xFFFE, 1, null);
        b.record(M, 0xFFFF, 1, null);
        int emitted = b.record(M, 0xFFFE, 40, null);
        assertEquals(0xFFFE, emitted, "no room for a synthetic number");
        CompiledPositions p = b.build();
        assertEquals(0xFFFE, p.getLine(M, 0xFFFE), "the line is still right");
        assertEquals(0, p.getColumn(M, 0xFFFE), "but the column is not guessed");
    }
}
