/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Table-driven opcode execution using lambdas for regex bytecode operations. */
public final class OpcodeTable {
    /** Handler for a specific regex bytecode operation. Stateless and reusable. */
    interface Handler {
        /**
         * Execute this opcode operation.
         *
         * @param ctx execution context containing input, state, and program
         * @return result indicating success/failure and next PC
         */
        ExecutionContext.Result handle(ExecutionContext ctx);
    }

    private static final Map<Byte, Handler> handlers = new HashMap<>();

    static {
        registerHandlers();
    }

    /** Register all opcode handlers using lambdas. */
    private static void registerHandlers() {
        // Zero-width assertions
        handlers.put(NativeRegExp.REOP_EMPTY, ctx -> ExecutionContext.Result.success(ctx.pc));

        handlers.put(
                NativeRegExp.REOP_BOL,
                ctx -> {
                    int cp = ctx.getCurrentPosition();
                    if (cp == 0) {
                        return ExecutionContext.Result.success(ctx.pc);
                    }
                    if (ctx.isMultiline() && NativeRegExp.isLineTerm(ctx.input.charAt(cp - 1))) {
                        return ExecutionContext.Result.success(ctx.pc);
                    }
                    return ExecutionContext.Result.failure();
                });

        handlers.put(
                NativeRegExp.REOP_EOL,
                ctx -> {
                    int cp = ctx.getCurrentPosition();
                    if (cp == ctx.end) {
                        return ExecutionContext.Result.success(ctx.pc);
                    }
                    if (ctx.isMultiline() && NativeRegExp.isLineTerm((char) ctx.getCurrentChar())) {
                        return ExecutionContext.Result.success(ctx.pc);
                    }
                    return ExecutionContext.Result.failure();
                });

        handlers.put(
                NativeRegExp.REOP_WBDRY,
                ctx -> {
                    int cp = ctx.getCurrentPosition();
                    boolean prevIsWord = (cp > 0) && NativeRegExp.isWord(ctx.input.charAt(cp - 1));
                    boolean currIsWord =
                            (cp < ctx.end) && NativeRegExp.isWord(ctx.input.charAt(cp));
                    return (prevIsWord ^ currIsWord)
                            ? ExecutionContext.Result.success(ctx.pc)
                            : ExecutionContext.Result.failure();
                });

        handlers.put(
                NativeRegExp.REOP_WNONBDRY,
                ctx -> {
                    int cp = ctx.getCurrentPosition();
                    boolean prevIsWord = (cp > 0) && NativeRegExp.isWord(ctx.input.charAt(cp - 1));
                    boolean currIsWord =
                            (cp < ctx.end) && NativeRegExp.isWord(ctx.input.charAt(cp));
                    return (prevIsWord == currIsWord)
                            ? ExecutionContext.Result.success(ctx.pc)
                            : ExecutionContext.Result.failure();
                });

        // Dot (matches any character except line terminators, unless /s flag)
        handlers.put(
                NativeRegExp.REOP_DOT,
                ctx -> {
                    if (!ctx.isMatchPositionInBounds()) {
                        return ExecutionContext.Result.failure();
                    }
                    int ch =
                            ctx.isUnicodeMode()
                                    ? ctx.input.codePointAt(ctx.getMatchPosition())
                                    : ctx.input.charAt(ctx.getMatchPosition());
                    if (!ctx.isDotAll() && NativeRegExp.isLineTerm((char) ch)) {
                        return ExecutionContext.Result.failure();
                    }
                    ctx.advancePosition(ctx.getMatchDelta());
                    return ExecutionContext.Result.success(ctx.pc);
                });

        // Character predicate matchers (shared logic)
        handlers.put(
                NativeRegExp.REOP_DIGIT,
                ctx -> matchPredicate(ctx, c -> NativeRegExp.isDigit((char) c.intValue()), false));
        handlers.put(
                NativeRegExp.REOP_NONDIGIT,
                ctx -> matchPredicate(ctx, c -> NativeRegExp.isDigit((char) c.intValue()), true));
        handlers.put(
                NativeRegExp.REOP_ALNUM,
                ctx -> matchPredicate(ctx, c -> NativeRegExp.isWord((char) c.intValue()), false));
        handlers.put(
                NativeRegExp.REOP_NONALNUM,
                ctx -> matchPredicate(ctx, c -> NativeRegExp.isWord((char) c.intValue()), true));
        handlers.put(
                NativeRegExp.REOP_SPACE,
                ctx ->
                        matchPredicate(
                                ctx, c -> NativeRegExp.isREWhiteSpace((char) c.intValue()), false));
        handlers.put(
                NativeRegExp.REOP_NONSPACE,
                ctx ->
                        matchPredicate(
                                ctx, c -> NativeRegExp.isREWhiteSpace((char) c.intValue()), true));

        // Unicode properties
        handlers.put(
                NativeRegExp.REOP_UPROP,
                ctx -> {
                    int encodedProp = ctx.readIndex();
                    if (!ctx.isMatchPositionInBounds()) {
                        return ExecutionContext.Result.failure();
                    }
                    int codePoint = ctx.input.codePointAt(ctx.getMatchPosition());
                    if (UnicodeProperties.hasProperty(encodedProp, codePoint)) {
                        ctx.advancePosition(ctx.getMatchDelta());
                        return ExecutionContext.Result.success(ctx.pc);
                    }
                    return ExecutionContext.Result.failure();
                });

        handlers.put(
                NativeRegExp.REOP_UPROP_NOT,
                ctx -> {
                    int encodedProp = ctx.readIndex();
                    if (!ctx.isMatchPositionInBounds()) {
                        return ExecutionContext.Result.failure();
                    }
                    int codePoint = ctx.input.codePointAt(ctx.getMatchPosition());
                    if (!UnicodeProperties.hasProperty(encodedProp, codePoint)) {
                        ctx.advancePosition(ctx.getMatchDelta());
                        return ExecutionContext.Result.success(ctx.pc);
                    }
                    return ExecutionContext.Result.failure();
                });

        // Character classes (shared logic for CLASS and NCLASS)
        handlers.put(NativeRegExp.REOP_CLASS, ctx -> matchCharacterClass(ctx, false));
        handlers.put(NativeRegExp.REOP_NCLASS, ctx -> matchCharacterClass(ctx, true));

        // String matcher - universal string matching (replaces FLAT* opcodes)
        handlers.put(
                NativeRegExp.REOP_STRING_MATCHER,
                ctx -> {
                    int stringMatcherIdx = ctx.readIndex();
                    StringMatcher matcher = ctx.gData.regexp.stringMatchers.get(stringMatcherIdx);
                    int matchLen =
                            matcher.match(ctx.input, ctx.getCurrentPosition(), ctx.matchBackward);

                    if (matchLen > 0) {
                        // In Unicode mode, validate we didn't split a surrogate pair
                        if (ctx.isUnicodeMode()) {
                            String literal = matcher.getLiteral();
                            int matchEnd = ctx.getCurrentPosition() + matchLen;
                            int matchStart = ctx.getCurrentPosition();

                            // Check if match ends with unpaired high surrogate
                            if (literal.length() > 0
                                    && Character.isHighSurrogate(
                                            literal.charAt(literal.length() - 1))
                                    && matchEnd < ctx.end
                                    && Character.isLowSurrogate(ctx.input.charAt(matchEnd))) {
                                matchLen = -1;
                            }

                            // Check if match starts with unpaired low surrogate
                            if (matchLen > 0
                                    && literal.length() > 0
                                    && Character.isLowSurrogate(literal.charAt(0))
                                    && matchStart > 0
                                    && Character.isHighSurrogate(
                                            ctx.input.charAt(matchStart - 1))) {
                                matchLen = -1;
                            }
                        }

                        if (matchLen > 0) {
                            ctx.advancePosition(ctx.matchBackward ? -matchLen : matchLen);
                            return ExecutionContext.Result.success(ctx.pc);
                        }
                    }
                    return ExecutionContext.Result.failure();
                });

        // Backreferences
        handlers.put(
                NativeRegExp.REOP_BACKREF,
                ctx -> {
                    int parenIndex = ctx.readIndex();
                    if (NativeRegExp.backrefMatcher(
                            ctx.gData, parenIndex, ctx.input, ctx.end, ctx.matchBackward)) {
                        return ExecutionContext.Result.success(ctx.pc);
                    }
                    return ExecutionContext.Result.failure();
                });

        handlers.put(
                NativeRegExp.REOP_NAMED_BACKREF,
                ctx -> {
                    int backRefNameIndex = ctx.readIndex();
                    if (ctx.gData.parens == null
                            || backRefNameIndex >= ctx.gData.regexp.namedBackRefs.size()) {
                        return ExecutionContext.Result.success(ctx.pc);
                    }

                    String backRefName = ctx.gData.regexp.namedBackRefs.get(backRefNameIndex);
                    List<Integer> indices = ctx.gData.regexp.namedCaptureGroups.get(backRefName);
                    boolean anyMatched = false;
                    boolean anyFailed = false;

                    for (int i : indices) {
                        if (ctx.gData.parensIndex(i) == -1) continue;
                        if (NativeRegExp.backrefMatcher(
                                ctx.gData, i, ctx.input, ctx.end, ctx.matchBackward)) {
                            anyMatched = true;
                            break;
                        } else {
                            anyFailed = true;
                        }
                    }

                    // Success if any backref matched OR none were attempted
                    return (anyMatched || !anyFailed)
                            ? ExecutionContext.Result.success(ctx.pc)
                            : ExecutionContext.Result.failure();
                });
    }

