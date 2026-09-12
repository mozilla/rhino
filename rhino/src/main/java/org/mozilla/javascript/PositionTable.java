/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mozilla.javascript.sourcemap.Position;

/**
 * Maps code to the source position it came from. The key is a bytecode offset for the interpreter
 * and a reported line number for the compiled backend; it is the key alone that is guaranteed to
 * increase from one entry to the next, while the line, column and source path each move in either
 * direction. An entry also records whether it is a statement's, since only those mark lines for the
 * debugger.
 *
 * <p>The stored form is a byte array of variable-length deltas, one entry after another: an
 * unsigned delta for the key, carrying the statement flag in its low bit, then signed deltas for
 * the line, the column and, where any entry names a source, its index. Only error reporting and the
 * debugger ever read a table, so it is worth keeping small rather than immediately usable: as fixed
 * words it would outweigh the code it describes.
 *
 * <p>A table large enough to be worth an index builds one the first time it is read, since the few
 * tables that are read are read many times over. A small one is walked instead, which for a handful
 * of entries costs less than the array an index would need. The index is itself packed rather than
 * a set of {@code int[]}: each column counts from its own smallest value, in as many bytes as its
 * span needs, which for most functions is one.
 */
final class PositionTable implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    static final PositionTable EMPTY = new PositionTable(new byte[0], null, 0);

    // Below this an index costs more than the entries it would index, and a walk is short enough
    // that it is no slower to search
    private static final int WORTH_INDEXING = 8;

    final byte[] deltas;
    private final String[] sourceNames;
    private final int count;

    // Two threads indexing at once produce the same thing, so no locking is needed; volatile so
    // that a reader sees the index as filled, not as it was being filled
    private transient volatile Index index;

    private PositionTable(byte[] deltas, String[] sourceNames, int count) {
        this.deltas = deltas;
        this.sourceNames = sourceNames;
        this.count = count;
    }

    /** The entry at exactly {@code key}, or null. */
    Position get(int key) {
        return at(key, true);
    }

    /** The last entry at or before {@code key}, or null. */
    Position floor(int key) {
        return at(key, false);
    }

    private Position at(int key, boolean exact) {
        Index i = index();
        if (i != null) return i.at(key, exact, sourceNames);

        int foundKey = 0, line = 0, column = 0, source = -1;
        boolean found = false;
        for (Cursor c = new Cursor(); c.advance() && c.key <= key; ) {
            foundKey = c.key;
            line = c.line;
            column = c.column;
            source = c.source;
            found = true;
        }
        if (!found || (exact && foundKey != key)) return null;
        return position(source, line, column, sourceNames);
    }

    private static Position position(int source, int line, int column, String[] names) {
        return new Position(source < 0 ? null : names[source], line, column);
    }

    /**
     * Every distinct line a statement is on, in the order first met. Always walks the stream: the
     * debugger asks once per function, and an index holds nothing that would answer this faster.
     */
    int[] lines() {
        Set<Integer> seen = new LinkedHashSet<>();
        for (Cursor c = new Cursor(); c.advance(); ) {
            if (c.statement) seen.add(c.line);
        }
        return seen.stream().mapToInt(Integer::intValue).toArray();
    }

    /** The index for this table, or null when it is small enough to walk instead. */
    private Index index() {
        if (count <= WORTH_INDEXING) return null;
        Index i = index;
        if (i == null) {
            i = new Index(this);
            index = i;
        }
        return i;
    }

    /**
     * An encoded table laid out for searching: one record per entry in a single byte array, each
     * column held as an offset from its own smallest value. A function's keys, lines and columns
     * rarely span more than a couple of hundred, so one byte apiece is usually enough where a plain
     * {@code int[]} would spend four. One width serves every column, which costs a byte here and
     * there against keeping a width per column, and saves describing them at all.
     */
    private static final class Index {
        private static final int KEY = 0, LINE = 1, COLUMN = 2, SOURCE = 3;

        private final byte[] records;
        private final int size;
        private final int columns;
        private final int width;
        private final int keyBase, lineBase, columnBase, sourceBase;

        Index(PositionTable table) {
            boolean named = table.sourceNames != null;
            columns = named ? 4 : 3;

            int lowKey = Integer.MAX_VALUE, highKey = Integer.MIN_VALUE;
            int lowLine = Integer.MAX_VALUE, highLine = Integer.MIN_VALUE;
            int lowColumn = Integer.MAX_VALUE, highColumn = Integer.MIN_VALUE;
            int lowSource = Integer.MAX_VALUE, highSource = Integer.MIN_VALUE;
            int n = 0;
            for (Cursor c = table.new Cursor(); c.advance(); ) {
                n++;
                if (c.key < lowKey) lowKey = c.key;
                if (c.key > highKey) highKey = c.key;
                if (c.line < lowLine) lowLine = c.line;
                if (c.line > highLine) highLine = c.line;
                if (c.column < lowColumn) lowColumn = c.column;
                if (c.column > highColumn) highColumn = c.column;
                if (c.source < lowSource) lowSource = c.source;
                if (c.source > highSource) highSource = c.source;
            }
            size = n;
            keyBase = lowKey;
            lineBase = lowLine;
            columnBase = lowColumn;
            sourceBase = named ? lowSource : 0;

            long span = Math.max((long) highKey - lowKey, (long) highLine - lowLine);
            span = Math.max(span, (long) highColumn - lowColumn);
            if (named) span = Math.max(span, (long) highSource - lowSource);
            width = span <= 0xFF ? 1 : span <= 0xFFFF ? 2 : 4;

            records = new byte[size * columns * width];
            int i = 0;
            for (Cursor c = table.new Cursor(); c.advance(); i++) {
                write(i, KEY, c.key - keyBase);
                write(i, LINE, c.line - lineBase);
                write(i, COLUMN, c.column - columnBase);
                if (named) write(i, SOURCE, c.source - sourceBase);
            }
        }

        private void write(int i, int column, int offset) {
            int at = (i * columns + column) * width;
            for (int b = 0; b < width; b++) {
                records[at + b] = (byte) (offset >>> (b * 8));
            }
        }

        /** The offset stored for a column, before its base is added back. */
        private int read(int i, int column) {
            int at = (i * columns + column) * width;
            int offset = 0;
            for (int b = 0; b < width; b++) {
                offset |= (records[at + b] & 0xFF) << (b * 8);
            }
            return offset;
        }

        Position at(int key, boolean exact, String[] names) {
            int i = floorIndex(key);
            if (i < 0 || (exact && keyBase + read(i, KEY) != key)) return null;
            int source = columns > SOURCE ? sourceBase + read(i, SOURCE) : -1;
            return position(source, lineBase + read(i, LINE), columnBase + read(i, COLUMN), names);
        }

        /** The last entry at or before {@code key}, or -1 when every entry comes after it. */
        private int floorIndex(int key) {
            int low = 0, high = size - 1, found = -1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                if (keyBase + read(middle, KEY) <= key) {
                    found = middle;
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
            return found;
        }
    }

    /** The entries of a table under construction, as parallel arrays in key order. */
    private static final class Entries {
        int[] keys = new int[0];
        int[] lines = new int[0];
        int[] columns = new int[0];
        int[] sources = new int[0];
        boolean[] statements = new boolean[0];
        int size;

        /** Adds an entry, or replaces the last one when it has the same key and yields to it. */
        void add(int key, int line, int column, int source, boolean statement) {
            boolean replacing = size > 0 && keys[size - 1] == key;
            // An entry holds one position, so a statement keeps the key against an expression
            // landing on the same one. The expression's is the finer of the two, but it is only
            // ever a column apart, where giving up the statement's line loses the debugger a
            // line it can stop on.
            if (replacing && statements[size - 1] && !statement) return;
            int at = replacing ? size - 1 : size++;
            if (at == keys.length) {
                int grown = Math.max(size * 2, 8);
                keys = Arrays.copyOf(keys, grown);
                lines = Arrays.copyOf(lines, grown);
                columns = Arrays.copyOf(columns, grown);
                sources = Arrays.copyOf(sources, grown);
                statements = Arrays.copyOf(statements, grown);
            }
            keys[at] = key;
            lines[at] = line;
            columns[at] = column;
            sources[at] = source;
            statements[at] = statement;
        }

        int indexOf(int key) {
            return Arrays.binarySearch(keys, 0, size, key);
        }
    }

    private final class Cursor {
        int at, key, line, column;
        int source = -1;
        boolean statement;

        boolean advance() {
            if (at >= deltas.length) return false;
            int keyDelta = unsigned();
            key += keyDelta >>> 1;
            statement = (keyDelta & 1) != 0;
            line += signed();
            column += signed();
            if (sourceNames != null) source += signed();
            return true;
        }

        private int unsigned() {
            int v = 0;
            for (int shift = 0; ; shift += 7) {
                byte b = deltas[at++];
                v |= (b & 0x7F) << shift;
                if (b >= 0) return v;
            }
        }

        private int signed() {
            int v = unsigned();
            return (v >>> 1) ^ -(v & 1);
        }
    }

    private static int writeUnsigned(byte[] out, int at, int v) {
        while ((v & ~0x7F) != 0) {
            out[at++] = (byte) (v | 0x80);
            v >>>= 7;
        }
        out[at++] = (byte) v;
        return at;
    }

    private static int writeSigned(byte[] out, int at, int v) {
        return writeUnsigned(out, at, (v << 1) ^ (v >> 31));
    }

    /** Collects entries in key order and encodes them once complete. */
    static final class Builder {
        private final Entries entries = new Entries();

        private final List<String> names = new ArrayList<>();
        private final Map<String, Integer> nameIndexes = new HashMap<>();

        /**
         * Adds an entry. Keys must not decrease; adding at the last key replaces that entry, except
         * that a statement's entry is not replaced by an expression's.
         */
        void add(int key, int line, int column, String sourceName, boolean statement) {
            if (entries.size > 0 && key < entries.keys[entries.size - 1]) {
                throw new IllegalArgumentException("keys must not decrease");
            }
            int source = sourceName == null ? -1 : nameIndex(sourceName);
            entries.add(key, line, column, source, statement);
        }

        private int nameIndex(String sourceName) {
            Integer index = nameIndexes.get(sourceName);
            if (index == null) {
                index = names.size();
                names.add(sourceName);
                nameIndexes.put(sourceName, index);
            }
            return index;
        }

        /** The entry added at exactly {@code key}, or null. */
        Position get(int key) {
            int i = entries.indexOf(key);
            if (i < 0) return null;
            return position(
                    entries.sources[i],
                    entries.lines[i],
                    entries.columns[i],
                    names.toArray(new String[0]));
        }

        PositionTable build() {
            int size = entries.size;
            if (size == 0) return EMPTY;
            boolean named = !names.isEmpty();
            // Four varints of at most five bytes each
            byte[] out = new byte[size * 4 + 20];
            int at = 0;
            int lastKey = 0, lastLine = 0, lastColumn = 0, lastSource = -1;
            for (int i = 0; i < size; i++) {
                if (out.length - at < 20) out = Arrays.copyOf(out, out.length * 2);
                int keyDelta = (entries.keys[i] - lastKey) << 1 | (entries.statements[i] ? 1 : 0);
                at = writeUnsigned(out, at, keyDelta);
                at = writeSigned(out, at, entries.lines[i] - lastLine);
                at = writeSigned(out, at, entries.columns[i] - lastColumn);
                if (named) at = writeSigned(out, at, entries.sources[i] - lastSource);
                lastKey = entries.keys[i];
                lastLine = entries.lines[i];
                lastColumn = entries.columns[i];
                lastSource = entries.sources[i];
            }
            return new PositionTable(
                    Arrays.copyOf(out, at), named ? names.toArray(new String[0]) : null, size);
        }
    }
}
