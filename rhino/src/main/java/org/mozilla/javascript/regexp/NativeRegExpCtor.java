/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.ScriptableObject.PERMANENT;

import org.mozilla.javascript.ClassDescriptor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
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

    static ClassDescriptor.Builder makeCtorBuilder() {
        var builder =
                new ClassDescriptor.Builder(
                                "RegExp",
                                2,
                                NativeRegExpCtor::js_constructCall,
                                NativeRegExpCtor::js_construct)
                        .withProp(
                                CTOR,
                                "multiline",
                                (c) -> ScriptRuntime.wrapBoolean(getImpl().multiline),
                                (c, v) -> getImpl().multiline = ScriptRuntime.toBoolean(v),
                                PERMANENT)
                        .withProp(
                                CTOR,
                                "$*",
                                (c) -> ScriptRuntime.wrapBoolean(getImpl().multiline),
                                (c, v) -> getImpl().multiline = ScriptRuntime.toBoolean(v),
                                PERMANENT)
                        .withProp(
                                CTOR,
                                "input",
                                (c) -> toStr(getImpl().input),
                                (c, v) -> getImpl().input = ScriptRuntime.toString(v),
                                PERMANENT)
                        .withProp(
                                CTOR,
                                "$_",
                                (c) -> toStr(getImpl().input),
                                (c, v) -> getImpl().input = ScriptRuntime.toString(v),
                                PERMANENT)
                        .withProp(
                                CTOR,
                                "lastMatch",
                                (c) -> toStr(getImpl().lastMatch),
                                null,
                                PERMANENT)
                        .withProp(CTOR, "$&", (c) -> toStr(getImpl().lastMatch), null, PERMANENT)
                        .withProp(
                                CTOR,
                                "lastParen",
                                (c) -> toStr(getImpl().lastParen),
                                null,
                                PERMANENT)
                        .withProp(CTOR, "$+", (c) -> toStr(getImpl().lastParen), null, PERMANENT)
                        .withProp(
                                CTOR,
                                "leftContext",
                                (c) -> toStr(getImpl().leftContext),
                                null,
                                PERMANENT)
                        .withProp(CTOR, "$`", (c) -> toStr(getImpl().leftContext), null, PERMANENT)
                        .withProp(
                                CTOR,
                                "rightContext",
                                (c) -> toStr(getImpl().rightContext),
                                null,
                                PERMANENT)
                        .withProp(
                                CTOR, "$'", (c) -> toStr(getImpl().rightContext), null, PERMANENT);

        for (int i = 1; i < 10; i++) {
            int c = i - 1;
            builder.withProp(
                    CTOR,
                    String.format("$%d", i),
                    (x) -> toStr(getImpl().getParenSubString(c)),
                    null,
                    PERMANENT);
        }
        return builder;
    }

    private static String toStr(String subStr) {
        return subStr == null ? "" : subStr;
    }

    private static String toStr(SubString subStr) {
        return subStr == null ? "" : subStr.toString();
    }

    private static Scriptable js_constructCall(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        if (args.length > 0
                && args[0] instanceof NativeRegExp
                && (args.length == 1 || args[1] == Undefined.instance)) {
            return (Scriptable) args[0];
        }
        return js_construct(cx, f, nt, s, thisObj, args);
    }

    private static Scriptable js_construct(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        NativeRegExp re = NativeRegExpInstantiator.withLanguageVersion(cx.getLanguageVersion());
        re.compile(cx, s, args);
        ScriptRuntime.setBuiltinProtoAndParent(re, s, TopLevel.Builtins.RegExp);
        return re;
    }

    private static RegExpImpl getImpl() {
        Context cx = Context.getCurrentContext();
        return (RegExpImpl) ScriptRuntime.getRegExpProxy(cx);
    }
}
