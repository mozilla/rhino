/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

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
                ScriptableObject.PERMANENT);
        ctor.defineProperty(
                cx,
                "$*",
                (c) -> ScriptRuntime.wrapBoolean(getImpl().multiline),
                (c, v) -> getImpl().multiline = ScriptRuntime.toBoolean(v),
                ScriptableObject.PERMANENT);
        ctor.defineProperty(
                cx,
                "input",
                (c) -> {
                    var res = getImpl().input;
                    return res == null ? "" : res.toString();
                },
                (c, v) -> getImpl().input = ScriptRuntime.toString(v),
                ScriptableObject.PERMANENT);
        ctor.defineProperty(
                cx,
                "$_",
                (c) -> {
                    var res = getImpl().input;
                    return res == null ? "" : res.toString();
                },
                (c, v) -> getImpl().input = ScriptRuntime.toString(v),
                ScriptableObject.PERMANENT);
        ctor.defineProperty(
                cx,
                "lastMatch",
                (c) -> {
                    var res = getImpl().lastMatch;
                    return res == null ? "" : res.toString();
                },
                null,
                ScriptableObject.PERMANENT);
        ctor.defineProperty(
                cx,
                "$&",
                (c) -> {
                    var res = getImpl().lastMatch;
                    return res == null ? "" : res.toString();
                },
                null,
                ScriptableObject.PERMANENT);
        ctor.defineProperty(
                cx,
                "lastParen",
                (c) -> {
                    var res = getImpl().lastParen;
                    return res == null ? "" : res.toString();
                },
                null,
                ScriptableObject.PERMANENT);
        ctor.defineProperty(
                cx,
                "$+",
                (c) -> {
                    var res = getImpl().lastParen;
                    return res == null ? "" : res.toString();
                },
                null,
                ScriptableObject.PERMANENT);
        ctor.defineProperty(
                cx,
                "leftContext",
                (c) -> {
                    var res = getImpl().leftContext;
                    return res == null ? "" : res.toString();
                },
                null,
                ScriptableObject.PERMANENT);
        ctor.defineProperty(
                cx,
                "$`",
                (c) -> {
                    var res = getImpl().leftContext;
                    return res == null ? "" : res.toString();
                },
                null,
                ScriptableObject.PERMANENT);
        ctor.defineProperty(
                cx,
                "rightContext",
                (c) -> {
                    var res = getImpl().rightContext;
                    return res == null ? "" : res.toString();
                },
                null,
                ScriptableObject.PERMANENT);
        ctor.defineProperty(
                cx,
                "$'",
                (c) -> {
                    var res = getImpl().rightContext;
                    return res == null ? "" : res.toString();
                },
                null,
                ScriptableObject.PERMANENT);
        for (int i = 1; i < 10; i++) {
            int c = i - 1;
            ctor.defineProperty(
                    cx,
                    String.format("$%d", i),
                    (x) -> {
                        var res = getImpl().getParenSubString(c);
                        return res == null ? "" : res.toString();
                    },
                    null,
                    ScriptableObject.PERMANENT);
        }
        return ctor;
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
}
