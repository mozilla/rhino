/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import java.util.List;
import java.util.Map;

/**
 * Character class compilation for regular expressions.
 *
 * <p>Handles parsing and compilation of character classes including ES2024 v-flag features: set
 * operations (&&, --), string literals (\q{}), nested classes, and Property of Strings.
 *
 * <p>See ECMA 262 §22.2.1.
 *
 * @see ClassContents
 * @see SetOperation
 */
class CharacterClassCompiler {

    // Character class parsing limits
    static final int MAX_CLASS_NESTING_DEPTH = 50;

    // Error message keys
    private static final String MSG_INVALID_NESTED_CLASS = "msg.regexp.invalid.nested.class";
    private static final String MSG_SET_OP_MISSING_OPERAND = "msg.regexp.set.op.missing.operand";
    private static final String MSG_INVALID_SET_OP_OPERAND = "msg.regexp.invalid.set.op.operand";

    /** Lambda-based registry for Property of Strings sequence loaders (ES2024) */
    private static final Map<String, java.util.function.Supplier<List<String>>>
            PROPERTY_OF_STRINGS_REGISTRY =
                    Map.of(
                            "basicemoji", EmojiSequenceData::getBasicEmoji,
                            "emojikeycapsequence", EmojiSequenceData::getKeycapSequences,
                            "rgiemojimodifiersequence", EmojiSequenceData::getModifierSequences,
                            "rgiemojiflagsequence", EmojiSequenceData::getFlagSequences,
                            "rgiemojitagsequence", EmojiSequenceData::getTagSequences,
                            "rgiemojizwjsequence", EmojiSequenceData::getZWJSequences,
                            "rgiemoji", EmojiSequenceData::getRGIEmoji);

    /**
     * Calculate bitmap size required for character class.
     *
     * @param flags Regexp flags
     * @param classContents Parsed character class contents
     * @param target RENode to store bitmap size
     * @return true on success
     */
    static boolean calculateBitmapSize(int flags, ClassContents classContents, RENode target) {
        int max = 0;

        for (int ch : classContents.codepoints) {
            if (ch > max) {
                max = ch;
            }
            if ((flags & NativeRegExp.JSREG_FOLD) != 0 && ch <= Character.MAX_VALUE) {
                char cu = Character.toUpperCase((char) ch);
                char cd = Character.toLowerCase((char) ch);
                int n = (cu >= cd) ? cu : cd;
                if (n > max) {
                    max = n;
                }
            }
        }

        for (int i = 1; i < classContents.ranges.size(); i += 2) {
            int rangeEnd = classContents.ranges.get(i);
            if (rangeEnd > max) {
                max = rangeEnd;
            }
            if ((flags & NativeRegExp.JSREG_FOLD) != 0 && rangeEnd <= Character.MAX_VALUE) {
                char cu = Character.toUpperCase((char) rangeEnd);
                char cd = Character.toLowerCase((char) rangeEnd);
                int n = (cu >= cd) ? cu : cd;
                if (n > max) {
                    max = n;
                }
            }
        }

        for (RENode node : classContents.escapeNodes) {
            if (node.op != NativeRegExp.REOP_FLAT) {
                target.bmsize = Character.MAX_VALUE + 1;
                break;
            }
        }

        target.bmsize = Math.max(target.bmsize, max + 1);
        return true;
    }

    /**
     * Parse character class escape as set operation operand (\d, \D, \w, \W, \s, \S).
     *
     * @param state Compiler state
     * @return true if successfully parsed, false otherwise
     */
    static boolean parseCharacterClassEscapeOperand(CompilerState state) {
        if (state.cp >= state.cpend) {
            return false;
        }

        char escapeChar = state.cpbegin[state.cp];
        byte opcode = NativeRegExp.getCharacterClassOpcode(escapeChar);

        if (opcode == -1) {
            return false;
        }

        state.result = new RENode(opcode);
        state.cp++; // Move past the escape character
        return true;
    }

    /**
     * Expect a specific character at the current position and advance past it.
     *
     * @param state Compiler state
     * @param expected The expected character
     * @param errorMsg Error message if character not found
     * @return true if character found and consumed, false otherwise
     */
    private static boolean expectChar(CompilerState state, char expected, String errorMsg) {
        if (state.cp >= state.cpend || state.cpbegin[state.cp] != expected) {
            NativeRegExp.reportError("msg.bad.regexp", errorMsg);
            return false;
        }
        state.cp++;
        return true;
    }

