/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1998.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Norris Boyd
 *   Igor Bukanov
 *   Brendan Eich
 *   Matthias Radestock
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript.regexp;

import java.io.Serializable;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;

/**
 * This class implements the RegExp native object.
 *
 * Revision History:
 * Implementation in C by Brendan Eich
 * Initial port to Java by Norris Boyd from jsregexp.c version 1.36
 * Merged up to version 1.38, which included Unicode support.
 * Merged bug fixes in version 1.39.
 * Merged JSFUN13_BRANCH changes up to 1.32.2.13
 *
 * @author Brendan Eich
 * @author Norris Boyd
 */



public class NativeRegExp extends IdScriptableObject
{
    static final long serialVersionUID = 4965263491464903264L;

    private static final Object REGEXP_TAG = new Object();

    public static final int JSREG_GLOB = 0x1;       // 'g' flag: global
    public static final int JSREG_FOLD = 0x2;       // 'i' flag: fold
    public static final int JSREG_MULTILINE = 0x4;  // 'm' flag: multiline

    //type of match to perform
    public static final int TEST = 0;
    public static final int MATCH = 1;
    public static final int PREFIX = 2;

    private static final boolean debug = false;

    private static final byte REOP_EMPTY         = 0;  /* match rest of input against rest of r.e. */
    private static final byte REOP_ALT           = 1;  /* alternative subexpressions in kid and next */
    private static final byte REOP_BOL           = 2;  /* beginning of input (or line if multiline) */
    private static final byte REOP_EOL           = 3;  /* end of input (or line if multiline) */
    private static final byte REOP_WBDRY         = 4;  /* match "" at word boundary */
    private static final byte REOP_WNONBDRY      = 5;  /* match "" at word non-boundary */
    private static final byte REOP_QUANT         = 6;  /* quantified atom: atom{1,2} */
    private static final byte REOP_STAR          = 7;  /* zero or more occurrences of kid */
    private static final byte REOP_PLUS          = 8;  /* one or more occurrences of kid */
    private static final byte REOP_OPT           = 9;  /* optional subexpression in kid */
    private static final byte REOP_LPAREN        = 10; /* left paren bytecode: kid is u.num'th sub-regexp */
    private static final byte REOP_RPAREN        = 11; /* right paren bytecode */
    private static final byte REOP_DOT           = 12; /* stands for any character */
//    private static final byte REOP_CCLASS        = 13; /* character class: [a-f] */
    private static final byte REOP_DIGIT         = 14; /* match a digit char: [0-9] */
    private static final byte REOP_NONDIGIT      = 15; /* match a non-digit char: [^0-9] */
    private static final byte REOP_ALNUM         = 16; /* match an alphanumeric char: [0-9a-z_A-Z] */
    private static final byte REOP_NONALNUM      = 17; /* match a non-alphanumeric char: [^0-9a-z_A-Z] */
    private static final byte REOP_SPACE         = 18; /* match a whitespace char */
    private static final byte REOP_NONSPACE      = 19; /* match a non-whitespace char */
    private static final byte REOP_BACKREF       = 20; /* back-reference (e.g., \1) to a parenthetical */
    private static final byte REOP_FLAT          = 21; /* match a flat string */
    private static final byte REOP_FLAT1         = 22; /* match a single char */
    private static final byte REOP_JUMP          = 23; /* for deoptimized closure loops */
//    private static final byte REOP_DOTSTAR       = 24; /* optimize .* to use a single opcode */
//    private static final byte REOP_ANCHOR        = 25; /* like .* but skips left context to unanchored r.e. */
//    private static final byte REOP_EOLONLY       = 26; /* $ not preceded by any pattern */
//    private static final byte REOP_UCFLAT        = 27; /* flat Unicode string; len immediate counts chars */
    private static final byte REOP_UCFLAT1       = 28; /* single Unicode char */
//    private static final byte REOP_UCCLASS       = 29; /* Unicode character class, vector of chars to match */
//    private static final byte REOP_NUCCLASS      = 30; /* negated Unicode character class */
//    private static final byte REOP_BACKREFi      = 31; /* case-independent REOP_BACKREF */
    private static final byte REOP_FLATi         = 32; /* case-independent REOP_FLAT */
    private static final byte REOP_FLAT1i        = 33; /* case-independent REOP_FLAT1 */
//    private static final byte REOP_UCFLATi       = 34; /* case-independent REOP_UCFLAT */
    private static final byte REOP_UCFLAT1i      = 35; /* case-independent REOP_UCFLAT1 */
//    private static final byte REOP_ANCHOR1       = 36; /* first-char discriminating REOP_ANCHOR */
//    private static final byte REOP_NCCLASS       = 37; /* negated 8-bit character class */
//    private static final byte REOP_DOTSTARMIN    = 38; /* ungreedy version of REOP_DOTSTAR */
//    private static final byte REOP_LPARENNON     = 39; /* non-capturing version of REOP_LPAREN */
//    private static final byte REOP_RPARENNON     = 40; /* non-capturing version of REOP_RPAREN */
    private static final byte REOP_ASSERT        = 41; /* zero width positive lookahead assertion */
    private static final byte REOP_ASSERT_NOT    = 42; /* zero width negative lookahead assertion */
    private static final byte REOP_ASSERTTEST    = 43; /* sentinel at end of assertion child */
    private static final byte REOP_ASSERTNOTTEST = 44; /* sentinel at end of !assertion child */
    private static final byte REOP_MINIMALSTAR   = 45; /* non-greedy version of * */
    private static final byte REOP_MINIMALPLUS   = 46; /* non-greedy version of + */
    private static final byte REOP_MINIMALOPT    = 47; /* non-greedy version of ? */
    private static final byte REOP_MINIMALQUANT  = 48; /* non-greedy version of {} */
    private static final byte REOP_ENDCHILD      = 49; /* sentinel at end of quantifier child */
    private static final byte REOP_CLASS         = 50; /* character class with index */
    private static final byte REOP_REPEAT        = 51; /* directs execution of greedy quantifier */
    private static final byte REOP_MINIMALREPEAT = 52; /* directs execution of non-greedy quantifier */
    private static final byte REOP_END           = 53;



    public static void init(Context cx, Scriptable scope, boolean sealed)
    {

        NativeRegExp proto = new NativeRegExp();
        proto.re = (RECompiled)compileRE(cx, "", null, false);
        proto.activatePrototypeMap(MAX_PROTOTYPE_ID);
        proto.setParentScope(scope);
        proto.setPrototype(getObjectPrototype(scope));

        NativeRegExpCtor ctor = new NativeRegExpCtor();
        // Bug #324006: ECMA-262 15.10.6.1 says "The initial value of
        // RegExp.prototype.constructor is the builtin RegExp constructor."
        proto.defineProperty("constructor", ctor, ScriptableObject.DONTENUM, false);

        ScriptRuntime.setFunctionProtoAndParent(ctor, scope);

        ctor.setImmunePrototypeProperty(proto);

        if (sealed) {
            proto.sealObject();
            ctor.sealObject();
        }

        defineProperty(scope, "RegExp", ctor, ScriptableObject.DONTENUM, false);
    }

    NativeRegExp(Scriptable scope, Object regexpCompiled)
    {
        this.re = (RECompiled)regexpCompiled;
        this.lastIndex = 0d;
        ScriptRuntime.setBuiltinProtoAndParent(this, scope, TopLevel.Builtins.RegExp);
    }

    @Override
    public String getClassName()
    {
        return "RegExp";
    }

    /**
     * Gets the value to be returned by the typeof operator called on this object.
     * @see org.mozilla.javascript.ScriptableObject#getTypeOf()
     * @return "object"
     */
    @Override
    public String getTypeOf()
    {
    	return "object";
    }

    Scriptable compile(Context cx, Scriptable scope, Object[] args)
    {
        if (args.length > 0 && args[0] instanceof NativeRegExp) {
            if (args.length > 1 && args[1] != Undefined.instance) {
                // report error
                throw ScriptRuntime.typeError0("msg.bad.regexp.compile");
            }
            NativeRegExp thatObj = (NativeRegExp) args[0];
            this.re = thatObj.re;
            this.lastIndex = thatObj.lastIndex;
            return this;
        }
        String s = args.length == 0 || args[0] == Undefined.instance 
            ? ""
            : ScriptRuntime.toString(args[0]);
        String global = args.length > 1 && args[1] != Undefined.instance
            ? ScriptRuntime.toString(args[1])
            : null;
        this.re = (RECompiled)compileRE(cx, s, global, false);
        this.lastIndex = 0d;
        return this;
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append('/');
        if (re.source.length != 0) {
            buf.append(re.source);
        } else {
            // See bugzilla 226045
            buf.append("(?:)");
        }
        buf.append('/');
        if ((re.flags & JSREG_GLOB) != 0)
            buf.append('g');
        if ((re.flags & JSREG_FOLD) != 0)
            buf.append('i');
        if ((re.flags & JSREG_MULTILINE) != 0)
            buf.append('m');
        return buf.toString();
    }

