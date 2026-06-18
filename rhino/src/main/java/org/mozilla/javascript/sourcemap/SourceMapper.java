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
     * Returns the text of the given line within the named original source file. Used to populate
     * parser error messages with the offending line from the original source.
     *
     * @param sourcePath the resolved source path (as returned in {@link Position#getSourcePath()})
     * @param lineNumber 1-indexed line number in the original source
     * @return the line text, or {@code null} if the source is unknown, the line is out of range, or
     *     no original-source content is available
     */
    String getSourceLineText(String sourcePath, int lineNumber);

    /**
     * Returns the full text of the primary original source so the debugger can display it during
     * compilation handoff. When the underlying source map references multiple sources, an
     * implementation must pick one (typically the first) — this method does not enumerate them.
     *
     * @return the primary original source content, or {@code null} if it is not available
     */
    String getPrimarySourceContent();
}