    /**
     * Parse \q{...} string literals in v-mode.
     *
     * @param state Compiler state
     * @param params Parser parameters
     * @param target ClassContents to add string literals to
     * @return true if successful, false on error
     */
    static boolean parseStringLiterals(
            CompilerState state, ParserParameters params, ClassContents target) {
        char[] src = state.cpbegin;

        // Expect \q{ sequence
        if (!expectChar(state, '\\', "Expected \\ before q")) return false;
        if (!expectChar(state, 'q', "Expected q after \\")) return false;
        if (!expectChar(state, '{', "\\q must be followed by {")) return false;

        // Parse string literal(s) within braces
        StringBuilder literal = new StringBuilder();
        while (state.cp < state.cpend && src[state.cp] != '}') {
            if (src[state.cp] == '|') {
                // Multiple alternatives: \q{abc|def}
                target.stringLiterals.add(literal.toString());
                literal = new StringBuilder();
                state.cp++;
            } else if (src[state.cp] == '\\') {
                // Handle escape sequences within \q{}
                state.cp++;
                if (state.cp >= state.cpend) {
                    NativeRegExp.reportError("msg.bad.regexp", "Incomplete escape in \\q{}");
                    return false;
                }
                char escapeChar = src[state.cp];
                if (escapeChar == 'u' || escapeChar == 'x') {
                    // Parse unicode/hex escape
                    if (!NativeRegExp.parseCharacterAndCharacterClassEscape(state, params)) {
                        NativeRegExp.reportError("msg.invalid.escape", "");
                        return false;
                    }
                    if (state.result.op != NativeRegExp.REOP_FLAT) {
                        NativeRegExp.reportError("msg.bad.regexp", "Invalid escape in \\q{}");
                        return false;
                    }
                    // Append the parsed character(s)
                    literal.append(state.result.chr);
                    if (state.result.lowSurrogate != 0) {
                        literal.append(state.result.lowSurrogate);
                    }
                } else {
                    // Simple escape
                    literal.append(escapeChar);
                    state.cp++;
                }
            } else {
                // Regular character
                int codePoint = Character.codePointAt(src, state.cp, state.cpend);
                literal.appendCodePoint(codePoint);
                state.cp += Character.charCount(codePoint);
            }
        }

        if (state.cp >= state.cpend) {
            NativeRegExp.reportError("msg.bad.regexp", "Unclosed \\q{");
            return false;
        }
        state.cp++; // Skip '}'

        target.stringLiterals.add(literal.toString());

        return true;
    }

    /**
     * Check if v-mode (unicodeSets) is enabled.
     *
     * @param state Compiler state (unused, kept for API compatibility)
     * @param params Parser parameters containing vMode
     * @return true if v-mode is enabled
     */
    static boolean isVMode(CompilerState state, ParserParameters params) {
        return params.isVMode();
    }

    /**
     * Parse set operations (-- and &&) in v-mode character classes.
     *
     * @param state Compiler state
     * @param params Parser parameters
     * @param depth Current nesting depth
     * @param contents ClassContents to add operations to
     * @return true if successful, false on error
     */
    static boolean parseSetOperations(
            CompilerState state, ParserParameters params, int depth, ClassContents contents) {
        if (!isVMode(state, params)) {
            return true; // Set operations only in v-mode
        }

        char[] src = state.cpbegin;
        while (state.cp + 1 < state.cpend) {
            char c1 = src[state.cp];
            char c2 = src[state.cp + 1];

            SetOperation.Type opType = null;
            if (c1 == '-' && c2 == '-') {
                opType = SetOperation.Type.SUBTRACT;
            } else if (c1 == '&' && c2 == '&') {
                opType = SetOperation.Type.INTERSECT;
            } else {
                break; // No more set operations
            }

            state.cp += 2; // Skip the operator (-- or &&)

            // Parse the set operand
            ClassContents operand = parseSetOperand(state, params, depth);
            if (operand == null) {
                return false;
            }

            contents.setOperations.add(new SetOperation(opType, operand));
        }

        return true;
    }

