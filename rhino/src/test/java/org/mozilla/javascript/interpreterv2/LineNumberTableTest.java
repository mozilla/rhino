package org.mozilla.javascript.interpreterv2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class LineNumberTableTest {
    @Test
    public void testGetFirstLineNumberFromNonEmptyMap() {
        LineNumberTable.Builder builder = new LineNumberTable.Builder();
        builder.add(0, 2, 2);
        LineNumberTable table = builder.buildFinalTable();
        assertEquals(2, LineNumberTable.getFirstLineNumber(table));
    }

    @Test
    public void testGetFirstLineNumberFromEmptyMap() {
        assertEquals(-1, LineNumberTable.getFirstLineNumber(null));
    }

    @Test
    public void testGetFirstLineNumberFromNullMap() {
        assertEquals(-1, LineNumberTable.getFirstLineNumber(null));
    }

    @Test
    public void testGetPcFirstLineNumberFromNullMap() {
        assertEquals(-1, LineNumberTable.getPcFirstLineNumber(null));
    }

    @Test
    public void testGetPcFirstLineNumberFromNonEmptyMap() {
        LineNumberTable.Builder builder = new LineNumberTable.Builder();
        builder.add(0, 2, 2);
        LineNumberTable table = builder.buildFinalTable();
        assertEquals(0, LineNumberTable.getPcFirstLineNumber(table));
    }

    @Test
    public void testGetPcFirstLineNumberFromEmptyMap() {
        assertEquals(-1, LineNumberTable.getPcFirstLineNumber(null));
    }

    @Test
    public void testGetLineNumberFromStandardMap() {
        // bytecodes [1, 4) refer to line 1, [4, ...) to line 2
        LineNumberTable.Builder builder = new LineNumberTable.Builder();
        builder.add(1, 1, 1);
        builder.add(4, 2, 2);
        LineNumberTable table = builder.buildFinalTable();
        assertEquals(-1, LineNumberTable.getLineNumberFromPc(table, 0));
        assertEquals(1, LineNumberTable.getLineNumberFromPc(table, 1));
        assertEquals(1, LineNumberTable.getLineNumberFromPc(table, 2));
        assertEquals(2, LineNumberTable.getLineNumberFromPc(table, 4));
        assertEquals(2, LineNumberTable.getLineNumberFromPc(table, 6));
    }

    @Test
    public void testGetLineNumberFromOnlyOneEntry() {
        LineNumberTable.Builder builder = new LineNumberTable.Builder();
        builder.add(1, 1, 1);
        LineNumberTable table = builder.buildFinalTable();
        assertEquals(-1, LineNumberTable.getLineNumberFromPc(table, 0));
        assertEquals(1, LineNumberTable.getLineNumberFromPc(table, 1));
        assertEquals(1, LineNumberTable.getLineNumberFromPc(table, 2));
    }

    @Test
    public void testGetLineNumberFromEmptyMap() {
        assertEquals(-1, LineNumberTable.getLineNumberFromPc(null, 0));
        assertEquals(-1, LineNumberTable.getLineNumberFromPc(null, 2));
    }

    @Test
    public void testGetLineNumberFromNullMap() {
        assertEquals(-1, LineNumberTable.getLineNumberFromPc(null, 0));
    }

    @Test
    public void testGetLineNumbers() {
        LineNumberTable.Builder builder = new LineNumberTable.Builder();
        builder.add(0, 1, 1);
        builder.add(4, 2, 2);
        LineNumberTable table = builder.buildFinalTable();
        assertArrayEquals(new int[] {1, 2}, LineNumberTable.getLineNumbers(table));
    }

    @Test
    public void testGetLineNumbersFromEmptyMap() {
        assertArrayEquals(new int[0], LineNumberTable.getLineNumbers(null));
    }

    @Test
    public void testGetLineNumbersFromNullMap() {
        assertArrayEquals(new int[0], LineNumberTable.getLineNumbers(null));
    }

    @Test
    public void testBuilder() {
        LineNumberTable.Builder builder = new LineNumberTable.Builder(6);
        builder.add(0, 4, 4);
        builder.add(0, 5, 5);
        builder.add(0, -1, -1);
        builder.add(4, 12, 12);
        builder.add(6, 13, 13);

        LineNumberTable table = builder.buildFinalTable();
        assertNotNull(table);

        // PC 0 should have lines 4, 5, and -1 (synthetic)
        // Note: getLineSetFromPc returns null for synthetic entries
        String debugString = LineNumberTable.getDebugString(table);
        assertTrue(debugString.contains("0 -> [4, 5, -1]"), "PC 0 should have lines 4, 5, -1");
        assertTrue(debugString.contains("4 -> [12]"), "PC 4 should have line 12");
        assertTrue(debugString.contains("6 -> [13]"), "PC 6 should have line 13");
    }

    @Test
    public void testBuilderEnforcesPcOrder() {
        LineNumberTable.Builder builder = new LineNumberTable.Builder(6);
        builder.add(2, 4, 4);
        assertThrows(IllegalArgumentException.class, () -> builder.add(0, 5, 5));
    }

    @Test
    public void testBuilderWillReturnNullForEmptyMap() {
        LineNumberTable.Builder builder = new LineNumberTable.Builder();
        assertNull(builder.buildFinalTable());
    }

    @Test
    public void testBuilderAccumulatesLines() {
        // Test that multiple adds to the same PC accumulate lines
        LineNumberTable.Builder builder = new LineNumberTable.Builder();
        builder.add(0, 1, 1);
        builder.add(0, 2, 2);
        builder.add(0, 3, 3);

        LineNumberTable table = builder.buildFinalTable();
        List<Integer> lines = LineNumberTable.getLineSetFromPc(table, 0);
        assertEquals(3, lines.size());
        assertTrue(lines.contains(1));
        assertTrue(lines.contains(2));
        assertTrue(lines.contains(3));
    }

    @Test
    public void testBuilderDeduplicatesLines() {
        // Test that duplicate lines are deduplicated
        LineNumberTable.Builder builder = new LineNumberTable.Builder();
        builder.add(0, 1, 1);
        builder.add(0, 1, 1);
        builder.add(0, 2, 2);

        LineNumberTable table = builder.buildFinalTable();
        List<Integer> lines = LineNumberTable.getLineSetFromPc(table, 0);
        assertEquals(2, lines.size()); // Only 1 and 2, not 1, 1, 2
    }

    /**
     * We want to check the lookup logic, so we'll build various sizes of maps and search for all
     * possible entries, to ensure we don't have any weird edge cases with strange sizes of the map.
     */
    @Test
    public void testGetLineNumberOnMultipleMapsNoGaps() {
        // Map is [1,101,  2,102,  3,103, ...]
        for (int size = 1; size <= 16; ++size) {
            LineNumberTable.Builder builder = new LineNumberTable.Builder();
            for (int pc = 1; pc <= size; ++pc) {
                builder.add(pc, 100 + pc, 100 + pc);
            }
            LineNumberTable table = builder.buildFinalTable();

            for (int pc = 1; pc <= size; ++pc) {
                assertEquals(
                        100 + pc,
                        LineNumberTable.getLineNumberFromPc(table, pc),
                        "Should find line for pc " + pc + " in table with no gaps of size " + size);
            }

            assertEquals(
                    -1,
                    LineNumberTable.getLineNumberFromPc(table, 0),
                    "Should not find line number for pc before the first one in table with no gaps of size "
                            + size);
            assertEquals(
                    100 + size,
                    LineNumberTable.getLineNumberFromPc(table, size),
                    "Should find line number for pc after the last one in table with no gaps of size "
                            + size);
        }
    }

    /**
     * Similar to {@link #testGetLineNumberOnMultipleMapsNoGaps()} but we do have gaps in the PC.
     */
    @Test
    public void testGetLineNumberOnMultipleMapsWithGaps() {
        // Map is [1,0,  2,1,  4,2,  8,3,  16,4, ...]
        for (int size = 1; size <= 10; ++size) {
            LineNumberTable.Builder builder = new LineNumberTable.Builder();
            for (int i = 0; i < size; ++i) {
                builder.add((int) Math.pow(2, i), i, i);
            }
            LineNumberTable table = builder.buildFinalTable();

            for (int pc = 1; pc < Math.pow(2, size); ++pc) {
                int log2OfPc = (int) Math.floor(Math.log(pc) / Math.log(2));
                assertEquals(
                        log2OfPc,
                        LineNumberTable.getLineNumberFromPc(table, pc),
                        "Should find line for pc " + pc + " in table with gaps of size " + size);
            }

            assertEquals(
                    -1,
                    LineNumberTable.getLineNumberFromPc(table, 0),
                    "Should not find line number for pc before the first one in table with gaps of size "
                            + size);
            assertEquals(
                    size - 1,
                    LineNumberTable.getLineNumberFromPc(table, (int) Math.pow(2, size)),
                    "Should find line number for pc after the last one in table with gaps of size "
                            + size);
        }
    }

    @Test
    public void testGetLineSetFromPc() {
        LineNumberTable.Builder builder = new LineNumberTable.Builder();
        // PC 0: single line 5
        builder.add(0, 5, 5);
        // PC 4: multiple lines 10, 11, 12
        builder.add(4, 10, 12);

        LineNumberTable table = builder.buildFinalTable();

        // Test single-line set
        List<Integer> lines = LineNumberTable.getLineSetFromPc(table, 0);
        assertNotNull(lines);
        assertEquals(1, lines.size());
        assertEquals(5, lines.get(0));

        // Test multi-line set
        lines = LineNumberTable.getLineSetFromPc(table, 4);
        assertNotNull(lines);
        assertEquals(3, lines.size());
        assertEquals(List.of(10, 11, 12), lines);

        // Test synthetic line
        LineNumberTable.Builder syntheticBuilder = new LineNumberTable.Builder();
        syntheticBuilder.add(0, -1, -1);
        LineNumberTable syntheticTable = syntheticBuilder.buildFinalTable();
        lines = LineNumberTable.getLineSetFromPc(syntheticTable, 0);
        assertNull(lines); // Synthetic lines return null

        // Test null for PC before first entry
        lines = LineNumberTable.getLineSetFromPc(table, -1);
        assertNull(lines);
    }
}
