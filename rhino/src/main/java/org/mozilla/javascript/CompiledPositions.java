/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
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

    // The largest number a LineNumberTable entry holds (JVMS 4.7.12). Markers are handed out
    // downwards from it, per method, since a lookup is keyed by method too.
    private static final int LAST_LINE = 0xFFFF;
    private static final int FIRST_POSITION_MARKER = LAST_LINE;

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
        dropUnloaded();
        BY_CLASS_NAME.put(className, new Ref(className, positions));
    }

    /** The table for a generated class, or null if it has none. */
    static CompiledPositions forClass(String className) {
        dropUnloaded();
        WeakReference<CompiledPositions> ref = BY_CLASS_NAME.get(className);
        return ref == null ? null : ref.get();
    }

    // Polled on lookups as well as registrations, so a process that has stopped compiling does
    // not keep the names of every class it unloaded
    private static void dropUnloaded() {
        for (Reference<? extends CompiledPositions> stale = STALE.poll();
                stale != null;
                stale = STALE.poll()) {
            BY_CLASS_NAME.remove(((Ref) stale).className, stale);
        }
    }

    private final Map<String, PositionTable> byMethod;

    private CompiledPositions(Map<String, PositionTable> byMethod) {
        this.byMethod = byMethod;
    }

    /** The position behind a reported line number, or null if the table has none for it. */
    Position get(String methodName, int reportedLine) {
        PositionTable positions = byMethod.get(methodName);
        return positions == null ? null : positions.get(reportedLine);
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
        Position found = positions == null ? null : positions.get(frame.getMethodName(), reported);
        if (found == null) found = new Position(null, reported, 0);
        return found.withSourcePathIfAbsent(fallbackSourceName);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Collects positions during code generation. Not thread safe; one per generated class. */
    public static final class Builder {

        // A reported number only has to be unique within its method, since lookups are keyed by
        // method, so every method gets the whole range rather than sharing one.
        private static final class MethodState {
            final TreeMap<Integer, Position> lines = new TreeMap<>();
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
         * @param at the position; a column of zero marks one the compiler invented
         */
        public int record(String methodName, Position at) {
            MethodState state = byMethod.computeIfAbsent(methodName, k -> new MethodState());
            int line = at.getLine();
            int column = at.getColumn();

            // A line is emitted as itself while the field can hold it and no marker has taken it
            boolean fits = line <= LAST_LINE && line <= state.nextMarker;
            Position claimed = fits ? state.lines.get(line) : null;
            if (fits) {
                if (line > state.highestRealLine) state.highestRealLine = line;
                if (claimed == null) {
                    state.lines.put(
                            line, column > 0 ? at : new Position(at.getSourcePath(), line, 0));
                    if (column > 0) state.emittedFor.put(at, line);
                    return line;
                }
                // A position with no column of its own can neither claim a line nor be worth a
                // marker; it just reuses whatever the line already resolves to.
                if (column <= 0) return line;
            }

            Integer already = state.emittedFor.get(at);
            if (already != null) return already.intValue();

            if (!markersEnabled || state.nextMarker <= state.highestRealLine) {
                // Either markers are unavailable, or this method has run out of them, which takes
                // tens of thousands of distinct positions in one function. Report the column as
                // unknown rather than emit a number that already means something else, and a
                // line the field cannot hold as no line at all.
                if (!fits) return 0;
                state.lines.put(line, new Position(claimed.getSourcePath(), line, 0));
                return line;
            }

            int marker = state.nextMarker--;
            state.lines.put(marker, at);
            state.emittedFor.put(at, marker);
            return marker;
        }

        public CompiledPositions build() {
            Map<String, PositionTable> out = new HashMap<>();
            for (Map.Entry<String, MethodState> e : byMethod.entrySet()) {
                PositionTable.Builder table = new PositionTable.Builder();
                for (Map.Entry<Integer, Position> line : e.getValue().lines.entrySet()) {
                    Position at = line.getValue();
                    table.add(
                            line.getKey(), at.getLine(), at.getColumn(), at.getSourcePath(), true);
                }
                out.put(e.getKey(), table.build());
            }
            return new CompiledPositions(out);
        }
    }
}
