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
 * a stack frame reports and hands out position markers when a real line is already taken.
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
    void aSecondPositionOnALineGetsAMarkerThatMapsBack() {
        var b = CompiledPositions.builder();
        assertEquals(7, b.record(M, 7, 12, null));
        int marker = b.record(M, 7, 30, null);
        assertNotEquals(7, marker, "must not reuse a number that identifies another position");

        CompiledPositions p = b.build();
        assertEquals(7, p.getLine(M, 7));
        assertEquals(12, p.getColumn(M, 7));
        assertEquals(7, p.getLine(M, marker), "a marker resolves to the real line");
        assertEquals(30, p.getColumn(M, marker));
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
        assertEquals(second, b.record(M, 7, 30, null), "an already allocated marker");
    }

    /**
     * A compiler-invented entry such as the script end line carries no column and claims nothing.
     */
    @Test
    void aPositionWithoutAColumnTakesNoMarker() {
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
     * Markers come from the top of the 16-bit range. A method whose own lines reach them falls back
     * to reporting the column as unknown rather than emitting a misleading number.
     */
    @Test
    void exhaustingTheNumberSpaceDegradesToUnknown() {
        var b = CompiledPositions.builder();
        b.record(M, 0xFFFE, 1, null);
        b.record(M, 0xFFFF, 1, null);
        int emitted = b.record(M, 0xFFFE, 40, null);
        assertEquals(0xFFFE, emitted, "no room for a marker");
        CompiledPositions p = b.build();
        assertEquals(0xFFFE, p.getLine(M, 0xFFFE), "the line is still right");
        assertEquals(0, p.getColumn(M, 0xFFFE), "but the column is not guessed");
    }

    /** Exhausting one method's numbers must not touch another's. */
    @Test
    void theNumberSpaceIsPerMethod() {
        var b = CompiledPositions.builder();
        b.record(M, 0xFFFE, 1, null);
        b.record(M, 0xFFFF, 1, null);
        assertEquals(0xFFFE, b.record(M, 0xFFFE, 40, null), "exhausted in this method");

        int other = b.record("_c_g_2", 5, 1, null);
        int otherSecond = b.record("_c_g_2", 5, 20, null);
        assertEquals(5, other);
        assertNotEquals(5, otherSecond, "a different method still has its whole range");

        CompiledPositions p = b.build();
        assertEquals(20, p.getColumn("_c_g_2", otherSecond));
        assertEquals(5, p.getLine("_c_g_2", otherSecond));
    }

    /**
     * A minified file puts every position on one line, so nearly every one needs a marker. The
     * range is per method, so what matters is the positions in a single function, not the file.
     */
    @Test
    void aThousandPositionsOnOneLineAreAllExact() {
        var b = CompiledPositions.builder();
        int[] emitted = new int[1000];
        for (int i = 0; i < emitted.length; i++) {
            emitted[i] = b.record(M, 1, i + 1, null);
        }
        CompiledPositions p = b.build();
        for (int i = 0; i < emitted.length; i++) {
            assertEquals(1, p.getLine(M, emitted[i]), "position " + i);
            assertEquals(i + 1, p.getColumn(M, emitted[i]), "position " + i);
        }
    }
}
