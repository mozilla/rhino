/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import java.io.Serializable;
import java.util.stream.IntStream;

/**
 * Universal string matcher for regular expressions.
 *
 * <p>Unified implementation for all string matching: single char, multi-char, emoji sequences.
 * Supports ES2024 Property of Strings for emoji matching.
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
     * Check if literal matches at the specified position in the input.
     *
     * @param input Input to match against
     * @param startPos Starting position in input
     * @return true if literal matches at startPos
     */
    private boolean matchesAt(CharSequence input, int startPos) {
        for (int i = 0; i < literal.length(); i++) {
            char inputChar = input.charAt(startPos + i);
            char literalChar = literal.charAt(i);

            if (caseInsensitive) {
                if (inputChar != literalChar
                        && Character.toUpperCase(inputChar) != Character.toUpperCase(literalChar)) {
                    return false;
                }
            } else {
                if (inputChar != literalChar) {
                    return false;
                }
            }
        }
        return true;
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

        return matchesAt(input, position) ? literal.length() : -1;
    }

    /**
     * Match backwards from the specified position (for lookbehind assertions).
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

        return matchesAt(input, startPos) ? literal.length() : -1;
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
        IntStream.range(0, literal.length())
                .map(literal::charAt)
                .forEach(
                        c -> {
                            if (c >= 32 && c < 127) {
                                sb.append((char) c);
                            } else {
                                sb.append(String.format("\\u%04X", c));
                            }
                        });

        if (caseInsensitive) {
            sb.append(", case-insensitive");
        }

        sb.append("]");
        return sb.toString();
    }

    // ==================================================================================
    // UNICODE CASE FOLDING FOR REGULAR EXPRESSIONS
    // ==================================================================================

    /**
     * Get all case variants of a character for Unicode mode. Implements "deep case closure" -
     * bidirectional case equivalence using Java's built-in Unicode case mapping (CLDR data).
     *
     * <p>Java's Character.toLowerCase(int) and Character.toUpperCase(int) implement full Unicode
     * case folding including special mappings like:
     *
     * <ul>
     *   <li>U+212A (KELVIN SIGN) → U+006B ('k')
     *   <li>U+2126 (OHM SIGN) → U+03C9 ('ω')
     *   <li>U+212B (ANGSTROM SIGN) → U+00E5 ('å')
     * </ul>
     *
     * <p>By using codepoint-based APIs (int parameter), this method supports both BMP characters
     * (U+0000-U+FFFF) and supplementary characters (U+10000-U+10FFFF).
     *
     * @param codePoint The Unicode codepoint to get case variants for
     * @param results List to add case variants to (will include codePoint itself)
     */
    public static void getUnicodeCaseVariants(int codePoint, java.util.List<Integer> results) {
        // Use a Set for O(1) lookups instead of O(n) List.contains()
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        seen.add(codePoint);
        results.add(codePoint);

        // Get case variants using Java's built-in Unicode case mapping
        int lower = Character.toLowerCase(codePoint);
        int upper = Character.toUpperCase(codePoint);

        if (lower != codePoint && seen.add(lower)) {
            results.add(lower);
            // Add uppercase of the lowercase (handles bidirectional mappings)
            int upperOfLower = Character.toUpperCase(lower);
            if (upperOfLower != codePoint && upperOfLower != upper && seen.add(upperOfLower)) {
                results.add(upperOfLower);
            }
        }

        if (upper != codePoint && seen.add(upper)) {
            results.add(upper);
            // Add lowercase of the uppercase (completes bidirectional closure)
            int lowerOfUpper = Character.toLowerCase(upper);
            if (lowerOfUpper != codePoint && lowerOfUpper != lower && seen.add(lowerOfUpper)) {
                results.add(lowerOfUpper);
            }
        }
    }

    /**
     * Get all case variants of a BMP character for Unicode mode. Convenience overload for char
     * input - delegates to codepoint version.
     *
     * @param ch The character to get case variants for
     * @param results List to add case variants to (will include ch itself)
     */
    public static void getUnicodeCaseVariants(char ch, java.util.List<Character> results) {
        java.util.List<Integer> codePointResults = new java.util.ArrayList<>();
        getUnicodeCaseVariants((int) ch, codePointResults);
        for (int cp : codePointResults) {
            results.add((char) cp); // Safe cast - input was char, so all results are BMP
        }
    }
}
