/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.mozilla.javascript.AbstractEcmaObjectOperations;
import org.mozilla.javascript.AbstractEcmaStringOperations;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptRuntimeES6;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.config.RhinoConfig;

/**
 * This class implements the RegExp native object.
 *
 * <p>See ECMA 262 §22.2.
 *
 * <p>Implementation in C by Brendan Eich. Port to Java by Norris Boyd from jsregexp.c version 1.36.
 * Merged up to version 1.38 with Unicode support. Merged bug fixes in version 1.39 and
 * JSFUN13_BRANCH changes up to 1.32.2.13. ES2024/ES2025 features added.
 *
 * @author Brendan Eich
 * @author Norris Boyd
 */
public class NativeRegExp extends IdScriptableObject {
    private static final long serialVersionUID = 4965263491464903264L;

    private static final Object REGEXP_TAG = new Object();

    // ==================================================================================
    // CONSTANTS
    // ==================================================================================

    // --------------------------------------------------
    // RegExp Flags (ECMAScript specification)
    // --------------------------------------------------
    public static final int JSREG_GLOB = 0x01; // 'g' flag
    public static final int JSREG_FOLD = 0x02; // 'i' flag
    public static final int JSREG_MULTILINE = 0x04; // 'm' flag
    public static final int JSREG_DOTALL = 0x08; // 's' flag (ES2018)
    public static final int JSREG_STICKY = 0x10; // 'y' flag (ES2015)
    public static final int JSREG_UNICODE = 0x20; // 'u' flag (ES2015)
    public static final int JSREG_HASINDICES = 0x40; // 'd' flag (ES2022)
    public static final int JSREG_UNICODESETS = 0x80; // 'v' flag (ES2024)

    // --------------------------------------------------
    // Unicode and Character Constants
    // --------------------------------------------------
    static final int BMP_MAX_CODEPOINT =
            Character.MAX_VALUE; // Maximum Basic Multilingual Plane codepoint (U+FFFF)
    static final char BACKSPACE_CHAR = '\b'; // Backspace character (\b in character class)

    // --------------------------------------------------
    // Bit Manipulation Constants
    // --------------------------------------------------
    static final int BYTE_BIT_MASK =
            0x7; // Mask for extracting bit position within byte (package-private for RECharSet)
    private static final int BIT_SHIFT_FOR_BYTE_INDEX =
            3; // Right shift to get byte index (divide by 8)

    // --------------------------------------------------
    // Match Type Constants
    // --------------------------------------------------
    public static final int TEST = 0;
    public static final int MATCH = 1;
    public static final int PREFIX = 2;

    // --------------------------------------------------
    // Debug Configuration
    // --------------------------------------------------
    static final boolean debug = RhinoConfig.get("rhino.debugRegexp", false);

    // --------------------------------------------------
    // RegExp Operation Codes (REOP)
    // --------------------------------------------------
    //
    // BYTECODE FORMAT REFERENCE
    //
    // The compiled RegExp program is a sequence of opcodes with operands encoded as follows:
    //
    // INDEX encoding: 16-bit big-endian integer (2 bytes)
    //   - Used for offsets, lengths, indices, and character values
    //   - Encoding: [high_byte][low_byte]
    //   - Example: 0x1234 → [0x12][0x34]
    //
    // Opcode format notation: [opcode:1][operand1:2][operand2:2]...
    //   - Numbers indicate byte count
    //   - All operands use INDEX encoding unless noted
    //
    // Common opcodes:
    //   REOP_EMPTY:          [opcode:1]
    //   REOP_FLAT1:          [opcode:1][char:2]
    //   REOP_BACKREF:        [opcode:1][parenIndex:2]
    //   REOP_CLASS:          [opcode:1][classIndex:2]
    //   REOP_QUANT:          [opcode:1][min:2][max:2][offset:2][parenIdx:2][parenCount:2]
    //   REOP_STAR:           [opcode:1][parenCount:2][parenIndex:2][offset:2]
    //   REOP_ALT:            [opcode:1][offset:2]
    //   REOP_LPAREN:         [opcode:1][parenIndex:2]
    //   REOP_STRING_MATCHER: [opcode:1][matcherIndex:2]
    //
    // See RegExpDebugger.disassemble() for complete opcode operand descriptions.
    //
    static final byte REOP_SIMPLE_START =
            1; /* start of 'simple opcodes' - table-driven without complex state */
    static final byte REOP_EMPTY = REOP_SIMPLE_START; /* match rest of input against rest of r.e. */
    static final byte REOP_BOL = REOP_EMPTY + 1; /* beginning of input (or line if multiline) */
    static final byte REOP_EOL = REOP_BOL + 1; /* end of input (or line if multiline) */
    static final byte REOP_WBDRY = REOP_EOL + 1; /* match "" at word boundary */
    static final byte REOP_WNONBDRY = REOP_WBDRY + 1; /* match "" at word non-boundary */
    static final byte REOP_DOT = REOP_WNONBDRY + 1; /* stands for any character */
    static final byte REOP_DIGIT = REOP_DOT + 1; /* match a digit char: [0-9] */
    static final byte REOP_NONDIGIT = REOP_DIGIT + 1; /* match a non-digit char: [^0-9] */
    static final byte REOP_ALNUM = REOP_NONDIGIT + 1; /* match an alphanumeric char: [0-9a-z_A-Z] */
    static final byte REOP_NONALNUM =
            REOP_ALNUM + 1; /* match a non-alphanumeric char: [^0-9a-z_A-Z] */
    static final byte REOP_SPACE = REOP_NONALNUM + 1; /* match a whitespace char */
    static final byte REOP_NONSPACE = REOP_SPACE + 1; /* match a non-whitespace char */
    static final byte REOP_BACKREF =
            REOP_NONSPACE + 1; /* back-reference (e.g., \1) to a parenthetical */
    static final byte REOP_FLAT = REOP_BACKREF + 1; /* match a flat string */
    static final byte REOP_FLAT1 = REOP_FLAT + 1; /* match a single char */
    static final byte REOP_FLATi = REOP_FLAT1 + 1; /* case-independent REOP_FLAT */
    static final byte REOP_FLAT1i = REOP_FLATi + 1; /* case-independent REOP_FLAT1 */
    static final byte REOP_UCFLAT1 = REOP_FLAT1i + 1; /* single Unicode char */
    static final byte REOP_UCFLAT1i = REOP_UCFLAT1 + 1; /* case-independent REOP_UCFLAT1 */
    static final byte REOP_UCSPFLAT1 = REOP_UCFLAT1i + 1; /* single Unicode surrogate pair */
    static final byte REOP_STRING_MATCHER =
            REOP_UCSPFLAT1 + 1; /* universal string matcher (ES2024) */
    static final byte REOP_CLASS = REOP_STRING_MATCHER + 1; /* character class with index */
    static final byte REOP_NCLASS = REOP_CLASS + 1; /* negated character class with index */
    static final byte REOP_NAMED_BACKREF = REOP_NCLASS + 1; /* named back-reference */
    static final byte REOP_UPROP = REOP_NAMED_BACKREF + 1; /* unicode property */
    static final byte REOP_UPROP_NOT = REOP_UPROP + 1; /* negated unicode property */
    static final byte REOP_SIMPLE_END = REOP_UPROP_NOT; /* end of 'simple opcodes' */
    // REOP_SIMPLE_END is not a real opcode, but a sentinel for the end of the simple opcodes

    static final byte REOP_QUANT = REOP_SIMPLE_END + 1; /* quantified atom: atom{1,2} */
    static final byte REOP_STAR = REOP_QUANT + 1; /* zero or more occurrences of kid */
    static final byte REOP_PLUS = REOP_STAR + 1; /* one or more occurrences of kid */
    static final byte REOP_OPT = REOP_PLUS + 1; /* optional subexpression in kid */
    static final byte REOP_LPAREN =
            REOP_OPT + 1; /* left paren bytecode: kid is u.num'th sub-regexp */
    static final byte REOP_RPAREN = REOP_LPAREN + 1; /* right paren bytecode */
    static final byte REOP_ALT = REOP_RPAREN + 1; /* alternative subexpressions in kid and next */
    static final byte REOP_JUMP = REOP_ALT + 1; /* for deoptimized closure loops */
    static final byte REOP_ASSERT = REOP_JUMP + 1; /* zero width positive lookahead assertion */
    static final byte REOP_ASSERT_NOT =
            REOP_ASSERT + 1; /* zero width negative lookahead assertion */
    static final byte REOP_ASSERTTEST =
            REOP_ASSERT_NOT + 1; /* sentinel at end of assertion child */
    static final byte REOP_ASSERTNOTTEST =
            REOP_ASSERTTEST + 1; /* sentinel at end of !assertion child */
    static final byte REOP_MINIMALSTAR = REOP_ASSERTNOTTEST + 1; /* non-greedy version of * */
    static final byte REOP_MINIMALPLUS = REOP_MINIMALSTAR + 1; /* non-greedy version of + */
    static final byte REOP_MINIMALOPT = REOP_MINIMALPLUS + 1; /* non-greedy version of ? */
    static final byte REOP_MINIMALQUANT = REOP_MINIMALOPT + 1; /* non-greedy version of {} */
    static final byte REOP_ENDCHILD =
            REOP_MINIMALQUANT + 1; /* sentinel at end of quantifier child */
    static final byte REOP_REPEAT = REOP_ENDCHILD + 1; /* directs execution of greedy quantifier */
    static final byte REOP_MINIMALREPEAT =
            REOP_REPEAT + 1; /* directs execution of non-greedy quantifier */
    static final byte REOP_ALTPREREQ =
            REOP_MINIMALREPEAT + 1; /* prerequisite for ALT, either of two chars */
    static final byte REOP_ALTPREREQi = REOP_ALTPREREQ + 1; /* case-independent REOP_ALTPREREQ */
    static final byte REOP_ALTPREREQ2 =
            REOP_ALTPREREQi + 1; /* prerequisite for ALT, a char or a class */
    static final byte REOP_ASSERTBACK =
            REOP_ALTPREREQ2 + 1; /* zero width positive lookbehind assertion */
    static final byte REOP_ASSERTBACK_NOT =
            REOP_ASSERTBACK + 1; /* zero width negative lookbehind assertion */
    static final byte REOP_ASSERTBACKTEST =
            REOP_ASSERTBACK_NOT + 1; /* sentinel at end of assertion child */
    static final byte REOP_ASSERTBACKNOTTEST =
            REOP_ASSERTBACKTEST + 1; /* sentinel at end of !assertion child */

    static final byte REOP_END = REOP_ASSERTBACKNOTTEST + 1;

    private static final int ANCHOR_BOL = -2;

    // ==================================================================================
    // INITIALIZATION AND CONSTRUCTORS
    // ==================================================================================

    static Object init(Context cx, Scriptable scope, boolean sealed) {

        NativeRegExp proto = NativeRegExpInstantiator.withLanguageVersion(cx.getLanguageVersion());
        proto.re = compileRE(cx, "", null, false);
        proto.activatePrototypeMap(MAX_PROTOTYPE_ID);
        proto.setParentScope(scope);
        proto.setPrototype(getObjectPrototype(scope));

        var ctor = NativeRegExpCtor.init(cx, scope, sealed);
        // Bug #324006: ECMA-262 15.10.6.1 says "The initial value of
        // RegExp.prototype.constructor is the builtin RegExp constructor."
        proto.defineProperty("constructor", ctor, ScriptableObject.DONTENUM);

        ScriptRuntime.setFunctionProtoAndParent(ctor, cx, scope);

        ctor.setImmunePrototypeProperty(proto);

        if (sealed) {
            proto.sealObject();
            ctor.sealObject();
        }

        ScriptableObject.defineProperty(scope, "RegExp", ctor, ScriptableObject.DONTENUM);

        ScriptRuntimeES6.addSymbolSpecies(cx, scope, ctor);

        return ctor;
    }

    NativeRegExp(Scriptable scope, RECompiled regexpCompiled) {
        this.re = regexpCompiled;
        setLastIndex(ScriptRuntime.zeroObj);
        ScriptRuntime.setBuiltinProtoAndParent(this, scope, TopLevel.Builtins.RegExp);
    }

    // ==================================================================================
    // PUBLIC API METHODS
    // ==================================================================================

    @Override
    public String getClassName() {
        return "RegExp";
    }

    /**
     * Gets the value to be returned by the typeof operator called on this object.
     *
     * @see org.mozilla.javascript.ScriptableObject#getTypeOf()
     * @return "object"
     */
    @Override
    public String getTypeOf() {
        return "object";
    }

    Scriptable compile(Context cx, Scriptable scope, Object[] args) {
        if (args.length >= 1
                && args[0] instanceof NativeRegExp
                && (args.length == 1 || args[1] == Undefined.instance)) {
            // Avoid recompiling the regex
            this.re = ((NativeRegExp) args[0]).re;
        } else {
            String pattern;
            if (args.length == 0 || args[0] == Undefined.instance) {
                pattern = "";
            } else if (args[0] instanceof NativeRegExp) {
                pattern = new String(((NativeRegExp) args[0]).re.source);
            } else {
                pattern = escapeRegExp(args[0]);
            }

            String flags =
                    args.length > 1 && args[1] != Undefined.instance
                            ? ScriptRuntime.toString(args[1])
                            : null;

            // Passing a regex and flags is allowed in ES6, but forbidden in ES5 and lower.
            // Spec ref: 15.10.4.1 in ES5, 22.2.4.1 in ES6
            if (args.length > 0
                    && args[0] instanceof NativeRegExp
                    && flags != null
                    && cx.getLanguageVersion() < Context.VERSION_ES6) {
                throw ScriptRuntime.typeErrorById("msg.bad.regexp.compile");
            }

            this.re = compileRE(cx, pattern, flags, false);
        }
        setLastIndex(ScriptRuntime.zeroObj);
        return this;
    }

    @Override
    public String toString() {
        // Performance: Pre-allocate capacity (source length + delimiters + ~8 flags)
        StringBuilder buf = new StringBuilder(re.source.length + 10);
        buf.append('/');
        if (re.source.length != 0) {
            buf.append(re.source);
        } else {
            // See bugzilla 226045
            buf.append("(?:)");
        }
        buf.append('/');
        appendFlags(buf);
        return buf.toString();
    }

    private void appendFlags(StringBuilder buf) {
        // Output flags in alphabetical order per ES spec
        if ((re.flags & JSREG_HASINDICES) != 0) buf.append('d');
        if ((re.flags & JSREG_GLOB) != 0) buf.append('g');
        if ((re.flags & JSREG_FOLD) != 0) buf.append('i');
        if ((re.flags & JSREG_MULTILINE) != 0) buf.append('m');
        if ((re.flags & JSREG_DOTALL) != 0) buf.append('s');
        if ((re.flags & JSREG_UNICODE) != 0) buf.append('u');
        if ((re.flags & JSREG_UNICODESETS) != 0) buf.append('v');
        if ((re.flags & JSREG_STICKY) != 0) buf.append('y');
    }

    NativeRegExp() {}

    private static RegExpImpl getImpl(Context cx) {
        return (RegExpImpl) ScriptRuntime.getRegExpProxy(cx);
    }

    private static String escapeRegExp(Object src) {
        String s = ScriptRuntime.toString(src);
        // Escape any naked slashes in regexp source, see bug #510265
        StringBuilder sb = null; // instantiated only if necessary
        int start = 0;
        int slash = s.indexOf('/');
        while (slash > -1) {
            if (slash == start || s.charAt(slash - 1) != '\\') {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(s, start, slash);
                sb.append("\\/");
                start = slash + 1;
            }
            slash = s.indexOf('/', slash + 1);
        }
        if (sb != null) {
            sb.append(s, start, s.length());
            s = sb.toString();
        }
        return s;
    }

    Object execSub(Context cx, Scriptable scopeObj, Object[] args, int matchType) {
        RegExpImpl reImpl = getImpl(cx);
        String str;
        if (args.length == 0) {
            str = reImpl.input;
            if (str == null) {
                str = ScriptRuntime.toString(Undefined.instance);
            }
        } else {
            str = ScriptRuntime.toString(args[0]);
        }

        boolean globalOrSticky = (re.flags & JSREG_GLOB) != 0 || (re.flags & JSREG_STICKY) != 0;
        double d = 0;
        if (globalOrSticky) {
            d = ScriptRuntime.toInteger(lastIndex);

            if (d < 0 || str.length() < d) {
                setLastIndex(ScriptRuntime.zeroObj);
                return null;
            }
        }

        int[] indexp = {(int) d};
        Object rval = executeRegExp(cx, scopeObj, reImpl, str, indexp, matchType);
        if (globalOrSticky) {
            if (rval == null || rval == Undefined.instance) {
                setLastIndex(ScriptRuntime.zeroObj);
            } else {
                setLastIndex(Double.valueOf(indexp[0]));
            }
        }
        return rval;
    }

    private static void extractNamedCaptureGroups(
            char[] src, RENode re, Map<String, List<Integer>> namedCaptureGroups) {
        RENode node = re;
        while (node != null) {
            if (node.op == REOP_LPAREN) {
                if (node.namedCaptureGroupName != null) {
                    // we set an initial capacity of 1 because we optimistically
                    // do not expect duplicate group names
                    ArrayList<Integer> entry = new ArrayList<>(1);

                    if (namedCaptureGroups.putIfAbsent(node.namedCaptureGroupName, entry) != null) {
                        reportError("msg.duplicate.group.name", node.namedCaptureGroupName);
                    }
                    entry.add(node.parenIndex);
                    extractNamedCaptureGroups(src, node.kid, namedCaptureGroups);
                }
            } else if (node.op == REOP_ALT) {
                // handle duplicate capture group names between kid1 and kid2
                // by storing all the parenIndex values in a list
                Map<String, List<Integer>> groupCaptures1 = new HashMap<>();
                Map<String, List<Integer>> groupCaptures2;
                extractNamedCaptureGroups(src, node.kid, groupCaptures1);

                if (groupCaptures1
                        .isEmpty()) { // then no duplicate group names are possible between kid1 and
                    // kid2
                    extractNamedCaptureGroups(src, node.kid2, namedCaptureGroups);
                } else {
                    groupCaptures2 = new HashMap<>();
                    extractNamedCaptureGroups(src, node.kid2, groupCaptures2);

                    groupCaptures2.forEach(
                            (key, value) ->
                                    groupCaptures1.merge(
                                            key,
                                            value,
                                            (v1, v2) -> {
                                                v1.addAll(v2);
                                                return v1;
                                            }));

                    groupCaptures1.forEach(
                            (key, value) -> {
                                if (namedCaptureGroups.putIfAbsent(key, value) != null) {
                                    reportError("msg.duplicate.group.name", key);
                                }
                            });
                }
            } else {
                extractNamedCaptureGroups(src, node.kid, namedCaptureGroups);
            }
            node = node.next;
        }
    }

