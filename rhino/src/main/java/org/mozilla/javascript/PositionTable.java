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
 * Source positions keyed by an integer that never decreases: the bytecode offset a position takes
 * effect at in the interpreter, or the line number a compiled frame reports.
 *
 * <p>Only error reporting and the debugger read this, so it is stored the way HotSpot stores its
 * own line number table: as a stream of variable-length deltas, decoded front to back. Stored as
 * fixed words the table outweighs the code it describes.
 */
final class PositionTable implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    static final PositionTable EMPTY = new PositionTable(new byte[0], null);

    // Per entry: the key delta, then the line, column and source deltas, the last only when some
    // entry names a source
    private final byte[] deltas;
    private final String[] sourceNames;

    private PositionTable(byte[] deltas, String[] sourceNames) {
        this.deltas = deltas;
        this.sourceNames = sourceNames;
    }

    /** The entry at exactly {@code key}, or null. */
    Position get(int key) {
        return find(key, true);
    }

    /** The last entry at or before {@code key}, or null. */
    Position floor(int key) {
        return find(key, false);
    }

    private Position find(int key, boolean exact) {
        Cursor c = new Cursor();
        int found = 0, line = 0, column = 0, source = -1;
        boolean any = false;
        while (c.advance() && c.key <= key) {
            any = true;
            found = c.key;
            line = c.line;
            column = c.column;
            source = c.source;
        }
        if (!any || (exact && found != key)) return null;
        return new Position(source < 0 ? null : sourceNames[source], line, column);
    }

    /** Every distinct line, in the order first met. */
    int[] lines() {
        Set<Integer> seen = new LinkedHashSet<>();
        for (Cursor c = new Cursor(); c.advance(); ) {
            seen.add(c.line);
        }
        return seen.stream().mapToInt(Integer::intValue).toArray();
    }

    private final class Cursor {
        int at, key, line, column;
        int source = -1;

        boolean advance() {
            if (at >= deltas.length) return false;
            key += unsigned();
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
        private int[] keys = new int[16];
        private int[] lines = new int[16];
        private int[] columns = new int[16];
        private int[] sources = new int[16];
        private int size;

        private final List<String> names = new ArrayList<>();
        private final Map<String, Integer> nameIndexes = new HashMap<>();

        /** Adds an entry. Keys must not decrease; adding at the last key replaces that entry. */
        void add(int key, int line, int column, String sourceName) {
            if (size > 0 && key < keys[size - 1]) {
                throw new IllegalArgumentException("keys must not decrease");
            }
            int at = size > 0 && keys[size - 1] == key ? size - 1 : size++;
            if (at == keys.length) {
                keys = Arrays.copyOf(keys, size * 2);
                lines = Arrays.copyOf(lines, size * 2);
                columns = Arrays.copyOf(columns, size * 2);
                sources = Arrays.copyOf(sources, size * 2);
            }
            keys[at] = key;
            lines[at] = line;
            columns[at] = column;
            sources[at] = sourceName == null ? -1 : nameIndex(sourceName);
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
            int i = Arrays.binarySearch(keys, 0, size, key);
            if (i < 0) return null;
            return new Position(
                    sources[i] < 0 ? null : names.get(sources[i]), lines[i], columns[i]);
        }

        PositionTable build() {
            if (size == 0) return EMPTY;
            boolean named = !names.isEmpty();
            byte[] out = new byte[size * 4];
            int at = 0;
            int lastKey = 0, lastLine = 0, lastColumn = 0, lastSource = -1;
            for (int i = 0; i < size; i++) {
                // Four varints of at most five bytes each
                if (out.length - at < 20) out = Arrays.copyOf(out, out.length * 2);
                at = writeUnsigned(out, at, keys[i] - lastKey);
                at = writeSigned(out, at, lines[i] - lastLine);
                at = writeSigned(out, at, columns[i] - lastColumn);
                if (named) at = writeSigned(out, at, sources[i] - lastSource);
                lastKey = keys[i];
                lastLine = lines[i];
                lastColumn = columns[i];
                lastSource = sources[i];
            }
            return new PositionTable(
                    Arrays.copyOf(out, at), named ? names.toArray(new String[0]) : null);
        }
    }
}