    /**
     * Parse set operation operand in v-mode character classes.
     *
     * @param state Compiler state
     * @param params Parser parameters
     * @param depth Current nesting depth
     * @return Parsed operand as ClassContents, or null on error
     */
    static ClassContents parseSetOperand(CompilerState state, ParserParameters params, int depth) {
        char[] src = state.cpbegin;
        ClassContents operand = new ClassContents();

        if (state.cp >= state.cpend) {
            NativeRegExp.reportError(MSG_SET_OP_MISSING_OPERAND, "");
            return null;
        }

        if (src[state.cp] == '[') {
            // Nested character class
            state.cp++; // Skip '['
            operand = parseClassContents(state, params, depth + 1);
            if (operand == null) {
                NativeRegExp.reportError(MSG_INVALID_NESTED_CLASS, "");
                return null;
            }
        } else if (src[state.cp] == '\\'
                && state.cp + 1 < state.cpend
                && src[state.cp + 1] == 'q') {
            // String disjunction: \q{...}
            if (!parseStringLiterals(state, params, operand)) {
                return null;
            }
        } else if (src[state.cp] == '\\' && state.cp + 1 < state.cpend) {
            // Character class escapes and Unicode property escapes
            char escapeChar = src[state.cp + 1];
            state.cp++; // Move to the escape character

            // Try Unicode property escapes first
            if (escapeChar == 'p' || escapeChar == 'P') {
                if (!NativeRegExp.parseUnicodePropertyEscape(state, params)) {
                    NativeRegExp.reportError("msg.invalid.property", "");
                    return null;
                }
                operand.escapeNodes.add(state.result);
            } else if (parseCharacterClassEscapeOperand(state)) {
                operand.escapeNodes.add(state.result);
            } else {
                NativeRegExp.reportError(MSG_INVALID_SET_OP_OPERAND, "");
                return null;
            }
        } else {
            // Operand must be bracketed class, \q{}, or character class/property escape
            NativeRegExp.reportError(MSG_INVALID_SET_OP_OPERAND, "");
            return null;
        }

        return operand;
    }

    /**
     * Validate syntax characters in v-mode.
     *
     * @param state Compiler state
     * @param thisCodePoint Current character code point to validate
     * @return true if valid, false if error was reported
     */
    static boolean validateVModeSyntax(CompilerState state, int thisCodePoint) {
        if ((state.flags & NativeRegExp.JSREG_UNICODESETS) == 0) {
            return true; // Not in v-mode, no validation needed
        }

        char current = (char) thisCodePoint;
        char[] src = state.cpbegin;

        // Check for single syntax characters that must be escaped in v-mode
        switch (current) {
            case '(':
            case ')':
            case '/':
            case '|':
            case '-':
                NativeRegExp.reportError("msg.invalid.class", "");
                return false;
        }

        // Check for invalid double punctuators (excluding && and -- which are operators)
        if (state.cp < state.cpend) {
            char next = src[state.cp];
            if (current == next) {
                switch (current) {
                    case '!':
                    case '#':
                    case '$':
                    case '%':
                    case '*':
                    case '+':
                    case ',':
                    case '.':
                    case ':':
                    case ';':
                    case '<':
                    case '=':
                    case '>':
                    case '?':
                    case '@':
                    case '^':
                    case '`':
                    case '~':
                        NativeRegExp.reportError("msg.invalid.class", "");
                        return false;
                }
            }
        }

        return true;
    }

    /**
     * Get emoji sequences for a Property of Strings, or null if not found.
     *
     * @param propertyName Property name (with or without underscores)
     * @return List of emoji sequences if property of strings, null if not found
     */
    static List<String> getPropertyOfStringsSequences(String propertyName) {
        // Normalize property name: remove underscores, lowercase
        String normalized = propertyName.replace("_", "").toLowerCase(java.util.Locale.ROOT);

        java.util.function.Supplier<List<String>> loader =
                PROPERTY_OF_STRINGS_REGISTRY.get(normalized);
        return loader != null ? loader.get() : null;
    }

