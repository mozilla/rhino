/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import static org.mozilla.javascript.ScriptableObject.PERMANENT;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.JSScope;
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

    public static LambdaConstructor init(Context cx, JSScope scopeArg, boolean sealed) {
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
        return ctor;
    }

    private static String toStr(String subStr) {
        return subStr == null ? "" : subStr;
    }

    private static String toStr(SubString subStr) {
        return subStr == null ? "" : subStr.toString();
    }

    private static Scriptable js_constructCall(
            Context cx, JSScope scope, Object thisObj, Object[] args) {
        if (args.length > 0
                && args[0] instanceof NativeRegExp
                && (args.length == 1 || args[1] == Undefined.instance)) {
            return (Scriptable) args[0];
        }
        return js_construct(cx, scope, args);
    }

    private static Scriptable js_construct(Context cx, JSScope scope, Object[] args) {
        NativeRegExp re = NativeRegExpInstantiator.withLanguageVersion(cx.getLanguageVersion());
        re.compile(cx, scope, args);
        ScriptRuntime.setBuiltinProtoAndParent(re, scope, TopLevel.Builtins.RegExp);
        return re;
    }

    private static RegExpImpl getImpl() {
        Context cx = Context.getCurrentContext();
        return (RegExpImpl) ScriptRuntime.getRegExpProxy(cx);
    }
}
