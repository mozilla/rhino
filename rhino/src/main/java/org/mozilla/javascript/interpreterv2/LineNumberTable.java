package org.mozilla.javascript.interpreterv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Packed array-based line number table implementation.
 *
 * <p>Uses four parallel arrays for memory efficiency:
 *
 * <ul>
 *   <li>{@code pcs[]} - Program counter values (sorted)
 *   <li>{@code lineOffsets[]} - Start index in {@code lines[]} for each PC
 *   <li>{@code lineCounts[]} - Number of lines for each PC
 *   <li>{@code lines[]} - All line numbers packed sequentially
 * </ul>
 *
 * <p>Example: PC 10 has lines [5,6,7], PC 15 has line [8]
 *
 * <pre>
 * pcs = [10, 15]
 * lineOffsets = [0, 3]
 * lineCounts = [3, 1]
 * lines = [5, 6, 7, 8]
 * </pre>
 *
 * <p>Lookup uses binary search on pcs[] with O(log n) performance. Memory usage is approximately 12
 * bytes per PC + 4 bytes per line number (no object overhead).
 */
public class LineNumberTable {
    private static final Logger LOG = Logger.getLogger(LineNumberTable.class.getName());

    private final short[] pcs;
    private final short[] lineOffsets;
    private final short[] lineCounts;
    private final short[] lines;

    private LineNumberTable(short[] pcs, short[] lineOffsets, short[] lineCounts, short[] lines) {
        this.pcs = pcs;
        this.lineOffsets = lineOffsets;
        this.lineCounts = lineCounts;
        this.lines = lines;
    }

    /** Builder for LineNumberTable. */
    public static class Builder {
        private final List<Integer> pcsList = new ArrayList<>();
        private final List<Set<Integer>> linesList = new ArrayList<>();
        private int lastPc = -1;

        public Builder() {}

        public Builder(int initialSize) {
            if (initialSize > 0) {
                ((ArrayList<Integer>) pcsList).ensureCapacity(initialSize);
                ((ArrayList<Set<Integer>>) linesList).ensureCapacity(initialSize);
            }
        }

        /**
         * Add line numbers to the set for a given PC.
         *
         * @param pc Program counter
         * @param startLine Start line (inclusive)
         * @param endLine End line (inclusive)
         */
        public void add(int pc, int startLine, int endLine) {
            if (pc < lastPc) {
                throw new IllegalArgumentException("Bytecode addresses must be increasing");
            }

            Set<Integer> lineSet;
            if (pc == lastPc && !pcsList.isEmpty()) {
                lineSet = linesList.get(linesList.size() - 1);
            } else {
                lineSet = new LinkedHashSet<>();
                pcsList.add(pc);
                linesList.add(lineSet);
                lastPc = pc;
            }
            if (startLine == -1 && endLine == -1) {
                lineSet.add(-1);
            } else {
                for (int line = startLine; line <= endLine; line++) {
                    lineSet.add(line);
                }
            }
        }

        public LineNumberTable buildFinalTable() {
            if (pcsList.isEmpty()) {
                return null;
            }

            int numPcs = pcsList.size();

            short[] pcs = new short[numPcs];
            short[] lineOffsets = new short[numPcs];
            short[] lineCounts = new short[numPcs];

            int totalLines = 0;
            for (Set<Integer> lineSet : linesList) {
                totalLines += lineSet.size();
            }
            short[] lines = new short[totalLines];

            int lineIdx = 0;
            for (int i = 0; i < numPcs; i++) {
                pcs[i] = (short) (pcsList.get(i) & 0xFFFF);
                lineOffsets[i] = (short) (lineIdx & 0xFFFF);

                Set<Integer> lineSet = linesList.get(i);
                lineCounts[i] = (short) (lineSet.size() & 0xFFFF);

                for (int line : lineSet) {
                    lines[lineIdx++] = (short) (line & 0xFFFF);
                }
            }

            return new LineNumberTable(pcs, lineOffsets, lineCounts, lines);
        }
    }

    public static int getFirstLineNumber(LineNumberTable lineNumberTable) {
        if (lineNumberTable == null
                || lineNumberTable.lines == null
                || lineNumberTable.lines.length == 0) {
            return -1;
        }
        return lineNumberTable.lines[0];
    }