    /**
     * Shared logic for character class matchers (CLASS and NCLASS).
     *
     * @param ctx execution context
     * @param negate true for NCLASS (negated class), false for CLASS
     * @return execution result
     */
    private static ExecutionContext.Result matchCharacterClass(
            ExecutionContext ctx, boolean negate) {
        int classIndex = ctx.readIndex();
        if (!ctx.isMatchPositionInBounds()) {
            return ExecutionContext.Result.failure();
        }

        // Try string literal match first (v flag feature)
        int matchPos = ctx.matchBackward ? ctx.getCurrentPosition() : ctx.getMatchPosition();
        int stringLiteralLen = ctx.matchStringLiteral(classIndex, matchPos);

        if (stringLiteralLen >= 0) {
            // String literal matched
            if (negate) {
                // For NCLASS, string match means failure (it's in the negated class)
                return ExecutionContext.Result.failure();
            }
            // For CLASS, string match means success
            if (ctx.matchBackward) {
                ctx.advancePosition(-stringLiteralLen);
            } else {
                ctx.advancePosition(stringLiteralLen);
            }
            return ExecutionContext.Result.success(ctx.pc);
        }

        // Fall back to single codepoint matching
        int codePoint =
                ctx.isUnicodeMode()
                        ? ctx.input.codePointAt(ctx.getMatchPosition())
                        : ctx.input.charAt(ctx.getMatchPosition());

        // classMatcher handles negation internally via charSet.sense
        if (ctx.matchesCharacterClass(classIndex, codePoint)) {
            ctx.advancePosition(ctx.getMatchDelta());
            return ExecutionContext.Result.success(ctx.pc);
        }

        return ExecutionContext.Result.failure();
    }

