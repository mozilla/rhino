/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

/** A decoded source-map segment. The optional fields use {@code -1} as the "absent" sentinel. */
final class Segment {
    static final int ABSENT = -1;

    private final int genCol;
    private final int sourceIndex;
    private final int srcLine;
    private final int srcCol;
    private final int nameIndex;

    /**
     * @param genCol 0-indexed generated column
     * @param sourceIndex index into the {@code sources} array, or {@code -1} for a 1-field segment
     * @param srcLine 0-indexed source line, or {@code -1} for a 1-field segment
     * @param srcCol 0-indexed source column, or {@code -1} for a 1-field segment
     * @param nameIndex index into the {@code names} array, or {@code -1} for a 1- or 4-field
     *     segment
     */
    Segment(int genCol, int sourceIndex, int srcLine, int srcCol, int nameIndex) {
        this.genCol = genCol;
        this.sourceIndex = sourceIndex;
        this.srcLine = srcLine;
        this.srcCol = srcCol;
        this.nameIndex = nameIndex;
    }

    boolean hasSource() {
        return sourceIndex != ABSENT;
    }

    public int getGenCol() {
        return genCol;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public int getSrcLine() {
        return srcLine;
    }

    public int getSrcCol() {
        return srcCol;
    }

    public int getNameIndex() {
        return nameIndex;
    }
}