    // ==================================================================================
    // COMPILATION METHODS
    // ==================================================================================

    static RECompiled compileRE(Context cx, String str, String global, boolean flat) {
        RECompiled regexp = new RECompiled(str);
        int length = str.length();

        // Parse and validate flags using RegExpFlags
        RegExpFlags flagsObj = RegExpFlags.parse(global, cx);
        int flags = flagsObj.getBitmask();
        regexp.flags = flags;

        CompilerState state = new CompilerState(cx, regexp.source, length, flags);
        if (flat && length > 0) {
            if (debug) {
                System.out.println("flat = \"" + str + "\"");
            }
            state.result = new RENode(REOP_STRING_MATCHER);
            boolean caseInsensitive = (flags & JSREG_FOLD) != 0;
            state.result.stringMatcher = new StringMatcher(str, caseInsensitive);
            state.result.chr = state.cpbegin[0];
            state.result.length = length;
            state.result.flatIndex = 0;
            state.progLength += 5;
        } else {
            // 'v' flag (ES2024 unicodeSets) implies Unicode mode
            boolean unicodeMode = (flags & JSREG_UNICODE) != 0 || (flags & JSREG_UNICODESETS) != 0;
            boolean vMode = (flags & JSREG_UNICODESETS) != 0;
            // if unicode mode is on, named capture groups are always on
            ParserParameters params = new ParserParameters(unicodeMode, unicodeMode, vMode);

            if (!parseDisjunction(state, params)) return null;
            // Need to reparse if pattern contains invalid backreferences:
            // "Note: if the number of left parentheses is less than the number
            // specified in \#, the \# is taken as an octal escape"
            CompilerState reParseState = null;
            if (state.maxBackReference > state.parenCount) {
                if (params.isUnicodeMode()) {
                    reportError("msg.invalid.escape", "");
                } else {
                    // Need to reparse if pattern contains invalid backreferences:
                    // "Note: if the number of left parentheses is less than the number
                    // specified in \#, the \# is taken as an octal escape"
                    reParseState = new CompilerState(cx, regexp.source, length, flags);
                    reParseState.backReferenceLimit = state.parenCount;
                }
            }
            if (state.namedCaptureGroupsFound && !params.hasNamedCaptureGroups()) {
                params = new ParserParameters(true, params.isUnicodeMode(), params.isVMode());
                if (reParseState == null) {
                    reParseState = new CompilerState(cx, regexp.source, length, flags);
                }
            }
            if (reParseState != null) {
                state = reParseState;
                if (!parseDisjunction(state, params)) return null;
            }
        }

        regexp.namedCaptureGroups = new HashMap<>();
        if (state.namedCaptureGroupsFound) {
            extractNamedCaptureGroups(regexp.source, state.result, regexp.namedCaptureGroups);
            regexp.namedBackRefs = state.namedCaptureBackRefs;
        }

        regexp.program = new byte[state.progLength + 1];
        if (state.classCount != 0) {
            regexp.classList = new RECharSet[state.classCount];
            regexp.classCount = state.classCount;
        }
        int endPC = emitREBytecode(state, regexp, 0, state.result);
        regexp.program[endPC++] = REOP_END;
        regexp.parenCount = state.parenCount;

        // Debug logging (enable with: -Djava.util.logging.config.level=FINEST)
        RegExpDebugger.logCompilation(
                new String(state.cpbegin, 0, state.cpend), state.flags, regexp);

        // If re starts with literal, init anchorCh accordingly
        switch (regexp.program[0]) {
            case REOP_STRING_MATCHER:
                // Get first codepoint from StringMatcher
                int smIndex = getIndex(regexp.program, 1);
                if (regexp.stringMatchers != null && !regexp.stringMatchers.isEmpty()) {
                    String literal = regexp.stringMatchers.get(smIndex).getLiteral();
                    if (literal.length() > 0) {
                        // Use codePointAt to handle surrogate pairs correctly
                        regexp.anchorCodePoint = literal.codePointAt(0);
                    }
                }
                break;
            case REOP_BOL:
                regexp.anchorCodePoint = ANCHOR_BOL;
                break;
            case REOP_ALT:
                RENode n = state.result;
                if (n.kid.op == REOP_BOL && n.kid2.op == REOP_BOL) {
                    regexp.anchorCodePoint = ANCHOR_BOL;
                }
                break;
        }

        if (debug) {
            if (regexp.anchorCodePoint >= 0) {
                System.out.println("Anchor ch = '" + (char) regexp.anchorCodePoint + "'");
            }
        }
        return regexp;
    }

    public static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    public static boolean isWord(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || isDigit(c) || c == '_';
    }

    private static boolean isControlLetter(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
    }

    public static boolean isLineTerm(char c) {
        return ScriptRuntime.isJSLineTerminator(c);
    }

    public static boolean isREWhiteSpace(int c) {
        return ScriptRuntime.isJSWhitespaceOrLineTerminator(c);
    }

    /*
     *
     * 1. If IgnoreCase is false, return ch.
     * 2. Let u be ch converted to upper case as if by calling
     *    String.prototype.toUpperCase on the one-character string ch.
     * 3. If u does not consist of a single character, return ch.
     * 4. Let cu be u's character.
     * 5. If ch's code point value is greater than or equal to decimal 128 and cu's
     *    code point value is less than decimal 128, then return ch.
     * 6. Return cu.
     */
    /**
     * Check if Unicode mode is enabled (u or v flag).
     *
     * @param flags The regexp flags bitmask
     * @return true if either JSREG_UNICODE or JSREG_UNICODESETS flag is set
     * @deprecated Internal use only. External callers should use {@link
     *     RegExpFlags#isUnicodeMode()}
     */
    @Deprecated
    private static boolean isUnicodeMode(int flags) {
        return RegExpFlags.fromBitmask(flags).isUnicodeMode();
    }

    // ==================================================================================
    // REGEXP PARSER AND COMPILER
    // ==================================================================================
    //
    // NOTE: Several classes have been extracted to separate files for better modularity:
    // - ParserParameters.java - Parser configuration and flags
    // - SetOperation.java - Unicode set operations for v-mode
    // - ClassContents.java - Character class contents (ranges, codepoints, strings)
    // - ExecutionContext.java - Regex execution state and context
    // - OpcodeTable.java - Table-driven opcode handlers
    // - StringMatcher.java - Optimized literal string matching
    //
    // Package-level helper classes (RENode, RECompiled, CompilerState, etc.) remain
    // in this file as they are tightly coupled to the parser/compiler implementation.

    /**
     * Parse a disjunction (top-level alternation).
     *
     * <p>Top-down regular expression grammar, based closely on Perl4.
     *
     * <p>Grammar: regexp: altern | altern '|' regexp
     *
     * <p>A regular expression is one or more alternatives separated by vertical bar.
     *
     * @param state Compiler state with current parse position and AST
     * @param params Parser parameters (flags, mode, etc.)
     * @return true if parsing succeeded, false on error
     */
    private static boolean parseDisjunction(CompilerState state, ParserParameters params) {
        if (!parseAlternative(state, params)) return false;
        char[] source = state.cpbegin;
        int index = state.cp;
        if (index != source.length && source[index] == '|') {
            RENode result;
            ++state.cp;
            result = new RENode(REOP_ALT);
            result.kid = state.result;
            if (!parseDisjunction(state, params)) return false;
            result.kid2 = state.result;
            state.result = result;
            /*
             * Optimization: Look at both alternates to see if there's a FLAT or CLASS at
             * the start of each. If so, use a prerequisite match to quickly reject
             * non-matching input without backtracking through the full alternation.
             *
             * Note: Currently only handles FLAT with lowSurrogate == 0 (simple chars).
             * Could be extended to handle surrogate pairs for additional optimization.
             */
            if (result.kid.op == REOP_FLAT
                    && result.kid2.op == REOP_FLAT
                    && result.kid.lowSurrogate == 0
                    && result.kid2.lowSurrogate == 0) {
                result.op = (state.flags & JSREG_FOLD) == 0 ? REOP_ALTPREREQ : REOP_ALTPREREQi;
                result.chr = result.kid.chr;
                result.index = result.kid2.chr;
                /* ALTPREREQ, uch1, uch2, <next>, ...,
                JUMP, <end> ... JUMP, <end> */
                state.progLength += 13;
            } else if (result.kid.op == REOP_CLASS
                    && result.kid.index < 256
                    && result.kid2.op == REOP_FLAT
                    && result.kid2.lowSurrogate == 0
                    && (state.flags & JSREG_FOLD) == 0) {
                result.op = REOP_ALTPREREQ2;
                result.chr = result.kid2.chr;
                result.index = result.kid.index;
                /* ALTPREREQ2, uch1, uch2, <next>, ...,
                JUMP, <end> ... JUMP, <end> */
                state.progLength += 13;
            } else if (result.kid.op == REOP_FLAT
                    && result.kid2.op == REOP_CLASS
                    && result.kid2.index < 256
                    && result.kid.lowSurrogate == 0
                    && (state.flags & JSREG_FOLD) == 0) {
                result.op = REOP_ALTPREREQ2;
                result.chr = result.kid.chr;
                result.index = result.kid2.index;
                /* ALTPREREQ2, uch1, uch2, <next>, ...,
                JUMP, <end> ... JUMP, <end> */
                state.progLength += 13;
            } else {
                /* ALT, <next>, ..., JUMP, <end> ... JUMP, <end> */
                state.progLength += 9;
            }
        }
        return true;
    }

    /**
     * Parse an alternative (sequence of concatenated items).
     *
     * <p>Grammar: altern: item | item altern
     *
     * <p>An alternative is one or more items concatenated together.
     *
     * @param state Compiler state with current parse position and AST
     * @param params Parser parameters (flags, mode, etc.)
     * @return true if parsing succeeded, false on error
     */
    private static boolean parseAlternative(CompilerState state, ParserParameters params) {
        RENode headTerm = null;
        RENode tailTerm = null;
        char[] source = state.cpbegin;
        while (true) {
            if (state.cp == state.cpend
                    || source[state.cp] == '|'
                    || (state.parenNesting != 0 && source[state.cp] == ')')) {
                if (headTerm == null) {
                    state.result = new RENode(REOP_EMPTY);
                } else state.result = headTerm;
                return true;
            }
            if (!parseTerm(state, params)) return false;
            if (headTerm == null) {
                headTerm = state.result;
                tailTerm = headTerm;
            } else tailTerm.next = state.result;
            while (tailTerm.next != null) {
                // concatenate FLATs if possible
                RENode n = tailTerm.next;
                if (tailTerm.op == REOP_FLAT
                        && tailTerm.flatIndex != -1
                        && n.op == REOP_FLAT
                        && n.flatIndex == (tailTerm.flatIndex + tailTerm.length)) {
                    tailTerm.length += n.length;
                    tailTerm.next = n.next;
                } else {
                    tailTerm = n;
                }
            }
        }
    }

    // calculateBitmapSize method extracted to CharacterClassCompiler

    /*
     *  item:       assertion               An item is either an assertion or
     *              quantatom               a quantified atom.
     *
     *  assertion:  '^'                     Assertions match beginning of string
     *                                      (or line if the class static property
     *                                      RegExp.multiline is true).
     *              '$'                     End of string (or line if the class
     *                                      static property RegExp.multiline is
     *                                      true).
     *              '\b'                    Word boundary (between \w and \W).
     *              '\B'                    Word non-boundary.
     *
     *  quantatom:  atom                    An unquantified atom.
     *              quantatom '{' n ',' m '}'
     *                                      Atom must occur between n and m times.
     *              quantatom '{' n ',' '}' Atom must occur at least n times.
     *              quantatom '{' n '}'     Atom must occur exactly n times.
     *              quantatom '*'           Zero or more times (same as {0,}).
     *              quantatom '+'           One or more times (same as {1,}).
     *              quantatom '?'           Zero or one time (same as {0,1}).
     *
     *              any of which can be optionally followed by '?' for ungreedy
     *
     *  atom:       '(' regexp ')'          A parenthesized regexp (what matched
     *                                      can be addressed using a backreference,
     *                                      see '\' n below).
     *              '.'                     Matches any char except '\n'.
     *              '[' classlist ']'       A character class.
     *              '[' '^' classlist ']'   A negated character class.
     *              '\f'                    Form Feed.
     *              '\n'                    Newline (Line Feed).
     *              '\r'                    Carriage Return.
     *              '\t'                    Horizontal Tab.
     *              '\v'                    Vertical Tab.
     *              '\d'                    A digit (same as [0-9]).
     *              '\D'                    A non-digit.
     *              '\w'                    A word character, [0-9a-z_A-Z].
     *              '\W'                    A non-word character.
     *              '\s'                    A whitespace character, [ \b\f\n\r\t\v].
     *              '\S'                    A non-whitespace character.
     *              '\' n                   A backreference to the nth (n decimal
     *                                      and positive) parenthesized expression.
     *              '\' octal               An octal escape sequence (octal must be
     *                                      two or three digits long, unless it is
     *                                      0 for the null character).
     *              '\x' hex                A hex escape (hex must be two digits).
     *              '\c' ctrl               A control character, ctrl is a letter.
     *              '\' literalatomchar     Any character except one of the above
     *                                      that follow '\' in an atom.
     *              otheratomchar           Any character not first among the other
     *                                      atom right-hand sides.
     */

    private static void doFlat(CompilerState state, char c) {
        state.result = new RENode(REOP_STRING_MATCHER);
        boolean caseInsensitive = (state.flags & JSREG_FOLD) != 0;
        state.result.stringMatcher = new StringMatcher(String.valueOf(c), caseInsensitive);
        state.result.chr = c;
        state.result.lowSurrogate = 0; /* valid range is 0xD800-0xDFFF */
        state.result.length = 1;
        state.result.flatIndex = -1;
        state.progLength += 3;
    }

    private static void doFlatSurrogatePair(CompilerState state, char high, char low) {
        // Validate surrogate pair
        if (!Character.isHighSurrogate(high) || !Character.isLowSurrogate(low)) {
            throw new IllegalArgumentException(
                    String.format("Invalid surrogate pair: U+%04X U+%04X", (int) high, (int) low));
        }
        state.result = new RENode(REOP_STRING_MATCHER);
        boolean caseInsensitive = (state.flags & JSREG_FOLD) != 0;
        String literal = new String(new char[] {high, low});
        state.result.stringMatcher = new StringMatcher(literal, caseInsensitive);
        state.result.chr = high;
        state.result.lowSurrogate = low;
        state.result.length = 2;
        state.result.flatIndex = -1;
        state.progLength += 5;
    }

    private static int getDecimalValue(char c, CompilerState state, String overflowMessageId) {
        boolean overflow = false;
        int start = state.cp;
        char[] src = state.cpbegin;
        int value = c - '0';
        for (; state.cp != state.cpend; ++state.cp) {
            c = src[state.cp];
            if (!isDigit(c)) {
                break;
            }
            if (!overflow) {
                int digit = c - '0';
                // Check for overflow BEFORE multiplication to prevent integer wraparound
                if (value > (65535 - digit) / 10) {
                    overflow = true;
                    value = 65535;
                } else {
                    value = value * 10 + digit;
                }
            }
        }
        if (overflow) {
            reportError(overflowMessageId, String.valueOf(src, start, state.cp - start));
        }
        return value;
    }

    private static RENode reverseNodeList(RENode head) {
        RENode prev = null;
        RENode node = head;
        while (node != null) {
            /* Don't reverse lookahead assertions. Lookbehind assertions should already have been reversed */
            if (node.kid != null
                    && node.op != REOP_ASSERT
                    && node.op != REOP_ASSERT_NOT
                    && node.op != REOP_ASSERTBACK
                    && node.op != REOP_ASSERTBACK_NOT) {
                node.kid = reverseNodeList(node.kid);
            }
            RENode next = node.next;
            node.next = prev;
            prev = node;
            node = next;
        }
        return prev;
    }

    private static boolean extractCaptureGroupName(CompilerState state, StringBuilder builder) {
        char[] src = state.cpbegin;
        int termBegin = state.cp;
        boolean isStart = true;
        int segmentStart = 0;
        int segmentLength = 0;

        if (state.cp >= state.cpend) {
            return false;
        }

        if (src[state.cp++] != '<') {
            state.cp = termBegin;
            return false;
        }

        while (state.cp < state.cpend && src[state.cp] != '>') {
            int codePoint;

            if (state.cp + 1 < state.cpend && src[state.cp] == '\\' && src[state.cp + 1] == 'u') {
                state.cp = state.cp + 2;
                int n =
                        readRegExpUnicodeEscapeSequence(
                                state, new ParserParameters(false, true, false));
                if (n == -1) {
                    reportError("msg.invalid.escape", "");
                    state.cp = termBegin;
                    return false;
                }
                codePoint = n;
                // if we have a src segment going on, add it to the builder along with the codepoint
                // if not, just add this codepoint to the builder
                if (segmentLength != 0) {
                    builder.append(src, segmentStart, segmentLength);
                    segmentLength = 0;
                }
                builder.appendCodePoint(codePoint);
            } else {
                codePoint = Character.codePointAt(src, state.cp);
                if (segmentLength != 0) {
                    segmentLength += Character.charCount(codePoint);
                } else {
                    segmentStart = state.cp;
                    segmentLength = Character.charCount(codePoint);
                }
                state.cp += Character.charCount(codePoint);
            }

            if (!(codePoint == '$'
                    || (isStart && codePoint == '_')
                    || (isStart && Character.isUnicodeIdentifierStart(codePoint))
                    || (!isStart && Character.isUnicodeIdentifierPart(codePoint)))) {
                state.cp = termBegin;
                return false;
            }
            isStart = false;
        }

        if (state.cp >= state.cpend || src[state.cp++] != '>') {
            state.cp = termBegin;
            return false;
        }

        if (segmentLength != 0) builder.append(src, segmentStart, segmentLength);

        return true;
    }