    /**
     * Shared logic for character predicate matchers (digit, word, space and their negations).
     *
     * @param ctx execution context
     * @param predicate test for the character
     * @param negate if true, invert the predicate result
     * @return execution result
     */
    private static ExecutionContext.Result matchPredicate(
            ExecutionContext ctx, Predicate<Integer> predicate, boolean negate) {
        if (!ctx.isMatchPositionInBounds()) {
            return ExecutionContext.Result.failure();
        }

        int ch =
                ctx.isUnicodeMode()
                        ? ctx.input.codePointAt(ctx.getMatchPosition())
                        : ctx.input.charAt(ctx.getMatchPosition());

        boolean matches = predicate.test(ch);
        if (negate) {
            matches = !matches;
        }

        if (matches) {
            ctx.advancePosition(ctx.getMatchDelta());
            return ExecutionContext.Result.success(ctx.pc);
        }

        return ExecutionContext.Result.failure();
    }

    /**
     * Check if this table has a handler for the given opcode.
     *
     * @param opcode the opcode to check
     * @return true if a handler exists
     */
    public static boolean hasHandler(byte opcode) {
        return handlers.containsKey(opcode);
    }

    /**
     * Execute a specific opcode.
     *
     * @param ctx execution context
     * @param opcode the opcode to execute
     * @return execution result
     */
    public static ExecutionContext.Result execute(ExecutionContext ctx, byte opcode) {
        Handler handler = handlers.get(opcode);
        if (handler != null) {
            return handler.handle(ctx);
        }
        return ExecutionContext.Result.failure();
    }
}
