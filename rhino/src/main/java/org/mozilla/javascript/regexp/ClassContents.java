/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Parsed character class contents for regular expressions.
 *
 * <p>Stores components of a character class: individual codepoints, ranges, escape sequences (\d,
 * \p{}, etc.), and ES2024 v-flag features (string literals, set operations, emoji sequences).
 *
 * <p>Ranges stored as flattened pairs: [start1, end1, start2, end2, ...]. All characters stored as
 * Unicode codepoints (0 to 0x10FFFF).
 *
 * @see SetOperation
 * @see RECharSet
 */
final class ClassContents {
    /** True if normal class, false if negated (from [^...]). Package-private for internal use. */
    boolean sense = true;

    /**
     * Individual code points (all Unicode code points, both BMP and non-BMP). Package-private for
     * internal mutation during parsing.
     */
    final ArrayList<Integer> codePoints = new ArrayList<>();

    /**
     * Character ranges stored as (start1, end1, start2, end2, ...) for all Unicode codepoints.
     * Package-private for internal mutation.
     */
    final ArrayList<Integer> ranges = new ArrayList<>();

    /**
     * Escape sequence nodes (\d, \D, \s, \S, \w, \W, \p{}, \P{}). Package-private for internal
     * mutation.
     */
    final ArrayList<RENode> escapeNodes = new ArrayList<>();

    /**
     * ES2024 v-flag set operations (intersection, subtraction). Package-private for internal
     * mutation.
     */
    final ArrayList<SetOperation> setOperations = new ArrayList<>();

    /** ES2024 v-flag string literals from \q{...} syntax. Package-private for internal mutation. */
    final ArrayList<String> stringLiterals = new ArrayList<>();

    /** ES2024 Property of Strings (emoji sequences). Package-private for internal mutation. */
    final ArrayList<StringMatcher> stringMatchers = new ArrayList<>();

    /**
     * Merge all contents from another ClassContents object into this one. Does not merge the sense
     * field.
     *
     * @param other The ClassContents to merge from
     */
    void mergeFrom(ClassContents other) {
        this.codePoints.addAll(other.codePoints);
        this.ranges.addAll(other.ranges);
        this.stringLiterals.addAll(other.stringLiterals);
        this.stringMatchers.addAll(other.stringMatchers);
        this.escapeNodes.addAll(other.escapeNodes);
    }

    /**
     * Iterate over all character ranges.
     *
     * @param consumer The BiConsumer to apply to each (start, end) pair
     */
    void forEachRange(BiConsumer<Integer, Integer> consumer) {
        for (int i = 0; i < ranges.size(); i += 2) {
            consumer.accept(ranges.get(i), ranges.get(i + 1));
        }
    }

    /**
     * Test if any range matches the given predicate. Short-circuits on first match.
     *
     * @param predicate The BiPredicate to test each (start, end) pair
     * @return true if any range matches the predicate
     */
    boolean anyRangeMatches(BiPredicate<Integer, Integer> predicate) {
        for (int i = 0; i < ranges.size(); i += 2) {
            if (predicate.test(ranges.get(i), ranges.get(i + 1))) {
                return true;
            }
        }
        return false;
    }
}
