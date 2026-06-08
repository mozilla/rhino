/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

import java.util.Objects;

/** A 1-indexed line and column position in a source file. */
public final class Position {
    /**
     * the path of the source file (may be {@code null} when the mapper has no source associated
     * with the position)
     */
    private final String sourcePath;

    /* 1-indexed line number */
    private final int line;

    /** 1-indexed column number */
    private final int column;

    public Position(String sourcePath, int line, int column) {
        this.sourcePath = sourcePath;
        this.line = line;
        this.column = column;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Position) obj;
        return Objects.equals(sourcePath, that.sourcePath)
                && this.line == that.line
                && this.column == that.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourcePath, line, column);
    }

    @Override
    public String toString() {
        return "Position["
                + "sourcePath='"
                + sourcePath
                + '\''
                + ", line="
                + line
                + ", column="
                + column
                + ']';
    }
}