    public static short getPcFirstLineNumber(LineNumberTable lineNumberTable) {
        if (isEmpty(lineNumberTable)) {
            return -1;
        }
        return lineNumberTable.pcs[0];
    }

    /**
     * Get the set of line numbers associated with a given PC.
     *
     * @param lineNumberTable The line number table
     * @param pc The program counter
     * @return List of line numbers, or null if PC not found or synthetic
     */
    public static List<Integer> getLineSetFromPc(LineNumberTable lineNumberTable, int pc) {
        if (isEmpty(lineNumberTable)) {
            return null;
        }

        // Binary search for PC
        int idx = Arrays.binarySearch(lineNumberTable.pcs, (short) (pc & 0xFFFF));
        if (idx < 0) return null;

        int count = lineNumberTable.lineCounts[idx] & 0xFFFF;
        if (count == 0) return null;

        int offset = lineNumberTable.lineOffsets[idx] & 0xFFFF;

        if (count == 1 && lineNumberTable.lines[offset] == -1) {
            return null;
        }

        List<Integer> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add((int) lineNumberTable.lines[offset + i]);
        }
        return result;
    }

    /**
     * Get the last (most specific) line number associated with a given PC. If the PC is not found,
     * returns the line number of the largest PC that is less than the given PC.
     *
     * <p>When multiple line numbers are recorded at the same PC (e.g., from nested structures like
     * a BLOCK containing statements), this returns the last recorded line, which is typically the
     * most specific one from the actual statement rather than the outer container.
     *
     * @param lineNumberTable The line number table
     * @param pc The program counter
     * @return The last line number, or -1 if not found or synthetic
     */
    public static int getLineNumberFromPc(LineNumberTable lineNumberTable, int pc) {
        if (isEmpty(lineNumberTable)) {
            return -1;
        }

        int idx = Arrays.binarySearch(lineNumberTable.pcs, (short) (pc & 0xFFFF));

        if (idx < 0) {
            idx = -idx - 2;
            if (idx < 0) return -1;
        }

        int count = lineNumberTable.lineCounts[idx] & 0xFFFF;
        if (count == 0) return -1;

        int offset = lineNumberTable.lineOffsets[idx] & 0xFFFF;
        // Return the last line number (most specific) instead of the first
        int lastLine = lineNumberTable.lines[offset + count - 1];

        return lastLine == -1 ? -1 : lastLine;
    }

    /**
     * Get all unique line numbers in the table (excluding synthetic -1).
     *
     * @param lineNumberTable The line number table
     * @return Array of unique line numbers
     */
    public static int[] getLineNumbers(LineNumberTable lineNumberTable) {
        if (lineNumberTable == null
                || lineNumberTable.lines == null
                || lineNumberTable.lines.length == 0) {
            return new int[0];
        }

        Set<Integer> uniqueLines = new LinkedHashSet<>();
        for (short line : lineNumberTable.lines) {
            if (line != -1) {
                uniqueLines.add((int) line);
            }
        }
        return uniqueLines.stream().mapToInt(n -> n).toArray();
    }

    /**
     * Get a debug string representation of the line number table.
     *
     * @param lineNumberTable The line number table
     * @return Debug string showing PC -> lines mapping
     */
    public static String getDebugString(LineNumberTable lineNumberTable) {
        if (isEmpty(lineNumberTable)) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < lineNumberTable.pcs.length; i++) {
            if (i > 0) {
                sb.append(",  ");
            }
            sb.append(lineNumberTable.pcs[i] & 0xFFFF);
            sb.append(" -> [");

            int offset = lineNumberTable.lineOffsets[i] & 0xFFFF;
            int count = lineNumberTable.lineCounts[i] & 0xFFFF;
            for (int j = 0; j < count; j++) {
                if (j > 0) sb.append(", ");
                sb.append((int) lineNumberTable.lines[offset + j]);
            }
            sb.append(']');
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Check if the line number table is empty.
     *
     * @param lineNumberTable The line number table
     * @return true if empty
     */
    public static boolean isEmpty(LineNumberTable lineNumberTable) {
        return lineNumberTable == null
                || lineNumberTable.pcs == null
                || lineNumberTable.pcs.length == 0;
    }
}