    // assume that the cp points to a decimal digit.
    // consumes as many octal characters as possible such that the resulting number is <= 0xFF
    private static boolean parseLegacyOctalEscapeSequence(CompilerState state) {
        char[] src = state.cpbegin;
        int num;
        int nDigits;
        char c = src[state.cp];

        if (c < '0' || c > '7') {
            return false;
        }
        num = c - '0';
        state.cp++;
        nDigits = 1;

        while (nDigits < 3 && num < 040 && state.cp < state.cpend) {
            c = src[state.cp];
            nDigits++;
            if ((c >= '0') && (c <= '7')) {
                state.cp++;
                num = 8 * num + (c - '0');
            } else break;
        }
        c = (char) num;
        doFlat(state, c);
        return true;
    }

    private static boolean parseIdentityEscape(CompilerState state, ParserParameters params) {
        // k is not a valid identity escape when named capture groups are enabled
        char[] src = state.cpbegin;

        if (state.cp < state.cpend) {
            char c = src[state.cp++];

            if (params.isUnicodeMode()) {
                switch (c) {
                    case '^':
                    case '$':
                    case '\\':
                    case '.':
                    case '*':
                    case '+':
                    case '?':
                    case '(':
                    case ')':
                    case '[':
                    case ']':
                    case '{':
                    case '}':
                    case '|':
                    case '/':
                        {
                            doFlat(state, c);
                            state.result.flatIndex = state.cp - 1;
                            return true;
                        }
                    case '8':
                    case '9':
                    default:
                        state.cp--;
                        return false;
                }
            } else {
                if ('c' != c) {
                    if (params.hasNamedCaptureGroups()) {
                        if ('k' != c) {
                            doFlat(state, c);
                            state.result.flatIndex = state.cp - 1;
                            return true;
                        }
                    } else {
                        doFlat(state, c);
                        state.result.flatIndex = state.cp - 1;
                        return true;
                    }
                }
            }
        }

        state.cp--;
        return false;
    }

    // returns -1 on failure
    // when it succeeds, it advances state.cp
    private static int readNHexDigits(CompilerState state, int nDigits, ParserParameters params) {
        int termBegin = state.cp;
        int n = 0;

        for (int i = 0; i < nDigits; i++) {
            if (state.cp >= state.cpend) {
                // in unicode mode, we need exact number of digits
                if (params.isUnicodeMode() || i == 0) {
                    state.cp = termBegin;
                    return -1;
                } else {
                    return n;
                }
            }
            char c = state.cpbegin[state.cp++];
            n = Kit.xDigitToInt(c, n);
            if (n < 0) {
                state.cp = termBegin;
                return -1;
            }
        }

        return n;
    }

    private static int parseUnicodeCodePoint(CompilerState state) {
        char[] src = state.cpbegin;
        int cpOriginal = state.cp;
        int n = 0;

        if (state.cp == state.cpend || src[state.cp++] != '{') {
            state.cp = cpOriginal;
            return -1;
        }
        if (state.cp == state.cpend || src[state.cp] == '}') {
            reportError("msg.invalid.escape", "");
        }
        while (state.cp != state.cpend) {
            if (src[state.cp] == '\\') break;

            int res = Kit.xDigitToInt(src[state.cp], n);
            if (res == -1) break;
            if (res > 0x10FFFF) {
                reportError("msg.invalid.escape", "");
            }

            n = res;
            state.cp += 1;
        }
        if (state.cp == state.cpend || src[state.cp++] != '}') {
            state.cp = cpOriginal;
            return -1;
        }

        return n;
    }

    // assume the leading 'u' has been consumed
    public static int readRegExpUnicodeEscapeSequence(
            CompilerState state, ParserParameters params) {
        char[] src = state.cpbegin;

        int n = readNHexDigits(state, 4, params);
        if (n < 0) {
            if (params.isUnicodeMode()) return parseUnicodeCodePoint(state);
        }

        if (params.isUnicodeMode()) {
            if (Character.isHighSurrogate((char) n)) {
                if (state.cp + 2 < state.cpend
                        && src[state.cp] == '\\'
                        && src[state.cp + 1] == 'u') {
                    state.cp += 2;
                    int n2 = readNHexDigits(state, 4, params);
                    if (n2 < 0) {
                        state.cp -= 2;
                    } else if (Character.isLowSurrogate((char) n2)) {
                        int codePoint = Character.toCodePoint((char) n, (char) n2);
                        return codePoint;
                    } else {
                        state.cp -= 6;
                    }
                }
            }
        }

        return n;
    }

    // assume the leading 'u' has been consumed
    public static boolean parseRegExpUnicodeEscapeSequence(
            CompilerState state, ParserParameters params) {
        int n = readRegExpUnicodeEscapeSequence(state, params);

        if (n < 0) {
            return false;
        } else if (n <= BMP_MAX_CODEPOINT) doFlat(state, (char) n);
        else {
            doFlatSurrogatePair(state, Character.highSurrogate(n), Character.lowSurrogate(n));
        }
        return true;
    }

    // only in the format \p{X} or \P{X}. Assume the \ has been consumed.
    // depending on p or P choose PROP_UPROP or PROP_UPROP_NOT.
    // X is ASCII letter, decimal or underscore
    public static boolean parseUnicodePropertyEscape(CompilerState state, ParserParameters params) {
        char[] src = state.cpbegin;
        int termBegin = state.cp;
        int contentBegin;
        int contentEnd;
        char c = src[state.cp++];
        boolean sense;

        if (c != 'p' && c != 'P') {
            state.cp = termBegin;
            return false;
        }

        sense = c == 'p';

        if (state.cp == state.cpend || src[state.cp++] != '{') {
            state.cp = termBegin;
            return false;
        }
        contentBegin = state.cp;
        while (state.cp != state.cpend) {
            c = src[state.cp++];
            if (c == '}') break;
        }

        contentEnd = state.cp;
        if (contentBegin == contentEnd) {
            state.cp = termBegin;
            return false;
        }

        String content = new String(src, contentBegin, contentEnd - contentBegin - 1);

        // ES2024: Check if this is a Property of Strings (emoji sequences)
        List<String> sequences = CharacterClassCompiler.getPropertyOfStringsSequences(content);
        if (sequences != null) {
            // This is a Property of Strings
            // ES2024 Spec: Property of Strings are only allowed inside character classes with v-flag
            // Outside character classes, they would need to be expanded to alternations,
            // which is not currently supported
            reportError(
                    "msg.property.strings.not.supported",
                    "Property of Strings \\p{" + content + "} can only be used inside character classes with the v flag");
            return false;
        }

        // Binary property - use existing lookup
        int encodedProp = UnicodeProperties.lookup(content);
        if (encodedProp == -1) {
            reportError("msg.invalid.escape", "");
            return false;
        }

        state.result = new RENode(sense ? REOP_UPROP : REOP_UPROP_NOT);
        state.result.unicodeProperty = encodedProp;
        state.progLength += 3;

        return true;
    }

    /**
     * Map character class escape character to corresponding opcode.
     *
     * @param escapeChar the escape character ('d', 'D', 'w', 'W', 's', 'S')
     * @return the opcode, or -1 if not a character class escape
     */
    static byte getCharacterClassOpcode(char escapeChar) {
        switch (escapeChar) {
            case 'd':
                return REOP_DIGIT;
            case 'D':
                return REOP_NONDIGIT;
            case 'w':
                return REOP_ALNUM;
            case 'W':
                return REOP_NONALNUM;
            case 's':
                return REOP_SPACE;
            case 'S':
                return REOP_NONSPACE;
            default:
                return -1;
        }
    }

    // Follows Annex B.1.2 of the ECMAScript specification
    static boolean parseCharacterAndCharacterClassEscape(
            CompilerState state, ParserParameters params) {
        char c;
        char[] src = state.cpbegin;
        int nDigits = 2;
        int termBegin = state.cp;

        if (state.cp >= state.cpend) {
            /* a trailing '\' is an error */
            reportError("msg.trail.backslash", "");
            return false;
        }

        c = src[state.cp++];
        switch (c) {
            case '0':
                // in non-unicode mode, if next character is a decimal digit, then it must be
                // an octal escape.
                if (state.cp < state.cpend && isDigit(src[state.cp])) {
                    if (params.isUnicodeMode()) {
                        reportError("msg.invalid.escape", "");
                        return false;
                    } else {
                        state.cp--;
                        if (!parseLegacyOctalEscapeSequence(state)) {
                            throw Kit.codeBug("parseLegacyOctalEscapeSequence failed");
                        }
                    }
                } else {
                    doFlat(state, (char) 0);
                }
                break;
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
                if (params.isUnicodeMode()) {
                    reportError("msg.invalid.escape", "");
                    return false;
                }
                state.cp--;
                if (!parseLegacyOctalEscapeSequence(state)) {
                    throw Kit.codeBug("parseLegacyOctalEscapeSequence failed");
                }
                break;
            /* Control escape */
            case 'f':
                c = 0xC;
                doFlat(state, c);
                break;
            case 'n':
                c = 0xA;
                doFlat(state, c);
                break;
            case 'r':
                c = 0xD;
                doFlat(state, c);
                break;
            case 't':
                c = 0x9;
                doFlat(state, c);
                break;
            case 'v':
                c = 0xB;
                doFlat(state, c);
                break;
            /* Control letter */
            case 'c':
                if ((state.cp < state.cpend) && isControlLetter(src[state.cp]))
                    c = (char) (src[state.cp++] & 0x1F);
                else {
                    state.cp = termBegin;
                    return false;
                }
                doFlat(state, c);
                break;
            /* UnicodeEscapeSequence */
            case 'u':
                if (!parseRegExpUnicodeEscapeSequence(state, params)) {
                    state.cp--; // rewind to the 'u'

                    if (parseIdentityEscape(state, params)) {
                        return true;
                    } else {
                        reportError("msg.invalid.escape", "");
                    }
                }
                break;
            case 'x': /* HexEscapeSequence */
                {
                    int n = readNHexDigits(state, 2, params);
                    if (n < 0) {
                        state.cp--; // rewind to the 'x'
                        if (parseIdentityEscape(state, params)) {
                            return true;
                        } else {
                            reportError("msg.invalid.escape", "");
                        }
                    }
                    doFlat(state, (char) n);
                }
                break;
            /* Character class escapes */
            case 'd':
            case 'D':
            case 's':
            case 'S':
            case 'w':
            case 'W':
                {
                    byte opcode = getCharacterClassOpcode(c);
                    state.result = new RENode(opcode);
                    state.progLength++;
                }
                break;
            case 'p':
            case 'P':
                state.cp--;
                if (!parseUnicodePropertyEscape(state, params)) {
                    reportError("msg.invalid.property", "");
                }
                break;
            /* IdentityEscape */
            default:
                state.cp--;
                return parseIdentityEscape(state, params);
        }

        return true;
    }

    // to be called when the current and next characters are '0'
    private static void parseMultipleLeadingZerosAsOctalEscape(CompilerState state) {
        char[] src = state.cpbegin;
        int num = 0;
        char c;
        reportWarning(state.cx, "msg.bad.backref", "");
        while (num < 040 && state.cp < state.cpend) {
            c = src[state.cp];
            if ((c >= '0') && (c <= '7')) {
                state.cp++;
                num = 8 * num + (c - '0');
            } else break;
        }
        c = (char) num;
        doFlat(state, c);
    }

    /**
     * Parse optional quantifier following a term.
     *
     * <p>Handles:
     *
     * <ul>
     *   <li><b>+</b> - One or more (greedy by default, non-greedy if followed by ?)
     *   <li><b>*</b> - Zero or more (greedy by default, non-greedy if followed by ?)
     *   <li><b>?</b> - Zero or one (greedy by default, non-greedy if followed by ?)
     *   <li><b>{n,m}</b> - Between n and m repetitions (greedy by default, non-greedy if followed
     *       by ?)
     * </ul>
     *
     * <p>Validates that quantifiers don't follow assertions (lookbehind, or lookahead in Unicode
     * mode).
     *
     * @param state Compiler state
     * @param params Parser parameters
     * @param term The term node that may be quantified
     * @param parenBaseCount Base paren count for this term
     * @return true if successful (with or without quantifier), false on error
     */
    private static boolean parseQuantifierIfPresent(
            CompilerState state, ParserParameters params, RENode term, int parenBaseCount) {
        // If at end of pattern, no quantifier possible
        if (state.cp == state.cpend) {
            return true;
        }

        char[] src = state.cpbegin;
        char c;
        boolean hasQ = false;

        // Check for quantifier characters
        switch (src[state.cp]) {
            case '+':
                state.result = new RENode(REOP_QUANT);
                state.result.min = 1;
                state.result.max = -1;
                /* <PLUS>, <parencount>, <parenindex>, <next> ... <ENDCHILD> */
                state.progLength += 8;
                hasQ = true;
                break;
            case '*':
                state.result = new RENode(REOP_QUANT);
                state.result.min = 0;
                state.result.max = -1;
                /* <STAR>, <parencount>, <parenindex>, <next> ... <ENDCHILD> */
                state.progLength += 8;
                hasQ = true;
                break;
            case '?':
                state.result = new RENode(REOP_QUANT);
                state.result.min = 0;
                state.result.max = 1;
                /* <OPT>, <parencount>, <parenindex>, <next> ... <ENDCHILD> */
                state.progLength += 8;
                hasQ = true;
                break;
            case '{': /* balance '}' */
                {
                    int min = 0;
                    int max = -1;
                    int leftCurl = state.cp;

                    /* For Perl etc. compatibility, if quantifier does not match
                     * \{\d+(,\d*)?\} exactly back off from it
                     * being a quantifier, and chew it up as a literal
                     * atom next time instead.
                     */

                    if (++state.cp < src.length && isDigit(c = src[state.cp])) {
                        ++state.cp;
                        min = getDecimalValue(c, state, "msg.overlarge.min");
                        if (state.cp < src.length) {
                            c = src[state.cp];
                            if (c == ',' && ++state.cp < src.length) {
                                c = src[state.cp];
                                if (isDigit(c) && ++state.cp < src.length) {
                                    max = getDecimalValue(c, state, "msg.overlarge.max");
                                    c = src[state.cp];
                                    if (min > max) {
                                        String msg =
                                                ScriptRuntime.getMessageById(
                                                        "msg.max.lt.min",
                                                        Integer.valueOf(max),
                                                        Integer.valueOf(min));
                                        throw ScriptRuntime.constructError("SyntaxError", msg);
                                    }
                                }
                            } else {
                                max = min;
                            }
                            /* balance '{' */
                            if (c == '}') {
                                state.result = new RENode(REOP_QUANT);
                                state.result.min = min;
                                state.result.max = max;
                                // QUANT, <min>, <max>, <parencount>,
                                // <parenindex>, <next> ... <ENDCHILD>
                                state.progLength += 12;
                                hasQ = true;
                            }
                        }
                    }
                    if (!hasQ) {
                        state.cp = leftCurl;
                    }
                    break;
                }
        }

        // No quantifier found - term stands alone
        if (!hasQ) return true;

        // Validate: quantifiers cannot follow lookbehind assertions
        if (term.op == REOP_ASSERTBACK || term.op == REOP_ASSERTBACK_NOT) {
            reportError("msg.bad.quant", "quantifier after lookbehind assertion");
            return false;
        }

        // Validate: in Unicode mode, quantifiers cannot follow lookahead assertions
        if (params.isUnicodeMode() && (term.op == REOP_ASSERT || term.op == REOP_ASSERT_NOT)) {
            reportError("msg.bad.quant", "quantifier after lookahead assertion in Unicode mode");
            return false;
        }

        // Set up quantifier node
        ++state.cp;
        state.result.kid = term;
        state.result.parenIndex = parenBaseCount;
        state.result.parenCount = state.parenCount - parenBaseCount;

        // Check for non-greedy modifier (?)
        if ((state.cp < state.cpend) && (src[state.cp] == '?')) {
            ++state.cp;
            state.result.greedy = false;
        } else {
            state.result.greedy = true;
        }

        return true;
    }

    /**
     * Parse grouping construct in term context.
     *
     * <p>Handles various parenthesis constructs:
     *
     * <ul>
     *   <li><b>Lookahead assertions:</b> {@code (?=...)} positive, {@code (?!...)} negative
     *   <li><b>Lookbehind assertions:</b> {@code (?<=...)} positive, {@code (?<!...)} negative
     *   <li><b>Non-capturing groups:</b> {@code (?:...)}
     *   <li><b>Named capture groups:</b> {@code (?<name>...)}
     *   <li><b>Capture groups:</b> {@code (...)}
     * </ul>
     *
     * <p>After parsing the group contents, lookbehind assertions have their node list reversed to
     * enable backward matching.
     *
     * @param state Compiler state
     * @param params Parser parameters
     * @return true if successfully parsed, false on error
     */
    private static boolean parseGroupInTerm(CompilerState state, ParserParameters params) {
        char[] src = state.cpbegin;
        char c;
        RENode result = null;

        // Check for special group types: (?=...), (?!...), (?:...)
        if (state.cp + 1 < state.cpend
                && src[state.cp] == '?'
                && ((c = src[state.cp + 1]) == '=' || c == '!' || c == ':')) {
            state.cp += 2;
            if (c == '=') {
                result = new RENode(REOP_ASSERT);
                /* ASSERT, <next>, ... ASSERTTEST */
                state.progLength += 4;
            } else if (c == '!') {
                result = new RENode(REOP_ASSERT_NOT);
                /* ASSERTNOT, <next>, ... ASSERTNOTTEST */
                state.progLength += 4;
            }
        }
        // Check for lookbehind: (?<=...), (?<!...)
        else if (state.cp + 2 < state.cpend
                && src[state.cp] == '?'
                && src[state.cp + 1] == '<'
                && ((c = src[state.cp + 2]) == '=' || c == '!')) {
            state.cp += 3;
            if (c == '=') {
                result = new RENode(REOP_ASSERTBACK);
                /* ASSERT, <next>, ... ASSERTBACKTEST */
                state.progLength += 4;
            } else { // c == '!'
                result = new RENode(REOP_ASSERTBACK_NOT);
                /* ASSERTNOT, <next>, ... ASSERTBACKNOTTEST */
                state.progLength += 4;
            }
        }
        // Regular capture group or named capture group
        else {
            result = new RENode(REOP_LPAREN);
            // Check for named capture group: (?<name>...)
            if (state.cp + 2 < state.cpend && src[state.cp] == '?' && src[state.cp + 1] == '<') {
                state.cp += 1;
                StringBuilder nameBuilder = new StringBuilder();
                if (!extractCaptureGroupName(state, nameBuilder)) {
                    reportError("msg.invalid.group.name", "");
                    return false;
                }
                result.namedCaptureGroupName = nameBuilder.toString();
                if (result.namedCaptureGroupName.isEmpty()) {
                    reportError("msg.invalid.group.name", "");
                    return false;
                }
                state.namedCaptureGroupsFound = true;
            }
            /* LPAREN, <index>, ... RPAREN, <index> */
            state.progLength += 6;
            result.parenIndex = state.parenCount++;
        }

        // Parse the group contents
        ++state.parenNesting;
        if (!parseDisjunction(state, params)) return false;

        // Expect closing parenthesis
        if (state.cp == state.cpend || src[state.cp] != ')') {
            reportError("msg.unterm.paren", "");
            return false;
        }
        ++state.cp;
        --state.parenNesting;

        // For lookbehind, reverse the node list to enable backward matching
        if (result != null) {
            if (result.op == REOP_ASSERTBACK || result.op == REOP_ASSERTBACK_NOT) {
                state.result = reverseNodeList(state.result);
            }
            result.kid = state.result;
            state.result = result;
        }

        return true;
    }

