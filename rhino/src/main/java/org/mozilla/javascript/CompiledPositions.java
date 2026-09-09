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
import org.mozilla.javascript.sourcemap.Position;

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
 * different one emits a <em>position marker</em> instead, taken from the top of the range, which
 * this table maps back to the real line and column. So the reported line is always exact, the
 * column is exact too, and a marker is only ever used where the real line could not have identified
 * the position anyway.
 *
 * <p>Markers are handed out per method rather than per class, since a lookup is keyed by method as
 * well. Each generated method therefore has the whole range to itself, less whatever its own lines
 * occupy.
 */
public final class CompiledPositions {

    /** Name of the static field holding the table on a generated class. */
    public static final String FIELD_NAME = "_positions";

    /** Position markers are handed out from the top of the 16-bit range downwards. */
    private static final int FIRST_POSITION_MARKER = 0xFFFF;

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

    private final Map<String, Map<Integer, Position>> byMethod;

    private CompiledPositions(Map<String, Map<Integer, Position>> byMethod) {
        this.byMethod = byMethod;
    }

    private Position lookup(String methodName, int reportedLine) {
        Map<Integer, Position> lines = byMethod.get(methodName);
        return lines == null ? null : lines.get(reportedLine);
    }

    /**
     * The position a compiled stack frame refers to, resolving the number it reports through the
     * table of whichever generated class it belongs to.
     *
     * <p>The reported number is the real line unless it is a marker, so this is the only place that
     * has to know the difference.
     *
     * @param fallbackSourceName the source to name when the frame's position has none of its own
     * @return the position, never null
     */
    static Position resolve(StackTraceElement frame, String fallbackSourceName) {
        int reported = frame.getLineNumber();
        CompiledPositions positions = forClass(frame.getClassName());
        Position found =
                positions == null ? null : positions.lookup(frame.getMethodName(), reported);
        if (found == null) {
            return new Position(fallbackSourceName, reported, 0);
        }
        return found.getSourcePath() == null
                ? new Position(fallbackSourceName, found.getLine(), found.getColumn())
                : found;
    }

    /**
     * The real source line for a reported line number, which differs from it when the position had
     * to be given a marker. Returns {@code reportedLine} unchanged when it is not in the table.
     */
    public int getLine(String methodName, int reportedLine) {
        Position e = lookup(methodName, reportedLine);
        return e == null ? reportedLine : e.getLine();
    }

    /** The one-based column, or zero when unknown. */
    public int getColumn(String methodName, int reportedLine) {
        Position e = lookup(methodName, reportedLine);
        return e == null ? 0 : e.getColumn();
    }

    /** The original source path, or null to use the class's own source file. */
    public String getSourceName(String methodName, int reportedLine) {
        Position e = lookup(methodName, reportedLine);
        return e == null ? null : e.getSourcePath();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Collects positions during code generation. Not thread safe; one per generated class. */
    public static final class Builder {

        /**
         * Allocation state for one method. A reported number only has to be unique within its
         * method, because that is how lookups are keyed, so every method gets the whole range
         * rather than sharing one with the rest of the class.
         */
        private static final class MethodState {
            final Map<Integer, Position> lines = new HashMap<>();
            final Map<Position, Integer> emittedFor = new HashMap<>();
            int nextMarker = FIRST_POSITION_MARKER;
            int highestRealLine;
        }

        private final Map<String, MethodState> byMethod = new HashMap<>();

        private boolean markersEnabled = true;

        /**
         * Stops handing out position markers.
         *
         * <p>A marker is only meaningful alongside this table, which is attached to the generated
         * class as it is defined. Code compiled ahead of time to a class file on disk is loaded
         * without that step, so it must carry real line numbers and report no column where a line
         * holds more than one position.
         */
        public void disableMarkers() {
            markersEnabled = false;
        }

        /**
         * Records a position and returns the line number to emit for it, which is the real line
         * unless that line already identifies a different position.
         *
         * @param column the one-based column, or zero for a position the compiler invented, which
         *     carries no column of its own
         */
        public int record(String methodName, int line, int column, String sourceName) {
            MethodState state = byMethod.computeIfAbsent(methodName, k -> new MethodState());
            if (line > state.highestRealLine) state.highestRealLine = line;

            Position claimed = state.lines.get(line);
            if (claimed == null) {
                state.lines.put(line, new Position(sourceName, line, Math.max(column, 0)));
                state.emittedFor.put(new Position(sourceName, line, column), line);
                return line;
            }

            boolean sameAsClaimed =
                    claimed.getLine() == line
                            && claimed.getColumn() == Math.max(column, 0)
                            && Objects.equals(claimed.getSourcePath(), sourceName);
            if (sameAsClaimed) {
                return line;
            }

            // A position with no column of its own can neither claim a line nor be worth a
            // marker; it just reuses whatever the line already resolves to.
            if (column <= 0) {
                return line;
            }

            Position key = new Position(sourceName, line, column);
            Integer already = state.emittedFor.get(key);
            if (already != null) {
                return already.intValue();
            }

            if (!markersEnabled || state.nextMarker <= state.highestRealLine) {
                // Either markers are unavailable, or this method has run out of them, which takes
                // tens of thousands of distinct positions in one function. Report the column as
                // unknown rather than emit a number that already means something else.
                state.lines.put(line, new Position(claimed.getSourcePath(), line, 0));
                return line;
            }

            int marker = state.nextMarker--;
            state.lines.put(marker, new Position(sourceName, line, column));
            state.emittedFor.put(key, marker);
            return marker;
        }

        public boolean isEmpty() {
            return byMethod.isEmpty();
        }

        public CompiledPositions build() {
            Map<String, Map<Integer, Position>> out = new HashMap<>();
            for (Map.Entry<String, MethodState> e : byMethod.entrySet()) {
                out.put(e.getKey(), Map.copyOf(e.getValue().lines));
            }
            return new CompiledPositions(out);
        }
    }

    @Override
    public String toString() {
        return "CompiledPositions" + byMethod.keySet();
    }
}