    NativeRegExp() {  }

    private static RegExpImpl getImpl(Context cx)
    {
        return (RegExpImpl) ScriptRuntime.getRegExpProxy(cx);
    }

    private Object execSub(Context cx, Scriptable scopeObj,
                           Object[] args, int matchType)
    {
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
        double d = 0;
        if ((re.flags & JSREG_GLOB) != 0) {
            d = ScriptRuntime.toInteger(lastIndex);
        }

        Object rval;
        if (d < 0 || str.length() < d) {
            lastIndex = 0d;
            rval = null;
        }
        else {
            int indexp[] = { (int)d };
            rval = executeRegExp(cx, scopeObj, reImpl, str, indexp, matchType);
            if ((re.flags & JSREG_GLOB) != 0) {
                lastIndex = (rval == null || rval == Undefined.instance)
                            ? 0 : indexp[0];
            }
        }
        return rval;
    }

    static Object compileRE(Context cx, String str, String global, boolean flat)
    {
        RECompiled regexp = new RECompiled();
        regexp.source = str.toCharArray();
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
                } else {
                    reportError("msg.invalid.re.flag", String.valueOf(c));
                }
                if ((flags & f) != 0) {
                    reportError("msg.invalid.re.flag", String.valueOf(c));
                }
                flags |= f;
            }
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
            if (!parseDisjunction(state))
                return null;
            // Need to reparse if pattern contains invalid backreferences:
            // "Note: if the number of left parentheses is less than the number
            // specified in \#, the \# is taken as an octal escape"
            if (state.maxBackReference > state.parenCount) {
                state = new CompilerState(cx, regexp.source, length, flags);
                state.backReferenceLimit = state.parenCount;
                if (!parseDisjunction(state))
                    return null;
            }
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
}
        regexp.parenCount = state.parenCount;

        // If re starts with literal, init anchorCh accordingly
        switch (regexp.program[0]) {
        case REOP_UCFLAT1:
        case REOP_UCFLAT1i:
            regexp.anchorCh = (char)getIndex(regexp.program, 1);
            break;
        case REOP_FLAT1:
        case REOP_FLAT1i:
            regexp.anchorCh = (char)(regexp.program[1] & 0xFF);
            break;
        case REOP_FLAT:
        case REOP_FLATi:
            int k = getIndex(regexp.program, 1);
            regexp.anchorCh = regexp.source[k];
            break;
        }

