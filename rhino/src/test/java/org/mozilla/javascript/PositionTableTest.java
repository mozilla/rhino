/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.sourcemap.Position;

class PositionTableTest {

    private static void assertAt(String source, int line, int column, Position at) {
        assertEquals(source, at.getSourcePath(), "source");
        assertEquals(line, at.getLine(), "line");
        assertEquals(column, at.getColumn(), "column");
    }

    @Test
    void floorFindsTheLastEntryAtOrBeforeAKey() {
        var b = new PositionTable.Builder();
        b.add(10, 1, 5, null, true);
        b.add(20, 1, 17, null, true);
        b.add(35, 3, 2, null, true);
        PositionTable t = b.build();

        assertNull(t.floor(9), "before the first entry");
        assertAt(null, 1, 5, t.floor(10));
        assertAt(null, 1, 5, t.floor(19));
        assertAt(null, 1, 17, t.floor(20));
        assertAt(null, 3, 2, t.floor(35));
        assertAt(null, 3, 2, t.floor(1_000_000));
    }

    @Test
    void getMatchesAKeyExactly() {
        var b = new PositionTable.Builder();
        b.add(10, 1, 5, null, true);
        b.add(20, 1, 17, null, true);
        PositionTable t = b.build();

        assertAt(null, 1, 17, t.get(20));
        assertNull(t.get(15));
        assertNull(t.get(21));
    }

    /** Columns and lines move backwards as often as forwards, and keys can jump a long way. */
    @Test
    void deltasOfEverySizeAndSignRoundTrip() {
        int[][] entries = {
            {0, 1, 1},
            {1, 1, 80},
            {2, 1, 3},
            {130, 2_000, 1},
            {131, 70_000, 65_536},
            {100_000, 5, 40_000},
            {Integer.MAX_VALUE, 1, 1},
        };
        var b = new PositionTable.Builder();
        for (int[] e : entries) b.add(e[0], e[1], e[2], null, true);
        PositionTable t = b.build();
        for (int[] e : entries) assertAt(null, e[1], e[2], t.get(e[0]));
    }

    @Test
    void sourceNamesAreSharedAndAbsenceIsKept() {
        var b = new PositionTable.Builder();
        b.add(1, 1, 1, "a.ts", true);
        b.add(2, 2, 1, null, true);
        b.add(3, 3, 1, "b.ts", true);
        b.add(4, 4, 1, "a.ts", true);
        PositionTable t = b.build();

        assertAt("a.ts", 1, 1, t.get(1));
        assertAt(null, 2, 1, t.get(2));
        assertAt("b.ts", 3, 1, t.get(3));
        assertAt("a.ts", 4, 1, t.get(4));
    }

    /** The interpreter records a statement and then its first expression at the same pc. */
    @Test
    void addingAtTheLastKeyReplacesThatEntry() {
        var b = new PositionTable.Builder();
        b.add(10, 1, 1, null, true);
        b.add(10, 1, 9, null, true);
        assertAt(null, 1, 9, b.get(10));
        PositionTable t = b.build();
        assertAt(null, 1, 9, t.get(10));
        assertEquals(1, t.lines().length);
    }

    @Test
    void keysMustNotDecrease() {
        var b = new PositionTable.Builder();
        b.add(10, 1, 1, null, true);
        assertThrows(IllegalArgumentException.class, () -> b.add(9, 1, 1, null, true));
    }

    @Test
    void linesAreDistinctInOrderOfFirstAppearance() {
        var b = new PositionTable.Builder();
        b.add(1, 5, 1, null, true);
        b.add(2, 5, 9, null, true);
        b.add(3, 2, 1, null, true);
        b.add(4, 5, 1, null, true);
        b.add(5, 7, 1, null, true);
        assertArrayEquals(new int[] {5, 2, 7}, b.build().lines());
    }

    /** Only statements mark lines for the debugger; the expressions within them add none. */
    @Test
    void linesComeFromStatementsOnly() {
        var b = new PositionTable.Builder();
        b.add(1, 5, 1, null, true);
        b.add(2, 6, 9, null, false);
        b.add(3, 7, 1, null, true);
        PositionTable t = b.build();
        assertArrayEquals(new int[] {5, 7}, t.lines());
        assertAt(null, 6, 9, t.get(2));
    }

    /** A statement's line stays marked even when an expression at the same pc supersedes it. */
    @Test
    void anExpressionReplacingAStatementKeepsItsLine() {
        var b = new PositionTable.Builder();
        b.add(1, 5, 1, null, true);
        b.add(1, 5, 9, null, false);
        assertArrayEquals(new int[] {5}, b.build().lines());
    }

    /** And keeps it whole: the line the debugger stops on is the statement's, not the read's. */
    @Test
    void anExpressionOnAnotherLineDoesNotTakeTheStatementsKey() {
        var b = new PositionTable.Builder();
        b.add(1, 5, 1, null, true);
        b.add(1, 11, 9, null, false);
        PositionTable t = b.build();
        assertArrayEquals(new int[] {5}, t.lines());
        assertAt(null, 5, 1, t.get(1));
    }

    /** A statement does take the key from an expression already there. */
    @Test
    void aStatementReplacesAnExpressionAtTheSameKey() {
        var b = new PositionTable.Builder();
        b.add(1, 11, 9, null, false);
        b.add(1, 5, 1, null, true);
        PositionTable t = b.build();
        assertArrayEquals(new int[] {5}, t.lines());
        assertAt(null, 5, 1, t.get(1));
    }

    @Test
    void anEmptyTableHasNothing() {
        PositionTable t = new PositionTable.Builder().build();
        assertNull(t.floor(0));
        assertNull(t.get(0));
        assertEquals(0, t.lines().length);
    }

    /** A table past the threshold builds an index; one below it is walked. Both must agree. */
    @Test
    void walkedAndIndexedTablesAnswerAlike() {
        assertAnswers(5);
        assertAnswers(50);
    }

    private static void assertAnswers(int n) {
        var b = new PositionTable.Builder();
        for (int i = 0; i < n; i++) {
            b.add(i * 10, 100 + i, 1 + i, null, i % 2 == 0);
        }
        PositionTable t = b.build();

        for (int i = 0; i < n; i++) {
            assertAt(null, 100 + i, 1 + i, t.get(i * 10));
            assertNull(t.get(i * 10 + 1), "no entry between keys");
            assertAt(null, 100 + i, 1 + i, t.floor(i * 10 + 9));
        }
        assertNull(t.floor(-1), "nothing before the first key");

        int[] lines = new int[(n + 1) / 2];
        for (int i = 0; i < lines.length; i++) lines[i] = 100 + i * 2;
        assertArrayEquals(lines, t.lines());
    }

    /** The point of the encoding: typical entries take a few bytes, not eight. */
    @Test
    void typicalEntriesTakeAFewBytes() {
        var b = new PositionTable.Builder();
        int pc = 0;
        for (int i = 0; i < 1000; i++) {
            pc += 3 + (i % 7);
            b.add(pc, 1 + i / 4, 1 + (i * 13) % 60, null, i % 3 == 0);
        }
        int bytes = b.build().deltas.length;
        assertTrue(bytes <= 4000, "was " + bytes + " bytes for 1000 entries");
    }
}
