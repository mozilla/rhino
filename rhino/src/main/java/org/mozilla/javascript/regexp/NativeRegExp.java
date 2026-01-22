/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.AbstractEcmaObjectOperations;
import org.mozilla.javascript.AbstractEcmaStringOperations;
import org.mozilla.javascript.AbstractEcmaStringOperations.ReplacementOperation;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.ClassDescriptor;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JSDescriptor;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptRuntimeES6;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.config.RhinoConfig;

/**
 * This class implements the RegExp native object.
 *
 * <p>Revision History: Implementation in C by Brendan Eich Initial port to Java by Norris Boyd from
 * jsregexp.c version 1.36 Merged up to version 1.38, which included Unicode support. Merged bug
 * fixes in version 1.39. Merged JSFUN13_BRANCH changes up to 1.32.2.13
 *
 * @author Brendan Eich
 * @author Norris Boyd
 */
public class NativeRegExp extends ScriptableObject {
    private static final long serialVersionUID = 4965263491464903264L;

    private static final Object REGEXP_TAG = new Object();

    public static final int JSREG_GLOB = 0x1; // 'g' flag: global
    public static final int JSREG_FOLD = 0x2; // 'i' flag: fold
    public static final int JSREG_MULTILINE = 0x4; // 'm' flag: multiline
    public static final int JSREG_DOTALL = 0x8; // 's' flag: dotAll
    public static final int JSREG_STICKY = 0x10; // 'y' flag: sticky
    public static final int JSREG_UNICODE = 0x20; // 'u' flag: unicode mode

    // type of match to perform
    public static final int TEST = 0;
    public static final int MATCH = 1;
    public static final int PREFIX = 2;

    private static final boolean debug = RhinoConfig.get("rhino.debugRegexp", false);

    private static final byte REOP_SIMPLE_START = 1; /* start of 'simple opcodes' */
    private static final byte REOP_EMPTY =
            REOP_SIMPLE_START; /* match rest of input against rest of r.e. */
    private static final byte REOP_BOL =
            REOP_EMPTY + 1; /* beginning of input (or line if multiline) */
    private static final byte REOP_EOL = REOP_BOL + 1; /* end of input (or line if multiline) */
    private static final byte REOP_WBDRY = REOP_EOL + 1; /* match "" at word boundary */
    private static final byte REOP_WNONBDRY = REOP_WBDRY + 1; /* match "" at word non-boundary */
    private static final byte REOP_DOT = REOP_WNONBDRY + 1; /* stands for any character */
    private static final byte REOP_DIGIT = REOP_DOT + 1; /* match a digit char: [0-9] */
    private static final byte REOP_NONDIGIT = REOP_DIGIT + 1; /* match a non-digit char: [^0-9] */
    private static final byte REOP_ALNUM =
            REOP_NONDIGIT + 1; /* match an alphanumeric char: [0-9a-z_A-Z] */
    private static final byte REOP_NONALNUM =
            REOP_ALNUM + 1; /* match a non-alphanumeric char: [^0-9a-z_A-Z] */
    private static final byte REOP_SPACE = REOP_NONALNUM + 1; /* match a whitespace char */
    private static final byte REOP_NONSPACE = REOP_SPACE + 1; /* match a non-whitespace char */
    private static final byte REOP_BACKREF =
            REOP_NONSPACE + 1; /* back-reference (e.g., \1) to a parenthetical */
    private static final byte REOP_FLAT = REOP_BACKREF + 1; /* match a flat string */
    private static final byte REOP_FLAT1 = REOP_FLAT + 1; /* match a single char */
    private static final byte REOP_FLATi = REOP_FLAT1 + 1; /* case-independent REOP_FLAT */
    private static final byte REOP_FLAT1i = REOP_FLATi + 1; /* case-independent REOP_FLAT1 */
    private static final byte REOP_UCFLAT1 = REOP_FLAT1i + 1; /* single Unicode char */
    private static final byte REOP_UCFLAT1i = REOP_UCFLAT1 + 1; /* case-independent REOP_UCFLAT1 */
    private static final byte REOP_UCSPFLAT1 =
            REOP_UCFLAT1i + 1; /* single Unicode surrogate pair */
    private static final byte REOP_CLASS = REOP_UCSPFLAT1 + 1; /* character class with index */
    private static final byte REOP_NCLASS = REOP_CLASS + 1; /* negated character class with index */
    private static final byte REOP_NAMED_BACKREF = REOP_NCLASS + 1; /* named back-reference */
    private static final byte REOP_UPROP = REOP_NAMED_BACKREF + 1; /* unicode property */
    private static final byte REOP_UPROP_NOT = REOP_UPROP + 1; /* negated unicode property */
    private static final byte REOP_SIMPLE_END = REOP_UPROP_NOT; /* end of 'simple opcodes' */
    // REOP_SIMPLE_END is not a real opcode, but a sentinel for the end of the simple opcodes

    private static final byte REOP_QUANT = REOP_SIMPLE_END + 1; /* quantified atom: atom{1,2} */
    private static final byte REOP_STAR = REOP_QUANT + 1; /* zero or more occurrences of kid */
    private static final byte REOP_PLUS = REOP_STAR + 1; /* one or more occurrences of kid */
    private static final byte REOP_OPT = REOP_PLUS + 1; /* optional subexpression in kid */
    private static final byte REOP_LPAREN =
            REOP_OPT + 1; /* left paren bytecode: kid is u.num'th sub-regexp */
    private static final byte REOP_RPAREN = REOP_LPAREN + 1; /* right paren bytecode */
    private static final byte REOP_ALT =
            REOP_RPAREN + 1; /* alternative subexpressions in kid and next */
    private static final byte REOP_JUMP = REOP_ALT + 1; /* for deoptimized closure loops */
    private static final byte REOP_ASSERT =
            REOP_JUMP + 1; /* zero width positive lookahead assertion */
    private static final byte REOP_ASSERT_NOT =
            REOP_ASSERT + 1; /* zero width negative lookahead assertion */
    private static final byte REOP_ASSERTTEST =
            REOP_ASSERT_NOT + 1; /* sentinel at end of assertion child */
    private static final byte REOP_ASSERTNOTTEST =
            REOP_ASSERTTEST + 1; /* sentinel at end of !assertion child */
    private static final byte REOP_MINIMALSTAR =
            REOP_ASSERTNOTTEST + 1; /* non-greedy version of * */
    private static final byte REOP_MINIMALPLUS = REOP_MINIMALSTAR + 1; /* non-greedy version of + */
    private static final byte REOP_MINIMALOPT = REOP_MINIMALPLUS + 1; /* non-greedy version of ? */
    private static final byte REOP_MINIMALQUANT =
            REOP_MINIMALOPT + 1; /* non-greedy version of {} */
    private static final byte REOP_ENDCHILD =
            REOP_MINIMALQUANT + 1; /* sentinel at end of quantifier child */
    private static final byte REOP_REPEAT =
            REOP_ENDCHILD + 1; /* directs execution of greedy quantifier */
    private static final byte REOP_MINIMALREPEAT =
            REOP_REPEAT + 1; /* directs execution of non-greedy quantifier */
    private static final byte REOP_ALTPREREQ =
            REOP_MINIMALREPEAT + 1; /* prerequisite for ALT, either of two chars */
    private static final byte REOP_ALTPREREQi =
            REOP_ALTPREREQ + 1; /* case-independent REOP_ALTPREREQ */
    private static final byte REOP_ALTPREREQ2 =
            REOP_ALTPREREQi + 1; /* prerequisite for ALT, a char or a class */
    private static final byte REOP_ASSERTBACK =
            REOP_ALTPREREQ2 + 1; /* zero width positive lookbehind assertion */
    private static final byte REOP_ASSERTBACK_NOT =
            REOP_ASSERTBACK + 1; /* zero width negative lookbehind assertion */
    private static final byte REOP_ASSERTBACKTEST =
            REOP_ASSERTBACK_NOT + 1; /* sentinel at end of assertion child */
    private static final byte REOP_ASSERTBACKNOTTEST =
            REOP_ASSERTBACKTEST + 1; /* sentinel at end of !assertion child */

    private static final byte REOP_END = REOP_ASSERTBACKNOTTEST + 1;

    private static final int ANCHOR_BOL = -2;

    private static final ClassDescriptor DESCRIPTOR;
    private static final JSDescriptor<JSFunction> EXEC_DESCRIPTOR;

    static {
        DESCRIPTOR =
                NativeRegExpCtor.makeCtorBuilder()
                        .withMethod(PROTO, "compile", 2, NativeRegExp::js_compile)
                        .withMethod(PROTO, "toString", 0, NativeRegExp::js_toString)
                        .withMethod(PROTO, "toSource", 0, NativeRegExp::js_toSource)
                        .withMethod(PROTO, "exec", 1, NativeRegExp::js_exec)
                        .withMethod(PROTO, "test", 1, NativeRegExp::js_test)
                        .withMethod(PROTO, "prefix", 1, NativeRegExp::js_prefix)
                        .withMethod(PROTO, SymbolKey.MATCH, 1, NativeRegExp::js_match)
                        .withMethod(PROTO, SymbolKey.MATCH_ALL, 1, NativeRegExp::js_matchAll)
                        .withMethod(PROTO, SymbolKey.SEARCH, 1, NativeRegExp::js_search)
                        .withMethod(PROTO, SymbolKey.REPLACE, 2, NativeRegExp::js_replace)
                        .withMethod(PROTO, SymbolKey.SPLIT, 2, NativeRegExp::js_split)
                        .withProp(CTOR, SymbolKey.SPECIES, ScriptRuntimeES6::symbolSpecies)
                        .withProp(
                                PROTO,
                                "source",
                                (c) -> new String(realThis(c, "get source").re.source),
                                null,
                                DONTENUM | READONLY | PERMANENT)
                        .withProp(
                                PROTO,
                                "flags",
                                (c) -> {
                                    StringBuilder buf = new StringBuilder();
                                    if (!ScriptRuntime.isObject(c)) {
                                        throw ScriptRuntime.typeErrorById(
                                                "msg.arg.not.object", ScriptRuntime.typeof(c));
                                    }
                                    appendFlags(c, buf);
                                    return buf.toString();
                                },
                                null,
                                DONTENUM | READONLY)
                        .withProp(
                                PROTO,
                                "global",
                                (c) ->
                                        ScriptRuntime.wrapBoolean(
                                                (realThis(c, "get global").re.flags & JSREG_GLOB)
                                                        != 0),
                                null,
                                DONTENUM | READONLY)
                        .withProp(
                                PROTO,
                                "ignoreCase",
                                (c) ->
                                        ScriptRuntime.wrapBoolean(
                                                (realThis(c, "").re.flags & JSREG_FOLD) != 0),
                                null,
                                DONTENUM | READONLY)
                        .withProp(
                                PROTO,
                                "multiline",
                                (c) ->
                                        ScriptRuntime.wrapBoolean(
                                                (realThis(c, "").re.flags & JSREG_MULTILINE) != 0),
                                null,
                                DONTENUM | READONLY)
                        .withProp(
                                PROTO,
                                "dotAll",
                                (c) ->
                                        ScriptRuntime.wrapBoolean(
                                                (realThis(c, "").re.flags & JSREG_DOTALL) != 0),
                                null,
                                DONTENUM | READONLY)
                        .withProp(
                                PROTO,
                                "sticky",
                                (c) ->
                                        ScriptRuntime.wrapBoolean(
                                                (realThis(c, "").re.flags & JSREG_STICKY) != 0),
                                null,
                                DONTENUM | READONLY)
                        .withProp(
                                PROTO,
                                "unicode",
                                (c) ->
                                        ScriptRuntime.wrapBoolean(
                                                (realThis(c, "").re.flags & JSREG_UNICODE) != 0),
                                null,
                                DONTENUM | READONLY)
                        .build();
        EXEC_DESCRIPTOR = DESCRIPTOR.findProtoDesc("exec");
    }

