/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import java.io.Serializable;

/**
 * Universal string matcher for regular expressions.
 *
 * <p>Revolutionary unification: ALL string matching (single char, multi-char, emoji) uses ONE
 * implementation. Replaces 8 duplicate opcodes with a single REOP_STRING_MATCHER opcode.
 *
 * <h3>Replaces These Opcodes (Now Deprecated):</h3>
 *
 * <ul>
 *   <li>REOP_FLAT1 - Single ASCII character
 *   <li>REOP_FLAT1i - Single ASCII case-insensitive
 *   <li>REOP_UCFLAT1 - Single Unicode character
 *   <li>REOP_UCFLAT1i - Single Unicode case-insensitive
 *   <li>REOP_UCSPFLAT1 - Unicode surrogate pair
 *   <li>REOP_FLAT - Multi-character string
 *   <li>REOP_FLATi - Multi-character case-insensitive
 *   <li>(Plus ES2024 Property of Strings emoji sequences)
 * </ul>
 *
 * <h3>Why Unification Works:</h3>
 *
 * <p>Fundamental insight: 'a' === "a" === "abc" === "#️⃣" - all are just "match this string at
 * current position". No need for separate ASCII/Unicode/BMP/emoji code paths.
 *
 * <h3>ES2024 Property of Strings:</h3>
 *
 * <p>Enables matching multi-character emoji sequences: keycaps (#️⃣), ZWJ sequences (👨‍💻), flags
 * (🇺🇸), skin tones, etc.
 *
 * @see EmojiSequenceData
 * @see NativeRegExp#REOP_STRING_MATCHER
 */
public final class StringMatcher implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String literal;
    private final boolean caseInsensitive;

    /**
     * Create a string matcher.
     *
     * @param literal The literal string to match (any encoding)
     * @param caseInsensitive Whether to perform case-insensitive matching
     */
    public StringMatcher(String literal, boolean caseInsensitive) {
        if (literal == null) {
            throw new IllegalArgumentException("literal cannot be null");
        }
        this.literal = literal;
        this.caseInsensitive = caseInsensitive;
    }

    /**
     * Match this string at the given position in input.
     *
     * @param input Input character sequence to match against
     * @param position Starting position in input
     * @param backward True for lookbehind (match backwards from position), false for normal forward
     *     matching
     * @return Number of characters consumed if matched, -1 if no match
     */
    public int match(CharSequence input, int position, boolean backward) {
        if (backward) {
            return matchBackward(input, position);
        } else {
            return matchForward(input, position);
        }
    }

    /**
     * Match forwards from the specified position.
     *
     * @param input Input to match against
     * @param position Starting position
     * @return Number of characters consumed if matched, -1 if no match
     */
    private int matchForward(CharSequence input, int position) {
        // Bounds check
        if (position < 0 || position + literal.length() > input.length()) {
            return -1;
        }

        // Character-by-character comparison
        for (int i = 0; i < literal.length(); i++) {
            char inputChar = input.charAt(position + i);
            char literalChar = literal.charAt(i);

            if (caseInsensitive) {
                if (!charsEqualIgnoreCase(inputChar, literalChar)) {
                    return -1;
                }
            } else {
                if (inputChar != literalChar) {
                    return -1;
                }
            }
        }

        return literal.length();
    }

    /**
     * Match backwards from the specified position.
     *
     * <p>Used for lookbehind assertions. The position parameter is the END of the match
     * (exclusive), and we match backwards to find the start.
     *
     * @param input Input to match against
     * @param position End position (exclusive) to match backwards from
     * @return Number of characters consumed if matched, -1 if no match
     */
    private int matchBackward(CharSequence input, int position) {
        int startPos = position - literal.length();

        // Bounds check
        if (startPos < 0 || position > input.length()) {
            return -1;
        }

        // Character-by-character comparison (same logic as forward)
        for (int i = 0; i < literal.length(); i++) {
            char inputChar = input.charAt(startPos + i);
            char literalChar = literal.charAt(i);

            if (caseInsensitive) {
                if (!charsEqualIgnoreCase(inputChar, literalChar)) {
                    return -1;
                }
            } else {
                if (inputChar != literalChar) {
                    return -1;
                }
            }
        }

        return literal.length();
    }

    /**
     * Case-insensitive character comparison.
     *
     * <p>Compares two characters ignoring case. Uses both toLowerCase and toUpperCase to handle all
     * Unicode case mappings correctly.
     *
     * @param c1 First character
     * @param c2 Second character
     * @return True if characters are equal ignoring case
     */
    private static boolean charsEqualIgnoreCase(char c1, char c2) {
        // Fast path: exact match
        if (c1 == c2) {
            return true;
        }

        // Slow path: case folding
        // Use both toLowerCase and toUpperCase for proper Unicode handling
        return Character.toLowerCase(c1) == Character.toLowerCase(c2)
                || Character.toUpperCase(c1) == Character.toUpperCase(c2);
    }

    /**
     * Get the literal string being matched.
     *
     * @return The literal string
     */
    public String getLiteral() {
        return literal;
    }

    /**
     * Check if this matcher is case-insensitive.
     *
     * @return True if case-insensitive
     */
    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("StringMatcher[");

        // Show literal with escape sequences for non-printable chars
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (c >= 32 && c < 127) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04X", (int) c));
            }
        }

        if (caseInsensitive) {
            sb.append(", case-insensitive");
        }

        sb.append("]");
        return sb.toString();
    }
}
