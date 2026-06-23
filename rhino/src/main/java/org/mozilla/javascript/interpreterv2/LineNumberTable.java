package org.mozilla.javascript.interpreterv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
public abstract class LineNumberTable {

    protected abstract int getFirstLineNo();

    protected abstract int getPcFirstLineNo();

    protected abstract List<Integer> getLineSetFromPc(int pc);

    protected abstract int getLineNoFromPc(int pc);

    protected abstract int[] getLineNos();

    protected abstract String debugString();

    protected abstract boolean isEmptyTable();

    private static class ShortLineNumberTable extends LineNumberTable {
        private final short[] pcs;
        private final short[] lineOffsets;
        private final short[] lineCounts;
        private final short[] lines;

        private ShortLineNumberTable(
                short[] pcs, short[] lineOffsets, short[] lineCounts, short[] lines) {
            this.pcs = pcs;
            this.lineOffsets = lineOffsets;
            this.lineCounts = lineCounts;
            this.lines = lines;
        }

        @Override
        protected String debugString() {
            if (isEmptyTable()) {
                return "[]";
            }

            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < pcs.length; i++) {
                if (i > 0) {
                    sb.append(",  ");
                }
                sb.append(pcs[i] & 0xFFFF);
                sb.append(" -> [");

                int offset = lineOffsets[i] & 0xFFFF;
                int count = lineCounts[i] & 0xFFFF;
                for (int j = 0; j < count; j++) {
                    if (j > 0) sb.append(", ");
                    sb.append((int) lines[offset + j]);
                }
                sb.append(']');
            }
            sb.append(']');
            return sb.toString();
        }

        @Override
        protected int getFirstLineNo() {
            if (this.lines == null || this.lines.length == 0) {
                return -1;
            }
            return lines[0];
        }

        @Override
        protected int getPcFirstLineNo() {
            if (isEmptyTable()) return -1;
            return pcs[0];
        }

        @Override
        protected int getLineNoFromPc(int pc) {
            int idx = Arrays.binarySearch(pcs, (short) (pc & 0xFFFF));

            if (idx < 0) {
                idx = -idx - 2;
                if (idx < 0) return -1;
            }

            int count = lineCounts[idx] & 0xFFFF;
            if (count == 0) return -1;

            int offset = lineOffsets[idx] & 0xFFFF;
            // Return the last line number (most specific) instead of the first
            int lastLine = lines[offset + count - 1];

            return lastLine == -1 ? -1 : lastLine;
        }

        @Override
        protected int[] getLineNos() {
            if (lines == null || lines.length == 0) {
                return new int[0];
            }

            Set<Integer> uniqueLines = new LinkedHashSet<>();
            for (short line : lines) {
                if (line != -1) {
                    uniqueLines.add((int) line);
                }
            }
            return uniqueLines.stream().mapToInt(n -> n).toArray();
        }

