/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import org.mozilla.javascript.BaseFunction;
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

    public static LambdaConstructor init(Context cx, ScriptableObject scope, boolean sealed) {
        var ctor = new LambdaConstructor(
            scope,
            "Regexp",
            2,
            NativeRegExpCtor::js_constructCall,
            NativeRegExpCtor::js_construct);
        ctor.defineProperty(cx, "multiline",
            (c) -> ScriptRuntime.wrapBoolean(getImpl().multiline),
            (c, v) -> getImpl().multiline = ScriptRuntime.toBoolean(v),
            ScriptableObject.PERMANENT);
        ctor.defineProperty(cx, "$*",
            (c) -> ScriptRuntime.wrapBoolean(getImpl().multiline),
            (c, v) -> getImpl().multiline = ScriptRuntime.toBoolean(v),
            ScriptableObject.PERMANENT);
        ctor.defineProperty(cx, "input",
            (c) -> getImpl().input,
            (c, v) -> getImpl().input = ScriptRuntime.toString(v),
            ScriptableObject.PERMANENT);
        ctor.defineProperty(cx, "$_",
            (c) -> getImpl().input,
            (c, v) -> getImpl().input = ScriptRuntime.toString(v),
            ScriptableObject.PERMANENT);
        ctor.defineProperty(cx, "lastMatch",
            (c) -> getImpl().lastMatch,
            null,
            ScriptableObject.PERMANENT);        
        ctor.defineProperty(cx, "$&",
            (c) -> getImpl().lastMatch,
            null,
            ScriptableObject.PERMANENT);
        ctor.defineProperty(cx, "lastParen",
            (c) -> getImpl().lastParen,
            null,
            ScriptableObject.PERMANENT);
        ctor.defineProperty(cx, "$+",
            (c) -> getImpl().lastParen,
            null,
            ScriptableObject.PERMANENT);
        ctor.defineProperty(cx, "leftContext",
            (c) -> getImpl().leftContext,
            null,
            ScriptableObject.PERMANENT);
        ctor.defineProperty(cx, "$`",
            (c) -> getImpl().leftContext,
            null,
            ScriptableObject.PERMANENT);
        ctor.defineProperty(cx, "rightContext",
            (c) -> getImpl().rightContext,
            null,
            ScriptableObject.PERMANENT);
        ctor.defineProperty(cx, "$'",
            (c) -> getImpl().rightContext,
            null,
            ScriptableObject.PERMANENT);
        for (int i = 1; i < 10; i++) {
            int c = i - 1;
            ctor.defineProperty(cx, String.format("$%d", i), (x) -> getImpl().getParenSubString(c), null, ScriptableObject.PERMANENT);
        }
        return ctor;
    }

    private static Scriptable js_constructCall(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length > 0
                && args[0] instanceof NativeRegExp
                && (args.length == 1 || args[1] == Undefined.instance)) {
            return (Scriptable)args[0];
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
