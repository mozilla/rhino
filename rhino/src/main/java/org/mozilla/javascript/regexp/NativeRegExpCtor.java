/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import static org.mozilla.javascript.ScriptableObject.PERMANENT;

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
 * <p>See ECMA 262 §22.2.
 *
 * <p>Implementation in C by Brendan Eich. Port to Java by Norris Boyd from jsregexp.c version 1.36.
 * Merged up to version 1.38 with Unicode support. Merged bug fixes in version 1.39 and
 * JSFUN13_BRANCH changes up to 1.32.2.11. ES2025 RegExp.escape() added.
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

        // ES2025: RegExp.escape(S)
        ctor.defineConstructorMethod(
                scope,
                "escape",
                1,
                (thisCx, thisScope, thisObj, args) -> {
                    if (args.length == 0) {
                        return js_escape("undefined");
                    }
                    String str = ScriptRuntime.toString(args[0]);
                    return js_escape(str);
                },
                PERMANENT,
                PERMANENT);

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
     * ES2025 §22.2.4.3 RegExp.escape(S). Escapes RegExp syntax characters for safe use in patterns.
     *
     * @param str The string to escape
     * @return Escaped string safe for use in RegExp pattern
     */
    private static String js_escape(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder(str.length() * 2);

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            // ES2025: Escape syntax characters
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
                    result.append('\\');
                    result.append(c);
                    break;
                default:
                    result.append(c);
                    break;
            }
        }

        return result.toString();
    }
}