        @Override
        protected List<Integer> getLineSetFromPc(int pc) {
            if (isEmptyTable()) {
                return null;
            }

            // Binary search for PC
            int idx = Arrays.binarySearch(pcs, (short) (pc & 0xFFFF));
            if (idx < 0) return null;

            int count = lineCounts[idx] & 0xFFFF;
            if (count == 0) return null;

            int offset = lineOffsets[idx] & 0xFFFF;

            if (count == 1 && lines[offset] == -1) {
                return null;
            }

            List<Integer> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                result.add((int) lines[offset + i]);
            }
            return result;
        }

        @Override
        protected boolean isEmptyTable() {
            return pcs == null || pcs.length == 0;
        }
    }

    private static class IntLineNumberTable extends LineNumberTable {
        private final int[] pcs;
        private final int[] lineOffsets;
        private final int[] lineCounts;
        private final int[] lines;

        private IntLineNumberTable(int[] pcs, int[] lineOffsets, int[] lineCounts, int[] lines) {
            this.pcs = pcs;
            this.lineOffsets = lineOffsets;
            this.lineCounts = lineCounts;
            this.lines = lines;
        }

        @Override
        protected String debugString() {
            if (isEmptyTable()) {
                return "[]";
            }

            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < pcs.length; i++) {
                if (i > 0) {
                    sb.append(",  ");
                }
                sb.append(pcs[i]);
                sb.append(" -> [");

                int offset = lineOffsets[i];
                int count = lineCounts[i];
                for (int j = 0; j < count; j++) {
                    if (j > 0) sb.append(", ");
                    sb.append((int) lines[offset + j]);
                }
                sb.append(']');
            }
            sb.append(']');
            return sb.toString();
        }

        @Override
        protected int getFirstLineNo() {
            if (this.lines == null || this.lines.length == 0) {
                return -1;
            }
            return lines[0];
        }

        @Override
        protected int getPcFirstLineNo() {
            if (isEmptyTable()) return -1;
            return pcs[0];
        }

        @Override
        protected int getLineNoFromPc(int pc) {
            int idx = Arrays.binarySearch(pcs, pc);

            if (idx < 0) {
                idx = -idx - 2;
                if (idx < 0) return -1;
            }

            int count = lineCounts[idx];
            if (count == 0) return -1;

            int offset = lineOffsets[idx];
            // Return the last line number (most specific) instead of the first
            int lastLine = lines[offset + count - 1];

            return lastLine == -1 ? -1 : lastLine;
        }

        @Override
        protected int[] getLineNos() {
            if (lines == null || lines.length == 0) {
                return new int[0];
            }

            Set<Integer> uniqueLines = new LinkedHashSet<>();
            for (int line : lines) {
                if (line != -1) {
                    uniqueLines.add(line);
                }
            }
            return uniqueLines.stream().mapToInt(n -> n).toArray();
        }

        @Override
        protected List<Integer> getLineSetFromPc(int pc) {
            if (isEmptyTable()) {
                return null;
            }

            // Binary search for PC
            int idx = Arrays.binarySearch(pcs, pc);
            if (idx < 0) return null;

            int count = lineCounts[idx];
            if (count == 0) return null;

            int offset = lineOffsets[idx];

            if (count == 1 && lines[offset] == -1) {
                return null;
            }

            List<Integer> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                result.add((int) lines[offset + i]);
            }
            return result;
        }

        @Override
        protected boolean isEmptyTable() {
            return pcs == null || pcs.length == 0;
        }
    }

    /** Builder for LineNumberTable. */
    public static class Builder {
        private final List<Integer> pcsList = new ArrayList<>();
        private final List<Set<Integer>> linesList = new ArrayList<>();
        private int lastPc = -1;
        private boolean largeCaseSeen;

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
            if (pc > 0xffff || startLine > 0xffff || endLine > 0xffff) {
                largeCaseSeen = true;
            }

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

            return largeCaseSeen ? makeIntTable() : makeShortTable();
        }

        private LineNumberTable makeIntTable() {
            int numPcs = pcsList.size();

            int[] pcs = new int[numPcs];
            int[] lineOffsets = new int[numPcs];
            int[] lineCounts = new int[numPcs];

            int totalLines = 0;
            for (Set<Integer> lineSet : linesList) {
                totalLines += lineSet.size();
            }
            int[] lines = new int[totalLines];

            int lineIdx = 0;
            for (int i = 0; i < numPcs; i++) {
                pcs[i] = pcsList.get(i);
                lineOffsets[i] = lineIdx;

                Set<Integer> lineSet = linesList.get(i);
                lineCounts[i] = lineSet.size();

                for (int line : lineSet) {
                    lines[lineIdx++] = line;
                }
            }

            return new IntLineNumberTable(pcs, lineOffsets, lineCounts, lines);
        }

        private LineNumberTable makeShortTable() {
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

            return new ShortLineNumberTable(pcs, lineOffsets, lineCounts, lines);
        }
    }

    public static int getFirstLineNumber(LineNumberTable lineNumberTable) {
        if (lineNumberTable == null) {
            return -1;
        }
        return lineNumberTable.getFirstLineNo();
    }

    public static int getPcFirstLineNumber(LineNumberTable lineNumberTable) {
        if (lineNumberTable == null) {
            return -1;
        }
        return lineNumberTable.getPcFirstLineNo();
    }

    /**
     * Get the set of line numbers associated with a given PC.
     *
     * @param lineNumberTable The line number table
     * @param pc The program counter
     * @return List of line numbers, or null if PC not found or synthetic
     */
    public static List<Integer> getLineSetFromPc(LineNumberTable lineNumberTable, int pc) {
        if (lineNumberTable == null) return null;
        return lineNumberTable.getLineSetFromPc(pc);
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

        return lineNumberTable.getLineNoFromPc(pc);
    }

    /**
     * Get all unique line numbers in the table (excluding synthetic -1).
     *
     * @param lineNumberTable The line number table
     * @return Array of unique line numbers
     */
    public static int[] getLineNumbers(LineNumberTable lineNumberTable) {
        if (lineNumberTable == null) {
            return new int[0];
        }

        return lineNumberTable.getLineNos();
    }

    /**
     * Get a debug string representation of the line number table.
     *
     * @param lineNumberTable The line number table
     * @return Debug string showing PC -> lines mapping
     */
    public static String getDebugString(LineNumberTable lineNumberTable) {
        if (lineNumberTable == null) {
            return "[]";
        }
        return lineNumberTable.debugString();
    }

    /**
     * Check if the line number table is empty.
     *
     * @param lineNumberTable The line number table
     * @return true if empty
     */
    public static boolean isEmpty(LineNumberTable lineNumberTable) {
        return lineNumberTable == null || lineNumberTable.isEmptyTable();
    }
}
