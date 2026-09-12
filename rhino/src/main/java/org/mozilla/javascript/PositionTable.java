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
 * of entries costs less than the arrays an index would need.
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
    // that a reader sees the arrays as filled, not as they were being filled
    private transient volatile Entries entries;

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
        Entries e = entries();
        if (e != null) {
            int i = exact ? e.indexOf(key) : e.floorIndex(key);
            return i < 0 ? null : e.at(i, sourceNames);
        }
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
        return new Position(source < 0 ? null : sourceNames[source], line, column);
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
    private Entries entries() {
        if (count <= WORTH_INDEXING) return null;
        Entries e = entries;
        if (e == null) {
            e = new Entries(count);
            for (Cursor c = new Cursor(); c.advance(); ) {
                e.add(c.key, c.line, c.column, c.source, c.statement);
            }
            entries = e;
        }
        return e;
    }

    /** Entries as parallel arrays sorted by key: the form they are built in and searched in. */
    private static final class Entries {
        int[] keys;
        int[] lines;
        int[] columns;
        int[] sources;
        boolean[] statements;
        int size;

        /** Sized for {@code capacity} entries, growing only if more arrive. */
        Entries(int capacity) {
            keys = new int[capacity];
            lines = new int[capacity];
            columns = new int[capacity];
            sources = new int[capacity];
            statements = new boolean[capacity];
        }

        /** Appends an entry, or replaces the last one when it has the same key and yields to it. */
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

        int floorIndex(int key) {
            int i = indexOf(key);
            return i >= 0 ? i : -i - 2;
        }

        Position at(int i, String[] names) {
            return new Position(sources[i] < 0 ? null : names[sources[i]], lines[i], columns[i]);
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
        private final Entries entries = new Entries(0);
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
            return i < 0 ? null : entries.at(i, names.toArray(new String[0]));
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
