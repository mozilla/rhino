/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

/**
 * Parameters controlling regular expression parsing behavior.
 *
 * <p>This class encapsulates the various modes and flags that affect how a regular expression
 * pattern is parsed. These parameters are determined by the regexp flags (u, v) and language
 * version, and control parsing rules for:
 *
 * <ul>
 *   <li>Named capture groups ((?<name>...))
 *   <li>Unicode mode (u flag - strict escapes, surrogate pairs, etc.)
 *   <li>Unicode Sets mode (v flag - ES2024 set operations, string literals, etc.)
 * </ul>
 *
 * <p>Extracted from NativeRegExp to improve modularity during refactoring while maintaining API
 * compatibility.
 */
class ParserParameters {
    /** Whether named capture groups ((?<name>...)) are supported. */
    boolean namedCaptureGroups;

    /** Whether Unicode mode (u flag) is enabled - affects escape sequences and matching. */
    boolean unicodeMode;

    /**
     * Whether Unicode Sets mode (v flag, ES2024) is enabled. This enables:
     *
     * <ul>
     *   <li>Set operations in character classes (union, intersection, subtraction)
     *   <li>String literals via \q{...} syntax
     *   <li>Properties of Strings (emoji sequences)
     *   <li>Different escape rules in character classes
     * </ul>
     */
    boolean vMode;

    /**
     * Creates parser parameters with specified parsing modes.
     *
     * @param namedCaptureGroups Whether named capture groups are enabled
     * @param unicodeMode Whether Unicode mode (u flag) is enabled
     * @param vMode Whether Unicode Sets mode (v flag) is enabled
     */
    ParserParameters(boolean namedCaptureGroups, boolean unicodeMode, boolean vMode) {
        this.namedCaptureGroups = namedCaptureGroups;
        this.unicodeMode = unicodeMode;
        this.vMode = vMode;
    }
}
