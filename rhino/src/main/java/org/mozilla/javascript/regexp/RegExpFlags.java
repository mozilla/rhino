/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import org.mozilla.javascript.Context;

/**
 * Immutable value object representing regular expression flags (g, i, m, s, u, v, y, d).
 *
 * <p>Handles flag parsing, validation, and querying. Flags u and v are mutually exclusive.
 *
 * <p>See ECMA 262 §22.2.7.
 */
final class RegExpFlags {

    // Flag bit masks (package-private for NativeRegExp compatibility)
    static final int JSREG_GLOB = 0x01; // 'g'
    static final int JSREG_FOLD = 0x02; // 'i'
    static final int JSREG_MULTILINE = 0x04; // 'm'
    static final int JSREG_DOTALL = 0x08; // 's'
    static final int JSREG_STICKY = 0x10; // 'y'
    static final int JSREG_UNICODE = 0x20; // 'u'
    static final int JSREG_HASINDICES = 0x40; // 'd'
    static final int JSREG_UNICODESETS = 0x80; // 'v'

    private final int bitmask;

    /**
     * Private constructor. Use {@link #parse} or {@link #fromBitmask} to create instances.
     *
     * @param bitmask the flag bitmask
     */
    private RegExpFlags(int bitmask) {
        this.bitmask = bitmask;
    }

    /**
     * Parse flag string into RegExpFlags instance. Validates flag combinations and version
     * requirements.
     *
     * @param flagString Flag string (e.g., "gim"), can be null
     * @param cx Context for language version checking
     * @return RegExpFlags instance
     * @throws org.mozilla.javascript.EvaluatorException if flags are invalid
     */
    static RegExpFlags parse(String flagString, Context cx) {
        if (flagString == null || flagString.isEmpty()) {
            return new RegExpFlags(0);
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

        return new RegExpFlags(flags);
    }

    /**
     * Create RegExpFlags from existing bitmask. For compatibility with legacy code.
     *
     * @param bitmask the flag bitmask
     * @return RegExpFlags instance
     */
    static RegExpFlags fromBitmask(int bitmask) {
        return new RegExpFlags(bitmask);
    }

    /**
     * Get the flag bitmask. For compatibility with legacy code.
     *
     * @return the flag bitmask
     */
    int getBitmask() {
        return bitmask;
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
     * @return true if either JSREG_UNICODE or JSREG_UNICODESETS is set
     */
    boolean isUnicodeMode() {
        return (bitmask & JSREG_UNICODE) != 0 || (bitmask & JSREG_UNICODESETS) != 0;
    }

    /**
     * Check if UnicodeSets mode (v flag) is enabled.
     *
     * @return true if JSREG_UNICODESETS is set
     */
    boolean isUnicodeSetsMode() {
        return (bitmask & JSREG_UNICODESETS) != 0;
    }

    /**
     * Check if case-insensitive mode (i flag) is enabled.
     *
     * @return true if JSREG_FOLD is set
     */
    boolean isCaseInsensitive() {
        return (bitmask & JSREG_FOLD) != 0;
    }

    /**
     * Check if global mode (g flag) is enabled.
     *
     * @return true if JSREG_GLOB is set
     */
    boolean isGlobal() {
        return (bitmask & JSREG_GLOB) != 0;
    }

    /**
     * Check if multiline mode (m flag) is enabled.
     *
     * @return true if JSREG_MULTILINE is set
     */
    boolean isMultiline() {
        return (bitmask & JSREG_MULTILINE) != 0;
    }

    /**
     * Check if dotAll mode (s flag) is enabled.
     *
     * @return true if JSREG_DOTALL is set
     */
    boolean isDotAll() {
        return (bitmask & JSREG_DOTALL) != 0;
    }

    /**
     * Check if sticky mode (y flag) is enabled.
     *
     * @return true if JSREG_STICKY is set
     */
    boolean isSticky() {
        return (bitmask & JSREG_STICKY) != 0;
    }

    /**
     * Check if hasIndices mode (d flag) is enabled.
     *
     * @return true if JSREG_HASINDICES is set
     */
    boolean hasIndices() {
        return (bitmask & JSREG_HASINDICES) != 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RegExpFlags)) return false;
        RegExpFlags other = (RegExpFlags) obj;
        return this.bitmask == other.bitmask;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(bitmask);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isGlobal()) sb.append('g');
        if (isCaseInsensitive()) sb.append('i');
        if (isMultiline()) sb.append('m');
        if (isDotAll()) sb.append('s');
        if (isSticky()) sb.append('y');
        if ((bitmask & JSREG_UNICODE) != 0) sb.append('u');
        if (hasIndices()) sb.append('d');
        if (isUnicodeSetsMode()) sb.append('v');
        return sb.toString();
    }

    // Static helper methods for backward compatibility with code that uses int flags
    // TODO: Gradually migrate all callers to use RegExpFlags instances

    /**
     * @deprecated Use {@link #parse} and {@link #isUnicodeMode()} instead
     */
    @Deprecated
    static boolean isUnicodeMode(int flags) {
        return fromBitmask(flags).isUnicodeMode();
    }

    /**
     * @deprecated Use {@link #parse} and {@link #isUnicodeSetsMode()} instead
     */
    @Deprecated
    static boolean isUnicodeSetsMode(int flags) {
        return fromBitmask(flags).isUnicodeSetsMode();
    }

    /**
     * @deprecated Use {@link #parse} and {@link #isCaseInsensitive()} instead
     */
    @Deprecated
    static boolean isCaseInsensitive(int flags) {
        return fromBitmask(flags).isCaseInsensitive();
    }

    /**
     * @deprecated Use {@link #parse} and {@link #isGlobal()} instead
     */
    @Deprecated
    static boolean isGlobal(int flags) {
        return fromBitmask(flags).isGlobal();
    }

    /**
     * @deprecated Use {@link #parse} and {@link #isMultiline()} instead
     */
    @Deprecated
    static boolean isMultiline(int flags) {
        return fromBitmask(flags).isMultiline();
    }

    /**
     * @deprecated Use {@link #parse} and {@link #isDotAll()} instead
     */
    @Deprecated
    static boolean isDotAll(int flags) {
        return fromBitmask(flags).isDotAll();
    }

    /**
     * @deprecated Use {@link #parse} and {@link #isSticky()} instead
     */
    @Deprecated
    static boolean isSticky(int flags) {
        return fromBitmask(flags).isSticky();
    }

    /**
     * @deprecated Use {@link #parse} and {@link #hasIndices()} instead
     */
    @Deprecated
    static boolean hasIndices(int flags) {
        return fromBitmask(flags).hasIndices();
    }

    /**
     * @deprecated Use {@link #parse} instead
     */
    @Deprecated
    static int parseFlags(String flagString, Context cx) {
        return parse(flagString, cx).getBitmask();
    }
}
