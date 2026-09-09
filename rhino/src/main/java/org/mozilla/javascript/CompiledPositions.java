/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source positions for one generated class, addressed by the line number a stack frame reports.
 *
 * <p>A compiled frame exposes exactly one datum that varies from position to position: the {@code
 * line_number} of a LineNumberTable entry, sixteen bits per JVMS 4.7.12. The class name, method
 * name and source file are fixed for the whole frame. Those sixteen bits are therefore the entire
 * channel through which a column can travel.
 *
 * <p>Rhino chooses what goes in them. A position whose line is not yet spoken for emits its real
 * line, which is what a Java debugger expects. A second position on a line already claimed by a
 * different one emits a synthetic number taken from the top of the range instead, and this table
 * maps it back to the real line and column. So the reported line is always exact, the column is
 * exact too, and a synthetic number is only ever used where the real line could not have identified
 * the position anyway.
 */
public final class CompiledPositions {

    /** Name of the static field holding the table on a generated class. */
    public static final String FIELD_NAME = "_positions";

    /** Synthetic line numbers are handed out from the top of the 16-bit range downwards. */
    private static final int FIRST_SYNTHETIC_LINE = 0xFFFF;

    /** What a reported line number resolves to. */
    private static final class Entry {
        final int line;
        final int column;
        final String sourceName;

        Entry(int line, int column, String sourceName) {
            this.line = line;
            this.column = column;
            this.sourceName = sourceName;
        }
    }

    /**
     * Tables by generated class name. A stack frame gives us only the class name, so the lookup
     * cannot be keyed on the class itself.
     *
     * <p>The reference is weak and the strong one lives in a static field of the generated class,
     * so a table is collected along with the code it describes. Every generated class has a fresh
     * name, so a strong map here would grow without bound as scripts are compiled and discarded.
     */
    private static final Map<String, WeakReference<CompiledPositions>> BY_CLASS_NAME =
            new ConcurrentHashMap<>();

    private static final ReferenceQueue<CompiledPositions> STALE = new ReferenceQueue<>();

    /** A named reference, so a cleared entry can be found and removed. */
    private static final class Ref extends WeakReference<CompiledPositions> {
        final String className;

        Ref(String className, CompiledPositions value) {
            super(value, STALE);
            this.className = className;
        }
    }

    public static void register(String className, CompiledPositions positions) {
        for (Reference<? extends CompiledPositions> stale = STALE.poll();
                stale != null;
                stale = STALE.poll()) {
            BY_CLASS_NAME.remove(((Ref) stale).className, stale);
        }
        BY_CLASS_NAME.put(className, new Ref(className, positions));
    }

    /** The table for a generated class, or null if it has none. */
    static CompiledPositions forClass(String className) {
        WeakReference<CompiledPositions> ref = BY_CLASS_NAME.get(className);
        return ref == null ? null : ref.get();
    }

    private final Map<String, Map<Integer, Entry>> byMethod;

    private CompiledPositions(Map<String, Map<Integer, Entry>> byMethod) {
        this.byMethod = byMethod;
    }

    private Entry lookup(String methodName, int reportedLine) {
        Map<Integer, Entry> lines = byMethod.get(methodName);
        return lines == null ? null : lines.get(reportedLine);
    }

    /**
     * The real source line for a reported line number, which differs from it when the position had
     * to be given a synthetic one. Returns {@code reportedLine} unchanged when it is not in the
     * table.
     */
    public int getLine(String methodName, int reportedLine) {
        Entry e = lookup(methodName, reportedLine);
        return e == null ? reportedLine : e.line;
    }

    /** The one-based column, or zero when unknown. */
    public int getColumn(String methodName, int reportedLine) {
        Entry e = lookup(methodName, reportedLine);
        return e == null ? 0 : e.column;
    }

    /** The original source path, or null to use the class's own source file. */
    public String getSourceName(String methodName, int reportedLine) {
        Entry e = lookup(methodName, reportedLine);
        return e == null ? null : e.sourceName;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Collects positions during code generation. Not thread safe; one per generated class. */
    public static final class Builder {

        private static final class Key {
            final String method;
            final int line;
            final int column;
            final String sourceName;

            Key(String method, int line, int column, String sourceName) {
                this.method = method;
                this.line = line;
                this.column = column;
                this.sourceName = sourceName;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Key)) return false;
                Key k = (Key) o;
                return line == k.line
                        && column == k.column
                        && method.equals(k.method)
                        && Objects.equals(sourceName, k.sourceName);
            }

            @Override
            public int hashCode() {
                return Objects.hash(method, line, column, sourceName);
            }
        }

        private final Map<String, Map<Integer, Entry>> byMethod = new HashMap<>();
        private final Map<Key, Integer> emittedFor = new HashMap<>();

        private int nextSynthetic = FIRST_SYNTHETIC_LINE;
        private int highestRealLine;

        /**
         * Records a position and returns the line number to emit for it, which is the real line
         * unless that line already identifies a different position.
         *
         * @param column the one-based column, or zero when the position is synthetic and carries no
         *     column of its own
         */
        public int record(String methodName, int line, int column, String sourceName) {
            if (line > highestRealLine) highestRealLine = line;
            Map<Integer, Entry> lines = byMethod.computeIfAbsent(methodName, k -> new HashMap<>());

            Entry claimed = lines.get(line);
            if (claimed == null) {
                lines.put(line, new Entry(line, Math.max(column, 0), sourceName));
                emittedFor.put(new Key(methodName, line, column, sourceName), line);
                return line;
            }

            boolean sameAsClaimed =
                    claimed.line == line
                            && claimed.column == Math.max(column, 0)
                            && Objects.equals(claimed.sourceName, sourceName);
            if (sameAsClaimed) {
                return line;
            }

            // A position with no column of its own can neither claim a line nor be worth a
            // synthetic number; it just reuses whatever the line already resolves to.
            if (column <= 0) {
                return line;
            }

            Integer already = emittedFor.get(new Key(methodName, line, column, sourceName));
            if (already != null) {
                return already.intValue();
            }

            if (nextSynthetic <= highestRealLine) {
                // The 16-bit space is exhausted, which needs a file of some 65000 lines. Report
                // the column as unknown rather than emit a number that means something else.
                lines.put(line, new Entry(line, 0, claimed.sourceName));
                return line;
            }

            int synthetic = nextSynthetic--;
            lines.put(synthetic, new Entry(line, column, sourceName));
            emittedFor.put(new Key(methodName, line, column, sourceName), synthetic);
            return synthetic;
        }

        public boolean isEmpty() {
            return byMethod.isEmpty();
        }

        public CompiledPositions build() {
            Map<String, Map<Integer, Entry>> out = new HashMap<>();
            for (Map.Entry<String, Map<Integer, Entry>> e : byMethod.entrySet()) {
                out.put(e.getKey(), Map.copyOf(e.getValue()));
            }
            return new CompiledPositions(out);
        }
    }

    @Override
    public String toString() {
        return "CompiledPositions" + byMethod.keySet();
    }
}
