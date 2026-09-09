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
 * Columns and original source paths for one generated class, keyed by method name and line.
 *
 * <p>The JVM {@code LineNumberTable} holds a line number and nothing else, and a class has a single
 * {@code SourceFile}, so a compiled frame cannot carry a column or a per-position source path on
 * its own. This table records what the class file cannot.
 *
 * <p>The line is the only per-position value a stack frame gives back, so it is also the key. Where
 * a line carries two different columns the column is reported as unknown rather than guessed: a
 * plausible but wrong column would send a source-map consumer to the wrong place, which is worse
 * than admitting ignorance. In normally formatted code a line holds one position and the column is
 * exact, and when a source mapper is attached the recorded line is already the mapped original
 * line, so collisions are rarer still.
 */
public final class CompiledPositions {

    /** Name of the static field holding the table on a generated class. */
    public static final String FIELD_NAME = "_positions";

    /** Column and source recorded for one line; column zero means "more than one, so unknown". */
    private static final class LinePosition {
        final int column;
        final String sourceName;

        LinePosition(int column, String sourceName) {
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

    private final Map<String, Map<Integer, LinePosition>> byMethod;

    private CompiledPositions(Map<String, Map<Integer, LinePosition>> byMethod) {
        this.byMethod = byMethod;
    }

    private LinePosition lookup(String methodName, int line) {
        Map<Integer, LinePosition> lines = byMethod.get(methodName);
        return lines == null ? null : lines.get(line);
    }

    /** The one-based column on a line, or zero when unknown or ambiguous. */
    public int getColumn(String methodName, int line) {
        LinePosition p = lookup(methodName, line);
        return p == null ? 0 : p.column;
    }

    /** The original source path for a line, or null to use the class's own source file. */
    public String getSourceName(String methodName, int line) {
        LinePosition p = lookup(methodName, line);
        return p == null ? null : p.sourceName;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Collects positions during code generation. Not thread safe; one per generated class. */
    public static final class Builder {
        private final Map<String, Map<Integer, LinePosition>> byMethod = new HashMap<>();

        /** Marks a line whose positions disagree, so it reports unknown. */
        private static final LinePosition AMBIGUOUS = new LinePosition(0, null);

        /**
         * @param column the one-based column, or zero when the position is synthetic and carries no
         *     column of its own.
         */
        public void add(String methodName, int line, int column, String sourceName) {
            Map<Integer, LinePosition> lines =
                    byMethod.computeIfAbsent(methodName, k -> new HashMap<>());
            LinePosition existing = lines.get(line);
            if (existing == null) {
                lines.put(line, new LinePosition(Math.max(column, 0), sourceName));
                return;
            }
            if (existing == AMBIGUOUS) {
                return;
            }
            if (!Objects.equals(existing.sourceName, sourceName)) {
                lines.put(line, AMBIGUOUS);
                return;
            }
            if (column <= 0) {
                // Carries no column, so it can neither supply nor contradict one.
                return;
            }
            if (existing.column == 0) {
                lines.put(line, new LinePosition(column, sourceName));
            } else if (existing.column != column) {
                lines.put(line, AMBIGUOUS);
            }
        }

        public boolean isEmpty() {
            return byMethod.isEmpty();
        }

        public CompiledPositions build() {
            Map<String, Map<Integer, LinePosition>> out = new HashMap<>();
            for (Map.Entry<String, Map<Integer, LinePosition>> e : byMethod.entrySet()) {
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
