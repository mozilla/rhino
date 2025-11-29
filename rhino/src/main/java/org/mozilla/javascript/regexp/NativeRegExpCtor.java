/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import static org.mozilla.javascript.ScriptableObject.DONTENUM;
import static org.mozilla.javascript.ScriptableObject.PERMANENT;
import static org.mozilla.javascript.ScriptableObject.READONLY;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;

/**
 * This class implements the RegExp constructor native object.
 *
 * <p>Revision History: Implementation in C by Brendan Eich Initial port to Java by Norris Boyd from
 * jsregexp.c version 1.36 Merged up to version 1.38, which included Unicode support. Merged bug
 * fixes in version 1.39. Merged JSFUN13_BRANCH changes up to 1.32.2.11
 *
 * @author Brendan Eich
 * @author Norris Boyd
 */
class NativeRegExpCtor {
    private static final long serialVersionUID = -5733330028285400526L;

    public static LambdaConstructor init(Context cx, Scriptable scopeArg, boolean sealed) {
        // We have to keep parameter types to match lazy evaluation.
        ScriptableObject scope = (ScriptableObject) scopeArg;

        var ctor =
                new LambdaConstructor(
                        scope,
                        "RegExp",
                        2,
                        NativeRegExpCtor::js_constructCall,
                        NativeRegExpCtor::js_construct);
        ctor.defineProperty(
                cx,
                "multiline",
                (c) -> ScriptRuntime.wrapBoolean(getImpl().multiline),
                (c, v) -> getImpl().multiline = ScriptRuntime.toBoolean(v),
                PERMANENT);
        ctor.defineProperty(
                cx,
                "$*",
                (c) -> ScriptRuntime.wrapBoolean(getImpl().multiline),
                (c, v) -> getImpl().multiline = ScriptRuntime.toBoolean(v),
                PERMANENT);
        ctor.defineProperty(
                cx,
                "input",
                (c) -> toStr(getImpl().input),
                (c, v) -> getImpl().input = ScriptRuntime.toString(v),
                PERMANENT);
        ctor.defineProperty(
                cx,
                "$_",
                (c) -> toStr(getImpl().input),
                (c, v) -> getImpl().input = ScriptRuntime.toString(v),
                PERMANENT);
        ctor.defineProperty(cx, "lastMatch", (c) -> toStr(getImpl().lastMatch), PERMANENT);
        ctor.defineProperty(cx, "$&", (c) -> toStr(getImpl().lastMatch), PERMANENT);
        ctor.defineProperty(cx, "lastParen", (c) -> toStr(getImpl().lastParen), PERMANENT);
        ctor.defineProperty(cx, "$+", (c) -> toStr(getImpl().lastParen), PERMANENT);
        ctor.defineProperty(cx, "leftContext", (c) -> toStr(getImpl().leftContext), PERMANENT);
        ctor.defineProperty(cx, "$`", (c) -> toStr(getImpl().leftContext), PERMANENT);
        ctor.defineProperty(cx, "rightContext", (c) -> toStr(getImpl().rightContext), PERMANENT);
        ctor.defineProperty(cx, "$'", (c) -> toStr(getImpl().rightContext), PERMANENT);
        for (int i = 1; i < 10; i++) {
            int c = i - 1;
            ctor.defineProperty(
                    cx,
                    String.format("$%d", i),
                    (x) -> toStr(getImpl().getParenSubString(c)),
                    null,
                    PERMANENT);
        }

        // ES2025: RegExp.escape
        ctor.defineConstructorMethod(
                scope, "escape", 1, NativeRegExpCtor::escape, DONTENUM, DONTENUM | READONLY);

        return ctor;
    }

    private static String toStr(String subStr) {
        return subStr == null ? "" : subStr;
    }

    private static String toStr(SubString subStr) {
        return subStr == null ? "" : subStr.toString();
    }

