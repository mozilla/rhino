/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import java.util.ArrayList;

/**
 * Represents parsed character class contents for regular expressions.
 *
 * <p>This class stores all the components that can appear in a character class (e.g., {@code
 * [a-z\d\p{Letter}]}), including:
 *
 * <ul>
 *   <li><b>Individual characters</b> - stored in {@code chars}
 *   <li><b>Character ranges</b> - stored in {@code bmpRanges} (BMP) and {@code nonBMPRanges} (non-BMP)
 *   <li><b>Escape sequences</b> - stored as RENode objects in {@code escapeNodes} (\d, \s, \p{},
 *       etc.)
 *   <li><b>ES2024 v-flag features:</b>
 *       <ul>
 *         <li>String literals from \q{...} syntax - stored in {@code stringLiterals}
 *         <li>Set operations (union, intersection, subtraction) - stored in {@code setOperations}
 *         <li>Properties of Strings (emoji sequences) - stored in {@code stringMatchers}
 *       </ul>
 * </ul>
 *
 * <p>The {@code sense} field indicates whether the class is normal (true) or negated (false, from
 * {@code [^...]}).
 *
 * <p><b>BMP vs Non-BMP:</b> Characters are separated by Unicode plane:
 *
 * <ul>
 *   <li><b>BMP</b> (Basic Multilingual Plane, U+0000-U+FFFF) - fits in a single char, stored in
 *       {@code chars} and {@code bmpRanges}
 *   <li><b>Non-BMP</b> (U+10000 and above) - requires surrogate pairs, stored in {@code
 *       nonBMPCodepoints} and {@code nonBMPRanges}
 * </ul>
 *
 * <p>Ranges are stored as flattened lists: [start1, end1, start2, end2, ...] for efficient
 * iteration.
 *
 * <p><b>Performance Note:</b> Fields are package-private for performance-critical access during
 * parsing and compilation within the regexp package. External access should use getter methods.
 *
 * <p>Extracted from NativeRegExp to improve modularity during refactoring.
 *
 * @see SetOperation
 * @see RECharSet
 */
final class ClassContents {
    /** True if normal class, false if negated (from [^...]). Package-private for internal use. */
    boolean sense = true;

    /** Individual BMP characters. Package-private for internal mutation during parsing. */
    final ArrayList<Character> chars = new ArrayList<>();

    /**
     * BMP character ranges stored as (start1, end1, start2, end2, ...). Package-private for
     * internal mutation.
     */
    final ArrayList<Character> bmpRanges = new ArrayList<>();

    /**
     * Escape sequence nodes (\d, \D, \s, \S, \w, \W, \p{}, \P{}). Package-private for internal
     * mutation.
     */
    final ArrayList<RENode> escapeNodes = new ArrayList<>();

    /**
     * Non-BMP character ranges stored as (start1, end1, start2, end2, ...). Package-private for
     * internal mutation.
     */
    final ArrayList<Integer> nonBMPRanges = new ArrayList<>();

    /** Individual non-BMP codepoints (beyond U+FFFF). Package-private for internal mutation. */
    final ArrayList<Integer> nonBMPCodepoints = new ArrayList<>();

    /**
     * ES2024 v-flag set operations (intersection, subtraction). Package-private for internal
     * mutation.
     */
    final ArrayList<SetOperation> setOperations = new ArrayList<>();

    /**
     * ES2024 v-flag string literals from \q{...} syntax. Package-private for internal mutation.
     */
    final ArrayList<String> stringLiterals = new ArrayList<>();

    /**
     * ES2024 Property of Strings (emoji sequences). Package-private for internal mutation.
     */
    final ArrayList<StringMatcher> stringMatchers = new ArrayList<>();

    /**
     * Merge all contents from another ClassContents object into this one.
     *
     * <p>Used for:
     *
     * <ul>
     *   <li>Nested character classes
     *   <li>Set operations (union, intersection, subtraction)
     *   <li>Building composite character classes
     * </ul>
     *
     * <p>The {@code sense} field is not merged - caller must handle negation logic separately.
     *
     * @param other The ClassContents to merge from
     */
    void mergeFrom(ClassContents other) {
        this.chars.addAll(other.chars);
        this.bmpRanges.addAll(other.bmpRanges);
        this.nonBMPCodepoints.addAll(other.nonBMPCodepoints);
        this.nonBMPRanges.addAll(other.nonBMPRanges);
        this.stringLiterals.addAll(other.stringLiterals);
        this.stringMatchers.addAll(other.stringMatchers);
        this.escapeNodes.addAll(other.escapeNodes);
    }
}