    static Object init(Context cx, Scriptable scope, boolean sealed) {

        NativeRegExp proto = NativeRegExpInstantiator.withLanguageVersion(cx.getLanguageVersion());
        proto.re = compileRE(cx, "", null, false);

        return DESCRIPTOR.buildConstructor(cx, scope, proto, sealed);
    }

    NativeRegExp(Scriptable scope, RECompiled regexpCompiled) {
        this.re = regexpCompiled;
        // This needs to be built in.
        createLastIndexProp();
        setLastIndex(ScriptRuntime.zeroObj);
        ScriptRuntime.setBuiltinProtoAndParent(this, scope, TopLevel.Builtins.RegExp);
    }

    NativeRegExp() {
        createLastIndexProp();
    }

    private void createLastIndexProp() {
        ScriptableObject.defineBuiltInProperty(
                this,
                "lastIndex",
                lastIndexAttr,
                NativeRegExp::lastIndexGetter,
                NativeRegExp::lastIndexSetter,
                NativeRegExp::lastIndexAttrSetter);
    }

    private static Object lastIndexGetter(NativeRegExp regexp, Scriptable start) {
        return regexp.lastIndex;
    }

    private static boolean lastIndexSetter(
            NativeRegExp builtIn,
            Object value,
            Scriptable owner,
            Scriptable start,
            boolean isThrow) {
        builtIn.setLastIndex(value);
        return true;
    }

