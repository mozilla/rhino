/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * The line-keyed column table used by the JVM bytecode backend. Its contract is that a reported
 * column is exact or absent, never approximate.
 */
class CompiledPositionsTest {

    private static final String M = "_c_f_1";

    @Test
    void reportsTheColumnOfALineWithOnePosition() {
        var b = CompiledPositions.builder();
        b.add(M, 7, 12, null);
        assertEquals(12, b.build().getColumn(M, 7));
    }

    @Test
    void reportsUnknownWhenALineHasTwoColumns() {
        var b = CompiledPositions.builder();
        b.add(M, 7, 12, null);
        b.add(M, 7, 30, null);
        assertEquals(0, b.build().getColumn(M, 7), "a guess would misdirect a source map lookup");
    }

    @Test
    void repeatingTheSameColumnIsNotAConflict() {
        var b = CompiledPositions.builder();
        b.add(M, 7, 12, null);
        b.add(M, 7, 12, null);
        assertEquals(12, b.build().getColumn(M, 7));
    }

    /** A synthetic entry, such as the script's end line, carries no column of its own. */
    @Test
    void aPositionWithoutAColumnNeitherSuppliesNorDestroysOne() {
        var withPlaceholderFirst = CompiledPositions.builder();
        withPlaceholderFirst.add(M, 7, 0, null);
        withPlaceholderFirst.add(M, 7, 12, null);
        assertEquals(12, withPlaceholderFirst.build().getColumn(M, 7));

        var withPlaceholderLast = CompiledPositions.builder();
        withPlaceholderLast.add(M, 7, 12, null);
        withPlaceholderLast.add(M, 7, 0, null);
        assertEquals(12, withPlaceholderLast.build().getColumn(M, 7));

        var placeholderOnly = CompiledPositions.builder();
        placeholderOnly.add(M, 7, 0, null);
        assertEquals(0, placeholderOnly.build().getColumn(M, 7));
    }

    @Test
    void linesAndMethodsAreKeyedSeparately() {
        var b = CompiledPositions.builder();
        b.add(M, 7, 12, null);
        b.add(M, 8, 30, null);
        b.add("_c_g_2", 7, 44, null);
        CompiledPositions p = b.build();
        assertEquals(12, p.getColumn(M, 7));
        assertEquals(30, p.getColumn(M, 8));
        assertEquals(44, p.getColumn("_c_g_2", 7));
    }

    @Test
    void unknownLinesAndMethodsReportUnknown() {
        var b = CompiledPositions.builder();
        b.add(M, 7, 12, null);
        CompiledPositions p = b.build();
        assertEquals(0, p.getColumn(M, 99));
        assertEquals(0, p.getColumn("_c_other_9", 7));
        assertNull(p.getSourceName(M, 99));
    }

    @Test
    void carriesThePerLineSourcePath() {
        var b = CompiledPositions.builder();
        b.add(M, 7, 12, "src/a.ts");
        b.add(M, 8, 3, "src/b.ts");
        CompiledPositions p = b.build();
        assertEquals("src/a.ts", p.getSourceName(M, 7));
        assertEquals("src/b.ts", p.getSourceName(M, 8));
    }

    @Test
    void conflictingSourcePathsOnOneLineReportUnknown() {
        var b = CompiledPositions.builder();
        b.add(M, 7, 12, "src/a.ts");
        b.add(M, 7, 12, "src/b.ts");
        CompiledPositions p = b.build();
        assertNull(p.getSourceName(M, 7));
        assertEquals(0, p.getColumn(M, 7));
    }
}