    /**
     * Parse escape sequence in term context (atom escape).
     *
     * <p>Handles: \b, \B, \1-9 (backreferences), \0 (null/octal), \k&lt;name&gt; (named
     * backreferences), and other character/class escapes.
     *
     * <p>Follows Annex B.1.2 of the ECMAScript specification.
     *
     * @param state Compiler state
     * @param params Parser parameters
     * @return true if successfully parsed, false on error
     */
    private static boolean parseEscapeInTerm(CompilerState state, ParserParameters params) {
        char[] src = state.cpbegin;

        if (state.cp >= state.cpend) {
            // a trailing '\' is an error
            reportError("msg.trail.backslash", "");
            return false;
        }

        char c = src[state.cp++];
        switch (c) {
            /* assertion escapes */
            case 'b':
                state.result = new RENode(REOP_WBDRY);
                state.progLength++;
                return true;
            case 'B':
                state.result = new RENode(REOP_WNONBDRY);
                state.progLength++;
                return true;
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                {
                    // decimal escape (backreference)
                    int termStart = state.cp - 1;
                    int num = getDecimalValue(c, state, "msg.overlarge.backref");
                    if (!params.isUnicodeMode() && num > state.backReferenceLimit) {
                        reportWarning(state.cx, "msg.bad.backref", "");
                        state.cp = termStart;
                        if (!parseCharacterAndCharacterClassEscape(state, params)) return false;
                    } else {
                        state.result = new RENode(REOP_BACKREF);
                        state.result.parenIndex = num - 1;
                        state.progLength += 3;
                        if (state.maxBackReference < num) {
                            state.maxBackReference = num;
                        }
                    }
                }
                return true;
            case '0':
                if (state.cp < state.cpend && src[state.cp] == '0') {
                    if (params.isUnicodeMode()) {
                        reportError("msg.invalid.escape", "");
                    } else {
                        /*
                         * We're deliberately violating the ECMA 5.1 specification and allow octal
                         * escapes to follow spidermonkey and general 'web reality':
                         * http://wiki.ecmascript.org/doku.php?id=harmony:regexp_match_web_reality
                         * http://wiki.ecmascript.org/doku.php?id=strawman:match_web_reality_spec
                         */

                        // follow spidermonkey and allow multiple leading zeros,
                        // e.g. let /\0000/ match the string "\0"
                        parseMultipleLeadingZerosAsOctalEscape(state);
                    }
                    return true;
                }
            /* fall through */
            default:
                state.cp--;
                if (!parseCharacterAndCharacterClassEscape(state, params)) {
                    if (c == 'k' && params.hasNamedCaptureGroups()) {
                        state.cp++;
                        StringBuilder groupNameBuilder = new StringBuilder();

                        if (extractCaptureGroupName(state, groupNameBuilder)) {
                            String groupName = groupNameBuilder.toString();
                            if (groupName.isEmpty()) {
                                reportError("msg.invalid.group.name", "");
                                return false;
                            }
                            state.result = new RENode(REOP_NAMED_BACKREF);
                            state.result.namedCaptureGroupBackRefIndex =
                                    state.namedCaptureBackRefs.size();
                            state.namedCaptureBackRefs.add(groupName);
                            // REOP_NAMED_BACKREF GROUPNAMEINDEX
                            state.progLength += 3;
                        } else reportError("msg.invalid.named.backref", "");
                    } else if ('c' == c && !params.isUnicodeMode()) { // in ExtendedAtom, when
                        // lookahead=c, parse the \\ as a
                        // literal
                        doFlat(state, '\\');
                    } else {
                        reportError("msg.invalid.escape", "");
                        return false;
                    }
                }
                return true;
        }
    }

    /**
     * Parse a term (atom with optional quantifier).
     *
     * <p>Parses atoms (characters, character classes, groups, assertions) and applies quantifiers
     * (*, +, ?, {n,m}) if present.
     *
     * @param state Compiler state with current parse position and AST
     * @param params Parser parameters (flags, mode, etc.)
     * @return true if parsing succeeded, false on error
     */
    private static boolean parseTerm(CompilerState state, ParserParameters params) {
        char[] src = state.cpbegin;
        char c = src[state.cp++];
        int parenBaseCount = state.parenCount;
        int num;
        RENode term;
        int termStart;

        switch (c) {
            /* assertions and atoms */
            case '^':
                state.result = new RENode(REOP_BOL);
                state.progLength++;
                return true;
            case '$':
                state.result = new RENode(REOP_EOL);
                state.progLength++;
                return true;
            case '\\':
                if (!parseEscapeInTerm(state, params)) {
                    return false;
                }
                break;
            case '(':
                if (!parseGroupInTerm(state, params)) {
                    return false;
                }
                break;
            case ')':
                reportError("msg.re.unmatched.right.paren", "");
                return false;
            case '[':
                ClassContents classContents =
                        CharacterClassCompiler.parseClassContents(state, params, 0);
                if (classContents == null) {
                    reportError("msg.unterm.class", "");
                    return false;
                }
                state.result = new RENode(REOP_CLASS);
                state.result.classContents = classContents;
                state.result.index = state.classCount++;
                /*
                 * Call calculateBitmapSize now as we want any errors it finds
                 * to be reported during the parse phase, not at execution.
                 */
                if (!CharacterClassCompiler.calculateBitmapSize(
                        state.flags, classContents, state.result)) return false;
                state.progLength += 3; /* CLASS, <index> */
                break;

            case '.':
                state.result = new RENode(REOP_DOT);
                state.progLength++;
                break;
            case '*':
            case '+':
            case '?':
                reportError("msg.bad.quant", String.valueOf(src[state.cp - 1]));
                return false;
            default:
                {
                    if (params.isUnicodeMode() && (c == ']' || c == '{' || c == '}'))
                        reportError("msg.lone.quantifier.bracket", "");

                    if (params.isUnicodeMode()
                            && Character.isHighSurrogate(c)
                            && state.cp < state.cpend
                            && Character.isLowSurrogate(src[state.cp])) {
                        char low = src[state.cp++];
                        doFlatSurrogatePair(state, c, low);
                    } else {
                        doFlat(state, c);
                        state.result.flatIndex = state.cp - 1;
                    }
                    break;
                }
        }

        term = state.result;
        return parseQuantifierIfPresent(state, params, term, parenBaseCount);
    }

    private static void resolveForwardJump(byte[] array, int from, int pc) {
        if (from > pc) throw Kit.codeBug();
        // Check for overflow before subtraction
        long offset = (long) pc - (long) from;
        if (offset > 0xFFFF) {
            throw Context.reportRuntimeError("Too complex regexp");
        }
        addIndex(array, from, (int) offset);
    }

    private static int getOffset(byte[] array, int pc) {
        return getIndex(array, pc);
    }

    private static int addIndex(byte[] array, int pc, int index) {
        if (index < 0) throw Kit.codeBug();
        // Maximum value that fits in 16 bits is 0xFFFF (65535)
        if (index > 0xFFFF) throw Context.reportRuntimeError("Too complex regexp");
        array[pc] = (byte) (index >> 8);
        array[pc + 1] = (byte) index;
        return pc + 2;
    }

    static int getIndex(byte[] array, int pc) {
        return ((array[pc] & 0xFF) << 8) | (array[pc + 1] & 0xFF);
    }

    static final int INDEX_LEN = 2;
    private static final int MAX_RECURSION_DEPTH = 1000;

    private static int emitREBytecode(CompilerState state, RECompiled re, int pc, RENode t) {
        return emitREBytecode(state, re, pc, t, 0);
    }

    private static int emitREBytecode(
            CompilerState state, RECompiled re, int pc, RENode t, int depth) {
        // Check recursion depth to prevent stack overflow
        if (depth > MAX_RECURSION_DEPTH) {
            throw Context.reportRuntimeError("regexp too complex (excessive nesting)");
        }

        RENode nextAlt;
        int nextAltFixup, nextTermFixup;
        byte[] program = re.program;

        while (t != null) {
            // Check for bytecode overflow before writing
            if (pc >= program.length) {
                throw Context.reportRuntimeError("regexp bytecode overflow");
            }
            program[pc++] = t.op;
            switch (t.op) {
                case REOP_EMPTY:
                    --pc;
                    break;
                case REOP_ALTPREREQ:
                case REOP_ALTPREREQi:
                case REOP_ALTPREREQ2:
                    boolean ignoreCase = t.op == REOP_ALTPREREQi;
                    addIndex(program, pc, ignoreCase ? Character.toUpperCase(t.chr) : t.chr);
                    pc += INDEX_LEN;
                    addIndex(
                            program,
                            pc,
                            ignoreCase ? Character.toUpperCase((char) t.index) : t.index);
                    pc += INDEX_LEN;
                // fall through to REOP_ALT
                case REOP_ALT:
                    nextAlt = t.kid2;
                    nextAltFixup = pc; /* address of next alternate */
                    pc += INDEX_LEN;
                    pc = emitREBytecode(state, re, pc, t.kid, depth + 1);
                    program[pc++] = REOP_JUMP;
                    nextTermFixup = pc; /* address of following term */
                    pc += INDEX_LEN;
                    resolveForwardJump(program, nextAltFixup, pc);
                    pc = emitREBytecode(state, re, pc, nextAlt, depth + 1);

                    program[pc++] = REOP_JUMP;
                    nextAltFixup = pc;
                    pc += INDEX_LEN;

                    resolveForwardJump(program, nextTermFixup, pc);
                    resolveForwardJump(program, nextAltFixup, pc);
                    break;
                case REOP_STRING_MATCHER:
                    // Universal string matcher - replaces all FLAT* opcodes
                    if (re.stringMatchers == null) {
                        re.stringMatchers = new ArrayList<>();
                    }
                    int stringMatcherIndex = re.stringMatchers.size();
                    re.stringMatchers.add(t.stringMatcher);
                    program[pc - 1] = REOP_STRING_MATCHER;
                    pc = addIndex(program, pc, stringMatcherIndex);
                    break;
                case REOP_LPAREN:
                    pc = addIndex(program, pc, t.parenIndex);
                    pc = emitREBytecode(state, re, pc, t.kid, depth + 1);
                    program[pc++] = REOP_RPAREN;
                    pc = addIndex(program, pc, t.parenIndex);
                    break;
                case REOP_BACKREF:
                    pc = addIndex(program, pc, t.parenIndex);
                    break;
                case REOP_NAMED_BACKREF:
                    {
                        String backRefName;
                        if (re.namedBackRefs == null) {
                            reportError("msg.invalid.named.backref", "");
                            return pc;
                        }

                        try {
                            backRefName = re.namedBackRefs.get(t.namedCaptureGroupBackRefIndex);
                        } catch (IndexOutOfBoundsException ioobe) {
                            Kit.codeBug(
                                    "emitREBytecode: namedBackRefIndex("
                                            + t.namedCaptureGroupBackRefIndex
                                            + ") out of bounds");
                            return pc;
                        }

                        List<Integer> indices = re.namedCaptureGroups.get(backRefName);
                        if (indices == null) {
                            reportError("msg.invalid.named.backref", "");
                            return pc;
                        }

                        if (indices.size() == 1) { // optimization for unique backrefs
                            program[pc - 1] = REOP_BACKREF;
                            pc = addIndex(program, pc, indices.get(0));
                        } else {
                            // backref doesn't have a unique parenIndex
                            pc = addIndex(program, pc, t.namedCaptureGroupBackRefIndex);
                        }
                    }
                    break;
                case REOP_ASSERT:
                case REOP_ASSERTBACK:
                    nextTermFixup = pc;
                    pc += INDEX_LEN;
                    pc = emitREBytecode(state, re, pc, t.kid, depth + 1);
                    program[pc++] = t.op == REOP_ASSERT ? REOP_ASSERTTEST : REOP_ASSERTBACKTEST;
                    resolveForwardJump(program, nextTermFixup, pc);
                    break;
                case REOP_ASSERT_NOT:
                case REOP_ASSERTBACK_NOT:
                    nextTermFixup = pc;
                    pc += INDEX_LEN;
                    pc = emitREBytecode(state, re, pc, t.kid, depth + 1);
                    program[pc++] =
                            t.op == REOP_ASSERT_NOT ? REOP_ASSERTNOTTEST : REOP_ASSERTBACKNOTTEST;
                    resolveForwardJump(program, nextTermFixup, pc);
                    break;
                case REOP_QUANT:
                    if ((t.min == 0) && (t.max == -1))
                        program[pc - 1] = t.greedy ? REOP_STAR : REOP_MINIMALSTAR;
                    else if ((t.min == 0) && (t.max == 1))
                        program[pc - 1] = t.greedy ? REOP_OPT : REOP_MINIMALOPT;
                    else if ((t.min == 1) && (t.max == -1))
                        program[pc - 1] = t.greedy ? REOP_PLUS : REOP_MINIMALPLUS;
                    else {
                        if (!t.greedy) program[pc - 1] = REOP_MINIMALQUANT;
                        pc = addIndex(program, pc, t.min);
                        // max can be -1 which addIndex does not accept
                        pc = addIndex(program, pc, t.max + 1);
                    }
                    pc = addIndex(program, pc, t.parenCount);
                    pc = addIndex(program, pc, t.parenIndex);
                    nextTermFixup = pc;
                    pc += INDEX_LEN;
                    pc = emitREBytecode(state, re, pc, t.kid, depth + 1);
                    program[pc++] = REOP_ENDCHILD;
                    resolveForwardJump(program, nextTermFixup, pc);
                    break;
                case REOP_CLASS:
                    if (!t.classContents.sense) program[pc - 1] = REOP_NCLASS;
                    pc = addIndex(program, pc, t.index);
                    re.classList[t.index] = new RECharSet(t.classContents, t.bmsize);
                    // Pre-build RECharSet objects for set operation operands
                    CharacterClassCompiler.buildOperandCharSets(t.classContents, t.bmsize);
                    break;
                case REOP_UPROP:
                case REOP_UPROP_NOT:
                    pc = addIndex(program, pc, t.unicodeProperty);
                    break;
                default:
                    break;
            }
            t = t.next;
        }
        return pc;
    }

    private static void pushProgState(
            REGlobalData gData,
            int min,
            int max,
            int cp,
            boolean matchBackward,
            REBackTrackData backTrackLastToSave,
            int continuationOp,
            int continuationPc) {
        gData.stateStackTop =
                new REProgState(
                        gData.stateStackTop,
                        min,
                        max,
                        cp,
                        backTrackLastToSave,
                        matchBackward,
                        continuationOp,
                        continuationPc);
    }

    private static REProgState popProgState(REGlobalData gData) {
        REProgState state = gData.stateStackTop;
        gData.stateStackTop = state.previous;
        return state;
    }

    private static void pushBackTrackState(REGlobalData gData, byte op, int pc) {
        REProgState state = gData.stateStackTop;
        gData.backTrackStackTop =
                new REBackTrackData(
                        gData, op, pc, gData.cp, state.continuationOp, state.continuationPc);
    }

    private static void pushBackTrackState(
            REGlobalData gData, byte op, int pc, int cp, int continuationOp, int continuationPc) {
        gData.backTrackStackTop =
                new REBackTrackData(gData, op, pc, cp, continuationOp, continuationPc);
    }

    /*
    1. Evaluate DecimalEscape to obtain an EscapeValue E.
    2. If E is not a character then go to step 6.
    3. Let ch be E's character.
    4. Let A be a one-element RECharSet containing the character ch.
    5. Call CharacterSetMatcher(A, false) and return its Matcher result.
    6. E must be an integer. Let n be that integer.
    7. If n=0 or n>NCapturingParens then throw a SyntaxError exception.
    8. Return an internal Matcher closure that takes two arguments, a State x
       and a Continuation c, and performs the following:
        1. Let cap be x's captures internal array.
        2. Let s be cap[n].
        3. If s is undefined, then call c(x) and return its result.
        4. Let e be x's endIndex.
        5. Let len be s's length.
        6. Let f be e+len.
        7. If f>InputLength, return failure.
        8. If there exists an integer i between 0 (inclusive) and len (exclusive)
           such that Canonicalize(s[i]) is not the same character as
           Canonicalize(Input [e+i]), then return failure.
        9. Let y be the State (f, cap).
        10. Call c(y) and return its result.
    */
    static boolean backrefMatcher(
            REGlobalData gData, int parenIndex, String input, int end, boolean matchBackward) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        int len;
        int i;
        if (gData.parens == null || parenIndex >= gData.parens.length) return false;
        int parenContent = gData.parensIndex(parenIndex);
        if (parenContent == -1) return true;

        len = gData.parensLength(parenIndex);