    private static Scriptable js_constructCall(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length > 0
                && args[0] instanceof NativeRegExp
                && (args.length == 1 || args[1] == Undefined.instance)) {
            return (Scriptable) args[0];
        }
        return js_construct(cx, scope, args);
    }

    private static Scriptable js_construct(Context cx, Scriptable scope, Object[] args) {
        NativeRegExp re = NativeRegExpInstantiator.withLanguageVersion(cx.getLanguageVersion());
        re.compile(cx, scope, args);
        ScriptRuntime.setBuiltinProtoAndParent(re, scope, TopLevel.Builtins.RegExp);
        return re;
    }

    private static RegExpImpl getImpl() {
        Context cx = Context.getCurrentContext();
        return (RegExpImpl) ScriptRuntime.getRegExpProxy(cx);
    }

    /**
     * ES2025 RegExp.escape ( string )
     *
     * <p>This method escapes special regex characters in the input string.
     *
     * @param cx the current context
     * @param scope the scope
     * @param thisObj the this object
     * @param args the arguments (first arg must be a string)
     * @return escaped string
     */
    private static Object escape(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // RegExp.escape requires a string argument - throw TypeError if not string (no coercion)
        if (args.length == 0) {
            throw ScriptRuntime.typeErrorById("msg.not.a.string");
        }

        Object arg = args[0];
        // Check if argument is NOT a string - throw TypeError (no coercion per spec)
        if (arg == null
                || arg == Undefined.instance
                || arg == Undefined.SCRIPTABLE_UNDEFINED
                || !(arg instanceof CharSequence)) {
            throw ScriptRuntime.typeErrorById("msg.not.a.string");
        }

        String input = ScriptRuntime.toString(arg);
        if (input.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean isFirst = true;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            // SPEC STEP 1: Escape initial character if it's a decimal digit or ASCII letter
            if (isFirst && (isDecimalDigit(c) || isAsciiLetter(c))) {
                result.append(String.format("\\x%02x", (int) c));
                isFirst = false;
                continue;
            }
            isFirst = false;

            // SPEC: Surrogates must be escaped as unicode
            if (Character.isSurrogate(c)) {
                result.append(String.format("\\u%04x", (int) c));
                continue;
            }

            // SPEC STEP 2: Control escapes (Table 64)
            switch (c) {
                case '\t':
                    result.append("\\t");
                    continue;
                case '\n':
                    result.append("\\n");
                    continue;
                case '\u000B': // vertical tab
                    result.append("\\v");
                    continue;
                case '\f':
                    result.append("\\f");
                    continue;
                case '\r':
                    result.append("\\r");
                    continue;
            }

            // SPEC STEP 3: Syntax characters (backslash escape)
            if (isSyntaxCharacter(c)) {
                result.append('\\').append(c);
                continue;
            }

            // SPEC STEP 4-5: Other punctuators, WhiteSpace, LineTerminator (hex/unicode escape)
            if (isOtherPunctuator(c) || isWhiteSpace(c) || isLineTerminator(c)) {
                if (c <= 0xFF) {
                    result.append(String.format("\\x%02x", (int) c));
                } else {
                    result.append(String.format("\\u%04x", (int) c));
                }
                continue;
            }

            // SPEC STEP 6: Return code point as-is (including underscore, other letters, digits)
            result.append(c);
        }

        return result.toString();
    }

    /** Check if character is a decimal digit (0-9) */
    private static boolean isDecimalDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /** Check if character is an ASCII letter (a-z, A-Z) */
    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * Check if character is a RegExp syntax character. These characters are: . * + ? ^ $ | ( ) [ ]
     * { } \ /
     */
    private static boolean isSyntaxCharacter(char c) {
        return c == '.' || c == '*' || c == '+' || c == '?' || c == '^' || c == '$' || c == '|'
                || c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}' || c == '\\'
                || c == '/';
    }

    /**
     * Check if character is in "other punctuators" list per spec: ,-=<>#&!%:;@~'`" These are the
     * exact characters from the ES2025 spec that must be escaped with \xNN format
     */
    private static boolean isOtherPunctuator(char c) {
        // Spec: otherPunctuators = ",-=<>#&!%:;@~'`" + code unit 0x0022 (QUOTATION MARK)
        return c == 0x002c // , COMMA
                || c == 0x002d // - HYPHEN-MINUS
                || c == 0x003d // = EQUALS SIGN
                || c == 0x003c // < LESS-THAN SIGN
                || c == 0x003e // > GREATER-THAN SIGN
                || c == 0x0023 // # NUMBER SIGN
                || c == 0x0026 // & AMPERSAND
                || c == 0x0021 // ! EXCLAMATION MARK
                || c == 0x0025 // % PERCENT SIGN
                || c == 0x003a // : COLON
                || c == 0x003b // ; SEMICOLON
                || c == 0x0040 // @ COMMERCIAL AT
                || c == 0x007e // ~ TILDE
                || c == 0x0027 // ' APOSTROPHE
                || c == 0x0060 // ` GRAVE ACCENT
                || c == 0x0022; // " QUOTATION MARK
    }

    /** Check if character is WhiteSpace (excluding control escapes already handled) */
    private static boolean isWhiteSpace(char c) {
        return c == '\u0020' // SPACE
                || c == '\u00A0' // NO-BREAK SPACE
                || c == '\uFEFF' // ZERO WIDTH NO-BREAK SPACE
                || c == '\u202F'; // NARROW NO-BREAK SPACE
    }

    /** Check if character is LineTerminator */
    private static boolean isLineTerminator(char c) {
        return c == '\u2028' // LINE SEPARATOR
                || c == '\u2029'; // PARAGRAPH SEPARATOR
    }
}
