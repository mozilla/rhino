package org.mozilla.javascript.interpreterv2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CompilerDataLineNumberTest {

    private CompilerData<?> build(LineNumberTable.Builder lineNumberTable) {
        CompilerData.Builder<?> b = new CompilerData.Builder<>();
        b.setLineNumberTable(lineNumberTable);
        return b.build();
    }

    @Test
    public void emptyLineNumberTable() {
        LineNumberTable.Builder lineNumberTable = new LineNumberTable.Builder();

        CompilerData<?> cData = build(lineNumberTable);
        assertArrayEquals(new int[0], cData.getLineNumbers());
        assertEquals(-1, cData.getFirstLineNumber());
        assertEquals(-1, cData.getPcFirstLineNumber());
        assertEquals("[]", cData.getLineNumberTableForDebug());
        assertEquals(-1, cData.getLineNumberFromPc(0, 0));
        assertEquals(-1, cData.getLineNumberFromPc(99, 0));
    }

    @Test
    public void lineNumberTableWithOneEntry() {
        LineNumberTable.Builder lineNumberTable = new LineNumberTable.Builder();
        lineNumberTable.add(1, 42, 42);

        CompilerData<?> cData = build(lineNumberTable);
        assertArrayEquals(new int[] {42}, cData.getLineNumbers());
        assertEquals(42, cData.getFirstLineNumber());
        assertEquals(1, cData.getPcFirstLineNumber());
        assertEquals("[1 -> [42]]", cData.getLineNumberTableForDebug());
        assertEquals(-1, cData.getLineNumberFromPc(0, 0));
        assertEquals(42, cData.getLineNumberFromPc(99, 0));
    }

    @Test
    public void lineNumberTableWithTwoEntries() {
        LineNumberTable.Builder lineNumberTable = new LineNumberTable.Builder();
        lineNumberTable.add(1, 1, 1);
        lineNumberTable.add(7, 2, 2);

        CompilerData<?> cData = build(lineNumberTable);
        assertArrayEquals(new int[] {1, 2}, cData.getLineNumbers());
        assertEquals(1, cData.getFirstLineNumber());
        assertEquals(1, cData.getPcFirstLineNumber());
        assertEquals("[1 -> [1],  7 -> [2]]", cData.getLineNumberTableForDebug());
        assertEquals(-1, cData.getLineNumberFromPc(0, 0));
        assertEquals(1, cData.getLineNumberFromPc(1, 0));
        assertEquals(1, cData.getLineNumberFromPc(6, 0));
        assertEquals(2, cData.getLineNumberFromPc(7, 0));
        assertEquals(2, cData.getLineNumberFromPc(99, 0));
    }

    @Test
    public void lineNumberTableLarge() {
        LineNumberTable.Builder lineNumberTable = new LineNumberTable.Builder();
        lineNumberTable.add(1, 2, 2);
        lineNumberTable.add(6, 3, 4);
        lineNumberTable.add(8, 5, 6);

        CompilerData<?> cData = build(lineNumberTable);
        assertArrayEquals(new int[] {2, 3, 4, 5, 6}, cData.getLineNumbers());
        assertEquals(2, cData.getFirstLineNumber());
        assertEquals(1, cData.getPcFirstLineNumber());
        assertEquals("[1 -> [2],  6 -> [3, 4],  8 -> [5, 6]]", cData.getLineNumberTableForDebug());
        assertEquals(-1, cData.getLineNumberFromPc(0, 0));
        assertEquals(2, cData.getLineNumberFromPc(1, 0));
        assertEquals(4, cData.getLineNumberFromPc(6, 0));
        assertEquals(4, cData.getLineNumberFromPc(7, 0));
        assertEquals(6, cData.getLineNumberFromPc(8, 0));
        assertEquals(6, cData.getLineNumberFromPc(99, 0));
    }

    @Test
    public void lineNumberTableReallyLarge() {
        LineNumberTable.Builder lineNumberTable = new LineNumberTable.Builder();
        for (int i = 1; i < 20; i = i + 4) {
            var instruciton = 1 << i;
            var start = 4 * i;
            var end = 3 + (4 * i);
            lineNumberTable.add(instruciton, start, end);
        }
        CompilerData<?> cData = build(lineNumberTable);
        assertEquals(4, cData.getFirstLineNumber());
        assertEquals(2, cData.getPcFirstLineNumber());
        assertEquals(
                "[2 -> [4, 5, 6, 7],  "
                        + "32 -> [20, 21, 22, 23],  "
                        + "512 -> [36, 37, 38, 39],  "
                        + "8192 -> [52, 53, 54, 55],  "
                        + "131072 -> [68, 69, 70, 71]]",
                cData.getLineNumberTableForDebug());
        assertEquals(-1, cData.getLineNumberFromPc(0, 0));
        assertEquals(-1, cData.getLineNumberFromPc(1, 0));
        assertEquals(7, cData.getLineNumberFromPc(2, 0));
        assertEquals(23, cData.getLineNumberFromPc(32, 0));
        assertEquals(39, cData.getLineNumberFromPc(512, 0));
        assertEquals(55, cData.getLineNumberFromPc(8192, 0));
        assertEquals(71, cData.getLineNumberFromPc(131072, 0));
    }
}