        // The capture is always "forward", i.e., in
        // the input order
        if (matchBackward) {
            if ((gData.cp - len) < 0) return false;

            if ((gData.regexp.flags & JSREG_FOLD) != 0) {
                // start from (cp - len) on the left and go to cp - 1 on the right
                for (i = 0; i < len; i++) {
                    char c1 = input.charAt(parenContent + i);
                    char c2 = input.charAt(gData.cp + i - len);
                    if (c1 != c2 && Character.toUpperCase(c1) != Character.toUpperCase(c2))
                        return false;
                }
            } else if (!input.regionMatches(parenContent, input, gData.cp - len, len)) {
                return false;
            }
            gData.cp -= len;
        } else {
            if ((gData.cp + len) > end) return false;

            if ((gData.regexp.flags & JSREG_FOLD) != 0) {
                for (i = 0; i < len; i++) {
                    char c1 = input.charAt(parenContent + i);
                    char c2 = input.charAt(gData.cp + i);
                    if (c1 != c2 && Character.toUpperCase(c1) != Character.toUpperCase(c2))
                        return false;
                }
            } else if (!input.regionMatches(parenContent, input, gData.cp, len)) {
                return false;
            }
            gData.cp += len;
        }
        return true;
    }

    /* Add a single character to the RECharSet */
    /*
     *   Initialize the character set if it is the first call.
     *   Test the bit - if the ^ flag was specified, non-inclusion is a success
     */
    /**
     * Check if any string literal in the character class matches at the current position. Returns
     * the length of the matched string, or -1 if no match. Zero-length matches are valid.
     *
     * @param charSet The character class containing string literals
     * @param input The input string to match against
     * @param position The position in the input where the match should occur. For forward matching,
     *     this is the start position. For backward matching (lookbehind), this is the end position
     *     (exclusive).
     * @param matchBackward true for lookbehind matching (match backwards from position), false for
     *     normal forward matching
     * @return The length of the matched string literal, or -1 if no match
     */
    public static int stringLiteralMatcher(
            RECharSet charSet, CharSequence input, int position, boolean matchBackward) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        if (charSet.classContents == null) {
            return -1;
        }

        // Return early if no string content to match
        if (charSet.classContents.stringLiterals.isEmpty()
                && charSet.classContents.stringMatchers.isEmpty()) {
            return -1;
        }

        // Try to match each string literal/matcher, return longest match for greedy matching
        int maxMatch = -1;

        // Try string literals (from \q{} syntax)
        String inputStr = input.toString();
        for (String literal : charSet.classContents.stringLiterals) {
            boolean matches;
            if (matchBackward) {
                // For lookbehind, match backwards from position
                int startPos = position - literal.length();
                matches =
                        startPos >= 0
                                && inputStr.regionMatches(startPos, literal, 0, literal.length());
            } else {
                // For normal matching, match forwards from position
                matches =
                        position + literal.length() <= input.length()
                                && inputStr.regionMatches(position, literal, 0, literal.length());
            }

            if (matches && (maxMatch == -1 || literal.length() > maxMatch)) {
                maxMatch = literal.length();
            }
        }

        // Try string matchers (from Property of Strings - ES2024 emoji sequences)
        for (StringMatcher matcher : charSet.classContents.stringMatchers) {
            int matchLen = matcher.match(input, position, matchBackward);
            if (matchLen > 0 && (maxMatch == -1 || matchLen > maxMatch)) {
                maxMatch = matchLen;
            }
        }

        return maxMatch;
    }

    public static boolean classMatcher(REGlobalData gData, RECharSet charSet, int codePoint) {
        // Validate codepoint is in valid Unicode range
        if (codePoint < 0 || codePoint > Character.MAX_CODE_POINT) {
            return false;
        }
        charSet.ensureBuilt(gData);

        // Fast path: check cache for set operations (ES2024 v-flag)
        if (!charSet.classContents.setOperations.isEmpty()) {
            if (charSet.setOpCache != null) {
                Boolean cached = charSet.setOpCache.get(codePoint);
                if (cached != null) {
                    return cached;
                }
            }
        }

        // Unified Unicode path - no special-casing ASCII
        boolean matches = false;

        if (codePoint <= BMP_MAX_CODEPOINT) {
            int byteIndex = codePoint >> BIT_SHIFT_FOR_BYTE_INDEX;
            if (!(charSet.length == 0
                    || codePoint >= charSet.length
                    || (charSet.bits[byteIndex] & (1 << (codePoint & BYTE_BIT_MASK))) == 0)) {
                matches = true;
            }
        }

        // Unified Unicode storage - check codepoints list (for all codepoints not in bitmap)
        if (!matches && charSet.classContents.codePoints.contains(codePoint)) {
            matches = true;
        }

        // Unified Unicode storage - check ranges list (for all ranges not in bitmap)
        if (!matches) {
            matches =
                    charSet.classContents.anyRangeMatches(
                            (start, end) -> codePoint >= start && codePoint <= end);
        }

        if (!matches) {
            for (int encodedProp : charSet.unicodeProps) {
                if (UnicodeProperties.hasProperty(encodedProp, codePoint)) {
                    matches = true;
                    break;
                }
            }
        }

        if (!matches) {
            for (int encodedProp : charSet.negUnicodeProps) {
                if (!UnicodeProperties.hasProperty(encodedProp, codePoint)) {
                    matches = true;
                    break;
                }
            }
        }

        // Apply set operations for 'v' flag
        if (!charSet.classContents.setOperations.isEmpty()) {
            for (SetOperation op : charSet.classContents.setOperations) {
                if (op.getType() == SetOperation.Type.SUBTRACT) {
                    // Remove from result if in subtract operand
                    boolean operandMatches = classMatcher(gData, op.getOperandCharSet(), codePoint);
                    if (operandMatches) {
                        matches = false;
                    }
                } else if (op.getType() == SetOperation.Type.INTERSECT) {
                    // Keep only if also in intersect operand
                    // Optimization: skip check if matches is already false
                    if (matches) {
                        boolean operandMatches =
                                classMatcher(gData, op.getOperandCharSet(), codePoint);
                        matches = operandMatches;
                    }
                }
            }

            // Cache the result for set operations (bounded LRU to prevent memory leak)
            boolean finalResult = matches == charSet.classContents.sense;
            if (charSet.setOpCache == null) {
                synchronized (charSet) {
                    if (charSet.setOpCache == null) {
                        charSet.setOpCache = createBoundedCache(1000);
                    }
                }
            }
            charSet.setOpCache.put(codePoint, finalResult);
            return finalResult;
        }

        return matches == charSet.classContents.sense;
    }

    /**
     * Create a bounded LRU cache for set operation results.
     *
     * <p>Prevents memory leak by limiting cache size. ES2024 v-flag character classes with set
     * operations (intersection, subtraction) cache match results per codepoint. Without bounds,
     * this could grow to 1.1M entries (full Unicode range). LRU eviction keeps memory usage under
     * control.
     *
     * @param maxSize Maximum number of entries before LRU eviction
     * @return Thread-safe bounded LRU cache
     */
    private static java.util.Map<Integer, Boolean> createBoundedCache(final int maxSize) {
        return java.util.Collections.synchronizedMap(
                new java.util.LinkedHashMap<Integer, Boolean>(32, 0.75f, true) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean removeEldestEntry(
                            java.util.Map.Entry<Integer, Boolean> eldest) {
                        return size() > maxSize;
                    }
                });
    }

    private static boolean reopIsSimple(int op) {
        return op >= REOP_SIMPLE_START && op <= REOP_SIMPLE_END;
    }

    /**
     * Execute a simple opcode using OpcodeTable.
     *
     * <p>This replaces the old simpleMatch method with a cleaner table-driven approach.
     *
     * @param gData global regex data
     * @param input input string
     * @param op opcode to execute
     * @param program bytecode program
     * @param pc current program counter
     * @param end end position in input
     * @param updatecp whether to update gData.cp on success
     * @param matchBackward whether matching backwards
     * @return new pc if match succeeds, -1 if it fails
     */
    private static int executeSimpleOpcode(
            REGlobalData gData,
            String input,
            int op,
            byte[] program,
            int pc,
            int end,
            boolean updatecp,
            boolean matchBackward) {
        int startcp = gData.cp;

        // Modern lambda-based dispatch via OpcodeTable
        ExecutionContext ctx =
                new ExecutionContext(null, gData, input, end, program, pc, matchBackward);
        ExecutionContext.Result execResult = OpcodeTable.execute(ctx, (byte) op);

        if (execResult.matched()) {
            if (!updatecp) gData.cp = startcp;
            return execResult.nextPc();
        }

        gData.cp = startcp;
        return -1;
    }

    private static boolean executeREBytecode(
            Context cx, REGlobalData gData, String input, int end) {
        int pc = 0;
        byte[] program = gData.regexp.program;
        int continuationOp = REOP_END;
        int continuationPc = 0;
        boolean result = false;
        boolean matchBackward = false; /* match forward by default */

        int op = program[pc++];

        /*
         * If the first node is a simple match, step the index into the string
         * until that match is made, or fail if it can't be found at all.
         */
        if (gData.regexp.anchorCodePoint < 0 && reopIsSimple(op)) {
            boolean anchor = false;
            // Add instruction counting for ReDoS protection in anchor loop
            final boolean instructionCountingAnchor = cx.getInstructionObserverThreshold() != 0;
            while (gData.cp <= end) {
                if (instructionCountingAnchor) {
                    ScriptRuntime.addInstructionCount(cx, 1);
                }
                int match = executeSimpleOpcode(gData, input, op, program, pc, end, true, false);
                if (match < 0) {
                    if ((gData.regexp.flags & JSREG_STICKY) != 0) {
                        return false;
                    }
                } else {
                    anchor = true;
                    pc = match; /* accept skip to next opcode */
                    op = program[pc++];
                    break;
                }

                if (isUnicodeMode(gData.regexp.flags) && gData.cp < end) {
                    int toSkip = Character.charCount(input.codePointAt(gData.cp));
                    gData.cp += toSkip;
                    gData.skipped += toSkip;
                } else {
                    gData.cp++;
                    gData.skipped++;
                }
            }
            if (!anchor) return false;
        }

        final boolean instructionCounting = cx.getInstructionObserverThreshold() != 0;
        for (; ; ) {
            if (instructionCounting) {
                ScriptRuntime.addInstructionCount(cx, 5);
            }

            if (reopIsSimple(op)) {
                int match =
                        executeSimpleOpcode(
                                gData, input, op, program, pc, end, true, matchBackward);
                result = match >= 0;
                if (result) pc = match; /* accept skip to next opcode */
            } else {
                switchStatement:
                switch (op) {
                    case REOP_ALTPREREQ:
                    case REOP_ALTPREREQi:
                    case REOP_ALTPREREQ2:
                        {
                            char matchCh1 = (char) getIndex(program, pc);
                            pc += INDEX_LEN;
                            char matchCh2 = (char) getIndex(program, pc);
                            pc += INDEX_LEN;

                            final int cpToMatch = gData.cp + (matchBackward ? -1 : 0);
                            final boolean cpInBounds = cpToMatch >= 0 && cpToMatch < end;

                            if (!cpInBounds) {
                                result = false;
                                break;
                            }
                            char c = input.charAt(cpToMatch);
                            if (op == REOP_ALTPREREQ2) {
                                if (c != matchCh1
                                        && !classMatcher(
                                                gData, gData.regexp.classList[matchCh2], c)) {
                                    result = false;
                                    break;
                                }
                            } else {
                                if (op == REOP_ALTPREREQi) c = Character.toUpperCase(c);
                                if (c != matchCh1 && c != matchCh2) {
                                    result = false;
                                    break;
                                }
                            }
                        }
                    /* else false thru... */
                    // fall through
                    case REOP_ALT:
                        {
                            int nextpc = pc + getOffset(program, pc);
                            pc += INDEX_LEN;
                            op = program[pc++];
                            int startcp = gData.cp;
                            if (reopIsSimple(op)) {
                                int match =
                                        executeSimpleOpcode(
                                                gData,
                                                input,
                                                op,
                                                program,
                                                pc,
                                                end,
                                                true,
                                                matchBackward);
                                if (match < 0) {
                                    op = program[nextpc++];
                                    pc = nextpc;
                                    continue;
                                }
                                result = true;
                                pc = match;
                                op = program[pc++];
                            }
                            byte nextop = program[nextpc++];
                            pushBackTrackState(
                                    gData, nextop, nextpc, startcp, continuationOp, continuationPc);
                        }
                        continue;

                    case REOP_JUMP:
                        {
                            int offset = getOffset(program, pc);
                            pc += offset;
                            op = program[pc++];
                        }
                        continue;

                    case REOP_LPAREN:
                        {
                            int parenIndex = getIndex(program, pc);
                            pc += INDEX_LEN;
                            gData.setParens(parenIndex, gData.cp, 0);
                            op = program[pc++];
                        }
                        continue;
                    case REOP_RPAREN:
                        {
                            int parenIndex = getIndex(program, pc);
                            pc += INDEX_LEN;
                            int cap_index = gData.parensIndex(parenIndex);
                            if (matchBackward)
                                // paren content is captured backwards. Therefore we
                                // reverse the capture here
                                gData.setParens(parenIndex, gData.cp, cap_index - gData.cp);
                            else gData.setParens(parenIndex, cap_index, gData.cp - cap_index);
                            op = program[pc++];
                        }
                        continue;
                    case REOP_ASSERTBACK:
                        {
                            int nextpc =
                                    pc + getIndex(program, pc); /* start of term after ASSERT */
                            pc += INDEX_LEN; /* start of ASSERT child */
                            op = program[pc++];

                            if (reopIsSimple(op)
                                    && executeSimpleOpcode(
                                                    gData, input, op, program, pc, end, false, true)
                                            < 0) {
                                result = false;
                                break;
                            }

                            pushProgState(
                                    gData,
                                    0,
                                    0,
                                    gData.cp,
                                    matchBackward,
                                    gData.backTrackStackTop,
                                    continuationOp,
                                    continuationPc);

                            pushBackTrackState(
                                    gData,
                                    REOP_ASSERTBACKTEST,
                                    nextpc,
                                    gData.cp,
                                    continuationOp,
                                    continuationPc);
                            matchBackward = true;
                        }
                        continue;
                    case REOP_ASSERTBACK_NOT:
                        {
                            int nextpc =
                                    pc + getIndex(program, pc); /* start of term after ASSERT */
                            pc += INDEX_LEN; /* start of ASSERT child */
                            op = program[pc++];

                            if (reopIsSimple(op)) {
                                int match =
                                        executeSimpleOpcode(
                                                gData, input, op, program, pc, end, false, true);
                                if (match >= 0 && program[match] == REOP_ASSERTBACKNOTTEST) {
                                    result = false;
                                    break;
                                }
                            }

                            pushProgState(
                                    gData,
                                    0,
                                    0,
                                    gData.cp,
                                    matchBackward,
                                    gData.backTrackStackTop,
                                    continuationOp,
                                    continuationPc);

                            pushBackTrackState(
                                    gData,
                                    REOP_ASSERTBACKNOTTEST,
                                    nextpc,
                                    gData.cp,
                                    continuationOp,
                                    continuationPc);
                            matchBackward = true;
                        }
                        continue;
                    case REOP_ASSERT:
                        {
                            int nextpc =
                                    pc + getIndex(program, pc); /* start of term after ASSERT */
                            pc += INDEX_LEN; /* start of ASSERT child */
                            op = program[pc++];
                            if (reopIsSimple(op)
                                    && executeSimpleOpcode(
                                                    gData, input, op, program, pc, end, false,
                                                    false)
                                            < 0) {
                                result = false;
                                break;
                            }
                            pushProgState(
                                    gData,
                                    0,
                                    0,
                                    gData.cp,
                                    matchBackward,
                                    gData.backTrackStackTop,
                                    continuationOp,
                                    continuationPc);
                            pushBackTrackState(gData, REOP_ASSERTTEST, nextpc);
                            matchBackward = false;
                        }
                        continue;
                    case REOP_ASSERT_NOT:
                        {
                            int nextpc =
                                    pc + getIndex(program, pc); /* start of term after ASSERT */
                            pc += INDEX_LEN; /* start of ASSERT child */
                            op = program[pc++];
                            if (reopIsSimple(op)) {
                                int match =
                                        executeSimpleOpcode(
                                                gData, input, op, program, pc, end, false, false);
                                if (match >= 0 && program[match] == REOP_ASSERTNOTTEST) {
                                    result = false;
                                    break;
                                }
                            }
                            pushProgState(
                                    gData,
                                    0,
                                    0,
                                    gData.cp,
                                    matchBackward,
                                    gData.backTrackStackTop,
                                    continuationOp,
                                    continuationPc);
                            pushBackTrackState(gData, REOP_ASSERTNOTTEST, nextpc);
                            matchBackward = false;
                        }
                        continue;

                    case REOP_ASSERTTEST:
                    case REOP_ASSERTBACKTEST:
                    case REOP_ASSERTNOTTEST:
                    case REOP_ASSERTBACKNOTTEST:
                        {
                            REProgState state = popProgState(gData);
                            gData.cp = state.index;
                            gData.backTrackStackTop = state.backTrack;
                            matchBackward = state.matchBackward;
                            continuationPc = state.continuationPc;
                            continuationOp = state.continuationOp;
                            if (op == REOP_ASSERTNOTTEST || op == REOP_ASSERTBACKNOTTEST) {
                                result = !result;
                            }
                        }
                        break;

                    case REOP_STAR:
                    case REOP_PLUS:
                    case REOP_OPT:
                    case REOP_QUANT:
                    case REOP_MINIMALSTAR:
                    case REOP_MINIMALPLUS:
                    case REOP_MINIMALOPT:
                    case REOP_MINIMALQUANT:
                        {
                            int min, max;
                            boolean greedy = false;
                            switch (op) {
                                case REOP_STAR:
                                    greedy = true;
                                // fallthrough
                                case REOP_MINIMALSTAR:
                                    min = 0;
                                    max = -1;
                                    break;
                                case REOP_PLUS:
                                    greedy = true;
                                // fallthrough
                                case REOP_MINIMALPLUS:
                                    min = 1;
                                    max = -1;
                                    break;
                                case REOP_OPT:
                                    greedy = true;
                                // fallthrough
                                case REOP_MINIMALOPT:
                                    min = 0;
                                    max = 1;
                                    break;
                                case REOP_QUANT:
                                    greedy = true;
                                // fallthrough
                                case REOP_MINIMALQUANT:
                                    min = getOffset(program, pc);
                                    pc += INDEX_LEN;
                                    // See comments in emitREBytecode for " - 1" reason
                                    max = getOffset(program, pc) - 1;
                                    pc += INDEX_LEN;
                                    break;
                                default:
                                    throw Kit.codeBug();
                            }
                            pushProgState(
                                    gData,
                                    min,
                                    max,
                                    gData.cp,
                                    matchBackward,
                                    null,
                                    continuationOp,
                                    continuationPc);
                            if (greedy) {
                                pushBackTrackState(gData, REOP_REPEAT, pc);
                                continuationOp = REOP_REPEAT;
                                continuationPc = pc;
                                /* Step over <parencount>, <parenindex> & <next> */
                                pc += 3 * INDEX_LEN;
                            } else {
                                if (min != 0) {
                                    continuationOp = REOP_MINIMALREPEAT;
                                    continuationPc = pc;
                                    /* <parencount> <parenindex> & <next> */
                                    pc += 3 * INDEX_LEN;
                                } else {
                                    pushBackTrackState(gData, REOP_MINIMALREPEAT, pc);
                                    popProgState(gData);
                                    pc += 2 * INDEX_LEN; // <parencount> & <parenindex>
                                    pc = pc + getOffset(program, pc);
                                }
                            }
                            op = program[pc++];
                        }
                        continue;

                    case REOP_ENDCHILD: /* marks the end of a quantifier child */
                        // If we have not gotten a result here, it is because of an
                        // empty match.  Do the same thing REOP_EMPTY would do.
                        result = true;
                        // Use the current continuation.
                        pc = continuationPc;
                        op = continuationOp;
                        continue;

                    case REOP_REPEAT:
                        {
                            int nextpc, nextop;
                            do {
                                REProgState state = popProgState(gData);
                                if (!result) {
                                    // Failed, see if we have enough children.
                                    if (state.min == 0) result = true;
                                    continuationPc = state.continuationPc;
                                    continuationOp = state.continuationOp;
                                    pc += 2 * INDEX_LEN; /* <parencount> & <parenindex> */
                                    pc += getOffset(program, pc);
                                    break switchStatement;
                                }
                                if (state.min == 0 && (gData.cp == state.index || state.max == 0)) {
                                    // matched an empty string or an {0} quantifier, that'll get us
                                    // nowhere
                                    result = false;
                                    continuationPc = state.continuationPc;
                                    continuationOp = state.continuationOp;
                                    pc += 2 * INDEX_LEN;
                                    pc += getOffset(program, pc);
                                    break switchStatement;
                                }
                                int new_min = state.min, new_max = state.max;
                                if (new_min != 0) new_min--;
                                if (new_max != -1) new_max--;
                                if (new_max == 0) {
                                    result = true;
                                    continuationPc = state.continuationPc;
                                    continuationOp = state.continuationOp;
                                    pc += 2 * INDEX_LEN;
                                    pc += getOffset(program, pc);
                                    break switchStatement;
                                }
                                nextpc = pc + 3 * INDEX_LEN;
                                nextop = program[nextpc];
                                int startcp = gData.cp;
                                if (reopIsSimple(nextop)) {
                                    nextpc++;
                                    int match =
                                            executeSimpleOpcode(
                                                    gData,
                                                    input,
                                                    nextop,
                                                    program,
                                                    nextpc,
                                                    end,
                                                    true,
                                                    matchBackward);
                                    if (match < 0) {
                                        result = (new_min == 0);
                                        continuationPc = state.continuationPc;
                                        continuationOp = state.continuationOp;
                                        pc += 2 * INDEX_LEN; /* <parencount> & <parenindex> */
                                        pc += getOffset(program, pc);
                                        break switchStatement;
                                    }
                                    result = true;
                                    nextpc = match;
                                }
                                continuationOp = REOP_REPEAT;
                                continuationPc = pc;
                                pushProgState(
                                        gData,
                                        new_min,
                                        new_max,
                                        startcp,
                                        matchBackward,
                                        null,
                                        state.continuationOp,
                                        state.continuationPc);
                                if (new_min == 0) {
                                    pushBackTrackState(
                                            gData,
                                            REOP_REPEAT,
                                            pc,
                                            startcp,
                                            state.continuationOp,
                                            state.continuationPc);
                                }
                                int parenCount = getIndex(program, pc);
                                int parenIndex = getIndex(program, pc + INDEX_LEN);
                                IntStream.range(0, parenCount)
                                        .forEach(k -> gData.setParens(parenIndex + k, -1, 0));
                            } while (program[nextpc] == REOP_ENDCHILD);

                            pc = nextpc;
                            op = program[pc++];
                        }
                        continue;

                    case REOP_MINIMALREPEAT:
                        {
                            REProgState state = popProgState(gData);
                            if (!result) {
                                //
                                // Non-greedy failure - try to consume another child.
                                //
                                if (state.max == -1 || state.max > 0) {
                                    pushProgState(
                                            gData,
                                            state.min,
                                            state.max,
                                            gData.cp,
                                            matchBackward,
                                            null,
                                            state.continuationOp,
                                            state.continuationPc);
                                    continuationOp = REOP_MINIMALREPEAT;
                                    continuationPc = pc;
                                    int parenCount = getIndex(program, pc);
                                    pc += INDEX_LEN;
                                    int parenIndex = getIndex(program, pc);
                                    pc += 2 * INDEX_LEN;
                                    IntStream.range(0, parenCount)
                                            .forEach(k -> gData.setParens(parenIndex + k, -1, 0));
                                    op = program[pc++];
                                    continue;
                                }
                                // Don't need to adjust pc since we're going to pop.
                                continuationPc = state.continuationPc;
                                continuationOp = state.continuationOp;
                                break;
                            }
                            if (state.min == 0 && gData.cp == state.index) {
                                // Matched an empty string, that'll get us nowhere.
                                result = false;
                                continuationPc = state.continuationPc;
                                continuationOp = state.continuationOp;
                                break;
                            }
                            int new_min = state.min, new_max = state.max;
                            if (new_min != 0) new_min--;
                            if (new_max != -1) new_max--;
                            pushProgState(
                                    gData,
                                    new_min,
                                    new_max,
                                    gData.cp,
                                    matchBackward,
                                    null,
                                    state.continuationOp,
                                    state.continuationPc);
                            if (new_min != 0) {
                                continuationOp = REOP_MINIMALREPEAT;
                                continuationPc = pc;
                                int parenCount = getIndex(program, pc);
                                pc += INDEX_LEN;
                                int parenIndex = getIndex(program, pc);
                                pc += 2 * INDEX_LEN;
                                IntStream.range(0, parenCount)
                                        .forEach(k -> gData.setParens(parenIndex + k, -1, 0));
                            } else {
                                continuationPc = state.continuationPc;
                                continuationOp = state.continuationOp;
                                pushBackTrackState(gData, REOP_MINIMALREPEAT, pc);
                                popProgState(gData);
                                pc += 2 * INDEX_LEN;
                                pc = pc + getOffset(program, pc);
                            }
                            op = program[pc++];
                            continue;
                        }

                    case REOP_END:
                        return true;

                    default:
                        throw Kit.codeBug("invalid bytecode");
                }
            }
            /*
             *  If the match failed and there's a backtrack option, take it.
             *  Otherwise this is a complete and utter failure.
             */
            if (!result) {
                REBackTrackData backTrackData = gData.backTrackStackTop;
                if (backTrackData != null) {
                    gData.backTrackStackTop = backTrackData.previous;
                    gData.parens = backTrackData.parens;
                    gData.cp = backTrackData.cp;
                    gData.stateStackTop = backTrackData.stateStackTop;
                    continuationOp = backTrackData.continuationOp;
                    continuationPc = backTrackData.continuationPc;
                    pc = backTrackData.pc;
                    op = backTrackData.op;
                    continue;
                }
                return false;
            }

            op = program[pc++];
        }
    }

    private static boolean matchRegExp(
            Context cx,
            REGlobalData gData,
            RECompiled re,
            String input,
            int start,
            int end,
            boolean multiline) {
        if (re.parenCount != 0) {
            gData.parens = new long[re.parenCount];
        } else {
            gData.parens = null;
        }

        gData.backTrackStackTop = null;
        gData.stateStackTop = null;

        gData.multiline = multiline || (re.flags & JSREG_MULTILINE) != 0;
        gData.regexp = re;

        int anchorCodePoint = gData.regexp.anchorCodePoint;
        //
        // have to include the position beyond the last character
        //  in order to detect end-of-input/line condition
        //
        for (int i = start; i <= end; ++i) {
            //
            // If the first node is a literal match, step the index into
            // the string until that match is made, or fail if it can't be
            // found at all.
            //
            if (anchorCodePoint >= 0) {
                for (; ; ) {
                    if (i == end) {
                        return false;
                    }

                    int charCount;
                    if (isUnicodeMode(gData.regexp.flags)) {
                        int matchCodePoint = input.codePointAt(i);
                        boolean matches = matchCodePoint == anchorCodePoint;
                        if (!matches && (gData.regexp.flags & JSREG_FOLD) != 0) {
                            // Unicode case-insensitive comparison
                            matches =
                                    Character.toLowerCase(matchCodePoint)
                                            == Character.toLowerCase(anchorCodePoint);
                        }
                        if (matches) {
                            break;
                        }
                        charCount = Character.charCount(matchCodePoint);
                    } else {
                        char matchCh = input.charAt(i);
                        if (matchCh == anchorCodePoint
                                || ((gData.regexp.flags & JSREG_FOLD) != 0
                                        && Character.toUpperCase(matchCh)
                                                == Character.toUpperCase((char) anchorCodePoint))) {
                            break;
                        }
                        charCount = 1;
                    }

                    if ((gData.regexp.flags & JSREG_STICKY) != 0) {
                        return false;
                    }

                    i += charCount;
                }
            }
            gData.cp = i;
            gData.skipped = i - start;
            if (gData.parens != null) {
                Arrays.fill(gData.parens, -1L);
            }
            boolean result = executeREBytecode(cx, gData, input, end);

            gData.backTrackStackTop = null;
            gData.stateStackTop = null;
            if (result) {
                return true;
            }
            if (anchorCodePoint == ANCHOR_BOL && !gData.multiline) {
                gData.skipped = end;
                return false;
            }

            if ((gData.regexp.flags & JSREG_STICKY) != 0) {
                return false;
            }

            i = start + gData.skipped;
        }
        return false;
    }

    /*
     * indexp is assumed to be an array of length 1
     */
    Object executeRegExp(
            Context cx, Scriptable scope, RegExpImpl res, String str, int[] indexp, int matchType) {
        REGlobalData gData = new REGlobalData();
        String[] namedCaptureGroups = null; // for indices and groups

        int start = indexp[0];
        int end = str.length();
        if (start > end) start = end;
        //
        // Call the recursive matcher to do the real work.
        //
        boolean matches = matchRegExp(cx, gData, re, str, start, end, res.multiline);
        if (!matches) {
            if (matchType != PREFIX) return null;
            return Undefined.instance;
        }
        int index = gData.cp;
        int ep = indexp[0] = index;
        int matchlen = ep - (start + gData.skipped);
        index -= matchlen;
        Object result;
        Scriptable obj;
        Scriptable groups = Undefined.SCRIPTABLE_UNDEFINED;

        if (matchType == TEST) {
            /*
             * Testing for a match and updating cx.regExpImpl: don't allocate
             * an array object, do return true.
             */
            result = Boolean.TRUE;
            obj = null;
        } else {
            /*
             * The array returned on match has element 0 bound to the matched
             * string, elements 1 through re.parenCount bound to the paren
             * matches, an index property telling the length of the left context,
             * and an input property referring to the input string.
             */
            result = cx.newArray(scope, 0);
            obj = (Scriptable) result;

            String matchstr = str.substring(index, index + matchlen);
            obj.put(0, obj, matchstr);
        }

        if (re.parenCount == 0) {
            res.parens = null;
            res.lastParen = new SubString();
        } else {
            SubString parsub = null;
            int num;

            if (matchType != TEST) {
                namedCaptureGroups = new String[re.parenCount];

                if (!re.namedCaptureGroups.isEmpty()) {
                    // We do a new NativeObject() and not cx.newObject(scope)
                    // since we want the groups to have null as prototype
                    groups = new NativeObject();
                }
                for (Map.Entry<String, List<Integer>> entry : re.namedCaptureGroups.entrySet()) {
                    String key = entry.getKey();
                    List<Integer> indices = entry.getValue();
                    for (int i : indices) {
                        namedCaptureGroups[i] = key;
                    }
                }
            }

            res.parens = new SubString[re.parenCount];
            for (num = 0; num < re.parenCount; num++) {
                int cap_index = gData.parensIndex(num);
                if (cap_index != -1) {
                    int cap_length = gData.parensLength(num);
                    parsub = new SubString(str, cap_index, cap_length);
                    res.parens[num] = parsub;
                    if (matchType != TEST) {
                        obj.put(num + 1, obj, parsub.toString());
                        if (namedCaptureGroups[num] != null) {
                            groups.put(namedCaptureGroups[num], groups, parsub.toString());
                        }
                    }
                } else {
                    if (matchType != TEST) {
                        obj.put(num + 1, obj, Undefined.instance);
                        if (namedCaptureGroups[num] != null
                                && !groups.has(namedCaptureGroups[num], groups)) {
                            groups.put(namedCaptureGroups[num], groups, Undefined.instance);
                        }
                    }
                }
            }
            res.lastParen = parsub;
        }

        if (!(matchType == TEST)) {
            /*
             * Define the index and input properties last for better for/in loop
             * order (so they come after the elements).
             */
            obj.put("index", obj, Integer.valueOf(start + gData.skipped));
            obj.put("input", obj, str);
            obj.put("groups", obj, groups);

            // ES2022 hasIndices support ('d' flag)
            if ((re.flags & JSREG_HASINDICES) != 0) {
                Scriptable indices = cx.newArray(scope, re.parenCount + 1);

                // Create indicesGroups with null prototype (like groups object)
                // Only create if there are named capture groups, otherwise undefined
                Scriptable indicesGroups = null;
                if (!re.namedCaptureGroups.isEmpty()) {
                    indicesGroups = new NativeObject();
                }

                // Index 0: overall match indices
                Scriptable overallIndices = cx.newArray(scope, 2);
                overallIndices.put(0, overallIndices, Integer.valueOf(start + gData.skipped));
                overallIndices.put(
                        1, overallIndices, Integer.valueOf(start + gData.skipped + matchlen));
                indices.put(0, indices, overallIndices);

                // Indices for each capture group
                for (int num = 0; num < re.parenCount; num++) {
                    int cap_index = gData.parensIndex(num);
                    if (cap_index != -1) {
                        int cap_length = gData.parensLength(num);
                        Scriptable groupIndices = cx.newArray(scope, 2);
                        groupIndices.put(0, groupIndices, Integer.valueOf(cap_index));
                        groupIndices.put(1, groupIndices, Integer.valueOf(cap_index + cap_length));
                        indices.put(num + 1, indices, groupIndices);

                        // Add to indicesGroups for named captures
                        if (namedCaptureGroups[num] != null && indicesGroups != null) {
                            indicesGroups.put(namedCaptureGroups[num], indicesGroups, groupIndices);
                        }
                    } else {
                        indices.put(num + 1, indices, Undefined.instance);
                        if (namedCaptureGroups[num] != null
                                && indicesGroups != null
                                && !indicesGroups.has(namedCaptureGroups[num], indicesGroups)) {
                            indicesGroups.put(
                                    namedCaptureGroups[num], indicesGroups, Undefined.instance);
                        }
                    }
                }

                // Set indices.groups to undefined if no named groups, otherwise use indicesGroups
                indices.put(
                        "groups",
                        indices,
                        indicesGroups != null ? indicesGroups : Undefined.instance);
                obj.put("indices", obj, indices);
            }
        }

        if (res.lastMatch == null) {
            res.lastMatch = new SubString();
            res.leftContext = new SubString();
            res.rightContext = new SubString();
        }
        res.lastMatch.str = str;
        res.lastMatch.index = index;
        res.lastMatch.length = matchlen;

        res.leftContext.str = str;
        // ECMAScript standard behavior (Perl5 semantics)
        res.leftContext.index = 0;
        res.leftContext.length = start + gData.skipped;

        res.rightContext.str = str;
        res.rightContext.index = ep;
        res.rightContext.length = end - ep;

        return result;
    }

    int getFlags() {
        return re.flags;
    }

    private static void reportWarning(Context cx, String messageId, String arg) {
        if (cx.hasFeature(Context.FEATURE_STRICT_MODE)) {
            String msg = ScriptRuntime.getMessageById(messageId, arg);
            Context.reportWarning(msg);
        }
    }

    // Package-private to allow access from extracted classes (RegExpFlags, etc.)
    static void reportError(String messageId, String arg) {
        String msg = ScriptRuntime.getMessageById(messageId, arg);
        throw ScriptRuntime.constructError("SyntaxError", msg);
    }

    private static final int Id_lastIndex = 1,
            Id_source = 2,
            Id_flags = 3,
            Id_global = 4,
            Id_ignoreCase = 5,
            Id_multiline = 6,
            Id_dotAll = 7,
            Id_sticky = 8,
            Id_unicode = 9,
            Id_hasIndices = 10,
            Id_unicodeSets = 11,
            MAX_INSTANCE_ID = 11;

    @Override
    protected int getMaxInstanceId() {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected int findInstanceIdInfo(String s) {
        int id;
        switch (s) {
            case "lastIndex":
                id = Id_lastIndex;
                break;
            case "source":
                id = Id_source;
                break;
            case "flags":
                id = Id_flags;
                break;
            case "global":
                id = Id_global;
                break;
            case "ignoreCase":
                id = Id_ignoreCase;
                break;
            case "multiline":
                id = Id_multiline;
                break;
            case "dotAll":
                id = Id_dotAll;
                break;
            case "sticky":
                id = Id_sticky;
                break;
            case "unicode":
                id = Id_unicode;
                break;
            case "hasIndices":
                id = Id_hasIndices;
                break;
            case "unicodeSets":
                id = Id_unicodeSets;
                break;
            default:
                id = 0;
                break;
        }

        if (id == 0) return super.findInstanceIdInfo(s);

        int attr;
        switch (id) {
            case Id_lastIndex:
                attr = lastIndexAttr;
                break;
            case Id_source:
            case Id_flags:
            case Id_global:
            case Id_ignoreCase:
            case Id_multiline:
            case Id_dotAll:
            case Id_sticky:
            case Id_unicode:
            case Id_hasIndices:
            case Id_unicodeSets:
                attr = PERMANENT | READONLY | DONTENUM;
                break;
            default:
                throw new IllegalStateException();
        }
        return instanceIdInfo(attr, id);
    }

    @Override
    protected String getInstanceIdName(int id) {
        switch (id) {
            case Id_lastIndex:
                return "lastIndex";
            case Id_source:
                return "source";
            case Id_flags:
                return "flags";
            case Id_global:
                return "global";
            case Id_ignoreCase:
                return "ignoreCase";
            case Id_multiline:
                return "multiline";
            case Id_dotAll:
                return "dotAll";
            case Id_sticky:
                return "sticky";
            case Id_unicode:
                return "unicode";
            case Id_hasIndices:
                return "hasIndices";
            case Id_unicodeSets:
                return "unicodeSets";
        }
        return super.getInstanceIdName(id);
    }

    // SPEC DEVIATION: ES spec requires flag getters (global, ignoreCase, multiline, dotAll, etc.)
    // to be prototype accessors with validation (throw TypeError for non-RegExp this values,
    // return undefined for RegExp.prototype). Currently implemented as instance properties.
    // This works for basic functionality but fails strict Test262 validation.
    // Fixing requires architectural changes to IdScriptableObject's property system.
    @Override
    protected Object getInstanceIdValue(int id) {
        switch (id) {
            case Id_lastIndex:
                return lastIndex;
            case Id_source:
                return new String(re.source);
            case Id_flags:
                {
                    StringBuilder buf = new StringBuilder();
                    appendFlags(buf);
                    return buf.toString();
                }
            case Id_global:
                return ScriptRuntime.wrapBoolean((re.flags & JSREG_GLOB) != 0);
            case Id_ignoreCase:
                return ScriptRuntime.wrapBoolean((re.flags & JSREG_FOLD) != 0);
            case Id_multiline:
                return ScriptRuntime.wrapBoolean((re.flags & JSREG_MULTILINE) != 0);
            case Id_dotAll:
                return ScriptRuntime.wrapBoolean((re.flags & JSREG_DOTALL) != 0);
            case Id_sticky:
                return ScriptRuntime.wrapBoolean((re.flags & JSREG_STICKY) != 0);
            case Id_unicode:
                return ScriptRuntime.wrapBoolean((re.flags & JSREG_UNICODE) != 0);
            case Id_hasIndices:
                return ScriptRuntime.wrapBoolean((re.flags & JSREG_HASINDICES) != 0);
            case Id_unicodeSets:
                return ScriptRuntime.wrapBoolean((re.flags & JSREG_UNICODESETS) != 0);
        }
        return super.getInstanceIdValue(id);
    }

    private void setLastIndex(ScriptableObject thisObj, Object value) {
        if ((thisObj.getAttributes("lastIndex") & READONLY) != 0) {
            throw ScriptRuntime.typeErrorById("msg.modify.readonly", "lastIndex");
        }
        setLastIndex((Scriptable) thisObj, value);
    }

    private void setLastIndex(Scriptable thisObj, Object value) {
        ScriptableObject.putProperty(thisObj, "lastIndex", value);
    }

    private void setLastIndex(Object value) {
        if ((lastIndexAttr & READONLY) != 0) {
            throw ScriptRuntime.typeErrorById("msg.modify.readonly", "lastIndex");
        }
        lastIndex = value;
    }

    @Override
    protected void setInstanceIdValue(int id, Object value) {
        switch (id) {
            case Id_lastIndex:
                setLastIndex(value);
                return;
            case Id_source:
            case Id_flags:
            case Id_global:
            case Id_ignoreCase:
            case Id_multiline:
            case Id_dotAll:
            case Id_sticky:
            case Id_unicode:
            case Id_hasIndices:
            case Id_unicodeSets:
                return;
        }
        super.setInstanceIdValue(id, value);
    }

    @Override
    protected void setInstanceIdAttributes(int id, int attr) {
        if (id == Id_lastIndex) {
            lastIndexAttr = attr;
            return;
        }
        super.setInstanceIdAttributes(id, attr);
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id == SymbolId_match) {
            initPrototypeMethod(REGEXP_TAG, id, SymbolKey.MATCH, "[Symbol.match]", 1);
            return;
        }
        if (id == SymbolId_matchAll) {
            initPrototypeMethod(REGEXP_TAG, id, SymbolKey.MATCH_ALL, "[Symbol.matchAll]", 1);
            return;
        }
        if (id == SymbolId_search) {
            initPrototypeMethod(REGEXP_TAG, id, SymbolKey.SEARCH, "[Symbol.search]", 1);
            return;
        }
        if (id == SymbolId_replace) {
            initPrototypeMethod(REGEXP_TAG, id, SymbolKey.REPLACE, "[Symbol.replace]", 2);
            return;
        }
        if (id == SymbolId_split) {
            initPrototypeMethod(REGEXP_TAG, id, SymbolKey.SPLIT, "[Symbol.split]", 2);
            return;
        }

        String s;
        int arity;
        switch (id) {
            case Id_compile:
                arity = 2;
                s = "compile";
                break;
            case Id_toString:
                arity = 0;
                s = "toString";
                break;
            case Id_toSource:
                arity = 0;
                s = "toSource";
                break;
            case Id_exec:
                arity = 1;
                s = "exec";
                break;
            case Id_test:
                arity = 1;
                s = "test";
                break;
            case Id_prefix:
                arity = 1;
                s = "prefix";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(REGEXP_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(REGEXP_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case Id_compile:
                return realThis(thisObj, f).compile(cx, scope, args);

            case Id_toString:
                // thisObj != scope is a strange hack but i had no better idea for the moment
                if (thisObj != scope && thisObj instanceof NativeObject) {
                    Object sourceObj = thisObj.get("source", thisObj);
                    String source =
                            sourceObj.equals(NOT_FOUND) ? "undefined" : escapeRegExp(sourceObj);
                    Object flagsObj = thisObj.get("flags", thisObj);
                    String flags = flagsObj.equals(NOT_FOUND) ? "undefined" : flagsObj.toString();

                    return "/" + source + "/" + flags;
                }
                return realThis(thisObj, f).toString();

            case Id_toSource:
                return realThis(thisObj, f).toString();

            case Id_exec:
                return js_exec(cx, scope, thisObj, args);

            case Id_test:
                {
                    Object x = realThis(thisObj, f).execSub(cx, scope, args, TEST);
                    return Boolean.TRUE.equals(x) ? Boolean.TRUE : Boolean.FALSE;
                }

            case Id_prefix:
                return realThis(thisObj, f).execSub(cx, scope, args, PREFIX);

            case SymbolId_match:
                return js_SymbolMatch(cx, scope, thisObj, args);

            case SymbolId_matchAll:
                return js_SymbolMatchAll(cx, scope, thisObj, args);

            case SymbolId_search:
                return js_SymbolSearch(cx, scope, thisObj, args);

            case SymbolId_replace:
                return js_SymbolReplace(cx, scope, thisObj, args);

            case SymbolId_split:
                return js_SymbolSplit(cx, scope, thisObj, args);
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    public static Object regExpExec(
            Scriptable regexp, String string, Context cx, Scriptable scope) {
        // See ECMAScript spec 22.2.7.1
        Object execMethod = ScriptRuntime.getObjectProp(regexp, "exec", cx, scope);
        if (execMethod instanceof Callable) {
            return ((Callable) execMethod).call(cx, scope, regexp, new Object[] {string});
        }
        return NativeRegExp.js_exec(cx, scope, regexp, new Object[] {string});
    }

    private Object js_SymbolMatch(
            Context cx, Scriptable scope, Scriptable thisScriptable, Object[] args) {
        // See ECMAScript spec 22.2.6.8
        var thisObj = ScriptableObject.ensureScriptableObject(thisScriptable);

        String string = ScriptRuntime.toString(args.length > 0 ? args[0] : Undefined.instance);
        String flags = ScriptRuntime.toString(ScriptRuntime.getObjectProp(thisObj, "flags", cx));
        boolean fullUnicode = flags.indexOf('u') != -1 || flags.indexOf('v') != -1;

        if (flags.indexOf('g') == -1) return regExpExec(thisObj, string, cx, scope);

        setLastIndex(thisObj, ScriptRuntime.zeroObj);
        Scriptable result = cx.newArray(scope, 0);
        int i = 0;
        while (true) {
            Object match = regExpExec(thisObj, string, cx, scope);
            if (match == null) {
                if (i == 0) return null;
                else return result;
            }

            String matchStr =
                    ScriptRuntime.toString(ScriptRuntime.getObjectIndex(match, 0, cx, scope));
            result.put(i++, result, matchStr);

            if (matchStr.isEmpty()) {
                long thisIndex = getLastIndex(cx, thisObj);
                long nextIndex = ScriptRuntime.advanceStringIndex(string, thisIndex, fullUnicode);
                setLastIndex(thisObj, nextIndex);
            }
        }
    }

    private Object js_SymbolSearch(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // See ECMAScript spec 22.2.6.12
        if (!ScriptRuntime.isObject(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(thisObj));
        }

        String string = ScriptRuntime.toString(args.length > 0 ? args[0] : Undefined.instance);
        // Use SameValue comparison per spec (distinguishes +0 from -0)
        Object previousLastIndex = ScriptRuntime.getObjectProp(thisObj, "lastIndex", cx);
        if (!ScriptRuntime.same(previousLastIndex, ScriptRuntime.zeroObj)) {
            setLastIndex(thisObj, ScriptRuntime.zeroObj);
        }

        Object result = regExpExec(thisObj, string, cx, scope);

        Object currentLastIndex = ScriptRuntime.getObjectProp(thisObj, "lastIndex", cx);
        if (!ScriptRuntime.same(previousLastIndex, currentLastIndex)) {
            setLastIndex(thisObj, previousLastIndex);
        }

        if (result == null) {
            return -1;
        } else {
            return ScriptRuntime.getObjectProp(result, "index", cx, scope);
        }
    }

    static Object js_exec(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return realThis(thisObj, "exec").execSub(cx, scope, args, MATCH);
    }

    private Object js_SymbolMatchAll(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // See ECMAScript spec 22.2.6.9
        if (!ScriptRuntime.isObject(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(thisObj));
        }

        String s = ScriptRuntime.toString(args.length > 0 ? args[0] : Undefined.instance);

        Scriptable topLevelScope = ScriptableObject.getTopLevelScope(scope);
        Function defaultConstructor =
                ScriptRuntime.getExistingCtor(cx, topLevelScope, getClassName());
        Constructable c =
                AbstractEcmaObjectOperations.speciesConstructor(cx, thisObj, defaultConstructor);

        String flags = ScriptRuntime.toString(ScriptRuntime.getObjectProp(thisObj, "flags", cx));

        Scriptable matcher = c.construct(cx, scope, new Object[] {thisObj, flags});

        long lastIndex = getLastIndex(cx, thisObj);
        setLastIndex(matcher, lastIndex);
        boolean global = flags.indexOf('g') != -1;
        boolean fullUnicode = flags.indexOf('u') != -1 || flags.indexOf('v') != -1;

        return new NativeRegExpStringIterator(scope, matcher, s, global, fullUnicode);
    }

    private Object js_SymbolReplace(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // See ECMAScript spec 22.2.6.11
        if (!ScriptRuntime.isObject(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(thisObj));
        }

        String s = ScriptRuntime.toString(args.length > 0 ? args[0] : Undefined.instance);
        int lengthS = s.length();
        Object replaceValue = args.length > 1 ? args[1] : Undefined.instance;
        boolean functionalReplace = replaceValue instanceof Callable;
        if (!functionalReplace) {
            replaceValue = ScriptRuntime.toString(replaceValue);
        }
        String flags = ScriptRuntime.toString(ScriptRuntime.getObjectProp(thisObj, "flags", cx));
        boolean global = flags.indexOf('g') != -1;
        boolean fullUnicode = flags.indexOf('u') != -1 || flags.indexOf('v') != -1;
        if (global) {
            setLastIndex(thisObj, ScriptRuntime.zeroObj);
        }

        List<Object> results = new ArrayList<>();
        boolean done = false;
        while (!done) {
            Object result = regExpExec(thisObj, s, cx, scope);
            if (result == null) {
                done = true;
            } else {
                results.add(result);
                if (!global) {
                    done = true;
                } else {
                    String matchStr =
                            ScriptRuntime.toString(
                                    ScriptRuntime.getObjectIndex(result, 0, cx, scope));
                    if (matchStr.isEmpty()) {
                        long thisIndex = getLastIndex(cx, thisObj);
                        long nextIndex =
                                ScriptRuntime.advanceStringIndex(s, thisIndex, fullUnicode);
                        setLastIndex(thisObj, nextIndex);
                    }
                }
            }
        }

        // Performance: Pre-allocate StringBuilder with input string length as capacity hint
        StringBuilder accumulatedResult = new StringBuilder(lengthS);
        int nextSourcePosition = 0;
        for (Object result : results) {
            long resultLength =
                    ScriptRuntime.toLength(
                            ScriptRuntime.getObjectProp(result, "length", cx, scope));
            long nCaptures = Math.max(resultLength - 1, 0);
            String matched =
                    ScriptRuntime.toString(ScriptRuntime.getObjectIndex(result, 0, cx, scope));
            int matchLength = matched.length();
            double positionDbl =
                    ScriptRuntime.toInteger(
                            ScriptRuntime.getObjectProp(result, "index", cx, scope));
            int position = ScriptRuntime.clamp((int) positionDbl, 0, lengthS);

            List<Object> captures = new ArrayList<>();
            int n = 1;
            while (n <= nCaptures) {
                Object capN = ScriptRuntime.getObjectElem(result, n, cx, scope);
                if (!Undefined.isUndefined(capN)) {
                    capN = ScriptRuntime.toString(capN);
                }
                captures.add(capN);
                ++n;
            }

            Object namedCaptures = ScriptRuntime.getObjectProp(result, "groups", cx, scope);
            String replacementString;

            if (functionalReplace) {
                List<Object> replacerArgs = new ArrayList<>();
                replacerArgs.add(matched);
                replacerArgs.addAll(captures);
                replacerArgs.add(position);
                replacerArgs.add(s);
                if (!Undefined.isUndefined(namedCaptures)) {
                    replacerArgs.add(namedCaptures);
                }

                Scriptable callThis =
                        ScriptRuntime.getApplyOrCallThis(
                                cx, scope, null, 0, (Callable) replaceValue);
                Object replacementValue =
                        ((Callable) replaceValue).call(cx, scope, callThis, replacerArgs.toArray());
                replacementString = ScriptRuntime.toString(replacementValue);
            } else {
                if (!Undefined.isUndefined(namedCaptures)) {
                    namedCaptures = ScriptRuntime.toObject(scope, namedCaptures);
                }

                NativeArray capturesArray = (NativeArray) cx.newArray(scope, captures.toArray());
                replacementString =
                        AbstractEcmaStringOperations.getSubstitution(
                                cx,
                                scope,
                                matched,
                                s,
                                position,
                                capturesArray,
                                namedCaptures,
                                (String) replaceValue);
            }

            if (position >= nextSourcePosition) {
                accumulatedResult.append(s, nextSourcePosition, position);
                accumulatedResult.append(replacementString);
                nextSourcePosition = position + matchLength;
            }
        }

        if (nextSourcePosition >= lengthS) {
            return accumulatedResult.toString();
        } else {
            accumulatedResult.append(s.substring(nextSourcePosition));
            return accumulatedResult.toString();
        }
    }

    private Object js_SymbolSplit(Context cx, Scriptable scope, Scriptable rx, Object[] args) {
        // See ECMAScript spec 22.2.6.14
        if (!ScriptRuntime.isObject(rx)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(rx));
        }

        String s = ScriptRuntime.toString(args.length > 0 ? args[0] : Undefined.instance);

        Scriptable topLevelScope = ScriptableObject.getTopLevelScope(scope);
        Function defaultConstructor =
                ScriptRuntime.getExistingCtor(cx, topLevelScope, getClassName());
        Constructable c =
                AbstractEcmaObjectOperations.speciesConstructor(cx, rx, defaultConstructor);

        String flags = ScriptRuntime.toString(ScriptRuntime.getObjectProp(rx, "flags", cx));
        boolean unicodeMatching = flags.indexOf('u') != -1 || flags.indexOf('v') != -1;
        String newFlags = flags.indexOf('y') != -1 ? flags : (flags + "y");

        Scriptable splitter = c.construct(cx, scope, new Object[] {rx, newFlags});

        NativeArray a = (NativeArray) cx.newArray(scope, 0);
        int lengthA = 0;

        Object limit = args.length > 1 ? args[1] : Undefined.instance;
        long lim;
        if (Undefined.isUndefined(limit)) {
            lim = Integer.MAX_VALUE;
        } else {
            lim = ScriptRuntime.toUint32(limit);
        }
        if (lim == 0) {
            return a;
        }

        if (s.isEmpty()) {
            Object z = regExpExec(splitter, s, cx, scope);
            if (z != null) {
                return a;
            }
            a.put(0, a, s);
            return a;
        }

        int size = s.length();
        long p = 0;
        long q = p;
        while (q < size) {
            setLastIndex(splitter, q);
            Object z = regExpExec(splitter, s, cx, scope);
            if (z == null) {
                q = ScriptRuntime.advanceStringIndex(s, q, unicodeMatching);
            } else {
                long e = getLastIndex(cx, splitter);
                e = Math.min(e, size);
                if (e == p) {
                    q = ScriptRuntime.advanceStringIndex(s, q, unicodeMatching);
                } else {
                    String t = s.substring((int) p, (int) q);
                    a.put((int) a.getLength(), a, t);
                    lengthA++;
                    if (a.getLength() == lim) {
                        return a;
                    }

                    p = e;
                    long numberOfCaptures =
                            ScriptRuntime.toLength(
                                    ScriptRuntime.getObjectProp(z, "length", cx, scope));
                    numberOfCaptures = Math.max(numberOfCaptures - 1, 0);
                    int i = 1;
                    while (i <= numberOfCaptures) {
                        Object nextCapture = ScriptRuntime.getObjectIndex(z, i, cx, scope);
                        a.put((int) a.getLength(), a, nextCapture);
                        i = i + 1;
                        lengthA++;
                        if (lengthA == lim) {
                            return a;
                        }
                    }
                    q = p;
                }
            }
        }
        String t = s.substring((int) p, size);
        a.put((int) a.getLength(), a, t);
        return a;
    }

    private static long getLastIndex(Context cx, Scriptable thisObj) {
        return ScriptRuntime.toLength(ScriptRuntime.getObjectProp(thisObj, "lastIndex", cx));
    }

    private static NativeRegExp realThis(Scriptable thisObj, IdFunctionObject f) {
        return realThis(thisObj, f.getFunctionName());
    }

    private static NativeRegExp realThis(Scriptable thisObj, String functionName) {
        return ensureType(thisObj, NativeRegExp.class, functionName);
    }

    @Override
    protected int findPrototypeId(Symbol k) {
        if (SymbolKey.MATCH.equals(k)) {
            return SymbolId_match;
        }
        if (SymbolKey.MATCH_ALL.equals(k)) {
            return SymbolId_matchAll;
        }
        if (SymbolKey.SEARCH.equals(k)) {
            return SymbolId_search;
        }
        if (SymbolKey.REPLACE.equals(k)) {
            return SymbolId_replace;
        }
        if (SymbolKey.SPLIT.equals(k)) {
            return SymbolId_split;
        }
        return 0;
    }

    @Override
    protected int findPrototypeId(String s) {
        int id;
        switch (s) {
            case "compile":
                id = Id_compile;
                break;
            case "toString":
                id = Id_toString;
                break;
            case "toSource":
                id = Id_toSource;
                break;
            case "exec":
                id = Id_exec;
                break;
            case "test":
                id = Id_test;
                break;
            case "prefix":
                id = Id_prefix;
                break;
            default:
                id = 0;
                break;
        }
        return id;
    }

    private static final int Id_compile = 1,
            Id_toString = 2,
            Id_toSource = 3,
            Id_exec = 4,
            Id_test = 5,
            Id_prefix = 6,
            SymbolId_match = 7,
            SymbolId_matchAll = 8,
            SymbolId_search = 9,
            SymbolId_replace = 10,
            SymbolId_split = 11,
            MAX_PROTOTYPE_ID = SymbolId_split;

    // ==================================================================================
    // INSTANCE FIELDS
    // ==================================================================================

    private RECompiled re;
    Object lastIndex = ScriptRuntime.zeroObj; /* index after last match, for //g iterator */
    private int lastIndexAttr = DONTENUM | PERMANENT;
} // class NativeRegExp

/** Compiled regular expression with bytecode, character classes, and capture group metadata. */
class RECompiled implements Serializable {
    private static final long serialVersionUID = -6144956577595844213L;

    final char[] source; /* locked source string, sans // */
    int parenCount; /* number of parenthesized submatches */
    Map<String, List<Integer>>
            namedCaptureGroups; // List<Int> to handle duplicate names in disjunctions
    ArrayList<String> namedBackRefs; // List of named back references
    int flags; /* flags  */
    byte[] program; /* regular expression bytecode */
    int classCount; /* count [...] bitmaps */
    RECharSet[] classList; /* list of [...] bitmaps */
    ArrayList<StringMatcher> stringMatchers; /* universal string matchers (ES2024) */
    int anchorCodePoint = -1; /* if >= 0, then re starts with this literal char */

    RECompiled(String str) {
        this.source = str.toCharArray();
    }
}

/** Regular expression parse tree node. Used during pattern compilation. */
class RENode {

    RENode(byte op) {
        this.op = op;
    }

    byte op; /* r.e. op bytecode */
    RENode next; /* next in concatenation order */
    RENode kid; /* first operand */

    RENode kid2; /* second operand */
    int parenIndex; /* or a parenthesis index */

    /* or a range */
    int min;
    int max;
    int parenCount;
    boolean greedy;

    /* or a character class */
    int bmsize; /* bitmap size, based on max char code */
    int index; /* index into class list */
    ClassContents classContents;

    /* or a literal sequence */
    char chr; /* of one character */
    char
            lowSurrogate; /* low surrogate, if chr is high surrogate that is part of a surrogate pair */
    int length; /* or many (via the index) */
    int flatIndex; /* which is -1 if not sourced */

    /* or a named capture group */
    String namedCaptureGroupName;

    /* or a back reference to a named capture group */
    int namedCaptureGroupBackRefIndex;

    /* or a unicode property */
    int unicodeProperty; // encoded using UnicodeProperty.encode()
    String propertyName; // for Property of Strings (ES2024 emoji sequences)

    /* or a string matcher (ES2024 universal string matching) */
    StringMatcher stringMatcher;

    /**
     * Fluent builder for RENode construction.
     *
     * <p>Provides a type-safe, readable API for creating RENode instances:
     *
     * <pre>
     * RENode node = RENode.builder(REOP_QUANT)
     *     .range(0, -1)
     *     .greedy(true)
     *     .kid(childNode)
     *     .build();
     * </pre>
     *
     * <p>The builder mutates an underlying RENode and returns it via build(). This is acceptable as
     * RENodes are created during parsing and remain immutable afterward.
     */
    static class Builder {
        private final RENode node;

        Builder(byte op) {
            this.node = new RENode(op);
        }

        Builder next(RENode next) {
            node.next = next;
            return this;
        }

        Builder kid(RENode kid) {
            node.kid = kid;
            return this;
        }

        Builder kid2(RENode kid2) {
            node.kid2 = kid2;
            return this;
        }

        Builder parenIndex(int parenIndex) {
            node.parenIndex = parenIndex;
            return this;
        }

        Builder range(int min, int max) {
            node.min = min;
            node.max = max;
            return this;
        }

        Builder parenCount(int parenCount) {
            node.parenCount = parenCount;
            return this;
        }

        Builder greedy(boolean greedy) {
            node.greedy = greedy;
            return this;
        }

        Builder classInfo(int bmsize, int index, ClassContents classContents) {
            node.bmsize = bmsize;
            node.index = index;
            node.classContents = classContents;
            return this;
        }

        Builder literal(char chr, int length, int flatIndex) {
            node.chr = chr;
            node.length = length;
            node.flatIndex = flatIndex;
            return this;
        }

        Builder surrogatePair(char chr, char lowSurrogate) {
            node.chr = chr;
            node.lowSurrogate = lowSurrogate;
            return this;
        }

        Builder namedCapture(String name) {
            node.namedCaptureGroupName = name;
            return this;
        }

        Builder namedBackRef(int index) {
            node.namedCaptureGroupBackRefIndex = index;
            return this;
        }

        Builder unicodeProperty(int unicodeProperty, String propertyName) {
            node.unicodeProperty = unicodeProperty;
            node.propertyName = propertyName;
            return this;
        }

        Builder stringMatcher(StringMatcher stringMatcher) {
            node.stringMatcher = stringMatcher;
            return this;
        }

        RENode build() {
            return node;
        }
    }

    /**
     * Creates a new builder for constructing an RENode.
     *
     * @param op the regex operation bytecode
     * @return a new builder instance
     */
    static Builder builder(byte op) {
        return new Builder(op);
    }
}

class CompilerState {

    CompilerState(Context cx, char[] source, int length, int flags) {
        // Validate parameters
        if (cx == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        if (length > source.length) {
            throw new IllegalArgumentException(
                    "Length (" + length + ") exceeds source length (" + source.length + ")");
        }

        this.cx = cx;
        this.cpbegin = source;
        this.cp = 0;
        this.cpend = length;
        this.flags = flags;
        this.backReferenceLimit = Integer.MAX_VALUE;
        this.maxBackReference = 0;
        this.parenCount = 0;
        this.classCount = 0;
        this.progLength = 0;
        this.namedCaptureGroupsFound = false;
        this.namedCaptureBackRefs = new ArrayList<String>();
    }

    Context cx;
    char[] cpbegin;
    int cpend;
    int cp;
    int flags;
    int backReferenceLimit;
    int maxBackReference;
    int parenCount;
    int parenNesting;
    int classCount; /* number of [] encountered */
    int progLength; /* estimated bytecode length */

    boolean namedCaptureGroupsFound; // have we found any named capture groups?
    ArrayList<String> namedCaptureBackRefs;
    RENode result;
}

class REProgState {
    REProgState(
            REProgState previous,
            int min,
            int max,
            int index,
            REBackTrackData backTrack,
            boolean matchBackward,
            int continuationOp,
            int continuationPc) {
        this.previous = previous;
        this.min = min;
        this.max = max;
        this.index = index;
        this.continuationOp = continuationOp;
        this.continuationPc = continuationPc;
        this.backTrack = backTrack;
        this.matchBackward = matchBackward;
    }

    final REProgState previous; // previous state in stack

    final int min; /* current quantifier min */
    final int max; /* current quantifier max */
    final int index; /* progress in text */
    final int continuationOp;
    final int continuationPc;
    final REBackTrackData backTrack; // used by ASSERT_  to recover state
    final boolean matchBackward;
}

class REBackTrackData {

    REBackTrackData(
            REGlobalData gData, int op, int pc, int cp, int continuationOp, int continuationPc) {
        previous = gData.backTrackStackTop;
        this.op = op;
        this.pc = pc;
        this.cp = cp;
        this.continuationOp = continuationOp;
        this.continuationPc = continuationPc;
        parens = gData.parens;
        stateStackTop = gData.stateStackTop;
    }

    final REBackTrackData previous;

    final int op; /* operator */
    final int pc; /* bytecode pointer */
    final int cp; /* char buffer index */
    final int continuationOp; /* continuation op */
    final int continuationPc; /* continuation pc */
    final long[] parens; /* parenthesis captures */
    final REProgState stateStackTop; /* state of op that backtracked */
}

class REGlobalData {
    boolean multiline;
    RECompiled regexp; /* the RE in execution */
    int skipped; /* chars skipped anchoring this r.e. */

    int cp; /* char buffer index */
    long[] parens; /* parens captures */

    REProgState stateStackTop; /* stack of state of current ancestors */

    REBackTrackData backTrackStackTop; /* last matched-so-far position */

    /** Get start of parenthesis capture contents, -1 for empty. */
    int parensIndex(int i) {
        if (parens == null || i < 0 || i >= parens.length) {
            return -1;
        }
        return (int) parens[i];
    }

    /** Get length of parenthesis capture contents. */
    int parensLength(int i) {
        if (parens == null || i < 0 || i >= parens.length) {
            return 0;
        }
        return (int) (parens[i] >>> 32);
    }

    void setParens(int i, int index, int length) {
        // clone parens array if it is shared with backtrack state
        if (backTrackStackTop != null && backTrackStackTop.parens == parens) {
            parens = parens.clone();
        }
        parens[i] = (index & 0xffffffffL) | ((long) length << 32);
    }
}

/*
 * This struct holds a bitmap representation of a class from a regexp.
 * There's a list of these referenced by the classList field in the NativeRegExp
 * struct below. The initial state has startIndex set to the offset in the
 * original regexp source of the beginning of the class contents. The first
 * use of the class converts the source representation into a bitmap.
 *
 */
final class RECharSet implements Serializable {
    private static final long serialVersionUID = 7931787979395898394L;
    ArrayList<Integer> unicodeProps = new ArrayList<Integer>();
    ArrayList<Integer> negUnicodeProps = new ArrayList<Integer>();

    RECharSet(ClassContents classContents, int length) {
        this.length = length;
        this.classContents = classContents;
    }

    final int length;
    final ClassContents classContents;

    transient volatile boolean converted;
    transient volatile byte[] bits;

    /**
     * Cache for set operation results. Only allocated if class has set operations. Key: codepoint,
     * Value: match result after applying all set operations. transient: not serialized, will be
     * rebuilt on first use after deserialization.
     */
    transient volatile java.util.Map<Integer, Boolean> setOpCache;

    /**
     * Ensure the character set bitmap is built. Thread-safe lazy initialization.
     *
     * @param gData global regex data containing flags
     */
    void ensureBuilt(REGlobalData gData) {
        synchronized (this) {
            if (!converted) {
                build(gData);
                converted = true;
            }
        }
    }

    /** Add a single character to the bitmap. */
    private void addCharacter(char c) {
        int byteIndex = (c / 8);
        if (c >= length) {
            throw ScriptRuntime.constructError("SyntaxError", "invalid range in character class");
        }
        bits[byteIndex] |= (byte) (1 << (c & NativeRegExp.BYTE_BIT_MASK));
    }

    /** Add a character range (inclusive) to the bitmap. */
    private void addCharacterRange(char c1, char c2) {
        // Validate codepoints before calculating byte indices
        if (c1 >= length) {
            throw ScriptRuntime.constructError("SyntaxError", "invalid range in character class");
        }
        if (c2 >= length || c1 > c2) {
            throw ScriptRuntime.constructError("SyntaxError", "invalid range in character class");
        }

        int byteIndex1 = (c1 / 8);
        int byteIndex2 = (c2 / 8);

        c1 = (char) (c1 & NativeRegExp.BYTE_BIT_MASK);
        c2 = (char) (c2 & NativeRegExp.BYTE_BIT_MASK);

        if (byteIndex1 == byteIndex2) {
            bits[byteIndex1] |= (byte) ((0xFF >> (7 - (c2 - c1))) << c1);
        } else {
            bits[byteIndex1] |= (byte) (0xFF << c1);
            for (int i = byteIndex1 + 1; i < byteIndex2; i++) bits[i] = (byte) 0xFF;
            bits[byteIndex2] |= (byte) (0xFF >> (7 - c2));
        }
    }

    /**
     * Apply case folding to a character and add all variants.
     *
     * <p>Uses functional programming to eliminate duplicate case folding logic.
     *
     * @param ch Character to process
     * @param unicodeCaseFolding True for Unicode deep case closure (u or v flags with i flag)
     * @param basicCaseFolding True for basic ASCII case folding (i flag without u/v)
     */
    private void addCharacterWithCaseFolding(
            char ch, boolean unicodeCaseFolding, boolean basicCaseFolding) {
        addCharacter(ch);

        if (unicodeCaseFolding) {
            // Deep case closure for Unicode mode (u or v flags with i flag)
            // Gets all Unicode case variants using Java's built-in CLDR case mapping
            List<Character> variants = new ArrayList<>();
            StringMatcher.getUnicodeCaseVariants(ch, variants);
            variants.stream()
                    .filter(variant -> variant != ch) // Don't add the original again
                    .forEach(this::addCharacter);
        } else if (basicCaseFolding) {
            // Basic case folding for non-Unicode mode (i flag without u/v)
            // Only handles ASCII-style upper/lower case
            char upper = Character.toUpperCase(ch);
            char lower = Character.toLowerCase(ch);
            if (ch != upper) addCharacter(upper);
            if (ch != lower) addCharacter(lower);
        }
    }

    /**
     * Build the bitmap representation from ClassContents.
     *
     * @param gData global regex data containing flags
     */
    private void build(REGlobalData gData) {
        // Validate length to prevent overflow in bitmap size calculation
        if (length < 0) {
            throw new IllegalArgumentException("Invalid bitmap length: " + length);
        }
        // Allow up to MAX_CODE_POINT for Unicode support (non-BMP handled separately)
        if (length > Character.MAX_CODE_POINT + 1) {
            throw new IllegalArgumentException("Bitmap length too large: " + length);
        }
        int byteLength = (length + 7) / 8;
        // Check for integer overflow in byteLength calculation
        if (byteLength < 0) {
            throw new IllegalArgumentException("Bitmap size overflow: " + length);
        }
        bits = new byte[byteLength];

        // Determine if we should use Unicode case folding (deep case closure)
        boolean unicodeCaseFolding =
                (gData.regexp.flags & NativeRegExp.JSREG_FOLD) != 0
                        && ((gData.regexp.flags & NativeRegExp.JSREG_UNICODE) != 0
                                || (gData.regexp.flags & NativeRegExp.JSREG_UNICODESETS) != 0);
        boolean basicCaseFolding =
                (gData.regexp.flags & NativeRegExp.JSREG_FOLD) != 0 && !unicodeCaseFolding;

        // Unified Unicode storage - process all codepoints
        // Non-BMP codepoints are stored but not added to bitmap (handled by codepoints list check)
        classContents.codePoints.stream()
                .filter(codepoint -> codepoint <= Character.MAX_VALUE)
                .forEach(
                        codepoint -> {
                            char ch = (char) (int) codepoint;
                            addCharacterWithCaseFolding(ch, unicodeCaseFolding, basicCaseFolding);
                        });

        // Unified Unicode storage - process all ranges (both BMP and non-BMP)
        classContents.forEachRange(
                (start, end) -> {
                    // Only add BMP ranges to bitmap (non-BMP handled by ranges list check)
                    if (start <= Character.MAX_VALUE && end <= Character.MAX_VALUE) {
                        char startChar = (char) (int) start;
                        char endChar = (char) (int) end;
                        if (unicodeCaseFolding || basicCaseFolding) {
                            // Case folding required - expand range and apply folding to each char
                            for (char ch = startChar; ch <= endChar; ) {
                                addCharacterWithCaseFolding(
                                        ch, unicodeCaseFolding, basicCaseFolding);
                                if (++ch == 0) break; // overflow
                            }
                        } else {
                            // No case folding - add range directly
                            addCharacterRange(startChar, endChar);
                        }
                    }
                    // Non-BMP ranges are stored but not added to bitmap (handled by ranges
                    // list check)
                });

        for (RENode escape : classContents.escapeNodes) {
            switch (escape.op) {
                case NativeRegExp.REOP_DIGIT:
                    addCharacterRange('0', '9');
                    break;
                case NativeRegExp.REOP_NONDIGIT:
                    addCharacterRange((char) 0, (char) ('0' - 1));
                    addCharacterRange((char) ('9' + 1), (char) (length - 1));
                    break;
                case NativeRegExp.REOP_SPACE:
                    IntStream.range(0, length)
                            .filter(NativeRegExp::isREWhiteSpace)
                            .forEach(i -> addCharacter((char) i));
                    break;
                case NativeRegExp.REOP_NONSPACE:
                    IntStream.range(0, length)
                            .filter(i -> !NativeRegExp.isREWhiteSpace(i))
                            .forEach(i -> addCharacter((char) i));
                    break;
                case NativeRegExp.REOP_ALNUM:
                    IntStream.range(0, length)
                            .filter(i -> NativeRegExp.isWord((char) i))
                            .forEach(i -> addCharacter((char) i));
                    break;
                case NativeRegExp.REOP_NONALNUM:
                    IntStream.range(0, length)
                            .filter(i -> !NativeRegExp.isWord((char) i))
                            .forEach(i -> addCharacter((char) i));
                    break;
                case NativeRegExp.REOP_UPROP:
                    unicodeProps.add(escape.unicodeProperty);
                    break;
                case NativeRegExp.REOP_UPROP_NOT:
                    negUnicodeProps.add(escape.unicodeProperty);
                    break;
                case NativeRegExp.REOP_STRING_MATCHER:
                    // String matchers in character classes: extract characters and add to bitmap
                    // This handles cases like [\n\t] where escape sequences create StringMatcher
                    // nodes
                    String literal = escape.stringMatcher.getLiteral();
                    IntStream.range(0, literal.length())
                            .mapToObj(literal::charAt)
                            .forEach(
                                    ch ->
                                            addCharacterWithCaseFolding(
                                                    ch, unicodeCaseFolding, basicCaseFolding));
                    break;
                default:
                    Kit.codeBug("classContents contains invalid escape node type");
            }
        }
    }
}
