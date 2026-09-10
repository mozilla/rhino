/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.sourcemap.Position;

/**
 * The position table used by the JVM bytecode backend, which addresses positions by the line number
 * a stack frame reports and hands out position markers when a real line is already taken.
 */
class CompiledPositionsTest {

    private static final String M = "_c_f_1";

    private static void assertAt(int line, int column, Position at) {
        assertEquals(line, at.getLine(), "line");
        assertEquals(column, at.getColumn(), "column");
    }

    @Test
    void aLineWithOnePositionEmitsItself() {
        var b = CompiledPositions.builder();
        assertEquals(7, b.record(M, new Position(null, 7, 12)));
        assertAt(7, 12, b.build().get(M, 7));
    }

    @Test
    void aSecondPositionOnALineGetsAMarkerThatMapsBack() {
        var b = CompiledPositions.builder();
        assertEquals(7, b.record(M, new Position(null, 7, 12)));
        int marker = b.record(M, new Position(null, 7, 30));
        assertNotEquals(7, marker, "must not reuse a number that identifies another position");

        CompiledPositions p = b.build();
        assertAt(7, 12, p.get(M, 7));
        assertAt(7, 30, p.get(M, marker));
    }

    @Test
    void manyPositionsOnOneLineAreAllExact() {
        var b = CompiledPositions.builder();
        int[] columns = {3, 11, 20, 34, 41};
        int[] emitted = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            emitted[i] = b.record(M, new Position(null, 4, columns[i]));
        }
        CompiledPositions p = b.build();
        for (int i = 0; i < columns.length; i++) {
            assertAt(4, columns[i], p.get(M, emitted[i]));
        }
    }

    @Test
    void repeatingAPositionReusesItsNumber() {
        var b = CompiledPositions.builder();
        assertEquals(7, b.record(M, new Position(null, 7, 12)));
        int second = b.record(M, new Position(null, 7, 30));
        assertEquals(7, b.record(M, new Position(null, 7, 12)), "the line's own position");
        assertEquals(second, b.record(M, new Position(null, 7, 30)), "an already allocated marker");
    }

    /**
     * A compiler-invented entry such as the script end line carries no column and claims nothing.
     */
    @Test
    void aPositionWithoutAColumnTakesNoMarker() {
        var b = CompiledPositions.builder();
        assertEquals(7, b.record(M, new Position(null, 7, 12)));
        assertEquals(7, b.record(M, new Position(null, 7, 0)));
        assertAt(7, 12, b.build().get(M, 7));
    }

    @Test
    void differingSourcePathsOnOneLineAreBothExact() {
        var b = CompiledPositions.builder();
        int first = b.record(M, new Position("src/a.ts", 7, 12));
        int second = b.record(M, new Position("src/b.ts", 7, 12));
        assertNotEquals(first, second);
        CompiledPositions p = b.build();
        assertEquals("src/a.ts", p.get(M, first).getSourcePath());
        assertEquals("src/b.ts", p.get(M, second).getSourcePath());
        assertEquals(7, p.get(M, second).getLine());
    }

    @Test
    void methodsAreKeyedSeparatelyAndDoNotCollide() {
        var b = CompiledPositions.builder();
        assertEquals(7, b.record(M, new Position(null, 7, 12)));
        assertEquals(
                7,
                b.record("_c_g_2", new Position(null, 7, 44)),
                "a different method may reuse the line");
        CompiledPositions p = b.build();
        assertAt(7, 12, p.get(M, 7));
        assertAt(7, 44, p.get("_c_g_2", 7));
    }

    @Test
    void unknownLinesAndMethodsAreNotInTheTable() {
        var b = CompiledPositions.builder();
        b.record(M, new Position(null, 7, 12));
        CompiledPositions p = b.build();
        assertNull(p.get(M, 99));
        assertNull(p.get("_c_other_9", 7));
    }

    /**
     * Markers come from the top of the 16-bit range. A method whose own lines reach them falls back
     * to reporting the column as unknown rather than emitting a misleading number.
     */
    @Test
    void exhaustingTheNumberSpaceDegradesToUnknown() {
        var b = CompiledPositions.builder();
        b.record(M, new Position(null, 0xFFFE, 1));
        b.record(M, new Position(null, 0xFFFF, 1));
        assertEquals(0xFFFE, b.record(M, new Position(null, 0xFFFE, 40)), "no room for a marker");
        assertAt(0xFFFE, 0, b.build().get(M, 0xFFFE));
    }

    /**
     * Ahead-of-time compilation loads a class without the table that gives a marker meaning, so it
     * must emit real line numbers and report no column where a line holds several positions.
     */
    @Test
    void disablingMarkersKeepsRealLineNumbers() {
        var b = CompiledPositions.builder();
        b.disableMarkers();
        assertEquals(7, b.record(M, new Position(null, 7, 12)));
        assertEquals(
                7, b.record(M, new Position(null, 7, 30)), "no marker, so the real line is reused");
        assertAt(7, 0, b.build().get(M, 7));
    }

    /** Exhausting one method's numbers must not touch another's. */
    @Test
    void theNumberSpaceIsPerMethod() {
        var b = CompiledPositions.builder();
        b.record(M, new Position(null, 0xFFFE, 1));
        b.record(M, new Position(null, 0xFFFF, 1));
        assertEquals(
                0xFFFE, b.record(M, new Position(null, 0xFFFE, 40)), "exhausted in this method");

        int other = b.record("_c_g_2", new Position(null, 5, 1));
        int otherSecond = b.record("_c_g_2", new Position(null, 5, 20));
        assertEquals(5, other);
        assertNotEquals(5, otherSecond, "a different method still has its whole range");
        assertAt(5, 20, b.build().get("_c_g_2", otherSecond));
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
            emitted[i] = b.record(M, new Position(null, 1, i + 1));
        }
        CompiledPositions p = b.build();
        for (int i = 0; i < emitted.length; i++) {
            assertAt(1, i + 1, p.get(M, emitted[i]));
        }
    }
}
