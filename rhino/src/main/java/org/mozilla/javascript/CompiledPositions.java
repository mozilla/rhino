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
 * Columns and original source paths for one generated class, looked up by the line number a stack
 * frame reports.
 *
 * <p>The class name, method name and source file are fixed for a whole frame, so a LineNumberTable
 * entry's {@code line_number} is the only thing that varies from position to position, and its
 * sixteen bits (JVMS 4.7.12) are the only channel a column can travel through. A position whose
 * line is still free emits its real line, which is what a Java debugger expects; a second position
 * on that line emits a marker from the top of the range instead, which this maps back. So a line is
 * only ever displaced where it could not have identified the position anyway.
 */
public final class CompiledPositions {

    /** Name of the static field holding the table on a generated class. */
    public static final String FIELD_NAME = "_positions";

    // Markers are handed out downwards, per method, since a lookup is keyed by method too
    private static final int FIRST_POSITION_MARKER = 0xFFFF;

    // A frame gives us only the class name, so this cannot be keyed on the class itself. The
    // reference is weak and the strong one lives in the generated class, so a table dies with the
    // code it describes; every generated class has a fresh name, so a strong map would only grow.
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
     * The position a compiled frame refers to, never null. The number it reports is the real line
     * unless it is a marker, so this is the only place that has to know the difference.
     *
     * @param fallbackSourceName named when the frame's position has no source of its own
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

    /** The real line behind a reported one, or {@code reportedLine} if it is not in the table. */
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

        // A reported number only has to be unique within its method, since lookups are keyed by
        // method, so every method gets the whole range rather than sharing one.
        private static final class MethodState {
            final Map<Integer, Position> lines = new HashMap<>();
            final Map<Position, Integer> emittedFor = new HashMap<>();
            int nextMarker = FIRST_POSITION_MARKER;
            int highestRealLine;
        }

        private final Map<String, MethodState> byMethod = new HashMap<>();

        private boolean markersEnabled = true;

        /**
         * Stops handing out markers. A marker only means anything alongside this table, which is
         * attached as the class is defined; code compiled ahead of time to a file is loaded without
         * that step, so it has to carry real line numbers and give up the column where a line holds
         * more than one position.
         */
        public void disableMarkers() {
            markersEnabled = false;
        }

        /**
         * Records a position and returns the line number to emit for it: the real line, unless that
         * line already identifies a different one.
         *
         * @param column the one-based column, or zero for a position the compiler invented
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