    /**
     * Parse character class contents ([...]) with ES2024 v-flag features.
     *
     * @param state Compiler state
     * @param params Parser parameters
     * @param depth Current nesting depth (protects against stack overflow)
     * @return Parsed ClassContents, or null on error
     */
    static ClassContents parseClassContents(
            CompilerState state, ParserParameters params, int depth) {
        // Protect against stack overflow from deeply nested character classes
        if (depth > MAX_CLASS_NESTING_DEPTH) {
            NativeRegExp.reportError(MSG_INVALID_NESTED_CLASS, "");
            return null;
        }

        char[] src = state.cpbegin;
        int rangeStart = 0;
        boolean inRange = false;
        int thisCodePoint = Integer.MAX_VALUE;
        ClassContents contents = new ClassContents();

        if (state.cp >= state.cpend) return null;

        if (src[state.cp] == ']') {
            state.cp++;
            return contents;
        }

        if (src[state.cp] == '^') {
            state.cp++;
            contents.sense = false;
        }

        // Main loop: parse characters until we hit ']' or a set operation (-- or &&)
        while (state.cp != state.cpend && src[state.cp] != ']') {
            // Check for set operations in v flag mode
            if (isVMode(state, params) && state.cp + 1 < state.cpend) {
                char c1 = src[state.cp];
                char c2 = src[state.cp + 1];
                if ((c1 == '-' && c2 == '-') || (c1 == '&' && c2 == '&')) {
                    break; // Stop main loop, parse set operation after
                }
            }

            if (src[state.cp] == '\\') {
                state.cp++;
                // Handle \q{...} string literals in v flag mode
                if (isVMode(state, params) && state.cp < state.cpend && src[state.cp] == 'q') {
                    state.cp--; // Back up to '\' for parseStringLiterals
                    if (!parseStringLiterals(state, params, contents)) {
                        return null;
                    }
                    continue; // Skip to next iteration
                } else if (state.cp < state.cpend && src[state.cp] == 'b') {
                    state.cp++;
                    thisCodePoint = NativeRegExp.BACKSPACE_CHAR;
                } else if (params.isUnicodeMode()
                        && state.cp < state.cpend
                        && src[state.cp] == '-') {
                    state.cp++;
                    thisCodePoint = '-';
                } else {
                    if (!NativeRegExp.parseCharacterAndCharacterClassEscape(state, params)) {
                        if (src[state.cp] == 'c' && !params.isUnicodeMode()) {
                            thisCodePoint = '\\';
                        } else {
                            NativeRegExp.reportError("msg.invalid.escape", "");
                            return null;
                        }
                    } else {
                        if (state.result.op == NativeRegExp.REOP_FLAT
                                || state.result.op == NativeRegExp.REOP_STRING_MATCHER) {
                            if (state.result.lowSurrogate == 0) {
                                thisCodePoint = state.result.chr;
                            } else {
                                thisCodePoint =
                                        Character.toCodePoint(
                                                state.result.chr, state.result.lowSurrogate);
                            }
                        } else {
                            // ES2024: Check if this is a Property of Strings
                            if ((state.result.op == NativeRegExp.REOP_UPROP
                                            || state.result.op == NativeRegExp.REOP_UPROP_NOT)
                                    && state.result.unicodeProperty == -2
                                    && state.result.propertyName != null) {

                                // Property of Strings - expand to string matchers
                                List<String> sequences =
                                        getPropertyOfStringsSequences(state.result.propertyName);

                                if (sequences == null) {
                                    NativeRegExp.reportError(
                                            "msg.invalid.property",
                                            "Property of Strings not found: "
                                                    + state.result.propertyName);
                                    return null;
                                }

                                // Validate: Property of Strings requires v-flag
                                if (!isVMode(state, params)) {
                                    NativeRegExp.reportError(
                                            "msg.property.requires.vflag",
                                            "Property "
                                                    + state.result.propertyName
                                                    + " requires v flag");
                                    return null;
                                }

                                // Validate: Property of Strings cannot be negated
                                if (state.result.op == NativeRegExp.REOP_UPROP_NOT) {
                                    NativeRegExp.reportError(
                                            "msg.property.not.negatable",
                                            "Property "
                                                    + state.result.propertyName
                                                    + " cannot be negated");
                                    return null;
                                }

                                // Add emoji sequences as string matchers
                                boolean caseInsensitive =
                                        (state.flags & NativeRegExp.JSREG_FOLD) != 0;
                                for (String sequence : sequences) {
                                    contents.stringMatchers.add(
                                            new StringMatcher(sequence, caseInsensitive));
                                }

                                // Don't add to escapeNodes - we've expanded it to stringMatchers
                            } else {
                                // Regular escape node (binary property, \d, \w, etc.)
                                contents.escapeNodes.add(state.result);
                            }

                            if (inRange) {
                                if (!params.isUnicodeMode()) {
                                    contents.codepoints.add((int) '-');
                                    inRange = false;
                                } else {
                                    NativeRegExp.reportError("msg.invalid.class", "");
                                }
                            } else {
                                // In unicode mode, '-' after a character class escape is invalid
                                // unless it's the start of a v-flag set operation (-- or &&)
                                if (state.cp < state.cpend
                                        && src[state.cp] == '-'
                                        && params.isUnicodeMode()) {
                                    boolean isSetOperationStart =
                                            (state.flags & NativeRegExp.JSREG_UNICODESETS) != 0
                                                    && state.cp + 1 < state.cpend
                                                    && src[state.cp + 1] == '-';
                                    if (!isSetOperationStart) {
                                        NativeRegExp.reportError("msg.invalid.class", "");
                                    }
                                }
                            }
                            // multi-character character escapes can't be part of ranges
                            continue;
                        }
                    }
                }
            } else {
                if (RegExpFlags.fromBitmask(state.flags).isUnicodeMode()) {
                    thisCodePoint = Character.codePointAt(src, state.cp, state.cpend);
                    state.cp += Character.charCount(thisCodePoint);
                } else {
                    thisCodePoint = src[state.cp];
                    state.cp++;
                }
            }

            // ES2024: Handle nested classes in v-mode
            if (isVMode(state, params) && thisCodePoint == '[') {
                // This is a nested class - parse it recursively
                ClassContents nestedContents = parseClassContents(state, params, depth + 1);
                if (nestedContents == null) {
                    NativeRegExp.reportError(MSG_INVALID_NESTED_CLASS, "");
                    return null;
                }
                // Merge the nested class contents into our contents
                contents.mergeFrom(nestedContents);
                continue; // Skip the rest of the loop
            }

            // ES2024: Validate syntax characters in v-mode
            if (!validateVModeSyntax(state, thisCodePoint)) {
                return null;
            }

            if (inRange) {
                if (rangeStart > thisCodePoint) {
                    NativeRegExp.reportError("msg.bad.range", "");
                    return null;
                }
                inRange = false;
                // Unified Unicode storage - no BMP vs non-BMP split
                contents.ranges.add(rangeStart);
                contents.ranges.add(thisCodePoint);
            } else {
                // Unified Unicode storage - no BMP vs non-BMP split
                contents.codepoints.add(thisCodePoint);
                if (state.cp + 1 < state.cpend && src[state.cp + 1] != ']') {
                    char currentChar = src[state.cp];

                    if (currentChar == '-') {
                        state.cp++;
                        inRange = true;
                        rangeStart = thisCodePoint;
                    }
                }
            }
        }

        // Parse set operations (-- and &&) for 'v' flag
        if (!parseSetOperations(state, params, depth, contents)) {
            return null;
        }

        if (state.cp < state.cpend && src[state.cp] == ']') {
            state.cp++;
        } else if (isVMode(state, params)) {
            // In v-mode, closing ']' is required for all character classes
            NativeRegExp.reportError("msg.unterm.class", "");
            return null;
        }

        // ES2024 validation: complement classes cannot contain multi-character strings
        if (!contents.sense && (state.flags & NativeRegExp.JSREG_UNICODESETS) != 0) {
            for (String literal : contents.stringLiterals) {
                if (literal.length() > 1) {
                    NativeRegExp.reportError("msg.invalid.complement.class", "");
                    return null;
                }
            }
        }

        return contents;
    }

    /**
     * Build character sets for set operation operands.
     *
     * @param contents ClassContents containing set operations
     * @param bmsize Bitmap size
     */
    static void buildOperandCharSets(ClassContents contents, int bmsize) {
        if (contents.setOperations.isEmpty()) {
            return;
        }

        for (SetOperation op : contents.setOperations) {
            op.operandCharSet = new RECharSet(op.getOperand(), bmsize);
            // Recursively build for nested set operations
            buildOperandCharSets(op.getOperand(), bmsize);
        }
    }
}
