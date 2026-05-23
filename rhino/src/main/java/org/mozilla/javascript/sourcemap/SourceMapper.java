/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

/**
 * Maps positions in a transpiled (target) script back to positions in the original source. Attach
 * an instance via {@link
 * org.mozilla.javascript.ScriptCompileSpec.Builder#sourceMapper(SourceMapper)} (or the equivalent
 * on {@link org.mozilla.javascript.FunctionCompileSpec}) so that line numbers in stack traces, the
 * debugger source handoff, and parser error messages refer to the original source rather than the
 * transpiled output.
 */
public interface SourceMapper {

    /**
     * Maps a target {@code (line, column)} position to the corresponding original-source position.
     *
     * @param targetLine 1-indexed line in the transpiled source
     * @param targetColumn 1-indexed column in the transpiled source
     * @return the original-source position, or {@code null} if no mapping exists for this position
     */
    Position mapPosition(int targetLine, int targetColumn);

    /**
     * Returns the full text of the original source so the debugger can display it during
     * compilation handoff.
     *
     * @return the original source, or {@code null} if it is not available
     */
    String getOriginalSource();

    /**
     * Returns the text of the given line of the original source. Used to populate parser error
     * messages with the offending line from the original source.
     *
     * @param sourceLine 1-indexed line number in the original source
     * @return the line text, or {@code null} if the line is out of range or unavailable
     */
    String getSourceLineText(int sourceLine);
}