    private static void lastIndexAttrSetter(NativeRegExp builtIn, int attrs) {
        builtIn.lastIndexAttr = attrs;
    }

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
        StringBuilder buf = new StringBuilder();
        buf.append('/');
        if (re.source.length != 0) {
            buf.append(re.source);
        } else {
            // See bugzilla 226045
            buf.append("(?:)");
        }
        buf.append('/');
        appendFlags(this, buf);
        return buf.toString();
    }

    private static void appendFlags(Scriptable thisObj, StringBuilder buf) {
        if (ScriptRuntime.toBoolean(ScriptableObject.getProperty(thisObj, "global")))
            buf.append('g');
        if (ScriptRuntime.toBoolean(ScriptableObject.getProperty(thisObj, "ignoreCase")))
            buf.append('i');
        if (ScriptRuntime.toBoolean(ScriptableObject.getProperty(thisObj, "multiline")))
            buf.append('m');
        if (ScriptRuntime.toBoolean(ScriptableObject.getProperty(thisObj, "dotAll")))
            buf.append('s');
        if (ScriptRuntime.toBoolean(ScriptableObject.getProperty(thisObj, "sticky")))
            buf.append('y');
        if (ScriptRuntime.toBoolean(ScriptableObject.getProperty(thisObj, "unicode")))
            buf.append('u');
    }

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
        double d = ScriptRuntime.toInteger(lastIndex);
        if (globalOrSticky) {
            if (d < 0 || str.length() < d) {
                setLastIndex(ScriptRuntime.zeroObj);
                return null;
            }
        } else {
            d = 0;
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

    private static void prettyPrintRE(RECompiled regexp) {
        for (int pc = 0; regexp.program[pc] != REOP_END; ) {
            System.out.print(pc + ": ");
            byte op = regexp.program[pc];
            pc++; // Increment pc after reading op
            switch (op) {
                case REOP_EMPTY:
                    System.out.println("EMPTY");
                    break;
                case REOP_BOL:
                    System.out.println("BOL");
                    break;
                case REOP_EOL:
                    System.out.println("EOL");
                    break;
                case REOP_WBDRY:
                    System.out.println("WBDRY");
                    break;
                case REOP_WNONBDRY:
                    System.out.println("WNONBDRY");
                    break;
                case REOP_DOT:
                    System.out.println("DOT");
                    break;
                case REOP_DIGIT:
                    System.out.println("DIGIT");
                    break;
                case REOP_NONDIGIT:
                    System.out.println("NONDIGIT");
                    break;
                case REOP_ALNUM:
                    System.out.println("ALNUM");
                    break;
                case REOP_NONALNUM:
                    System.out.println("NONALNUM");
                    break;
                case REOP_SPACE:
                    System.out.println("SPACE");
                    break;
                case REOP_NONSPACE:
                    System.out.println("NONSPACE");
                    break;
                case REOP_BACKREF:
                    int backrefIndex = getIndex(regexp.program, pc);
                    System.out.println("BACKREF " + backrefIndex);
                    pc += INDEX_LEN;
                    break;
                case REOP_NAMED_BACKREF:
                    int namedBackrefIndex = getIndex(regexp.program, pc);
                    System.out.println(
                            "NAMED_BACKREF " + regexp.namedBackRefs.get(namedBackrefIndex));
                    pc += 2 * INDEX_LEN;
                    break;
                case REOP_FLAT:
                    int flatIndex = getIndex(regexp.program, pc);
                    int flatLength = getIndex(regexp.program, pc + INDEX_LEN);
                    System.out.print("FLAT: ");
                    for (int i = 0; i < flatLength; i++) {
                        System.out.print(regexp.source[flatIndex + i]);
                    }
                    System.out.println();
                    pc += 2 * INDEX_LEN;
                    break;
                case REOP_FLAT1:
                    char flat1Char = (char) (regexp.program[pc] & 0xFF);
                    System.out.println("FLAT1: " + flat1Char);
                    pc += 1;
                    break;
                case REOP_FLATi:
                    int flatiIndex = getIndex(regexp.program, pc);
                    int flatiLength = getIndex(regexp.program, pc + INDEX_LEN);
                    System.out.print("FLATi: ");
                    for (int i = 0; i < flatiLength; i++) {
                        System.out.print(regexp.source[flatiIndex + i]);
                    }
                    System.out.println();
                    pc += 2 * INDEX_LEN;
                    break;
                case REOP_FLAT1i:
                    char flat1iChar = (char) (regexp.program[pc] & 0xFF);
                    System.out.println("FLAT1i: " + flat1iChar);
                    pc += 1;
                    break;
                case REOP_UCFLAT1:
                    char ucFlat1Char = (char) getIndex(regexp.program, pc);
                    System.out.println("UCFLAT1: " + ucFlat1Char);
                    pc += INDEX_LEN;
                    break;
                case REOP_UCFLAT1i:
                    char ucFlat1iChar = (char) getIndex(regexp.program, pc);
                    System.out.println("UCFLAT1i: " + ucFlat1iChar);
                    pc += INDEX_LEN;
                    break;
                case REOP_UCSPFLAT1:
                    // high and low surrogates
                    char highSurrogate = (char) getIndex(regexp.program, pc);
                    pc += INDEX_LEN;
                    char lowSurrogate = (char) getIndex(regexp.program, pc);
                    pc += INDEX_LEN;
                    System.out.println(
                            "UCSPFLAT1: "
                                    + Character.toString(
                                            Character.toCodePoint(highSurrogate, lowSurrogate)));
                    break;
                case REOP_CLASS:
                    int classIndex = getIndex(regexp.program, pc);
                    System.out.println("CLASS: " + classIndex);
                    pc += INDEX_LEN;
                    break;
                case REOP_NCLASS:
                    int nclassIndex = getIndex(regexp.program, pc);
                    System.out.println("NCLASS: " + nclassIndex);
                    pc += INDEX_LEN;
                    break;
                case REOP_STAR:
                case REOP_PLUS:
                case REOP_OPT:
                case REOP_MINIMALSTAR:
                case REOP_MINIMALPLUS:
                case REOP_MINIMALOPT:
                case REOP_MINIMALQUANT:
                case REOP_QUANT:
                    {
                        boolean greedy;
                        int min, max;

                        greedy =
                                op == REOP_STAR
                                        || op == REOP_PLUS
                                        || op == REOP_OPT
                                        || op == REOP_QUANT;

                        // set min and max
                        if (op == REOP_STAR || op == REOP_MINIMALSTAR) {
                            min = 0;
                            max = Integer.MAX_VALUE;
                        } else if (op == REOP_PLUS || op == REOP_MINIMALPLUS) {
                            min = 1;
                            max = Integer.MAX_VALUE;
                        } else if (op == REOP_OPT || op == REOP_MINIMALOPT) {
                            min = 0;
                            max = 1;
                        } else {
                            min = getIndex(regexp.program, pc);
                            max = getIndex(regexp.program, pc + INDEX_LEN);
                            pc += 2 * INDEX_LEN;
                        }

                        int parenCount = getIndex(regexp.program, pc);
                        int parenIndex = getIndex(regexp.program, pc + INDEX_LEN);
                        pc += 2 * INDEX_LEN;

                        int next = getIndex(regexp.program, pc) + pc;
                        System.out.println(
                                "QUANT "
                                        + "greedy="
                                        + greedy
                                        + " min="
                                        + min
                                        + " max="
                                        + (max == Integer.MAX_VALUE ? "MAX" : max)
                                        + " parenCount="
                                        + parenCount
                                        + " parenIndex="
                                        + parenIndex
                                        + " next="
                                        + next);
                        pc += INDEX_LEN;
                    }
                    break;
                case REOP_LPAREN:
                    int parenIndex = getIndex(regexp.program, pc);
                    System.out.println("LPAREN: " + parenIndex);
                    pc += INDEX_LEN;
                    break;
                case REOP_RPAREN:
                    System.out.println("RPAREN");
                    pc += INDEX_LEN;
                    break;
                case REOP_ALT:
                    int altIndex = getIndex(regexp.program, pc);
                    System.out.println("ALT: " + altIndex);
                    pc += INDEX_LEN;
                    break;
                case REOP_JUMP:
                    int jumpIndex = getIndex(regexp.program, pc) + pc;
                    System.out.println("JUMP: " + jumpIndex);
                    pc += INDEX_LEN;
                    break;
                case REOP_ASSERT:
                    int assertNextPc = pc + getIndex(regexp.program, pc);
                    System.out.println("ASSERT: " + assertNextPc);
                    pc += INDEX_LEN;
                    break;
                case REOP_ASSERT_NOT:
                    int assertNotNextPc = pc + getIndex(regexp.program, pc);
                    System.out.println("ASSERT_NOT: " + assertNotNextPc);
                    pc += INDEX_LEN;
                    break;
                case REOP_ASSERTBACK:
                    int assertBackNextPc = pc + getIndex(regexp.program, pc);
                    System.out.println("ASSERTBACK: " + assertBackNextPc);
                    pc += INDEX_LEN;
                    break;
                case REOP_ASSERTBACK_NOT:
                    int assertBackNotNextPc = pc + getIndex(regexp.program, pc);
                    System.out.println("ASSERTBACK_NOT: " + assertBackNotNextPc);
                    pc += INDEX_LEN;
                    break;
                case REOP_ASSERTTEST:
                    System.out.println("ASSERTTEST");
                    break;
                case REOP_ASSERTNOTTEST:
                    System.out.println("ASSERTNOTTEST");
                    break;
                case REOP_ASSERTBACKTEST:
                    System.out.println("ASSERTBACKTEST");
                    break;
                case REOP_ASSERTBACKNOTTEST:
                    System.out.println("ASSERTBACKNOTTEST");
                    break;
                case REOP_ENDCHILD:
                    System.out.println("ENDCHILD");
                    break;
                case REOP_REPEAT:
                    System.out.println("REPEAT");
                    break;
                case REOP_MINIMALREPEAT:
                    System.out.println("MINIMALREPEAT");
                    break;
                case REOP_ALTPREREQ:
                case REOP_ALTPREREQi:
                case REOP_ALTPREREQ2:
                    String opCode =
                            (op == REOP_ALTPREREQ)
                                    ? "REOP_ALTPREREQ"
                                    : (op == REOP_ALTPREREQi)
                                            ? "REOP_ALTPREREQi"
                                            : "REOP_ALTPREREQ2";
                    char matchCh1 = (char) getIndex(regexp.program, pc);
                    pc += INDEX_LEN;
                    char matchCh2 = (char) getIndex(regexp.program, pc);
                    pc += INDEX_LEN;
                    int nextPc = pc + getIndex(regexp.program, pc);
                    pc += INDEX_LEN;
                    System.out.println(opCode + " " + matchCh1 + " " + matchCh2 + " " + nextPc);
                    break;
                case REOP_END:
                    System.out.println("END");
                    break;
                default:
                    System.out.println("UNKNOWN: " + op);
                    break;
            }
        }
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

                    for (Map.Entry<String, List<Integer>> entry : groupCaptures2.entrySet()) {
                        groupCaptures1.merge(
                                entry.getKey(),
                                entry.getValue(),
                                (v1, v2) -> {
                                    v1.addAll(v2);
                                    return v1;
                                });
                    }

                    for (Map.Entry<String, List<Integer>> entry : groupCaptures1.entrySet()) {
                        if (namedCaptureGroups.putIfAbsent(entry.getKey(), entry.getValue())
                                != null) {
                            reportError("msg.duplicate.group.name", entry.getKey());
                        }
                    }
                }
            } else {
                extractNamedCaptureGroups(src, node.kid, namedCaptureGroups);
            }
            node = node.next;
        }
    }

    static RECompiled compileRE(Context cx, String str, String global, boolean flat) {
        RECompiled regexp = new RECompiled(str);
        int length = str.length();
        int flags = 0;
        if (global != null) {
            for (int i = 0; i < global.length(); i++) {
                char c = global.charAt(i);
                int f = 0;
                if (c == 'g') {
                    f = JSREG_GLOB;
                } else if (c == 'i') {
                    f = JSREG_FOLD;
                } else if (c == 'm') {
                    f = JSREG_MULTILINE;
                } else if (c == 's') {
                    f = JSREG_DOTALL;
                } else if (c == 'y') {
                    f = JSREG_STICKY;
                } else if (c == 'u') {
                    f = JSREG_UNICODE;
                } else {
                    reportError("msg.invalid.re.flag", String.valueOf(c));
                }
                if ((flags & f) != 0) {
                    reportError("msg.invalid.re.flag", String.valueOf(c));
                }
                flags |= f;
            }
        }

        // We don't support u and i flags together, yet.
        if ((flags & JSREG_UNICODE) != 0 && (flags & JSREG_FOLD) != 0) {
            reportError("msg.invalid.re.flag", "u and i");
        }

        // We support unicode mode in ES6 and later.
        if ((flags & JSREG_UNICODE) != 0 && cx.getLanguageVersion() < Context.VERSION_ES6) {
            reportError("msg.invalid.re.flag", "u");
        }

        regexp.flags = flags;

        CompilerState state = new CompilerState(cx, regexp.source, length, flags);
        if (flat && length > 0) {
            if (debug) {
                System.out.println("flat = \"" + str + "\"");
            }
            state.result = new RENode(REOP_FLAT);
            state.result.chr = state.cpbegin[0];
            state.result.length = length;
            state.result.flatIndex = 0;
            state.progLength += 5;
        } else {
            boolean unicodeMode = (flags & JSREG_UNICODE) != 0;
            // if unicode mode is on, named capture groups are always on
            ParserParameters params = new ParserParameters(unicodeMode, unicodeMode);

            if (!parseDisjunction(state, params)) return null;
            // Need to reparse if pattern contains invalid backreferences:
            // "Note: if the number of left parentheses is less than the number
            // specified in \#, the \# is taken as an octal escape"
            CompilerState reParseState = null;
            if (state.maxBackReference > state.parenCount) {
                if (params.unicodeMode) {
                    reportError("msg.invalid.escape", "");
                } else {
                    // Need to reparse if pattern contains invalid backreferences:
                    // "Note: if the number of left parentheses is less than the number
                    // specified in \#, the \# is taken as an octal escape"
                    reParseState = new CompilerState(cx, regexp.source, length, flags);
                    reParseState.backReferenceLimit = state.parenCount;
                }
            }
            if (state.namedCaptureGroupsFound && !params.namedCaptureGroups) {
                params.namedCaptureGroups = true;
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

        if (debug) {
            System.out.println("Prog. length = " + endPC);
            for (int i = 0; i < endPC; i++) {
                System.out.print(regexp.program[i]);
                if (i < (endPC - 1)) System.out.print(", ");
            }
            System.out.println();

            prettyPrintRE(regexp);
        }
        regexp.parenCount = state.parenCount;

        // If re starts with literal, init anchorCh accordingly
        switch (regexp.program[0]) {
            case REOP_UCFLAT1:
            case REOP_UCFLAT1i:
                regexp.anchorCodePoint = (char) getIndex(regexp.program, 1);
                break;
            case REOP_FLAT1:
            case REOP_FLAT1i:
                regexp.anchorCodePoint = (char) (regexp.program[1] & 0xFF);
                break;
            case REOP_FLAT:
            case REOP_FLATi:
                int k = getIndex(regexp.program, 1);
                regexp.anchorCodePoint = regexp.source[k];
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

    static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    private static boolean isWord(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || isDigit(c) || c == '_';
    }

    private static boolean isControlLetter(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
    }

    private static boolean isLineTerm(char c) {
        return ScriptRuntime.isJSLineTerminator(c);
    }

    private static boolean isREWhiteSpace(int c) {
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
    private static char upcase(char ch) {
        if (ch < 128) {
            if ('a' <= ch && ch <= 'z') {
                return (char) (ch + ('A' - 'a'));
            }
            return ch;
        }
        char cu = Character.toUpperCase(ch);
        return (cu < 128) ? ch : cu;
    }

    private static char downcase(char ch) {
        if (ch < 128) {
            if ('A' <= ch && ch <= 'Z') {
                return (char) (ch + ('a' - 'A'));
            }
            return ch;
        }
        char cl = Character.toLowerCase(ch);
        return (cl < 128) ? ch : cl;
    }

    static class ParserParameters {
        boolean namedCaptureGroups;
        boolean unicodeMode;

        ParserParameters(boolean namedCaptureGroups, boolean unicodeMode) {
            this.namedCaptureGroups = namedCaptureGroups;
            this.unicodeMode = unicodeMode;
        }
    }

    /*
     * Top-down regular expression grammar, based closely on Perl4.
     *
     *  regexp:     altern                  A regular expression is one or more
     *              altern '|' regexp       alternatives separated by vertical bar.
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
             * Look at both alternates to see if there's a FLAT or a CLASS at
             * the start of each. If so, use a prerequisite match.
             *
             * TODO: Include FLAT with non-zero lowSurrogate for a
             *  prerequisite match.
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

    /*
     *  altern:     item                    An alternative is one or more items,
     *              item altern             concatenated together.
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
            // Mark greedy quantifier as atomic if child can't overlap with next
            if (tailTerm.op == REOP_QUANT && tailTerm.greedy) {
                if (!couldQuantifierChildOverlap(tailTerm.kid, tailTerm.next, state.flags)) {
                    tailTerm.atomic = true;
                }
            }
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

    /* calculate the total size of the bitmap required for a class expression */
    private static boolean calculateBitmapSize(
            int flags, ClassContents classContents, RENode target) {
        int max = 0;

        for (char ch : classContents.chars) {
            if (ch > max) {
                max = ch;
            }
            if ((flags & JSREG_FOLD) != 0) {
                char cu = upcase(ch);
                char cd = downcase(ch);
                int n = (cu >= cd) ? cu : cd;
                if (n > max) {
                    max = n;
                }
            }
        }

        for (int i = 1; i < classContents.bmpRanges.size(); i += 2) {
            char rangeEnd = classContents.bmpRanges.get(i);
            if (rangeEnd > max) {
                max = rangeEnd;
            }
            if ((flags & JSREG_FOLD) != 0) {
                char cu = upcase(rangeEnd);
                char cd = downcase(rangeEnd);
                int n = (cu >= cd) ? cu : cd;
                if (n > max) {
                    max = n;
                }
            }
        }

        for (RENode node : classContents.escapeNodes) {
            if (node.op != REOP_FLAT) {
                target.bmsize = Character.MAX_VALUE + 1;
                break;
            }
        }

        target.bmsize = Math.max(target.bmsize, max + 1);
        return true;
    }

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
        state.result = new RENode(REOP_FLAT);
        state.result.chr = c;
        state.result.lowSurrogate = 0; /* valid range is 0xD800-0xDFFF */
        state.result.length = 1;
        state.result.flatIndex = -1;
        state.progLength += 3;
    }

    private static void doFlatSurrogatePair(CompilerState state, char high, char low) {
        state.result = new RENode(REOP_FLAT);
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
                int v = value * 10 + (c - '0');
                if (v < 65535) {
                    value = v;
                } else {
                    overflow = true;
                    value = 65535;
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
                int n = readRegExpUnicodeEscapeSequence(state, new ParserParameters(false, true));
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

            if (params.unicodeMode) {
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
                    if (params.namedCaptureGroups) {
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
                if (params.unicodeMode || i == 0) {
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
            if (params.unicodeMode) return parseUnicodeCodePoint(state);
        }

        if (params.unicodeMode) {
            if (Character.isHighSurrogate((char) n)) {
                if (state.cp + 2 < state.cpend
                        && src[state.cp] == '\\'
                        && src[state.cp + 1] == 'u') {
                    state.cp += 2;
                    int n2 = readNHexDigits(state, 4, params);
                    if (n2 < 0) {
                        state.cp -= 2;
                    } else if (Character.isLowSurrogate((char) n2)) {
                        return Character.toCodePoint((char) n, (char) n2);
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
        } else if (n <= 0xFFFF) doFlat(state, (char) n);
        else {
            doFlatSurrogatePair(state, Character.highSurrogate(n), Character.lowSurrogate(n));
        }
        return true;
    }

    // only in the format \p{X} or \P{X}. Assume the \ has been consumed.
    // depending on p or P choose PROP_UPROP or PROP_UPROP_NOT.
    // X is ASCII letter, decimal or underscore
    public static boolean parseUnicodePropertyEscape(CompilerState state) {
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

    // Follows Annex B.1.2 of the ECMAScript specification
    private static boolean parseCharacterAndCharacterClassEscape(
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
                    if (params.unicodeMode) {
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
                if (params.unicodeMode) {
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
                state.result = new RENode(REOP_DIGIT);
                state.progLength++;
                break;
            case 'D':
                state.result = new RENode(REOP_NONDIGIT);
                state.progLength++;
                break;
            case 's':
                state.result = new RENode(REOP_SPACE);
                state.progLength++;
                break;
            case 'S':
                state.result = new RENode(REOP_NONSPACE);
                state.progLength++;
                break;
            case 'w':
                state.result = new RENode(REOP_ALNUM);
                state.progLength++;
                break;
            case 'W':
                state.result = new RENode(REOP_NONALNUM);
                state.progLength++;
                break;
            case 'p':
            case 'P':
                state.cp--;
                if (!parseUnicodePropertyEscape(state)) {
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

    static class ClassContents {
        boolean sense = true;
        ArrayList<Character> chars = new ArrayList<>();
        ArrayList<Character> bmpRanges =
                new ArrayList<>(); // ranges stored as (start1, end1, start2, end2, ...)
        ArrayList<RENode> escapeNodes = new ArrayList<>();
        ArrayList<Integer> nonBMPRanges =
                new ArrayList<Integer>(); // ranges stored as (start1, end1, start2, end2, ...)
        ArrayList<Integer> nonBMPCodepoints = new ArrayList<Integer>();
    }

    private static ClassContents parseClassContents(CompilerState state, ParserParameters params) {
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

        while (state.cp != state.cpend && src[state.cp] != ']') {
            if (src[state.cp] == '\\') {
                state.cp++;
                if (state.cp < state.cpend && src[state.cp] == 'b') {
                    state.cp++;
                    thisCodePoint = (char) 0x08;
                } else if (params.unicodeMode && state.cp < state.cpend && src[state.cp] == '-') {
                    state.cp++;
                    thisCodePoint = '-';
                } else {
                    if (!parseCharacterAndCharacterClassEscape(state, params)) {
                        if (src[state.cp] == 'c'
                                && !params.unicodeMode) { // when lookahead=c, parse the \\ as a
                            // literal
                            thisCodePoint = '\\';
                        } else {
                            reportError("msg.invalid.escape", "");
                            return null;
                        }
                    } else {
                        if (state.result.op == REOP_FLAT) {
                            if (state.result.lowSurrogate == 0) {
                                thisCodePoint = state.result.chr;
                            } else {
                                thisCodePoint =
                                        Character.toCodePoint(
                                                state.result.chr, state.result.lowSurrogate);
                            }
                        } else {
                            contents.escapeNodes.add(state.result);
                            if (inRange) {
                                if (!params.unicodeMode) {
                                    contents.chars.add('-');
                                    inRange = false;
                                } else {
                                    reportError("msg.invalid.class", "");
                                }
                            } else {
                                // if we have a '-' after this and we're in unicode mode, we fail
                                if (state.cp < state.cpend
                                        && src[state.cp] == '-'
                                        && params.unicodeMode) {
                                    reportError("msg.invalid.class", "");
                                }
                            }
                            // multi-character character escapes can't be part of ranges
                            continue;
                        }
                    }
                }
            } else {
                if ((state.flags & JSREG_UNICODE) != 0) {
                    thisCodePoint = Character.codePointAt(src, state.cp, state.cpend);
                    state.cp += Character.charCount(thisCodePoint);
                } else {
                    thisCodePoint = src[state.cp];
                    state.cp++;
                }
            }
            if (inRange) {
                if (rangeStart > thisCodePoint) {
                    reportError("msg.bad.range", "");
                    return null;
                }
                inRange = false;
                if (rangeStart > 0xFFFF || thisCodePoint > 0xFFFF) {
                    contents.nonBMPRanges.add(rangeStart);
                    contents.nonBMPRanges.add(thisCodePoint);
                } else {
                    contents.bmpRanges.add((char) rangeStart);
                    contents.bmpRanges.add((char) thisCodePoint);
                }
            } else {
                if (thisCodePoint > 0xFFFF) {
                    contents.nonBMPCodepoints.add(thisCodePoint);
                } else {
                    contents.chars.add((char) thisCodePoint);
                }
                if (state.cp + 1 < state.cpend && src[state.cp + 1] != ']') {
                    if (src[state.cp] == '-') {
                        state.cp++;
                        inRange = true;
                        rangeStart = thisCodePoint;
                    }
                }
            }
        }

        if (state.cp < state.cpend && src[state.cp] == ']') {
            state.cp++;
        }

        return contents;
    }

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
                // atom escape; B.1.2 of the ECMAScript specification
                if (state.cp < state.cpend) {
                    c = src[state.cp++];
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
                            // decimal escape
                            termStart = state.cp - 1;
                            num = getDecimalValue(c, state, "msg.overlarge.backref");
                            if (!params.unicodeMode && num > state.backReferenceLimit) {
                                reportWarning(state.cx, "msg.bad.backref", "");
                                state.cp = termStart;
                                if (!parseCharacterAndCharacterClassEscape(state, params))
                                    return false;
                            } else {
                                state.result = new RENode(REOP_BACKREF);
                                state.result.parenIndex = num - 1;
                                state.progLength += 3;
                                if (state.maxBackReference < num) {
                                    state.maxBackReference = num;
                                }
                            }
                            break;
                        case '0':
                            if (state.cp < state.cpend && src[state.cp] == '0') {
                                if (params.unicodeMode) {
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
                                break;
                            }
                        /* fall through */
                        default:
                            state.cp--;
                            if (!parseCharacterAndCharacterClassEscape(state, params)) {
                                if (c == 'k' && params.namedCaptureGroups) {
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
                                } else if ('c' == c
                                        && !params.unicodeMode) { // in ExtendedAtom, when
                                    // lookahead=c, parse the \\ as a
                                    // literal
                                    doFlat(state, '\\');
                                } else {
                                    reportError("msg.invalid.escape", "");
                                    return false;
                                }
                            }
                    }
                    break;
                }
                /* a trailing '\' is an error */
                reportError("msg.trail.backslash", "");
                break;
            case '(':
                {
                    RENode result = null;
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
                    } else if (state.cp + 2 < state.cpend
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
                    } else {
                        result = new RENode(REOP_LPAREN);
                        if (state.cp + 2 < state.cpend
                                && src[state.cp] == '?'
                                && src[state.cp + 1] == '<') {
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
                    ++state.parenNesting;
                    if (!parseDisjunction(state, params)) return false;
                    if (state.cp == state.cpend || src[state.cp] != ')') {
                        reportError("msg.unterm.paren", "");
                        return false;
                    }
                    ++state.cp;
                    --state.parenNesting;
                    if (result != null) {
                        /* if we have a lookbehind then we reverse state.result linked list */
                        if (result.op == REOP_ASSERTBACK || result.op == REOP_ASSERTBACK_NOT) {
                            state.result = reverseNodeList(state.result);
                        }
                        result.kid = state.result;
                        state.result = result;
                    }
                    break;
                }
            case ')':
                reportError("msg.re.unmatched.right.paren", "");
                return false;
            case '[':
                ClassContents classContents = parseClassContents(state, params);
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
                if (!calculateBitmapSize(state.flags, classContents, state.result)) return false;
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
                    if (params.unicodeMode && (c == ']' || c == '{' || c == '}'))
                        reportError("msg.lone.quantifier.bracket", "");

                    if (params.unicodeMode
                            && Character.isHighSurrogate(c)
                            && state.cp < state.cpend
                            && Character.isLowSurrogate(src[state.cp])) {
                        char low = src[state.cp++];
                        doFlatSurrogatePair(state, c, low);
                    } else {
                        state.result = new RENode(REOP_FLAT);
                        state.result.chr = c;
                        state.result.length = 1;
                        state.result.flatIndex = state.cp - 1;
                        state.progLength += 3;
                    }
                    break;
                }
        }

        term = state.result;
        if (state.cp == state.cpend) {
            return true;
        }
        boolean hasQ = false;
        switch (src[state.cp]) {
            case '+':
                state.result = new RENode(REOP_QUANT);
                state.result.min = 1;
                state.result.max = -1;
                /* <PLUS>, <parencount>, <parenindex>, <atomic>, <next> ... <ENDCHILD> */
                state.progLength += 9;
                hasQ = true;
                break;
            case '*':
                state.result = new RENode(REOP_QUANT);
                state.result.min = 0;
                state.result.max = -1;
                /* <STAR>, <parencount>, <parenindex>, <atomic>, <next> ... <ENDCHILD> */
                state.progLength += 9;
                hasQ = true;
                break;
            case '?':
                state.result = new RENode(REOP_QUANT);
                state.result.min = 0;
                state.result.max = 1;
                /* <OPT>, <parencount>, <parenindex>, <atomic>, <next> ... <ENDCHILD> */
                state.progLength += 9;
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
                                // <parenindex>, <atomic>, <next> ... <ENDCHILD>
                                state.progLength += 13;
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
        if (!hasQ) return true;

        if (term.op == REOP_ASSERTBACK || term.op == REOP_ASSERTBACK_NOT) {
            reportError("msg.bad.quant", "");
            return false;
        }

        if (params.unicodeMode && (term.op == REOP_ASSERT || term.op == REOP_ASSERT_NOT)) {
            reportError("msg.bad.quant", "");
            return false;
        }

        ++state.cp;
        state.result.kid = term;
        state.result.parenIndex = parenBaseCount;
        state.result.parenCount = state.parenCount - parenBaseCount;
        if ((state.cp < state.cpend) && (src[state.cp] == '?')) {
            ++state.cp;
            state.result.greedy = false;
        } else state.result.greedy = true;
        return true;
    }

    private static void resolveForwardJump(byte[] array, int from, int pc) {
        if (from > pc) throw Kit.codeBug();
        addIndex(array, from, pc - from);
    }

    private static int getOffset(byte[] array, int pc) {
        return getIndex(array, pc);
    }

    private static int addIndex(byte[] array, int pc, int index) {
        if (index < 0) throw Kit.codeBug();
        if (index > 0xFFFF) throw Context.reportRuntimeError("Too complex regexp");
        array[pc] = (byte) (index >> 8);
        array[pc + 1] = (byte) index;
        return pc + 2;
    }

    private static int getIndex(byte[] array, int pc) {
        return ((array[pc] & 0xFF) << 8) | (array[pc + 1] & 0xFF);
    }

    private static final int INDEX_LEN = 2;

    private static int emitREBytecode(CompilerState state, RECompiled re, int pc, RENode t) {
        RENode nextAlt;
        int nextAltFixup, nextTermFixup;
        byte[] program = re.program;

        while (t != null) {
            program[pc++] = t.op;
            switch (t.op) {
                case REOP_EMPTY:
                    --pc;
                    break;
                case REOP_ALTPREREQ:
                case REOP_ALTPREREQi:
                case REOP_ALTPREREQ2:
                    boolean ignoreCase = t.op == REOP_ALTPREREQi;
                    addIndex(program, pc, ignoreCase ? upcase(t.chr) : t.chr);
                    pc += INDEX_LEN;
                    addIndex(program, pc, ignoreCase ? upcase((char) t.index) : t.index);
                    pc += INDEX_LEN;
                // fall through to REOP_ALT
                case REOP_ALT:
                    nextAlt = t.kid2;
                    nextAltFixup = pc; /* address of next alternate */
                    pc += INDEX_LEN;
                    pc = emitREBytecode(state, re, pc, t.kid);
                    program[pc++] = REOP_JUMP;
                    nextTermFixup = pc; /* address of following term */
                    pc += INDEX_LEN;
                    resolveForwardJump(program, nextAltFixup, pc);
                    pc = emitREBytecode(state, re, pc, nextAlt);

                    program[pc++] = REOP_JUMP;
                    nextAltFixup = pc;
                    pc += INDEX_LEN;

                    resolveForwardJump(program, nextTermFixup, pc);
                    resolveForwardJump(program, nextAltFixup, pc);
                    break;
                case REOP_FLAT:
                    if ((t.flatIndex != -1) && (t.length > 1)) {
                        if ((state.flags & JSREG_FOLD) != 0) program[pc - 1] = REOP_FLATi;
                        else program[pc - 1] = REOP_FLAT;
                        pc = addIndex(program, pc, t.flatIndex);
                        pc = addIndex(program, pc, t.length);
                    } else {
                        if (t.chr < 256) {
                            if ((state.flags & JSREG_FOLD) != 0) program[pc - 1] = REOP_FLAT1i;
                            else program[pc - 1] = REOP_FLAT1;
                            program[pc++] = (byte) t.chr;
                        } else if (t.lowSurrogate == 0) {
                            if ((state.flags & JSREG_FOLD) != 0) program[pc - 1] = REOP_UCFLAT1i;
                            else program[pc - 1] = REOP_UCFLAT1;
                            pc = addIndex(program, pc, t.chr);
                        } else {
                            program[pc - 1] = REOP_UCSPFLAT1;
                            pc = addIndex(program, pc, t.chr);
                            pc = addIndex(program, pc, t.lowSurrogate);
                        }
                    }
                    break;
                case REOP_LPAREN:
                    pc = addIndex(program, pc, t.parenIndex);
                    pc = emitREBytecode(state, re, pc, t.kid);
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
                    pc = emitREBytecode(state, re, pc, t.kid);
                    program[pc++] = t.op == REOP_ASSERT ? REOP_ASSERTTEST : REOP_ASSERTBACKTEST;
                    resolveForwardJump(program, nextTermFixup, pc);
                    break;
                case REOP_ASSERT_NOT:
                case REOP_ASSERTBACK_NOT:
                    nextTermFixup = pc;
                    pc += INDEX_LEN;
                    pc = emitREBytecode(state, re, pc, t.kid);
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
                    program[pc++] = (byte) (t.atomic ? 1 : 0);
                    nextTermFixup = pc;
                    pc += INDEX_LEN;
                    pc = emitREBytecode(state, re, pc, t.kid);
                    program[pc++] = REOP_ENDCHILD;
                    resolveForwardJump(program, nextTermFixup, pc);
                    break;
                case REOP_CLASS:
                    if (!t.classContents.sense) program[pc - 1] = REOP_NCLASS;
                    pc = addIndex(program, pc, t.index);
                    re.classList[t.index] = new RECharSet(t.classContents, t.bmsize);
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

    /** Update the top of the prog state stack in place (avoids allocation). */
    private static void updateProgState(REGlobalData gData, int min, int max, int cp) {
        REProgState state = gData.stateStackTop;
        state.min = min;
        state.max = max;
        state.index = cp;
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
     *   Consecutive literal characters.
     */
    private static boolean flatNMatcher(
            REGlobalData gData, int matchChars, int length, String input, int end) {
        if ((gData.cp + length) > end) return false;
        for (int i = 0; i < length; i++) {
            if (gData.regexp.source[matchChars + i] != input.charAt(gData.cp + i)) {
                return false;
            }
        }
        gData.cp += length;
        return true;
    }

    private static boolean flatNMatcherBackward(
            REGlobalData gData, int matchChars, int length, String input) {
        if ((gData.cp - length) < 0) return false;

        // in the input, start from cp - 1 and go back length chars
        // in the regex source, do it the other way
        for (int i = 1; i <= length; i++) {
            if (gData.regexp.source[matchChars + length - i] != input.charAt(gData.cp - i)) {
                return false;
            }
        }

        gData.cp -= length;
        return true;
    }

    private static boolean flatNIMatcher(
            REGlobalData gData, int matchChars, int length, String input, int end) {
        if ((gData.cp + length) > end) return false;
        char[] source = gData.regexp.source;
        for (int i = 0; i < length; i++) {
            char c1 = source[matchChars + i];
            char c2 = input.charAt(gData.cp + i);
            if (c1 != c2 && upcase(c1) != upcase(c2)) {
                return false;
            }
        }
        gData.cp += length;
        return true;
    }

    private static boolean flatNIMatcherBackward(
            REGlobalData gData, int matchChars, int length, String input) {
        if ((gData.cp - length) < 0) return false;

        // in the input, start from cp - 1 and go back length chars
        // in the regex source, do it the other way
        for (int i = 1; i <= length; i++) {
            char c1 = gData.regexp.source[matchChars + length - i];
            char c2 = input.charAt(gData.cp - i);
            if (c1 != c2 && upcase(c1) != upcase(c2)) {
                return false;
            }
        }

        gData.cp -= length;
        return true;
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
    private static boolean backrefMatcher(
            REGlobalData gData, int parenIndex, String input, int end, boolean matchBackward) {
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
                    if (c1 != c2 && upcase(c1) != upcase(c2)) return false;
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
                    if (c1 != c2 && upcase(c1) != upcase(c2)) return false;
                }
            } else if (!input.regionMatches(parenContent, input, gData.cp, len)) {
                return false;
            }
            gData.cp += len;
        }
        return true;
    }

    /* Add a single character to the RECharSet */
    private static void addCharacterToCharSet(RECharSet cs, char c) {
        int byteIndex = (c / 8);
        if (c >= cs.length) {
            throw ScriptRuntime.constructError("SyntaxError", "invalid range in character class");
        }
        cs.bits[byteIndex] |= (byte) (1 << (c & 0x7));
    }

    /* Add a character range, c1 to c2 (inclusive) to the RECharSet */
    private static void addCharacterRangeToCharSet(RECharSet cs, char c1, char c2) {
        int i;

        int byteIndex1 = (c1 / 8);
        int byteIndex2 = (c2 / 8);

        if ((c2 >= cs.length) || (c1 > c2)) {
            throw ScriptRuntime.constructError("SyntaxError", "invalid range in character class");
        }

        c1 = (char) (c1 & 0x7);
        c2 = (char) (c2 & 0x7);

        if (byteIndex1 == byteIndex2) {
            cs.bits[byteIndex1] |= (byte) ((0xFF >> (7 - (c2 - c1))) << c1);
        } else {
            cs.bits[byteIndex1] |= (byte) (0xFF << c1);
            for (i = byteIndex1 + 1; i < byteIndex2; i++) cs.bits[i] = (byte) 0xFF;
            cs.bits[byteIndex2] |= (byte) (0xFF >> (7 - c2));
        }
    }

    /* Compile the source of the class into a RECharSet */
    private static void processCharSet(REGlobalData gData, RECharSet charSet) {
        synchronized (charSet) {
            if (!charSet.converted) {
                processCharSetImpl(gData, charSet);
                charSet.converted = true;
            }
        }
    }

    private static void processCharSetImpl(REGlobalData gData, RECharSet charSet) {
        char thisCh;
        int byteLength;
        int i;
        ClassContents classContents = charSet.classContents;

        byteLength = (charSet.length + 7) / 8;
        charSet.bits = new byte[byteLength];

        for (char ch : classContents.chars) {
            addCharacterToCharSet(charSet, ch);
            if ((gData.regexp.flags & JSREG_FOLD) != 0) {
                char uch = upcase(ch);
                char dch = downcase(ch);
                if (ch != uch) addCharacterToCharSet(charSet, uch);
                if (ch != dch) addCharacterToCharSet(charSet, dch);
            }
        }

        for (int j = 0; j < classContents.bmpRanges.size(); j += 2) {
            char start = classContents.bmpRanges.get(j);
            char end = classContents.bmpRanges.get(j + 1);
            if ((gData.regexp.flags & JSREG_FOLD) != 0) {
                for (char ch = start; ch <= end; ) {
                    addCharacterToCharSet(charSet, ch);
                    char uch = upcase(ch);
                    char dch = downcase(ch);
                    if (ch != uch) addCharacterToCharSet(charSet, uch);
                    if (ch != dch) addCharacterToCharSet(charSet, dch);
                    if (++ch == 0) break; // overflow
                }
            } else addCharacterRangeToCharSet(charSet, start, end);
        }

        for (RENode escape : classContents.escapeNodes) {
            switch (escape.op) {
                case REOP_DIGIT:
                    addCharacterRangeToCharSet(charSet, '0', '9');
                    break;
                case REOP_NONDIGIT:
                    addCharacterRangeToCharSet(charSet, (char) 0, (char) ('0' - 1));
                    addCharacterRangeToCharSet(
                            charSet, (char) ('9' + 1), (char) (charSet.length - 1));
                    break;
                case REOP_SPACE:
                    for (i = (charSet.length - 1); i >= 0; i--)
                        if (isREWhiteSpace(i)) addCharacterToCharSet(charSet, (char) i);
                    break;
                case REOP_NONSPACE:
                    for (i = (charSet.length - 1); i >= 0; i--)
                        if (!isREWhiteSpace(i)) addCharacterToCharSet(charSet, (char) i);
                    break;
                case REOP_ALNUM:
                    for (i = (charSet.length - 1); i >= 0; i--)
                        if (isWord((char) i)) addCharacterToCharSet(charSet, (char) i);
                    break;
                case REOP_NONALNUM:
                    for (i = (charSet.length - 1); i >= 0; i--)
                        if (!isWord((char) i)) addCharacterToCharSet(charSet, (char) i);
                    break;
                case REOP_UPROP:
                    charSet.unicodeProps.add(escape.unicodeProperty);
                    break;
                case REOP_UPROP_NOT:
                    charSet.negUnicodeProps.add(escape.unicodeProperty);
                    break;
                default:
                    Kit.codeBug("classContents contains invalid escape node type");
            }
        }
    }

    /*
     *   Initialize the character set if it is the first call.
     *   Test the bit - if the ^ flag was specified, non-inclusion is a success
     */
    private static boolean classMatcher(REGlobalData gData, RECharSet charSet, int codePoint) {
        if (!charSet.converted) {
            processCharSet(gData, charSet);
        }

        if (codePoint <= 0xFFFF) {
            int byteIndex = codePoint >> 3;
            if (!(charSet.length == 0
                    || codePoint >= charSet.length
                    || (charSet.bits[byteIndex] & (1 << (codePoint & 0x7))) == 0))
                return charSet.classContents.sense;
        }

        if (charSet.classContents.nonBMPCodepoints.contains(codePoint))
            return charSet.classContents.sense;

        for (int i = 0; i < charSet.classContents.nonBMPRanges.size(); i += 2) {
            if (codePoint >= charSet.classContents.nonBMPRanges.get(i)
                    && codePoint <= charSet.classContents.nonBMPRanges.get(i + 1)) {
                return charSet.classContents.sense;
            }
        }

        for (int encodedProp : charSet.unicodeProps) {
            if (UnicodeProperties.hasProperty(encodedProp, codePoint))
                return charSet.classContents.sense;
        }
        for (int encodedProp : charSet.negUnicodeProps) {
            if (!UnicodeProperties.hasProperty(encodedProp, codePoint))
                return charSet.classContents.sense;
        }
        return !charSet.classContents.sense;
    }

    private static boolean reopIsSimple(int op) {
        return op >= REOP_SIMPLE_START && op <= REOP_SIMPLE_END;
    }

    /*
     *   Apply the current op against the given input to see if
     *   it's going to match or fail. Return false if we don't
     *   get a match, true if we do and update the state of the
     *   input and pc if the update flag is true.
     */
    private static int simpleMatch(
            REGlobalData gData,
            String input,
            int op,
            byte[] program,
            int pc,
            int end,
            boolean updatecp,
            boolean matchBackward) {
        boolean result = false;
        int matchCodePoint;
        int parenIndex;
        int offset, length, index;
        int startcp = gData.cp;
        int cpDelta;
        final int cpToMatch;
        final boolean cpInBounds;

        if ((gData.regexp.flags & JSREG_UNICODE) != 0 && gData.cp < end) {
            if (matchBackward) {
                if (gData.cp - 2 >= 0
                        && Character.isSurrogatePair(
                                input.charAt(gData.cp - 2), input.charAt(gData.cp - 1))) {
                    cpDelta = -2;
                    cpToMatch = gData.cp - 2;
                } else {
                    cpDelta = -1;
                    cpToMatch = gData.cp - 1;
                }
            } else {
                cpDelta = Character.charCount(input.codePointAt(gData.cp));
                cpToMatch = gData.cp;
            }
        } else {
            cpDelta = (matchBackward ? -1 : 1);
            cpToMatch = gData.cp + (matchBackward ? -1 : 0);
        }
        cpInBounds = cpToMatch >= 0 && cpToMatch < end;

        switch (op) {
            case REOP_EMPTY:
                result = true;
                break;

            // We just use gData.cp and not cpToMatch in the BOL, EOL, WBDRY, WNONBDRY cases
            // since their behaviour is identical in both forward and backward matching
            case REOP_BOL:
                if (gData.cp != 0) {
                    if (!gData.multiline || !isLineTerm(input.charAt(gData.cp - 1))) {
                        break;
                    }
                }
                result = true;
                break;
            case REOP_EOL:
                if (gData.cp != end) {
                    if (!gData.multiline || !isLineTerm(input.charAt(gData.cp))) {
                        break;
                    }
                }
                result = true;
                break;
            case REOP_WBDRY:
                result =
                        ((gData.cp == 0 || !isWord(input.charAt(gData.cp - 1)))
                                ^ !((gData.cp < end) && isWord(input.charAt(gData.cp))));
                break;
            case REOP_WNONBDRY:
                result =
                        ((gData.cp == 0 || !isWord(input.charAt(gData.cp - 1)))
                                ^ ((gData.cp < end) && isWord(input.charAt(gData.cp))));
                break;
            case REOP_DOT:
                if (cpInBounds
                        && ((gData.regexp.flags & JSREG_DOTALL) != 0
                                || !isLineTerm(input.charAt(cpToMatch)))) {
                    result = true;
                    gData.cp += cpDelta;
                }
                break;
            case REOP_DIGIT:
                if (cpInBounds && isDigit(input.charAt(cpToMatch))) {
                    result = true;
                    gData.cp += cpDelta;
                }
                break;
            case REOP_NONDIGIT:
                if (cpInBounds && !isDigit(input.charAt(cpToMatch))) {
                    result = true;
                    gData.cp += cpDelta;
                }
                break;
            case REOP_ALNUM:
                if (cpInBounds && isWord(input.charAt(cpToMatch))) {
                    result = true;
                    gData.cp += cpDelta;
                }
                break;
            case REOP_NONALNUM:
                if (cpInBounds && !isWord(input.charAt(cpToMatch))) {
                    result = true;
                    gData.cp += cpDelta;
                }
                break;
            case REOP_SPACE:
                if (cpInBounds && isREWhiteSpace(input.charAt(cpToMatch))) {
                    result = true;
                    gData.cp += cpDelta;
                }
                break;
            case REOP_NONSPACE:
                if (cpInBounds && !isREWhiteSpace(input.charAt(cpToMatch))) {
                    result = true;
                    gData.cp += cpDelta;
                }
                break;
            case REOP_BACKREF:
                {
                    parenIndex = getIndex(program, pc);
                    pc += INDEX_LEN;
                    result = backrefMatcher(gData, parenIndex, input, end, matchBackward);
                }
                break;
            case REOP_NAMED_BACKREF:
                {
                    int backRefNameIndex = getIndex(program, pc);
                    pc += INDEX_LEN;
                    if (gData.parens == null
                            || backRefNameIndex >= gData.regexp.namedBackRefs.size()) {
                        break;
                    }

                    String backRefName = gData.regexp.namedBackRefs.get(backRefNameIndex);
                    List<Integer> indices = gData.regexp.namedCaptureGroups.get(backRefName);
                    boolean failed = false;
                    for (int i : indices) {
                        if (gData.parensIndex(i) == -1) continue;
                        result = backrefMatcher(gData, i, input, end, matchBackward);
                        if (result) {
                            break;
                        } else failed = true;
                    }
                    if (!failed) result = true;
                    break;
                }
            case REOP_FLAT:
                {
                    offset = getIndex(program, pc);
                    pc += INDEX_LEN;
                    length = getIndex(program, pc);
                    pc += INDEX_LEN;

                    if (matchBackward) result = flatNMatcherBackward(gData, offset, length, input);
                    else result = flatNMatcher(gData, offset, length, input, end);
                }
                break;
            case REOP_FLAT1:
                matchCodePoint = (program[pc++] & 0xFF);
                if (cpInBounds) {
                    int inputCodePoint;

                    if ((gData.regexp.flags & JSREG_UNICODE) != 0) {
                        inputCodePoint = input.codePointAt(cpToMatch);
                    } else {
                        inputCodePoint = input.charAt(cpToMatch);
                    }
                    if (inputCodePoint == matchCodePoint) {
                        result = true;
                        gData.cp += cpDelta;
                    }
                }
                break;
            case REOP_FLATi:
                {
                    offset = getIndex(program, pc);
                    pc += INDEX_LEN;
                    length = getIndex(program, pc);
                    pc += INDEX_LEN;

                    if (matchBackward) result = flatNIMatcherBackward(gData, offset, length, input);
                    else result = flatNIMatcher(gData, offset, length, input, end);
                }
                break;
            case REOP_FLAT1i:
                {
                    // Note: No support for unicode with REOP_FLAT1i
                    matchCodePoint = (program[pc++] & 0xFF);
                    if (cpInBounds) {
                        char c = input.charAt(cpToMatch);
                        if (matchCodePoint == c || upcase((char) matchCodePoint) == upcase(c)) {
                            result = true;
                            gData.cp += cpDelta;
                        }
                    }
                }
                break;
            case REOP_UCFLAT1:
                matchCodePoint = getIndex(program, pc);
                pc += INDEX_LEN;

                if (cpInBounds) {
                    int inputCodePoint;

                    if ((gData.regexp.flags & JSREG_UNICODE) != 0) {
                        inputCodePoint = input.codePointAt(cpToMatch);
                    } else {
                        inputCodePoint = input.charAt(cpToMatch);
                    }
                    if (inputCodePoint == matchCodePoint) {
                        result = true;
                        gData.cp += cpDelta;
                    }
                }
                break;
            case REOP_UCFLAT1i:
                {
                    // Note: No support for unicode with REOP_UCFLAT1i
                    matchCodePoint = getIndex(program, pc);
                    pc += INDEX_LEN;
                    if (cpInBounds) {
                        char c = input.charAt(cpToMatch);
                        if (matchCodePoint == c || upcase((char) matchCodePoint) == upcase(c)) {
                            result = true;
                            gData.cp += cpDelta;
                        }
                    }
                }
                break;

            case REOP_CLASS:
            case REOP_NCLASS:
                {
                    index = getIndex(program, pc);
                    pc += INDEX_LEN;
                    if (cpInBounds) {
                        int inputCodePoint =
                                (gData.regexp.flags & JSREG_UNICODE) != 0
                                        ? input.codePointAt(cpToMatch)
                                        : input.charAt(cpToMatch);
                        if (classMatcher(gData, gData.regexp.classList[index], inputCodePoint)) {
                            gData.cp += cpDelta;
                            result = true;
                            break;
                        }
                    }
                }
                break;
            case REOP_UCSPFLAT1:
                {
                    char highSurrogate = (char) getIndex(program, pc);
                    pc += INDEX_LEN;
                    char lowSurrogate = (char) getIndex(program, pc);
                    pc += INDEX_LEN;
                    matchCodePoint = Character.toCodePoint(highSurrogate, lowSurrogate);
                    if (cpInBounds) {
                        int inputCodePoint = input.codePointAt(cpToMatch);
                        if (matchCodePoint == inputCodePoint) {
                            result = true;
                            gData.cp += cpDelta;
                        }
                    }
                }
                break;

            case REOP_UPROP:
            case REOP_UPROP_NOT:
                {
                    int encodedProp = getIndex(program, pc);
                    pc += INDEX_LEN;
                    if (cpInBounds) {
                        boolean sense = (op == REOP_UPROP);
                        result =
                                sense
                                        ^ !UnicodeProperties.hasProperty(
                                                encodedProp, input.codePointAt(cpToMatch));
                        gData.cp += cpDelta;
                    }
                    break;
                }
            default:
                throw Kit.codeBug();
        }
        if (result) {
            if (!updatecp) gData.cp = startcp;
            return pc;
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
            while (gData.cp <= end) {
                int match = simpleMatch(gData, input, op, program, pc, end, true, false);
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

                if ((gData.regexp.flags & JSREG_UNICODE) != 0 && gData.cp < end) {
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
                int match = simpleMatch(gData, input, op, program, pc, end, true, matchBackward);
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
                                if (op == REOP_ALTPREREQi) c = upcase(c);
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
                                        simpleMatch(
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
                                    && simpleMatch(gData, input, op, program, pc, end, false, true)
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
                                        simpleMatch(
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
                                    && simpleMatch(gData, input, op, program, pc, end, false, false)
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
                                        simpleMatch(
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
                                /* Step over <parencount>, <parenindex>, <atomic> & <next> */
                                pc += 3 * INDEX_LEN + 1;
                            } else {
                                if (min != 0) {
                                    continuationOp = REOP_MINIMALREPEAT;
                                    continuationPc = pc;
                                    /* <parencount> <parenindex> <atomic> & <next> */
                                    pc += 3 * INDEX_LEN + 1;
                                } else {
                                    pushBackTrackState(gData, REOP_MINIMALREPEAT, pc);
                                    popProgState(gData);
                                    pc += 2 * INDEX_LEN + 1; // <parencount> <parenindex> <atomic>
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
                            boolean atomic = program[pc + 2 * INDEX_LEN] != 0;
                            do {
                                // For atomic quantifiers, peek at state (update in place later)
                                // For non-atomic, pop state (will push new one later)
                                REProgState state =
                                        atomic ? gData.stateStackTop : popProgState(gData);
                                if (!result) {
                                    // Failed, see if we have enough children.
                                    if (state.min == 0) result = true;
                                    continuationPc = state.continuationPc;
                                    continuationOp = state.continuationOp;
                                    if (atomic) popProgState(gData); // clean up
                                    pc +=
                                            2 * INDEX_LEN
                                                    + 1; /* <parencount> <parenindex> <atomic> */
                                    pc += getOffset(program, pc);
                                    break switchStatement;
                                }
                                if (state.min == 0 && (gData.cp == state.index || state.max == 0)) {
                                    // matched an empty string or an {0} quantifier, that'll get us
                                    // nowhere
                                    result = false;
                                    continuationPc = state.continuationPc;
                                    continuationOp = state.continuationOp;
                                    if (atomic) popProgState(gData); // clean up
                                    pc += 2 * INDEX_LEN + 1;
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
                                    if (atomic) popProgState(gData); // clean up
                                    pc += 2 * INDEX_LEN + 1;
                                    pc += getOffset(program, pc);
                                    break switchStatement;
                                }
                                nextpc = pc + 3 * INDEX_LEN + 1;
                                nextop = program[nextpc];
                                int startcp = gData.cp;
                                if (reopIsSimple(nextop)) {
                                    nextpc++;
                                    int match =
                                            simpleMatch(
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
                                        if (atomic) popProgState(gData); // clean up
                                        pc +=
                                                2 * INDEX_LEN
                                                        + 1; /* <parencount> <parenindex> <atomic> */
                                        pc += getOffset(program, pc);
                                        break switchStatement;
                                    }
                                    result = true;
                                    nextpc = match;
                                }
                                continuationOp = REOP_REPEAT;
                                continuationPc = pc;
                                if (atomic) {
                                    // Update state in place - no allocation
                                    updateProgState(gData, new_min, new_max, startcp);
                                } else {
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
                                }
                                int parenCount = getIndex(program, pc);
                                int parenIndex = getIndex(program, pc + INDEX_LEN);
                                for (int k = 0; k < parenCount; k++) {
                                    gData.setParens(parenIndex + k, -1, 0);
                                }
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
                                    pc += 2 * INDEX_LEN + 1; // <parenindex> <atomic>
                                    for (int k = 0; k < parenCount; k++) {
                                        gData.setParens(parenIndex + k, -1, 0);
                                    }
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
                                pc += 2 * INDEX_LEN + 1; // <parenindex> <atomic>
                                for (int k = 0; k < parenCount; k++) {
                                    gData.setParens(parenIndex + k, -1, 0);
                                }
                            } else {
                                continuationPc = state.continuationPc;
                                continuationOp = state.continuationOp;
                                pushBackTrackState(gData, REOP_MINIMALREPEAT, pc);
                                popProgState(gData);
                                pc += 2 * INDEX_LEN + 1; // <parencount> <parenindex> <atomic>
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
                    if ((gData.regexp.flags & JSREG_UNICODE) != 0) {
                        int matchCodePoint = input.codePointAt(i);
                        if (matchCodePoint == anchorCodePoint) {
                            break;
                        }
                        charCount = Character.charCount(matchCodePoint);
                    } else {
                        char matchCh = input.charAt(i);
                        if (matchCh == anchorCodePoint
                                || ((gData.regexp.flags & JSREG_FOLD) != 0
                                        && upcase(matchCh) == upcase((char) anchorCodePoint))) {
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
            for (int j = 0; j < re.parenCount; j++) {
                gData.parens[j] = -1L;
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

    private static class ExecResult {
        final String match;
        final ArrayList<String> captures = new ArrayList<>();
        final LinkedHashMap<String, String> groups = new LinkedHashMap<>();
        final int index;
        final String input;

        ExecResult(int index, String input) {
            this.match = null;
            this.index = index;
            this.input = input;
        }

        ExecResult(int index, String input, String match) {
            this.match = match;
            this.index = index;
            this.input = input;
        }
    }

    Object executeRegExp(
            Context cx, Scriptable scope, RegExpImpl res, String str, int[] indexp, int matchType) {
        var result = executeRegExpInternal(cx, scope, res, str, indexp, matchType);

        if (result == null) {
            if (matchType != PREFIX) return null;
            return Undefined.instance;
        } else if (matchType == TEST) {
            /*
             * Testing for a match and updating cx.regExpImpl: don't allocate
             * an array object, do return true.
             */
            return Boolean.TRUE;
        } else {
            Object[] captures = result.captures.toArray();
            Scriptable obj = cx.newArray(scope, captures.length + 1);

            obj.put(0, obj, result.match);
            for (int i = 0; i < captures.length; i++) {
                obj.put(i + 1, obj, (captures[i] == null) ? Undefined.instance : captures[i]);
            }

            obj.put("index", obj, Integer.valueOf(result.index));
            obj.put("input", obj, str);
            if (!result.groups.isEmpty()) {
                var groups = new NativeObject();
                for (var g : result.groups.entrySet()) {
                    groups.put(
                            g.getKey(),
                            groups,
                            g.getValue() == null ? Undefined.instance : g.getValue());
                }
                obj.put("groups", obj, groups);
            } else {
                obj.put("groups", obj, Undefined.instance);
            }
            return obj;
        }
    }

    /*
     * indexp is assumed to be an array of length 1
     */
    ExecResult executeRegExpInternal(
            Context cx, Scriptable scope, RegExpImpl res, String str, int[] indexp, int matchType) {
        REGlobalData gData = new REGlobalData();

        int start = indexp[0];
        int end = str.length();
        if (start > end) start = end;
        //
        // Call the recursive matcher to do the real work.
        //
        boolean matches = matchRegExp(cx, gData, re, str, start, end, res.multiline);
        if (!matches) return null;

        int index = gData.cp;
        int ep = indexp[0] = index;
        int matchlen = ep - (start + gData.skipped);
        index -= matchlen;
        ExecResult result;

        if (matchType == TEST) {
            result = new ExecResult(index, str);
        } else {
            /*
             * The array returned on match has element 0 bound to the matched
             * string, elements 1 through re.parenCount bound to the paren
             * matches, an index property telling the length of the left context,
             * and an input property referring to the input string.
             */

            String matchstr = str.substring(index, index + matchlen);
            result = new ExecResult(index, str, matchstr);
        }

        if (re.parenCount == 0) {
            res.parens = null;
            res.lastParen = new SubString();
        } else {
            SubString parsub = null;
            int num;
            String[] namedCaptureGroups = null; // to ensure groups appear in source order

            if (matchType != TEST) {
                namedCaptureGroups = new String[re.parenCount];

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
                        result.captures.add(parsub.toString());
                        if (namedCaptureGroups[num] != null) {
                            result.groups.put(namedCaptureGroups[num], parsub.toString());
                        }
                    }
                } else {
                    result.captures.add(null);
                    if (matchType != TEST) {
                        if (namedCaptureGroups[num] != null
                                && !result.groups.containsKey(namedCaptureGroups[num])) {
                            result.groups.put(namedCaptureGroups[num], null);
                        }
                    }
                }
            }
            res.lastParen = parsub;
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

    private static void reportError(String messageId, String arg) {
        String msg = ScriptRuntime.getMessageById(messageId, arg);
        throw ScriptRuntime.constructError("SyntaxError", msg);
    }

    private static void setLastIndexOrThrow(ScriptableObject thisObj, Object value) {
        if ((thisObj.getAttributes("lastIndex") & READONLY) != 0) {
            throw ScriptRuntime.typeErrorById("msg.modify.readonly", "lastIndex");
        }
        setLastIndex(thisObj, value);
    }

    private static void setLastIndex(Scriptable thisObj, Object value) {
        ScriptableObject.putProperty(thisObj, "lastIndex", value);
    }

    private void setLastIndex(Object value) {
        if ((lastIndexAttr & READONLY) != 0) {
            throw ScriptRuntime.typeErrorById("msg.modify.readonly", "lastIndex");
        }
        lastIndex = value;
    }

    private static Object js_compile(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return realThis(thisObj, f).compile(cx, s, args);
    }

    private static Object js_toString(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        // thisObj != scope is a strange hack but i had no better idea for the moment
        if (thisObj != s && thisObj instanceof NativeObject) {
            NativeObject realThis = (NativeObject) thisObj;
            Object sourceObj = realThis.get("source", realThis);
            String source = sourceObj.equals(NOT_FOUND) ? "undefined" : escapeRegExp(sourceObj);
            Object flagsObj = realThis.get("flags", realThis);
            String flags = flagsObj.equals(NOT_FOUND) ? "undefined" : flagsObj.toString();

            return "/" + source + "/" + flags;
        }
        return realThis(thisObj, f).toString();
    }

    private static Object js_toSource(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return realThis(thisObj, f).toString();
    }

    private static Object js_exec(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return js_exec(cx, s, thisObj, args);
    }

    private static Object js_test(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        {
            Object x = realThis(thisObj, f).execSub(cx, s, args, TEST);
            return Boolean.TRUE.equals(x) ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    private static Object js_prefix(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return realThis(thisObj, f).execSub(cx, s, args, PREFIX);
    }

    private static Object js_match(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return js_SymbolMatch(cx, s, thisObj, args);
    }

    private static Object js_matchAll(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return js_SymbolMatchAll(cx, s, thisObj, args);
    }

    private static Object js_search(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return js_SymbolSearch(cx, s, thisObj, args);
    }

    private static Object js_replace(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return js_SymbolReplace(cx, s, thisObj, args);
    }

    private static Object js_split(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return js_SymbolSplit(cx, s, thisObj, args);
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

    private static Object js_SymbolMatch(
            Context cx, Scriptable scope, Object thisScriptable, Object[] args) {
        // See ECMAScript spec 22.2.6.8
        var thisObj = ScriptableObject.ensureScriptableObject(thisScriptable);

        String string = ScriptRuntime.toString(args.length > 0 ? args[0] : Undefined.instance);
        String flags = ScriptRuntime.toString(ScriptRuntime.getObjectProp(thisObj, "flags", cx));
        boolean fullUnicode = flags.indexOf('u') != -1 || flags.indexOf('v') != -1;

        if (flags.indexOf('g') == -1) return regExpExec(thisObj, string, cx, scope);

        setLastIndexOrThrow(thisObj, ScriptRuntime.zeroObj);
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
                setLastIndexOrThrow(thisObj, nextIndex);
            }
        }
    }

    private static Object js_SymbolSearch(
            Context cx, Scriptable scope, Object thisObj, Object[] args) {
        // See ECMAScript spec 22.2.6.12
        if (!ScriptRuntime.isObject(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(thisObj));
        }

        Scriptable realThis = (Scriptable) thisObj;

        String string = ScriptRuntime.toString(args.length > 0 ? args[0] : Undefined.instance);
        long previousLastIndex = getLastIndex(cx, realThis);
        if (previousLastIndex != 0) {
            setLastIndex(realThis, ScriptRuntime.zeroObj);
        }

        Object result = regExpExec(realThis, string, cx, scope);

        long currentLastIndex = getLastIndex(cx, realThis);
        if (previousLastIndex != currentLastIndex) {
            setLastIndex(realThis, previousLastIndex);
        }

        if (result == null) {
            return -1;
        } else {
            return ScriptRuntime.getObjectProp(result, "index", cx, scope);
        }
    }

    static Object js_exec(Context cx, Scriptable scope, Object thisObj, Object[] args) {
        return realThis(thisObj, "exec").execSub(cx, scope, args, MATCH);
    }

    private static Object js_SymbolMatchAll(
            Context cx, Scriptable scope, Object thisObj, Object[] args) {
        // See ECMAScript spec 22.2.6.9
        if (!ScriptRuntime.isObject(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(thisObj));
        }

        Scriptable realThis = (Scriptable) thisObj;

        String s = ScriptRuntime.toString(args.length > 0 ? args[0] : Undefined.instance);

        Scriptable topLevelScope = ScriptableObject.getTopLevelScope(scope);
        Function defaultConstructor = ScriptRuntime.getExistingCtor(cx, topLevelScope, "RegExp");
        Constructable c =
                AbstractEcmaObjectOperations.speciesConstructor(cx, realThis, defaultConstructor);

        String flags = ScriptRuntime.toString(ScriptRuntime.getObjectProp(realThis, "flags", cx));

        Scriptable matcher = c.construct(cx, scope, new Object[] {thisObj, flags});

        long lastIndex = getLastIndex(cx, realThis);
        setLastIndex(matcher, lastIndex);
        boolean global = flags.indexOf('g') != -1;
        boolean fullUnicode = flags.indexOf('u') != -1 || flags.indexOf('v') != -1;

        return new NativeRegExpStringIterator(scope, matcher, s, global, fullUnicode);
    }

    private static Object js_SymbolReplace(
            Context cx, Scriptable scope, Object thisObj, Object[] args) {
        if (thisObj instanceof NativeRegExp) {
            var regexp = (NativeRegExp) thisObj;
            var exec = ScriptableObject.getProperty(regexp, "exec");
            if ((regexp.lastIndexAttr & READONLY) == 0
                    && exec instanceof JSFunction
                    && ((JSFunction) exec).getDescriptor() == EXEC_DESCRIPTOR)
                return regexp.js_SymbolReplaceFast(cx, scope, (NativeRegExp) thisObj, args);
        }
        return js_SymbolReplaceSlow(cx, scope, (Scriptable) thisObj, args);
    }

    private Object js_SymbolReplaceFast(
            Context cx, Scriptable scope, NativeRegExp thisObj, Object[] args) {
        String s = ScriptRuntime.toString(args.length > 0 ? args[0] : Undefined.instance);
        int lengthS = s.length();
        Object replaceValue = args.length > 1 ? args[1] : Undefined.instance;
        boolean functionalReplace = replaceValue instanceof Callable;
        List<ReplacementOperation> replaceOps;
        Callable replaceFn;
        if (!functionalReplace) {
            replaceFn = null;
            replaceOps =
                    AbstractEcmaStringOperations.buildReplacementList(
                            ScriptRuntime.toString(replaceValue));
        } else {
            replaceFn = (Callable) replaceValue;
            replaceOps = List.of();
        }
        String flags = ScriptRuntime.toString(ScriptRuntime.getObjectProp(thisObj, "flags", cx));
        boolean fullUnicode = flags.indexOf('u') != -1 || flags.indexOf('v') != -1;

        List<ExecResult> results = new ArrayList<>();
        boolean done = false;

        RegExpImpl reImpl = getImpl(cx);
        boolean sticky = (re.flags & JSREG_STICKY) != 0;
        boolean global = (re.flags & JSREG_GLOB) != 0;

        int[] indexp = {0};
        if (sticky) {
            indexp[0] = (int) getLastIndex(cx, thisObj);
        }
        while (!done) {
            ExecResult result;
            if (indexp[0] < 0 || indexp[0] > s.length()) {
                result = null;
            } else {
                result = executeRegExpInternal(cx, scope, reImpl, s, indexp, MATCH);
            }
            if (result == null) {
                if (global || sticky) {
                    indexp[0] = 0;
                }
                done = true;
            } else {
                results.add(result);
                if (!global) {
                    done = true;
                } else {
                    String matchStr = result.match;
                    if (matchStr.isEmpty()) {
                        indexp[0] =
                                (int) ScriptRuntime.advanceStringIndex(s, indexp[0], fullUnicode);
                    }
                }
            }
        }
        setLastIndexOrThrow(thisObj, indexp[0]);

        StringBuilder accumulatedResult = new StringBuilder();
        int nextSourcePosition = 0;
        for (ExecResult result : results) {
            String matched = result.match;
            int matchLength = matched.length();
            double positionDbl = result.index;
            int position = ScriptRuntime.clamp((int) positionDbl, 0, lengthS);

            List<String> captures = result.captures;
            Object namedCaptures;
            if (!result.groups.isEmpty()) {
                var groups = new NativeObject();
                for (var g : result.groups.entrySet()) {
                    groups.put(
                            g.getKey(),
                            groups,
                            g.getValue() == null ? Undefined.instance : g.getValue());
                }
                namedCaptures = groups;
            } else {
                namedCaptures = Undefined.instance;
            }

            String replacementString =
                    functionalReplace
                            ? makeComplexReplacement(
                                    cx,
                                    scope,
                                    matched,
                                    captures,
                                    position,
                                    s,
                                    namedCaptures,
                                    replaceFn)
                            : makeSimpleReplacement(
                                    cx,
                                    scope,
                                    matched,
                                    captures,
                                    position,
                                    s,
                                    namedCaptures,
                                    replaceOps);

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

    private static Object js_SymbolReplaceSlow(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // See ECMAScript spec 22.2.6.11
        if (!ScriptRuntime.isObject(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(thisObj));
        }

        String s = ScriptRuntime.toString(args.length > 0 ? args[0] : Undefined.instance);
        int lengthS = s.length();
        Object replaceValue = args.length > 1 ? args[1] : Undefined.instance;
        boolean functionalReplace = replaceValue instanceof Callable;
        List<ReplacementOperation> replaceOps;
        Callable replaceFn;

        if (!functionalReplace) {
            replaceFn = null;
            replaceOps =
                    AbstractEcmaStringOperations.buildReplacementList(
                            ScriptRuntime.toString(replaceValue));
        } else {
            replaceFn = (Callable) replaceValue;
            replaceOps = List.of();
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

        StringBuilder accumulatedResult = new StringBuilder();
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
            String replacementString =
                    functionalReplace
                            ? makeComplexReplacement(
                                    cx,
                                    scope,
                                    matched,
                                    captures,
                                    position,
                                    s,
                                    namedCaptures,
                                    replaceFn)
                            : makeSimpleReplacement(
                                    cx,
                                    scope,
                                    matched,
                                    captures,
                                    position,
                                    s,
                                    namedCaptures,
                                    replaceOps);

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

    private static String makeComplexReplacement(
            Context cx,
            Scriptable scope,
            String matched,
            List<?> captures,
            int position,
            String s,
            Object namedCaptures,
            Callable replaceFunction) {
        Object[] replacerArgs =
                new Object[1 + captures.size() + (Undefined.isUndefined(namedCaptures) ? 2 : 3)];
        replacerArgs[0] = matched;
        int i = 1;
        for (; i <= captures.size(); i++) {
            var capture = captures.get(i - 1);
            replacerArgs[i] = capture == null ? Undefined.instance : capture;
        }
        replacerArgs[i++] = position;
        replacerArgs[i++] = s;
        if (!Undefined.isUndefined(namedCaptures)) {
            replacerArgs[i++] = namedCaptures;
        }

        Scriptable callThis = ScriptRuntime.getApplyOrCallThis(cx, scope, null, 0, replaceFunction);
        Object replacementValue = replaceFunction.call(cx, scope, callThis, replacerArgs);
        return ScriptRuntime.toString(replacementValue);
    }

    private static String makeSimpleReplacement(
            Context cx,
            Scriptable scope,
            String matched,
            List<?> captures,
            int position,
            String s,
            Object namedCaptures,
            List<ReplacementOperation> replaceOps) {
        if (!Undefined.isUndefined(namedCaptures)) {
            namedCaptures = ScriptRuntime.toObject(scope, namedCaptures);
        }

        return AbstractEcmaStringOperations.getSubstitution(
                cx, scope, matched, s, position, captures, namedCaptures, replaceOps);
    }

    private static Object js_SymbolSplit(
            Context cx, Scriptable scope, Object thisObj, Object[] args) {
        // See ECMAScript spec 22.2.6.14
        if (!ScriptRuntime.isObject(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(thisObj));
        }

        Scriptable rx = (Scriptable) thisObj;

        String s = ScriptRuntime.toString(args.length > 0 ? args[0] : Undefined.instance);

        Scriptable topLevelScope = ScriptableObject.getTopLevelScope(scope);
        Function defaultConstructor = ScriptRuntime.getExistingCtor(cx, topLevelScope, "RegExp");
        Constructable c =
                AbstractEcmaObjectOperations.speciesConstructor(cx, rx, defaultConstructor);

        String flags = ScriptRuntime.toString(ScriptRuntime.getObjectProp(rx, "flags", cx));
        boolean unicodeMatching = flags.indexOf('u') != -1 || flags.indexOf('v') != -1;
        NativeArray a = (NativeArray) cx.newArray(scope, 0);
        String newFlags = flags.indexOf('y') != -1 ? flags : (flags + "y");
        Scriptable splitter = c.construct(cx, scope, new Object[] {rx, newFlags});

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

        if (splitter instanceof NativeRegExp) {
            var regexp = (NativeRegExp) splitter;
            var exec = ScriptableObject.getProperty(regexp, "exec");
            if ((regexp.lastIndexAttr & READONLY) == 0
                    && exec instanceof JSFunction
                    && ((JSFunction) exec).getDescriptor() == EXEC_DESCRIPTOR)
                return js_SymbolSplitFast(
                        cx, scope, (NativeRegExp) splitter, s, lim, unicodeMatching, a);
        }

        return js_SymbolSplitSlow(cx, scope, splitter, s, lim, unicodeMatching, a);
    }

    private static Object js_SymbolSplitSlow(
            Context cx,
            Scriptable scope,
            Scriptable splitter,
            String s,
            long lim,
            boolean unicodeMatching,
            NativeArray a) {
        int lengthA = 0;

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

    private static Object js_SymbolSplitFast(
            Context cx,
            Scriptable scope,
            NativeRegExp splitter,
            String s,
            long lim,
            boolean unicodeMatching,
            NativeArray a) {
        int lengthA = 0;

        int[] indexp = {0};
        RegExpImpl reImpl = getImpl(cx);
        if (s.isEmpty()) {
            ExecResult result = splitter.executeRegExpInternal(cx, scope, reImpl, s, indexp, MATCH);
            if (result != null) {
                return a;
            }
            a.put(0, a, s);
            return a;
        }

        int size = s.length();
        long p = 0;
        long q = p;
        while (q < size) {
            indexp[0] = (int) q;
            ExecResult result = splitter.executeRegExpInternal(cx, scope, reImpl, s, indexp, MATCH);

            if (result == null) {
                q = ScriptRuntime.advanceStringIndex(s, q, unicodeMatching);
            } else {
                long e = indexp[0];
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
                    int i = 0;
                    while (i < result.captures.size()) {
                        Object nextCapture = result.captures.get(i);
                        a.put(
                                (int) a.getLength(),
                                a,
                                nextCapture == null ? Undefined.instance : nextCapture);
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

    private static NativeRegExp realThis(Object thisObj, JSFunction f) {
        return realThis(thisObj, f.getFunctionName());
    }

    private static NativeRegExp realThis(Object thisObj, String functionName) {
        return ensureType(thisObj, NativeRegExp.class, functionName);
    }

    /**
     * Check if a quantifier's child could overlap with what follows. If not, the quantifier can be
     * made atomic (no backtracking needed).
     *
     * @param child the quantifier's child node
     * @param next the node following the quantifier
     * @param flags the regex flags
     * @return true if they could overlap (conservative), false if definitely no overlap
     */
    private static boolean couldQuantifierChildOverlap(RENode child, RENode next, int flags) {
        if (child == null || next == null) return true;

        // In multiline mode, $ matches at line boundaries, so backtracking may be needed
        // if the child could match \n (we conservatively disable the optimization entirely)
        if ((flags & JSREG_MULTILINE) != 0) {
            return true;
        }

        if (!reopIsSimple(child.op)) {
            return true;
        }

        if (next.op == REOP_EOL) {
            return false;
        }

        return true;
    }

    private RECompiled re;
    Object lastIndex = ScriptRuntime.zeroObj; /* index after last match, for //g iterator */
    private int lastIndexAttr = DONTENUM | PERMANENT;
} // class NativeRegExp

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
    int anchorCodePoint = -1; /* if >= 0, then re starts with this literal char */

    RECompiled(String str) {
        this.source = str.toCharArray();
    }
}

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
    boolean atomic; // true if backtracking won't help (child can't overlap with next)

    /* or a character class */
    int bmsize; /* bitmap size, based on max char code */
    int index; /* index into class list */
    NativeRegExp.ClassContents classContents;

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
}

class CompilerState {

    CompilerState(Context cx, char[] source, int length, int flags) {
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

    int min; /* current quantifier min */
    int max; /* current quantifier max */
    int index; /* progress in text */
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
        return (int) parens[i];
    }

    /** Get length of parenthesis capture contents. */
    int parensLength(int i) {
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

    RECharSet(NativeRegExp.ClassContents classContents, int length) {
        this.length = length;
        this.classContents = classContents;
    }

    final int length;
    final NativeRegExp.ClassContents classContents;

    transient volatile boolean converted;
    transient volatile byte[] bits;
}
