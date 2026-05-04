/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

/**
 * A decoded source-map segment. The optional fields use {@code -1} as the "absent" sentinel.
 *
 * @param genCol 0-indexed generated column
 * @param sourceIndex index into the {@code sources} array, or {@code -1} for a 1-field segment
 * @param srcLine 0-indexed source line, or {@code -1} for a 1-field segment
 * @param srcCol 0-indexed source column, or {@code -1} for a 1-field segment
 * @param nameIndex index into the {@code names} array, or {@code -1} for a 1- or 4-field segment
 */
record Segment(int genCol, int sourceIndex, int srcLine, int srcCol, int nameIndex) {

    static final int ABSENT = -1;

    boolean hasSource() {
        return sourceIndex != ABSENT;
    }
}
