/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import org.mozilla.javascript.Context;

/**
 * Regular expression flag parsing and validation.
 *
 * <p>Handles parsing of RegExp flag strings (e.g., "gim", "vu") into integer bitmasks and
 * validates flag combinations according to ECMAScript specifications.
 *
 * <p><b>Supported Flags:</b>
 *
 * <ul>
 *   <li><b>g</b> - Global: match all occurrences, not just first
 *   <li><b>i</b> - IgnoreCase: case-insensitive matching
 *   <li><b>m</b> - Multiline: ^ and $ match line boundaries
 *   <li><b>s</b> - DotAll: . matches newlines (ES2018)
 *   <li><b>u</b> - Unicode: Unicode mode with strict escapes (ES2015)
 *   <li><b>v</b> - UnicodeSets: ES2024 set operations and string literals
 *   <li><b>y</b> - Sticky: match at exact position (ES2015)
 *   <li><b>d</b> - HasIndices: capture group indices (ES2022)
 * </ul>
 *
 * <p><b>Flag Constraints:</b>
 *
 * <ul>
 *   <li>Each flag can appear at most once
 *   <li>u and v flags are mutually exclusive
 *   <li>u flag requires ES6+
 * </ul>
 *
 * <p>Extracted from NativeRegExp to improve modularity.
 */
class RegExpFlags {

    // Flag bit masks (must match NativeRegExp constants)
    static final int JSREG_GLOB = 0x01; // 'g'
    static final int JSREG_FOLD = 0x02; // 'i'
    static final int JSREG_MULTILINE = 0x04; // 'm'
    static final int JSREG_DOTALL = 0x08; // 's'
    static final int JSREG_STICKY = 0x10; // 'y'
    static final int JSREG_UNICODE = 0x20; // 'u'
    static final int JSREG_HASINDICES = 0x40; // 'd'
    static final int JSREG_UNICODESETS = 0x80; // 'v'

    /**
     * Parse flag string into integer bitmask.
     *
     * <p>Validates:
     *
     * <ul>
     *   <li>All characters are valid flags
     *   <li>No duplicate flags
     *   <li>Flag combinations are legal (u and v mutually exclusive)
     *   <li>Language version supports flags (u requires ES6+)
     * </ul>
     *
     * @param flagString Flag string (e.g., "gim"), can be null
     * @param cx Context for language version checking
     * @return Integer bitmask of flags
     * @throws org.mozilla.javascript.EvaluatorException if flags are invalid
     */
    static int parseFlags(String flagString, Context cx) {
        if (flagString == null || flagString.isEmpty()) {
            return 0;
        }

        int flags = 0;

        for (int i = 0; i < flagString.length(); i++) {
            char c = flagString.charAt(i);
            int flagBit = charToFlag(c);

            if (flagBit == -1) {
                NativeRegExp.reportError("msg.invalid.re.flag", String.valueOf(c));
            }

            // Check for duplicate flags
            if ((flags & flagBit) != 0) {
                NativeRegExp.reportError("msg.invalid.re.flag", String.valueOf(c));
            }

            flags |= flagBit;
        }

        // Validate flag combinations
        validateFlagCombinations(flags, cx);

        return flags;
    }

    /**
     * Convert flag character to bitmask.
     *
     * @param c Flag character
     * @return Flag bitmask, or -1 if invalid character
     */
    private static int charToFlag(char c) {
        switch (c) {
            case 'g':
                return JSREG_GLOB;
            case 'i':
                return JSREG_FOLD;
            case 'm':
                return JSREG_MULTILINE;
            case 's':
                return JSREG_DOTALL;
            case 'y':
                return JSREG_STICKY;
            case 'u':
                return JSREG_UNICODE;
            case 'd':
                return JSREG_HASINDICES;
            case 'v':
                return JSREG_UNICODESETS;
            default:
                return -1;
        }
    }

    /**
     * Validate flag combinations and language version constraints.
     *
     * @param flags Parsed flag bitmask
     * @param cx Context for language version checking
     * @throws org.mozilla.javascript.EvaluatorException if combination is invalid
     */
    private static void validateFlagCombinations(int flags, Context cx) {
        // u and v flags are mutually exclusive (ES2024 spec)
        if ((flags & JSREG_UNICODE) != 0 && (flags & JSREG_UNICODESETS) != 0) {
            NativeRegExp.reportError("msg.invalid.re.flag", "u and v");
        }

        // u flag requires ES6+
        if ((flags & JSREG_UNICODE) != 0 && cx.getLanguageVersion() < Context.VERSION_ES6) {
            NativeRegExp.reportError("msg.invalid.re.flag", "u");
        }

        // Note: u+i and v+i combinations are supported with Unicode case folding
    }

    /**
     * Check if Unicode mode is enabled (u or v flag).
     *
     * @param flags Flag bitmask
     * @return true if either JSREG_UNICODE or JSREG_UNICODESETS is set
     */
    static boolean isUnicodeMode(int flags) {
        return (flags & JSREG_UNICODE) != 0 || (flags & JSREG_UNICODESETS) != 0;
    }

    /**
     * Check if UnicodeSets mode (v flag) is enabled.
     *
     * @param flags Flag bitmask
     * @return true if JSREG_UNICODESETS is set
     */
    static boolean isUnicodeSetsMode(int flags) {
        return (flags & JSREG_UNICODESETS) != 0;
    }

    /**
     * Check if case-insensitive mode (i flag) is enabled.
     *
     * @param flags Flag bitmask
     * @return true if JSREG_FOLD is set
     */
    static boolean isCaseInsensitive(int flags) {
        return (flags & JSREG_FOLD) != 0;
    }

    /**
     * Check if global mode (g flag) is enabled.
     *
     * @param flags Flag bitmask
     * @return true if JSREG_GLOB is set
     */
    static boolean isGlobal(int flags) {
        return (flags & JSREG_GLOB) != 0;
    }

    /**
     * Check if multiline mode (m flag) is enabled.
     *
     * @param flags Flag bitmask
     * @return true if JSREG_MULTILINE is set
     */
    static boolean isMultiline(int flags) {
        return (flags & JSREG_MULTILINE) != 0;
    }

    /**
     * Check if dotAll mode (s flag) is enabled.
     *
     * @param flags Flag bitmask
     * @return true if JSREG_DOTALL is set
     */
    static boolean isDotAll(int flags) {
        return (flags & JSREG_DOTALL) != 0;
    }

    /**
     * Check if sticky mode (y flag) is enabled.
     *
     * @param flags Flag bitmask
     * @return true if JSREG_STICKY is set
     */
    static boolean isSticky(int flags) {
        return (flags & JSREG_STICKY) != 0;
    }

    /**
     * Check if hasIndices mode (d flag) is enabled.
     *
     * @param flags Flag bitmask
     * @return true if JSREG_HASINDICES is set
     */
    static boolean hasIndices(int flags) {
        return (flags & JSREG_HASINDICES) != 0;
    }
}