if (debug) {
if (regexp.anchorCh >= 0) {
    System.out.println("Anchor ch = '" + (char)regexp.anchorCh + "'");
}
}
        return regexp;
    }

    static boolean isDigit(char c)
    {
        return '0' <= c && c <= '9';
    }

    private static boolean isWord(char c)
    {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || isDigit(c) || c == '_';
    }

    private static boolean isControlLetter(char c)
    {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
    }

    private static boolean isLineTerm(char c)
    {
        return ScriptRuntime.isJSLineTerminator(c);
    }

    private static boolean isREWhiteSpace(int c)
    {
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
    private static char upcase(char ch)
    {
        if (ch < 128) {
            if ('a' <= ch && ch <= 'z') {
                return (char)(ch + ('A' - 'a'));
            }
            return ch;
        }
        char cu = Character.toUpperCase(ch);
        if ((ch >= 128) && (cu < 128)) return ch;
        return cu;
    }

    private static char downcase(char ch)
    {
        if (ch < 128) {
            if ('A' <= ch && ch <= 'Z') {
                return (char)(ch + ('a' - 'A'));
            }
            return ch;
        }
        char cl = Character.toLowerCase(ch);
        if ((ch >= 128) && (cl < 128)) return ch;
        return cl;
    }

/*
 * Validates and converts hex ascii value.
 */
    private static int toASCIIHexDigit(int c)
    {
        if (c < '0')
            return -1;
        if (c <= '9') {
            return c - '0';
        }
        c |= 0x20;
        if ('a' <= c && c <= 'f') {
            return c - 'a' + 10;
        }
        return -1;
    }

/*
 * Top-down regular expression grammar, based closely on Perl4.
 *
 *  regexp:     altern                  A regular expression is one or more
 *              altern '|' regexp       alternatives separated by vertical bar.
 */
    private static boolean parseDisjunction(CompilerState state)
    {
        if (!parseAlternative(state))
            return false;
        char[] source = state.cpbegin;
        int index = state.cp;
        if (index != source.length && source[index] == '|') {
            RENode altResult;
            ++state.cp;
            altResult = new RENode(REOP_ALT);
            altResult.kid = state.result;
            if (!parseDisjunction(state))
                return false;
            altResult.kid2 = state.result;
            state.result = altResult;
            /* ALT, <next>, ..., JUMP, <end> ... JUMP <end> */
            state.progLength += 9;
        }
        return true;
    }

/*
 *  altern:     item                    An alternative is one or more items,
 *              item altern             concatenated together.
 */
    private static boolean parseAlternative(CompilerState state)
    {
        RENode headTerm = null;
        RENode tailTerm = null;
        char[] source = state.cpbegin;
        while (true) {
            if (state.cp == state.cpend || source[state.cp] == '|'
                || (state.parenNesting != 0 && source[state.cp] == ')'))
            {
                if (headTerm == null) {
                    state.result = new RENode(REOP_EMPTY);
                }
                else
                    state.result = headTerm;
                return true;
            }
            if (!parseTerm(state))
                return false;
            if (headTerm == null) {
                headTerm = state.result;
                tailTerm = headTerm;
            }
            else
                tailTerm.next = state.result;
            while (tailTerm.next != null) tailTerm = tailTerm.next;
        }
    }

    /* calculate the total size of the bitmap required for a class expression */
    private static boolean
    calculateBitmapSize(CompilerState state, RENode target, char[] src,
                        int index, int end)
    {
        char rangeStart = 0;
        char c;
        int n;
        int nDigits;
        int i;
        int max = 0;
        boolean inRange = false;

        target.bmsize = 0;

        if (index == end)
            return true;

        if (src[index] == '^')
            ++index;

        while (index != end) {
            int localMax = 0;
            nDigits = 2;
            switch (src[index]) {
            case '\\':
                ++index;
                c = src[index++];
                switch (c) {
                case 'b':
                    localMax = 0x8;
                    break;
                case 'f':
                    localMax = 0xC;
                    break;
                case 'n':
                    localMax = 0xA;
                    break;
                case 'r':
                    localMax = 0xD;
                    break;
                case 't':
                    localMax = 0x9;
                    break;
                case 'v':
                    localMax = 0xB;
                    break;
                case 'c':
                    if ((index < end) && isControlLetter(src[index]))
                        localMax = (char)(src[index++] & 0x1F);
                    else
                        --index;
                        localMax = '\\';
                    break;
                case 'u':
                    nDigits += 2;
                    // fall thru...
                case 'x':
                    n = 0;
                    for (i = 0; (i < nDigits) && (index < end); i++) {
                        c = src[index++];
                        n = Kit.xDigitToInt(c, n);
                        if (n < 0) {
                            // Back off to accepting the original
                            // '\' as a literal
                            index -= (i + 1);
                            n = '\\';
                            break;
                        }
                    }
                    localMax = n;
                    break;
                case 'd':
                    if (inRange) {
                        reportError("msg.bad.range", "");
                        return false;
                    }
                    localMax = '9';
                    break;
                case 'D':
                case 's':
                case 'S':
                case 'w':
                case 'W':
                    if (inRange) {
                        reportError("msg.bad.range", "");
                        return false;
                    }
                    target.bmsize = 65536;
                    return true;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    /*
                     *  This is a non-ECMA extension - decimal escapes (in this
                     *  case, octal!) are supposed to be an error inside class
                     *  ranges, but supported here for backwards compatibility.
                     *
                     */
                    n = (c - '0');
                    c = src[index];
                    if ('0' <= c && c <= '7') {
                        index++;
                        n = 8 * n + (c - '0');
                        c = src[index];
                        if ('0' <= c && c <= '7') {
                            index++;
                            i = 8 * n + (c - '0');
                            if (i <= 0377)
                                n = i;
                            else
                                index--;
                        }
                    }
                    localMax = n;
                    break;

                default:
                    localMax = c;
                    break;
                }
                break;
            default:
                localMax = src[index++];
                break;
            }
            if (inRange) {
                if (rangeStart > localMax) {
                    reportError("msg.bad.range", "");
                    return false;
                }
                inRange = false;
            }
            else {
                if (index < (end - 1)) {
                    if (src[index] == '-') {
                        ++index;
                        inRange = true;
                        rangeStart = (char)localMax;
                        continue;
                    }
                }
            }
            if ((state.flags & JSREG_FOLD) != 0){
                char cu = upcase((char)localMax);
                char cd = downcase((char)localMax);
                localMax = (cu >= cd) ? cu : cd;
            }
            if (localMax > max)
                max = localMax;
        }
        target.bmsize = max + 1;
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

    private static void doFlat(CompilerState state, char c)
    {
        state.result = new RENode(REOP_FLAT);
        state.result.chr = c;
        state.result.length = 1;
        state.result.flatIndex = -1;
        state.progLength += 3;
    }

    private static int
    getDecimalValue(char c, CompilerState state, int maxValue,
                    String overflowMessageId)
    {
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
                if (v < maxValue) {
                    value = v;
                } else {
                    overflow = true;
                    value = maxValue;
                }
            }
        }
        if (overflow) {
            reportError(overflowMessageId,
                        String.valueOf(src, start, state.cp - start));
        }
        return value;
    }

    private static boolean
    parseTerm(CompilerState state)
    {
        char[] src = state.cpbegin;
        char c = src[state.cp++];
        int nDigits = 2;
        int parenBaseCount = state.parenCount;
        int num, tmp;
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
            if (state.cp < state.cpend) {
                c = src[state.cp++];
                switch (c) {
                /* assertion escapes */
                case 'b' :
                    state.result = new RENode(REOP_WBDRY);
                    state.progLength++;
                    return true;
                case 'B':
                    state.result = new RENode(REOP_WNONBDRY);
                    state.progLength++;
                    return true;
                /* Decimal escape */
                case '0':
/*
 * We're deliberately violating the ECMA 5.1 specification and allow octal
 * escapes to follow spidermonkey and general 'web reality':
 * http://wiki.ecmascript.org/doku.php?id=harmony:regexp_match_web_reality
 * http://wiki.ecmascript.org/doku.php?id=strawman:match_web_reality_spec
 */
                    reportWarning(state.cx, "msg.bad.backref", "");
                    /* octal escape */
                    num = 0;
                    // follow spidermonkey and allow multiple leading zeros,
                    // e.g. let /\0000/ match the string "\0"
                    while (num < 040 && state.cp < state.cpend) {
                        c = src[state.cp];
                        if ((c >= '0') && (c <= '7')) {
                            state.cp++;
                            num = 8 * num + (c - '0');
                        }
                        else
                            break;
                    }
                    c = (char)(num);
                    doFlat(state, c);
                    break;
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    termStart = state.cp - 1;
                    num = getDecimalValue(c, state, 0xFFFF,
                                          "msg.overlarge.backref");
                    if (num > state.backReferenceLimit)
                        reportWarning(state.cx, "msg.bad.backref", "");
                    /*
                     * n > count of parentheses, then treat as octal instead.
                     * Also see note above concerning 'web reality'
                     */
                    if (num > state.backReferenceLimit) {
                        state.cp = termStart;
                        if (c >= '8') {
                            // invalid octal escape, follow spidermonkey and
                            // treat as \\8 resp. \\9
                            c = '\\';
                            doFlat(state, c);
                            break;
                        }
                        state.cp++;
                        num = c - '0';
                        while (num < 040 && state.cp < state.cpend) {
                            c = src[state.cp];
                            if ((c >= '0') && (c <= '7')) {
                                state.cp++;
                                num = 8 * num + (c - '0');
                            }
                            else
                                break;
                        }
                        c = (char)(num);
                        doFlat(state, c);
                        break;
                    }
                    /* otherwise, it's a back-reference */
                    state.result = new RENode(REOP_BACKREF);
                    state.result.parenIndex = num - 1;
                    state.progLength += 3;
                    if (state.maxBackReference < num) {
                        state.maxBackReference = num;
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
                    if ((state.cp < state.cpend) &&
                                        isControlLetter(src[state.cp]))
                        c = (char)(src[state.cp++] & 0x1F);
                    else {
                        /* back off to accepting the original '\' as a literal */
                        --state.cp;
                        c = '\\';
                    }
                    doFlat(state, c);
                    break;
                /* UnicodeEscapeSequence */
                case 'u':
                    nDigits += 2;
                    // fall thru...
                /* HexEscapeSequence */
                case 'x':
                    {
                        int n = 0;
                        int i;
                        for (i = 0; (i < nDigits)
                                && (state.cp < state.cpend); i++) {
                            c = src[state.cp++];
                            n = Kit.xDigitToInt(c, n);
                            if (n < 0) {
                                // Back off to accepting the original
                                // 'u' or 'x' as a literal
                                state.cp -= (i + 2);
                                n = src[state.cp++];
                                break;
                            }
                        }
                        c = (char)(n);
                    }
                    doFlat(state, c);
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
                /* IdentityEscape */
                default:
                    state.result = new RENode(REOP_FLAT);
                    state.result.chr = c;
                    state.result.length = 1;
                    state.result.flatIndex = state.cp - 1;
                    state.progLength += 3;
                    break;
                }
                break;
            }
            else {
                /* a trailing '\' is an error */
                reportError("msg.trail.backslash", "");
                return false;
            }
        case '(': {
            RENode result = null;
            termStart = state.cp;
            if (state.cp + 1 < state.cpend && src[state.cp] == '?'
                && ((c = src[state.cp + 1]) == '=' || c == '!' || c == ':'))
            {
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
            } else {
                result = new RENode(REOP_LPAREN);
                /* LPAREN, <index>, ... RPAREN, <index> */
                state.progLength += 6;
                result.parenIndex = state.parenCount++;
            }
            ++state.parenNesting;
            if (!parseDisjunction(state))
                return false;
            if (state.cp == state.cpend || src[state.cp] != ')') {
                reportError("msg.unterm.paren", "");
                return false;
            }
            ++state.cp;
            --state.parenNesting;
            if (result != null) {
                result.kid = state.result;
                state.result = result;
            }
            break;
        }
        case ')':
          reportError("msg.re.unmatched.right.paren", "");
          return false;
        case '[':
            state.result = new RENode(REOP_CLASS);
            termStart = state.cp;
            state.result.startIndex = termStart;
            while (true) {
                if (state.cp == state.cpend) {
                    reportError("msg.unterm.class", "");
                    return false;
                }
                if (src[state.cp] == '\\')
                    state.cp++;
                else {
                    if (src[state.cp] == ']') {
                        state.result.kidlen = state.cp - termStart;
                        break;
                    }
                }
                state.cp++;
            }
            state.result.index = state.classCount++;
            /*
             * Call calculateBitmapSize now as we want any errors it finds
             * to be reported during the parse phase, not at execution.
             */
            if (!calculateBitmapSize(state, state.result, src, termStart, state.cp++))
                return false;
            state.progLength += 3; /* CLASS, <index> */
            break;

        case '.':
            state.result = new RENode(REOP_DOT);
            state.progLength++;
            break;
        case '{':
        case '*':
        case '+':
        case '?':
            reportError("msg.bad.quant", String.valueOf(src[state.cp - 1]));
            return false;
        default:
            state.result = new RENode(REOP_FLAT);
            state.result.chr = c;
            state.result.length = 1;
            state.result.flatIndex = state.cp - 1;
            state.progLength += 3;
            break;
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
            case '{':  /* balance '}' */
            {
                int min = 0;
                int max = -1;
                int leftCurl = state.cp;

               /* For Perl etc. compatibility, if quntifier does not match
                * \{\d+(,\d*)?\} exactly back off from it
                * being a quantifier, and chew it up as a literal
                * atom next time instead.
                */

                c = src[++state.cp];
                if (isDigit(c)) {
                    ++state.cp;
                    min = getDecimalValue(c, state, 0xFFFF,
                                          "msg.overlarge.min");
                    c = src[state.cp];
                    if (c == ',') {
                        c = src[++state.cp];
                        if (isDigit(c)) {
                            ++state.cp;
                            max = getDecimalValue(c, state, 0xFFFF,
                                                  "msg.overlarge.max");
                            c = src[state.cp];
                            if (min > max) {
                                reportError("msg.max.lt.min",
                                            String.valueOf(src[state.cp]));
                                return false;
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
                if (!hasQ) {
                    state.cp = leftCurl;
                }
                break;
            }
        }
        if (!hasQ)
            return true;

        ++state.cp;
        state.result.kid = term;
        state.result.parenIndex = parenBaseCount;
        state.result.parenCount = state.parenCount - parenBaseCount;
        if ((state.cp < state.cpend) && (src[state.cp] == '?')) {
            ++state.cp;
            state.result.greedy = false;
        }
        else
            state.result.greedy = true;
        return true;
    }

    private static void resolveForwardJump(byte[] array, int from, int pc)
    {
        if (from > pc) throw Kit.codeBug();
        addIndex(array, from, pc - from);
    }

    private static int getOffset(byte[] array, int pc)
    {
        return getIndex(array, pc);
    }

    private static int addIndex(byte[] array, int pc, int index)
    {
        if (index < 0) throw Kit.codeBug();
        if (index > 0xFFFF)
            throw Context.reportRuntimeError("Too complex regexp");
        array[pc] = (byte)(index >> 8);
        array[pc + 1] = (byte)(index);
        return pc + 2;
    }

    private static int getIndex(byte[] array, int pc)
    {
        return ((array[pc] & 0xFF) << 8) | (array[pc + 1] & 0xFF);
    }

    private static final int OFFSET_LEN = 2;
    private static final int INDEX_LEN  = 2;

    private static int
    emitREBytecode(CompilerState state, RECompiled re, int pc, RENode t)
    {
        RENode nextAlt;
        int nextAltFixup, nextTermFixup;
        byte[] program = re.program;

        while (t != null) {
            program[pc++] = t.op;
            switch (t.op) {
            case REOP_EMPTY:
                --pc;
                break;
            case REOP_ALT:
                nextAlt = t.kid2;
                nextAltFixup = pc;    /* address of next alternate */
                pc += OFFSET_LEN;
                pc = emitREBytecode(state, re, pc, t.kid);
                program[pc++] = REOP_JUMP;
                nextTermFixup = pc;    /* address of following term */
                pc += OFFSET_LEN;
                resolveForwardJump(program, nextAltFixup, pc);
                pc = emitREBytecode(state, re, pc, nextAlt);

                program[pc++] = REOP_JUMP;
                nextAltFixup = pc;
                pc += OFFSET_LEN;

                resolveForwardJump(program, nextTermFixup, pc);
                resolveForwardJump(program, nextAltFixup, pc);
                break;
            case REOP_FLAT:
                /*
                 * Consecutize FLAT's if possible.
                 */
                if (t.flatIndex != -1) {
                    while ((t.next != null) && (t.next.op == REOP_FLAT)
                            && ((t.flatIndex + t.length)
                                            == t.next.flatIndex)) {
                        t.length += t.next.length;
                        t.next = t.next.next;
                    }
                }
                if ((t.flatIndex != -1) && (t.length > 1)) {
                    if ((state.flags & JSREG_FOLD) != 0)
                        program[pc - 1] = REOP_FLATi;
                    else
                        program[pc - 1] = REOP_FLAT;
                    pc = addIndex(program, pc, t.flatIndex);
                    pc = addIndex(program, pc, t.length);
                }
                else {
                    if (t.chr < 256) {
                        if ((state.flags & JSREG_FOLD) != 0)
                            program[pc - 1] = REOP_FLAT1i;
                        else
                            program[pc - 1] = REOP_FLAT1;
                        program[pc++] = (byte)(t.chr);
                    }
                    else {
                        if ((state.flags & JSREG_FOLD) != 0)
                            program[pc - 1] = REOP_UCFLAT1i;
                        else
                            program[pc - 1] = REOP_UCFLAT1;
                        pc = addIndex(program, pc, t.chr);
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
            case REOP_ASSERT:
                nextTermFixup = pc;
                pc += OFFSET_LEN;
                pc = emitREBytecode(state, re, pc, t.kid);
                program[pc++] = REOP_ASSERTTEST;
                resolveForwardJump(program, nextTermFixup, pc);
                break;
            case REOP_ASSERT_NOT:
                nextTermFixup = pc;
                pc += OFFSET_LEN;
                pc = emitREBytecode(state, re, pc, t.kid);
                program[pc++] = REOP_ASSERTNOTTEST;
                resolveForwardJump(program, nextTermFixup, pc);
                break;
            case REOP_QUANT:
                if ((t.min == 0) && (t.max == -1))
                    program[pc - 1] = (t.greedy) ? REOP_STAR : REOP_MINIMALSTAR;
                else
                if ((t.min == 0) && (t.max == 1))
                    program[pc - 1] = (t.greedy) ? REOP_OPT : REOP_MINIMALOPT;
                else
                if ((t.min == 1) && (t.max == -1))
                    program[pc - 1] = (t.greedy) ? REOP_PLUS : REOP_MINIMALPLUS;
                else {
                    if (!t.greedy) program[pc - 1] = REOP_MINIMALQUANT;
                    pc = addIndex(program, pc, t.min);
                    // max can be -1 which addIndex does not accept
                    pc = addIndex(program, pc, t.max + 1);
                }
                pc = addIndex(program, pc, t.parenCount);
                pc = addIndex(program, pc, t.parenIndex);
                nextTermFixup = pc;
                pc += OFFSET_LEN;
                pc = emitREBytecode(state, re, pc, t.kid);
                program[pc++] = REOP_ENDCHILD;
                resolveForwardJump(program, nextTermFixup, pc);
                break;
            case REOP_CLASS:
                pc = addIndex(program, pc, t.index);
                re.classList[t.index] = new RECharSet(t.bmsize, t.startIndex,
                                                      t.kidlen);
                break;
            default:
                break;
            }
            t = t.next;
        }
        return pc;
    }

    private static void
    pushProgState(REGlobalData gData, int min, int max,
                  REBackTrackData backTrackLastToSave,
                  int continuation_pc, int continuation_op)
    {
        gData.stateStackTop = new REProgState(gData.stateStackTop, min, max,
                                              gData.cp, backTrackLastToSave,
                                              continuation_pc,
                                              continuation_op);
    }

    private static REProgState
    popProgState(REGlobalData gData)
    {
        REProgState state = gData.stateStackTop;
        gData.stateStackTop = state.previous;
        return state;
    }

    private static void
    pushBackTrackState(REGlobalData gData, byte op, int target)
    {
        gData.backTrackStackTop = new REBackTrackData(gData, op, target);
    }

    /*
     *   Consecutive literal characters.
     */
    private static boolean
    flatNMatcher(REGlobalData gData, int matchChars,
                 int length, String input, int end)
    {
        if ((gData.cp + length) > end)
            return false;
        for (int i = 0; i < length; i++) {
            if (gData.regexp.source[matchChars + i] != input.charAt(gData.cp + i)) {
                return false;
            }
        }
        gData.cp += length;
        return true;
    }

    private static boolean
    flatNIMatcher(REGlobalData gData, int matchChars,
                  int length, String input, int end)
    {
        if ((gData.cp + length) > end)
            return false;
        for (int i = 0; i < length; i++) {
            if (upcase(gData.regexp.source[matchChars + i])
                != upcase(input.charAt(gData.cp + i)))
            {
                return false;
            }
        }
        gData.cp += length;
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
    private static boolean
    backrefMatcher(REGlobalData gData, int parenIndex,
                   String input, int end)
    {
        int len;
        int i;
        if (gData.parens == null || parenIndex >= gData.parens.length)
            return false;
        int parenContent = gData.parens_index(parenIndex);
        if (parenContent == -1)
            return true;

        len = gData.parens_length(parenIndex);
        if ((gData.cp + len) > end)
            return false;

        if ((gData.regexp.flags & JSREG_FOLD) != 0) {
            for (i = 0; i < len; i++) {
                if (upcase(input.charAt(parenContent + i)) != upcase(input.charAt(gData.cp + i)))
                    return false;
            }
        }
        else {
            for (i = 0; i < len; i++) {
                if (input.charAt(parenContent + i) != input.charAt(gData.cp + i))
                    return false;
            }
        }
        gData.cp += len;
        return true;
    }


    /* Add a single character to the RECharSet */
    private static void
    addCharacterToCharSet(RECharSet cs, char c)
    {
        int byteIndex = (c / 8);
        if (c >= cs.length) {
            throw ScriptRuntime.constructError("SyntaxError",
                    "invalid range in character class");
        }
        cs.bits[byteIndex] |= 1 << (c & 0x7);
    }


    /* Add a character range, c1 to c2 (inclusive) to the RECharSet */
    private static void
    addCharacterRangeToCharSet(RECharSet cs, char c1, char c2)
    {
        int i;

        int byteIndex1 = (c1 / 8);
        int byteIndex2 = (c2 / 8);

        if ((c2 >= cs.length) || (c1 > c2)) {
            throw ScriptRuntime.constructError("SyntaxError",
                    "invalid range in character class");
        }

        c1 &= 0x7;
        c2 &= 0x7;

        if (byteIndex1 == byteIndex2) {
            cs.bits[byteIndex1] |= ((0xFF) >> (7 - (c2 - c1))) << c1;
        }
        else {
            cs.bits[byteIndex1] |= 0xFF << c1;
            for (i = byteIndex1 + 1; i < byteIndex2; i++)
                cs.bits[i] = (byte)0xFF;
            cs.bits[byteIndex2] |= (0xFF) >> (7 - c2);
        }
    }

    /* Compile the source of the class into a RECharSet */
    private static void
    processCharSet(REGlobalData gData, RECharSet charSet)
    {
        synchronized (charSet) {
            if (!charSet.converted) {
                processCharSetImpl(gData, charSet);
                charSet.converted = true;
            }
        }
    }


    private static void
    processCharSetImpl(REGlobalData gData, RECharSet charSet)
    {
        int src = charSet.startIndex;
        int end = src + charSet.strlength;

        char rangeStart = 0, thisCh;
        int byteLength;
        char c;
        int n;
        int nDigits;
        int i;
        boolean inRange = false;

        charSet.sense = true;
        byteLength = (charSet.length + 7) / 8;
        charSet.bits = new byte[byteLength];

        if (src == end)
            return;

        if (gData.regexp.source[src] == '^') {
            charSet.sense = false;
            ++src;
        }

        while (src != end) {
            nDigits = 2;
            switch (gData.regexp.source[src]) {
            case '\\':
                ++src;
                c = gData.regexp.source[src++];
                switch (c) {
                case 'b':
                    thisCh = 0x8;
                    break;
                case 'f':
                    thisCh = 0xC;
                    break;
                case 'n':
                    thisCh = 0xA;
                    break;
                case 'r':
                    thisCh = 0xD;
                    break;
                case 't':
                    thisCh = 0x9;
                    break;
                case 'v':
                    thisCh = 0xB;
                    break;
                case 'c':
                    if ((src < end) && isControlLetter(gData.regexp.source[src]))
                        thisCh = (char)(gData.regexp.source[src++] & 0x1F);
                    else {
                        --src;
                        thisCh = '\\';
                    }
                    break;
                case 'u':
                    nDigits += 2;
                    // fall thru
                case 'x':
                    n = 0;
                    for (i = 0; (i < nDigits) && (src < end); i++) {
                        c = gData.regexp.source[src++];
                        int digit = toASCIIHexDigit(c);
                        if (digit < 0) {
                            /* back off to accepting the original '\'
                             * as a literal
                             */
                            src -= (i + 1);
                            n = '\\';
                            break;
                        }
                        n = (n << 4) | digit;
                    }
                    thisCh = (char)(n);
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    /*
                     *  This is a non-ECMA extension - decimal escapes (in this
                     *  case, octal!) are supposed to be an error inside class
                     *  ranges, but supported here for backwards compatibility.
                     *
                     */
                    n = (c - '0');
                    c = gData.regexp.source[src];
                    if ('0' <= c && c <= '7') {
                        src++;
                        n = 8 * n + (c - '0');
                        c = gData.regexp.source[src];
                        if ('0' <= c && c <= '7') {
                            src++;
                            i = 8 * n + (c - '0');
                            if (i <= 0377)
                                n = i;
                            else
                                src--;
                        }
                    }
                    thisCh = (char)(n);
                    break;

                case 'd':
                    addCharacterRangeToCharSet(charSet, '0', '9');
                    continue;   /* don't need range processing */
                case 'D':
                    addCharacterRangeToCharSet(charSet, (char)0, (char)('0' - 1));
                    addCharacterRangeToCharSet(charSet, (char)('9' + 1),
                                                (char)(charSet.length - 1));
                    continue;
                case 's':
                    for (i = (charSet.length - 1); i >= 0; i--)
                        if (isREWhiteSpace(i))
                            addCharacterToCharSet(charSet, (char)(i));
                    continue;
                case 'S':
                    for (i = (charSet.length - 1); i >= 0; i--)
                        if (!isREWhiteSpace(i))
                            addCharacterToCharSet(charSet, (char)(i));
                    continue;
                case 'w':
                    for (i = (charSet.length - 1); i >= 0; i--)
                        if (isWord((char)i))
                            addCharacterToCharSet(charSet, (char)(i));
                    continue;
                case 'W':
                    for (i = (charSet.length - 1); i >= 0; i--)
                        if (!isWord((char)i))
                            addCharacterToCharSet(charSet, (char)(i));
                    continue;
                default:
                    thisCh = c;
                    break;

                }
                break;

            default:
                thisCh = gData.regexp.source[src++];
                break;

            }
            if (inRange) {
                if ((gData.regexp.flags & JSREG_FOLD) != 0) {
                    addCharacterRangeToCharSet(charSet,
                                               upcase(rangeStart),
                                               upcase(thisCh));
                    addCharacterRangeToCharSet(charSet,
                                               downcase(rangeStart),
                                               downcase(thisCh));
                } else {
                    addCharacterRangeToCharSet(charSet, rangeStart, thisCh);
                }
                inRange = false;
            }
            else {
                if ((gData.regexp.flags & JSREG_FOLD) != 0) {
                    addCharacterToCharSet(charSet, upcase(thisCh));
                    addCharacterToCharSet(charSet, downcase(thisCh));
                } else {
                    addCharacterToCharSet(charSet, thisCh);
                }
                if (src < (end - 1)) {
                    if (gData.regexp.source[src] == '-') {
                        ++src;
                        inRange = true;
                        rangeStart = thisCh;
                    }
                }
            }
        }
    }


    /*
     *   Initialize the character set if it this is the first call.
     *   Test the bit - if the ^ flag was specified, non-inclusion is a success
     */
    private static boolean
    classMatcher(REGlobalData gData, RECharSet charSet, char ch)
    {
        if (!charSet.converted) {
            processCharSet(gData, charSet);
        }

        int byteIndex = ch / 8;
        if (charSet.sense) {
            if ((charSet.length == 0) ||
                 ( (ch >= charSet.length)
                    || ((charSet.bits[byteIndex] & (1 << (ch & 0x7))) == 0) ))
                return false;
        } else {
            if (! ((charSet.length == 0) ||
                     ( (ch >= charSet.length)
                        || ((charSet.bits[byteIndex] & (1 << (ch & 0x7))) == 0) )))
                return false;
        }
        return true;
    }

    private static boolean
    executeREBytecode(REGlobalData gData, String input, int end)
    {
        int pc = 0;
        byte program[] = gData.regexp.program;
        int currentContinuation_op;
        int currentContinuation_pc;
        boolean result = false;

        currentContinuation_pc = 0;
        currentContinuation_op = REOP_END;
if (debug) {
System.out.println("Input = \"" + input + "\", start at " + gData.cp);
}
        int op = program[pc++];
        for (;;) {
if (debug) {
System.out.println("Testing at " + gData.cp + ", op = " + op);
}
            switch (op) {
            case REOP_EMPTY:
                result = true;
                break;
            case REOP_BOL:
                if (gData.cp != 0) {
                    if (gData.multiline ||
                            ((gData.regexp.flags & JSREG_MULTILINE) != 0)) {
                        if (!isLineTerm(input.charAt(gData.cp - 1))) {
                            result = false;
                            break;
                        }
                    }
                    else {
                        result = false;
                        break;
                    }
                }
                result = true;
                break;
            case REOP_EOL:
                if (gData.cp != end) {
                    if (gData.multiline ||
                            ((gData.regexp.flags & JSREG_MULTILINE) != 0)) {
                        if (!isLineTerm(input.charAt(gData.cp))) {
                            result = false;
                            break;
                        }
                    }
                    else {
                        result = false;
                        break;
                    }
                }
                result = true;
                break;
            case REOP_WBDRY:
                result = ((gData.cp == 0 || !isWord(input.charAt(gData.cp - 1)))
                          ^ !((gData.cp < end) && isWord(input.charAt(gData.cp))));
                break;
            case REOP_WNONBDRY:
                result = ((gData.cp == 0 || !isWord(input.charAt(gData.cp - 1)))
                          ^ ((gData.cp < end) && isWord(input.charAt(gData.cp))));
                break;
            case REOP_DOT:
                result = (gData.cp != end && !isLineTerm(input.charAt(gData.cp)));
                if (result) {
                    gData.cp++;
                }
                break;
            case REOP_DIGIT:
                result = (gData.cp != end && isDigit(input.charAt(gData.cp)));
                if (result) {
                    gData.cp++;
                }
                break;
            case REOP_NONDIGIT:
                result = (gData.cp != end && !isDigit(input.charAt(gData.cp)));
                if (result) {
                    gData.cp++;
                }
                break;
            case REOP_SPACE:
                result = (gData.cp != end && isREWhiteSpace(input.charAt(gData.cp)));
                if (result) {
                    gData.cp++;
                }
                break;
            case REOP_NONSPACE:
                result = (gData.cp != end && !isREWhiteSpace(input.charAt(gData.cp)));
                if (result) {
                    gData.cp++;
                }
                break;
            case REOP_ALNUM:
                result = (gData.cp != end && isWord(input.charAt(gData.cp)));
                if (result) {
                    gData.cp++;
                }
                break;
            case REOP_NONALNUM:
                result = (gData.cp != end && !isWord(input.charAt(gData.cp)));
                if (result) {
                    gData.cp++;
                }
                break;
            case REOP_FLAT:
                {
                    int offset = getIndex(program, pc);
                    pc += INDEX_LEN;
                    int length = getIndex(program, pc);
                    pc += INDEX_LEN;
                    result = flatNMatcher(gData, offset, length, input, end);
                }
                break;
            case REOP_FLATi:
                {
                    int offset = getIndex(program, pc);
                    pc += INDEX_LEN;
                    int length = getIndex(program, pc);
                    pc += INDEX_LEN;
                    result = flatNIMatcher(gData, offset, length, input, end);
                }
                break;
            case REOP_FLAT1:
                {
                    char matchCh = (char)(program[pc++] & 0xFF);
                    result = (gData.cp != end && input.charAt(gData.cp) == matchCh);
                    if (result) {
                        gData.cp++;
                    }
                }
                break;
            case REOP_FLAT1i:
                {
                    char matchCh = (char)(program[pc++] & 0xFF);
                    result = (gData.cp != end
                              && upcase(input.charAt(gData.cp)) == upcase(matchCh));
                    if (result) {
                        gData.cp++;
                    }
                }
                break;
            case REOP_UCFLAT1:
                {
                    char matchCh = (char)getIndex(program, pc);
                    pc += INDEX_LEN;
                    result = (gData.cp != end && input.charAt(gData.cp) == matchCh);
                    if (result) {
                        gData.cp++;
                    }
                }
                break;
            case REOP_UCFLAT1i:
                {
                    char matchCh = (char)getIndex(program, pc);
                    pc += INDEX_LEN;
                    result = (gData.cp != end
                              && upcase(input.charAt(gData.cp)) == upcase(matchCh));
                    if (result) {
                        gData.cp++;
                    }
                }
                break;
            case REOP_ALT:
                {
                    int nextpc;
                    byte nextop;
                    pushProgState(gData, 0, 0, null,
                                  currentContinuation_pc,
                                  currentContinuation_op);
                    nextpc = pc + getOffset(program, pc);
                    nextop = program[nextpc++];
                    pushBackTrackState(gData, nextop, nextpc);
                    pc += INDEX_LEN;
                    op = program[pc++];
                }
                continue;

            case REOP_JUMP:
                {
                    int offset;
                    REProgState state = popProgState(gData);
                    currentContinuation_pc = state.continuation_pc;
                    currentContinuation_op = state.continuation_op;
                    offset = getOffset(program, pc);
                    pc += offset;
                    op = program[pc++];
                }
                continue;


            case REOP_LPAREN:
                {
                    int parenIndex = getIndex(program, pc);
                    pc += INDEX_LEN;
                    gData.set_parens(parenIndex, gData.cp, 0);
                    op = program[pc++];
                }
                continue;
            case REOP_RPAREN:
                {
                    int cap_index;
                    int parenIndex = getIndex(program, pc);
                    pc += INDEX_LEN;
                    cap_index = gData.parens_index(parenIndex);
                    gData.set_parens(parenIndex, cap_index,
                                     gData.cp - cap_index);
                    if (parenIndex > gData.lastParen)
                        gData.lastParen = parenIndex;
                    op = program[pc++];
                }
                continue;
            case REOP_BACKREF:
                {
                    int parenIndex = getIndex(program, pc);
                    pc += INDEX_LEN;
                    result = backrefMatcher(gData, parenIndex, input, end);
                }
                break;

            case REOP_CLASS:
                {
                    int index = getIndex(program, pc);
                    pc += INDEX_LEN;
                    if (gData.cp != end) {
                        if (classMatcher(gData, gData.regexp.classList[index],
                                         input.charAt(gData.cp)))
                        {
                            gData.cp++;
                            result = true;
                            break;
                        }
                    }
                    result = false;
                }
                break;

            case REOP_ASSERT:
            case REOP_ASSERT_NOT:
                {
                    byte testOp;
                    pushProgState(gData, 0, 0, gData.backTrackStackTop,
                                  currentContinuation_pc,
                                  currentContinuation_op);
                    if (op == REOP_ASSERT) {
                        testOp = REOP_ASSERTTEST;
                    } else {
                        testOp = REOP_ASSERTNOTTEST;
                    }
                    pushBackTrackState(gData, testOp,
                                       pc + getOffset(program, pc));
                    pc += INDEX_LEN;
                    op = program[pc++];
                }
                continue;

            case REOP_ASSERTTEST:
            case REOP_ASSERTNOTTEST:
                {
                    REProgState state = popProgState(gData);
                    gData.cp = state.index;
                    gData.backTrackStackTop = state.backTrack;
                    currentContinuation_pc = state.continuation_pc;
                    currentContinuation_op = state.continuation_op;
                    if (result) {
                        if (op == REOP_ASSERTTEST) {
                            result = true;
                        } else {
                            result = false;
                        }
                    } else {
                        if (op == REOP_ASSERTTEST) {
                            // Do nothing
                        } else {
                            result = true;
                        }
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
                    pushProgState(gData, min, max, null,
                                  currentContinuation_pc,
                                  currentContinuation_op);
                    if (greedy) {
                        currentContinuation_op = REOP_REPEAT;
                        currentContinuation_pc = pc;
                        pushBackTrackState(gData, REOP_REPEAT, pc);
                        /* Step over <parencount>, <parenindex> & <next> */
                        pc += 3 * INDEX_LEN;
                        op = program[pc++];
                    } else {
                        if (min != 0) {
                            currentContinuation_op = REOP_MINIMALREPEAT;
                            currentContinuation_pc = pc;
                            /* <parencount> <parenindex> & <next> */
                            pc += 3 * INDEX_LEN;
                            op = program[pc++];
                        } else {
                            pushBackTrackState(gData, REOP_MINIMALREPEAT, pc);
                            popProgState(gData);
                            pc += 2 * INDEX_LEN;  // <parencount> & <parenindex>
                            pc = pc + getOffset(program, pc);
                            op = program[pc++];
                        }
                    }
                }
                continue;

            case REOP_ENDCHILD:
                //
                // If we have not gotten a result here, it is because of an
                // empty match.  Do the same thing REOP_EMPTY would do.
                //
                result = true;
                // Use the current continuation.
                pc = currentContinuation_pc;
                op = currentContinuation_op;
                continue;

            case REOP_REPEAT:
                {
                    REProgState state = popProgState(gData);
                    if (!result) {
                        //
                        // There's been a failure, see if we have enough
                        // children.
                        //
                        if (state.min == 0)
                            result = true;
                        currentContinuation_pc = state.continuation_pc;
                        currentContinuation_op = state.continuation_op;
                        pc += 2 * INDEX_LEN;  /* <parencount> & <parenindex> */
                        pc = pc + getOffset(program, pc);
                        break;
                    }
                    else {
                        if (state.min == 0 && gData.cp == state.index) {
                            // matched an empty string, that'll get us nowhere
                            result = false;
                            currentContinuation_pc = state.continuation_pc;
                            currentContinuation_op = state.continuation_op;
                            pc += 2 * INDEX_LEN;
                            pc = pc + getOffset(program, pc);
                            break;
                        }
                        int new_min = state.min, new_max = state.max;
                        if (new_min != 0) new_min--;
                        if (new_max != -1) new_max--;
                        if (new_max == 0) {
                            result = true;
                            currentContinuation_pc = state.continuation_pc;
                            currentContinuation_op = state.continuation_op;
                            pc += 2 * INDEX_LEN;
                            pc = pc + getOffset(program, pc);
                            break;
                        }
                        pushProgState(gData, new_min, new_max, null,
                                      state.continuation_pc,
                                      state.continuation_op);
                        currentContinuation_op = REOP_REPEAT;
                        currentContinuation_pc = pc;
                        pushBackTrackState(gData, REOP_REPEAT, pc);
                        int parenCount = getIndex(program, pc);
                        pc += INDEX_LEN;
                        int parenIndex = getIndex(program, pc);
                        pc += 2 * INDEX_LEN;
                        op = program[pc++];
                        for (int k = 0; k < parenCount; k++) {
                            gData.set_parens(parenIndex + k, -1, 0);
                        }
                    }
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
                            pushProgState(gData, state.min, state.max, null,
                                          state.continuation_pc,
                                          state.continuation_op);
                            currentContinuation_op = REOP_MINIMALREPEAT;
                            currentContinuation_pc = pc;
                            int parenCount = getIndex(program, pc);
                            pc += INDEX_LEN;
                            int parenIndex = getIndex(program, pc);
                            pc += 2 * INDEX_LEN;
                            for (int k = 0; k < parenCount; k++) {
                                gData.set_parens(parenIndex + k, -1, 0);
                            }
                            op = program[pc++];
                            continue;
                        } else {
                            // Don't need to adjust pc since we're going to pop.
                            currentContinuation_pc = state.continuation_pc;
                            currentContinuation_op = state.continuation_op;
                            break;
                        }
                    } else {
                        if (state.min == 0 && gData.cp == state.index) {
                            // Matched an empty string, that'll get us nowhere.
                            result = false;
                            currentContinuation_pc = state.continuation_pc;
                            currentContinuation_op = state.continuation_op;
                            break;
                        }
                        int new_min = state.min, new_max = state.max;
                        if (new_min != 0) new_min--;
                        if (new_max != -1) new_max--;
                        pushProgState(gData, new_min, new_max, null,
                                      state.continuation_pc,
                                      state.continuation_op);
                        if (new_min != 0) {
                            currentContinuation_op = REOP_MINIMALREPEAT;
                            currentContinuation_pc = pc;
                            int parenCount = getIndex(program, pc);
                            pc += INDEX_LEN;
                            int parenIndex = getIndex(program, pc);
                            pc += 2 * INDEX_LEN;
                            for (int k = 0; k < parenCount; k++) {
                                gData.set_parens(parenIndex + k, -1, 0);
                            }
                            op = program[pc++];
                        } else {
                            currentContinuation_pc = state.continuation_pc;
                            currentContinuation_op = state.continuation_op;
                            pushBackTrackState(gData, REOP_MINIMALREPEAT, pc);
                            popProgState(gData);
                            pc += 2 * INDEX_LEN;
                            pc = pc + getOffset(program, pc);
                            op = program[pc++];
                        }
                        continue;
                    }
                }

            case REOP_END:
                return true;

            default:
                throw Kit.codeBug();

            }
            /*
             *  If the match failed and there's a backtrack option, take it.
             *  Otherwise this is a complete and utter failure.
             */
            if (!result) {
                REBackTrackData backTrackData = gData.backTrackStackTop;
                if (backTrackData != null) {
                    gData.backTrackStackTop = backTrackData.previous;

                    gData.lastParen = backTrackData.lastParen;

                    // XXX: If backTrackData will no longer be used, then
                    // there is no need to clone backTrackData.parens
                    if (backTrackData.parens != null) {
                        gData.parens = backTrackData.parens.clone();
                    }

                    gData.cp = backTrackData.cp;

                    gData.stateStackTop = backTrackData.stateStackTop;

                    currentContinuation_op
                        = gData.stateStackTop.continuation_op;
                    currentContinuation_pc
                        = gData.stateStackTop.continuation_pc;
                    pc = backTrackData.continuation_pc;
                    op = backTrackData.continuation_op;
                    continue;
                }
                else
                    return false;
            }

            op = program[pc++];
        }

    }

    private static boolean
    matchRegExp(REGlobalData gData, RECompiled re,
                String input, int start, int end, boolean multiline)
    {
        if (re.parenCount != 0) {
            gData.parens = new long[re.parenCount];
        } else {
            gData.parens = null;
        }

        gData.backTrackStackTop = null;

        gData.stateStackTop = null;

        gData.multiline = multiline;
        gData.regexp = re;
        gData.lastParen = 0;

        int anchorCh = gData.regexp.anchorCh;
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
            if (anchorCh >= 0) {
                for (;;) {
                    if (i == end) {
                        return false;
                    }
                    char matchCh = input.charAt(i);
                    if (matchCh == anchorCh ||
                            ((gData.regexp.flags & JSREG_FOLD) != 0
                             && upcase(matchCh) == upcase((char)anchorCh)))
                    {
                        break;
                    }
                    ++i;
                }
            }
            gData.cp = i;
            for (int j = 0; j < re.parenCount; j++) {
                gData.set_parens(j, -1, 0);
            }
            boolean result = executeREBytecode(gData, input, end);

            gData.backTrackStackTop = null;
            gData.stateStackTop = null;
            if (result) {
                gData.skipped = i - start;
                return true;
            }
        }
        return false;
    }

    /*
     * indexp is assumed to be an array of length 1
     */
    Object executeRegExp(Context cx, Scriptable scope, RegExpImpl res,
                         String str, int indexp[], int matchType)
    {
        REGlobalData gData = new REGlobalData();

        int start = indexp[0];
        int end = str.length();
        if (start > end)
            start = end;
        //
        // Call the recursive matcher to do the real work.
        //
        boolean matches = matchRegExp(gData, re, str, start, end,
                                      res.multiline);
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

        if (matchType == TEST) {
            /*
             * Testing for a match and updating cx.regExpImpl: don't allocate
             * an array object, do return true.
             */
            result = Boolean.TRUE;
            obj = null;
        }
        else {
            /*
             * The array returned on match has element 0 bound to the matched
             * string, elements 1 through re.parenCount bound to the paren
             * matches, an index property telling the length of the left context,
             * and an input property referring to the input string.
             */
            result = cx.newArray(scope, 0);
            obj = (Scriptable) result;

            String matchstr = str.substring(index, index + matchlen);
            obj.put(0, obj, matchstr, true);
        }

        if (re.parenCount == 0) {
            res.parens = null;
            res.lastParen = SubString.emptySubString;
        } else {
            SubString parsub = null;
            int num;
            res.parens = new SubString[re.parenCount];
            for (num = 0; num < re.parenCount; num++) {
                int cap_index = gData.parens_index(num);
                String parstr;
                if (cap_index != -1) {
                    int cap_length = gData.parens_length(num);
                    parsub = new SubString(str, cap_index, cap_length);
                    res.parens[num] = parsub;
                    if (matchType == TEST) continue;
                    parstr = parsub.toString();
                    obj.put(num+1, obj, parstr, true);
                }
                else {
                    if (matchType != TEST)
                        obj.put(num+1, obj, Undefined.instance, true);
                }
            }
            res.lastParen = parsub;
        }

        if (! (matchType == TEST)) {
            /*
             * Define the index and input properties last for better for/in loop
             * order (so they come after the elements).
             */
            obj.put("index", obj, Integer.valueOf(start + gData.skipped), true);
            obj.put("input", obj, str, true);
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
        if (cx.getLanguageVersion() == Context.VERSION_1_2) {
            /*
             * JS1.2 emulated Perl4.0.1.8 (patch level 36) for global regexps used
             * in scalar contexts, and unintentionally for the string.match "list"
             * psuedo-context.  On "hi there bye", the following would result:
             *
             * Language     while(/ /g){print("$`");}   s/ /$`/g
             * perl4.036    "hi", "there"               "hihitherehi therebye"
             * perl5        "hi", "hi there"            "hihitherehi therebye"
             * js1.2        "hi", "there"               "hihitheretherebye"
             *
             * Insofar as JS1.2 always defined $` as "left context from the last
             * match" for global regexps, it was more consistent than perl4.
             */
            res.leftContext.index = start;
            res.leftContext.length = gData.skipped;
        } else {
            /*
             * For JS1.3 and ECMAv2, emulate Perl5 exactly:
             *
             * js1.3        "hi", "hi there"            "hihitherehi therebye"
             */
            res.leftContext.index = 0;
            res.leftContext.length = start + gData.skipped;
        }

        res.rightContext.str = str;
        res.rightContext.index = ep;
        res.rightContext.length = end - ep;

        return result;
    }

    int getFlags()
    {
        return re.flags;
    }

    private static void reportWarning(Context cx, String messageId, String arg)
    {
        if (cx.hasFeature(Context.FEATURE_STRICT_MODE)) {
            String msg = ScriptRuntime.getMessage1(messageId, arg);
            Context.reportWarning(msg);
        }
    }

    private static void reportError(String messageId, String arg)
    {
        String msg = ScriptRuntime.getMessage1(messageId, arg);
        throw ScriptRuntime.constructError("SyntaxError", msg);
    }

// #string_id_map#

    private static final int
        Id_lastIndex    = 1,
        Id_source       = 2,
        Id_global       = 3,
        Id_ignoreCase   = 4,
        Id_multiline    = 5,

        MAX_INSTANCE_ID = 5;

    @Override
    protected int getMaxInstanceId()
    {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected int findInstanceIdInfo(String s)
    {
        int id;
// #generated# Last update: 2007-05-09 08:16:24 EDT
        L0: { id = 0; String X = null; int c;
            int s_length = s.length();
            if (s_length==6) {
                c=s.charAt(0);
                if (c=='g') { X="global";id=Id_global; }
                else if (c=='s') { X="source";id=Id_source; }
            }
            else if (s_length==9) {
                c=s.charAt(0);
                if (c=='l') { X="lastIndex";id=Id_lastIndex; }
                else if (c=='m') { X="multiline";id=Id_multiline; }
            }
            else if (s_length==10) { X="ignoreCase";id=Id_ignoreCase; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
// #/string_id_map#

        if (id == 0) return super.findInstanceIdInfo(s);

        int attr;
        switch (id) {
          case Id_lastIndex:
            attr = lastIndexAttr;
            break;
          case Id_source:
          case Id_global:
          case Id_ignoreCase:
          case Id_multiline:
            attr = PERMANENT | READONLY | DONTENUM;
            break;
          default:
            throw new IllegalStateException();
        }
        return instanceIdInfo(attr, id);
    }

    @Override
    protected String getInstanceIdName(int id)
    {
        switch (id) {
            case Id_lastIndex:  return "lastIndex";
            case Id_source:     return "source";
            case Id_global:     return "global";
            case Id_ignoreCase: return "ignoreCase";
            case Id_multiline:  return "multiline";
        }
        return super.getInstanceIdName(id);
    }

    @Override
    protected Object getInstanceIdValue(int id)
    {
        switch (id) {
          case Id_lastIndex:
            return lastIndex;
          case Id_source:
            return new String(re.source);
          case Id_global:
            return ScriptRuntime.wrapBoolean((re.flags & JSREG_GLOB) != 0);
          case Id_ignoreCase:
            return ScriptRuntime.wrapBoolean((re.flags & JSREG_FOLD) != 0);
          case Id_multiline:
            return ScriptRuntime.wrapBoolean((re.flags & JSREG_MULTILINE) != 0);
        }
        return super.getInstanceIdValue(id);
    }

    @Override
    protected void setInstanceIdValue(int id, Object value)
    {
        switch (id) {
          case Id_lastIndex:
            lastIndex = value;
            return;
          case Id_source:
          case Id_global:
          case Id_ignoreCase:
          case Id_multiline:
            return;
        }
        super.setInstanceIdValue(id, value);
    }

    @Override
    protected void setInstanceIdAttributes(int id, int attr)
    {
        switch (id) {
          case Id_lastIndex:
            lastIndexAttr = attr;
            return;
        }
        super.setInstanceIdAttributes(id, attr);
    }

    @Override
    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        switch (id) {
          case Id_compile:  arity=2; s="compile";  break;
          case Id_toString: arity=0; s="toString"; break;
          case Id_toSource: arity=0; s="toSource"; break;
          case Id_exec:     arity=1; s="exec";     break;
          case Id_test:     arity=1; s="test";     break;
          case Id_prefix:   arity=1; s="prefix";   break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(REGEXP_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Object thisObj, Object[] args)
    {
        if (!f.hasTag(REGEXP_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
          case Id_compile:
            return realThis(thisObj, f).compile(cx, scope, args);

          case Id_toString:
          case Id_toSource:
            return realThis(thisObj, f).toString();

          case Id_exec:
            return realThis(thisObj, f).execSub(cx, scope, args, MATCH);

          case Id_test: {
            Object x = realThis(thisObj, f).execSub(cx, scope, args, TEST);
            return Boolean.TRUE.equals(x) ? Boolean.TRUE : Boolean.FALSE;
          }

          case Id_prefix:
            return realThis(thisObj, f).execSub(cx, scope, args, PREFIX);
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    private static NativeRegExp realThis(Object thisObj, IdFunctionObject f)
    {
        if (!(thisObj instanceof NativeRegExp))
            throw incompatibleCallError(f);
        return (NativeRegExp)thisObj;
    }

// #string_id_map#
    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2007-05-09 08:16:24 EDT
        L0: { id = 0; String X = null; int c;
            L: switch (s.length()) {
            case 4: c=s.charAt(0);
                if (c=='e') { X="exec";id=Id_exec; }
                else if (c=='t') { X="test";id=Id_test; }
                break L;
            case 6: X="prefix";id=Id_prefix; break L;
            case 7: X="compile";id=Id_compile; break L;
            case 8: c=s.charAt(3);
                if (c=='o') { X="toSource";id=Id_toSource; }
                else if (c=='t') { X="toString";id=Id_toString; }
                break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_compile       = 1,
        Id_toString      = 2,
        Id_toSource      = 3,
        Id_exec          = 4,
        Id_test          = 5,
        Id_prefix        = 6,

        MAX_PROTOTYPE_ID = 6;

// #/string_id_map#

    private RECompiled re;
    Object lastIndex = 0d;        /* index after last match, for //g iterator */
    private int lastIndexAttr = DONTENUM | PERMANENT;
}       // class NativeRegExp

class RECompiled implements Serializable
{
    static final long serialVersionUID = -6144956577595844213L;

    char []source;          /* locked source string, sans // */
    int parenCount;         /* number of parenthesized submatches */
    int flags;              /* flags  */
    byte[] program;         /* regular expression bytecode */
    int classCount;         /* count [...] bitmaps */
    RECharSet[] classList;  /* list of [...] bitmaps */
    int anchorCh = -1;      /* if >= 0, then re starts with this literal char */
}

class RENode {

    RENode(byte op)
    {
        this.op = op;
    }

    byte            op;         /* r.e. op bytecode */
    RENode          next;       /* next in concatenation order */
    RENode          kid;        /* first operand */

    RENode          kid2;       /* second operand */
    int             parenIndex; /* or a parenthesis index */

                                /* or a range */
    int             min;
    int             max;
    int             parenCount;
    boolean         greedy;

                                /* or a character class */
    int             startIndex;
    int             kidlen;     /* length of string at kid, in chars */
    int             bmsize;     /* bitmap size, based on max char code */
    int             index;      /* index into class list */

                                /* or a literal sequence */
    char            chr;        /* of one character */
    int             length;     /* or many (via the index) */
    int             flatIndex;  /* which is -1 if not sourced */

}

class CompilerState {

    CompilerState(Context cx, char[] source, int length, int flags)
    {
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
    }

    Context     cx;
    char        cpbegin[];
    int         cpend;
    int         cp;
    int         flags;
    int         backReferenceLimit;
    int         maxBackReference;
    int         parenCount;
    int         parenNesting;
    int         classCount;   /* number of [] encountered */
    int         progLength;   /* estimated bytecode length */
    RENode      result;
}

class REProgState
{
    REProgState(REProgState previous, int min, int max, int index,
                REBackTrackData backTrack,
                int continuation_pc, int continuation_op)
    {
        this.previous = previous;
        this.min = min;
        this.max = max;
        this.index = index;
        this.continuation_op = continuation_op;
        this.continuation_pc = continuation_pc;
        this.backTrack = backTrack;
    }

    REProgState previous; // previous state in stack

    int min;                      /* current quantifier min */
    int max;                      /* current quantifier max */
    int index;                    /* progress in text */
    int continuation_op;
    int continuation_pc;
    REBackTrackData backTrack; // used by ASSERT_  to recover state
}

class REBackTrackData {

    REBackTrackData(REGlobalData gData, int op, int pc)
    {
        previous = gData.backTrackStackTop;
        continuation_op = op;
        continuation_pc = pc;
        lastParen = gData.lastParen;
        if (gData.parens != null) {
            parens = gData.parens.clone();
        }
        cp = gData.cp;
        stateStackTop = gData.stateStackTop;
    }

    REBackTrackData previous;

    int continuation_op;                /* where to backtrack to */
    int continuation_pc;
    int lastParen;
    long[] parens;                      /* parenthesis captures */
    int cp;                             /* char buffer index */
    REProgState stateStackTop;          /* state of op that backtracked */
}

class REGlobalData {
    boolean multiline;
    RECompiled regexp;              /* the RE in execution */
    int lastParen;                  /* highest paren set so far */
    int skipped;                    /* chars skipped anchoring this r.e. */

    int cp;                         /* char buffer index */
    long[] parens;                  /* parens captures */

    REProgState stateStackTop;       /* stack of state of current ancestors */

    REBackTrackData backTrackStackTop;  /* last matched-so-far position */


    /**
     * Get start of parenthesis capture contents, -1 for empty.
     */
    int parens_index(int i)
    {
        return (int)(parens[i]);
    }

    /**
     * Get length of parenthesis capture contents.
     */
    int parens_length(int i)
    {
        return (int)(parens[i] >>> 32);
    }

    void set_parens(int i, int index, int length)
    {
        parens[i] = (index & 0xffffffffL) | ((long)length << 32);
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
final class RECharSet implements Serializable
{
    static final long serialVersionUID = 7931787979395898394L;

    RECharSet(int length, int startIndex, int strlength)
    {
        this.length = length;
        this.startIndex = startIndex;
        this.strlength = strlength;
    }

    int length;
    int startIndex;
    int strlength;

    volatile transient boolean converted;
    volatile transient boolean sense;
    volatile transient byte[] bits;
}


