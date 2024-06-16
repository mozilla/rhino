/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
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
class NativeRegExpCtor extends BaseFunction {
    private static final long serialVersionUID = -5733330028285400526L;

    NativeRegExpCtor() {}

    @Override
    public String getFunctionName() {
        return "RegExp";
    }

    @Override
    public int getLength() {
        return 2;
    }

    @Override
    public int getArity() {
        return 2;
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length > 0
                && args[0] instanceof NativeRegExp
                && (args.length == 1 || args[1] == Undefined.instance)) {
            return args[0];
        }
        return construct(cx, scope, args);
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        NativeRegExp re = NativeRegExpInstantiator.withLanguageVersion(cx.getLanguageVersion());
        re.compile(cx, scope, args);
        ScriptRuntime.setBuiltinProtoAndParent(re, scope, TopLevel.Builtins.RegExp);
        return re;
    }

    private static RegExpImpl getImpl() {
        Context cx = Context.getCurrentContext();
        return (RegExpImpl) ScriptRuntime.getRegExpProxy(cx);
    }

    private static final int Id_multiline = 1,
            Id_STAR = 2,
            Id_input = 3,
            Id_UNDERSCORE = 4,
            Id_lastMatch = 5,
            Id_AMPERSAND = 6,
            Id_lastParen = 7,
            Id_PLUS = 8,
            Id_leftContext = 9,
            Id_BACK_QUOTE = 10,
            Id_rightContext = 11,
            Id_QUOTE = 12,
            DOLLAR_ID_BASE = 12;
    private static final int Id_DOLLAR_1 = DOLLAR_ID_BASE + 1,
            Id_DOLLAR_2 = DOLLAR_ID_BASE + 2,
            Id_DOLLAR_3 = DOLLAR_ID_BASE + 3,
            Id_DOLLAR_4 = DOLLAR_ID_BASE + 4,
            Id_DOLLAR_5 = DOLLAR_ID_BASE + 5,
            Id_DOLLAR_6 = DOLLAR_ID_BASE + 6,
            Id_DOLLAR_7 = DOLLAR_ID_BASE + 7,
            Id_DOLLAR_8 = DOLLAR_ID_BASE + 8,
            Id_DOLLAR_9 = DOLLAR_ID_BASE + 9,
            MAX_INSTANCE_ID = DOLLAR_ID_BASE + 9;

    @Override
    protected int getMaxInstanceId() {
        return super.getMaxInstanceId() + MAX_INSTANCE_ID;
    }

    @Override
    protected int findInstanceIdInfo(String s) {
        int id;
        switch (s) {
            case "multiline":
                id = Id_multiline;
                break;
            case "$*":
                id = Id_STAR;
                break;
            case "input":
                id = Id_input;
                break;
            case "$_":
                id = Id_UNDERSCORE;
                break;
            case "lastMatch":
                id = Id_lastMatch;
                break;
            case "$&":
                id = Id_AMPERSAND;
                break;
            case "lastParen":
                id = Id_lastParen;
                break;
            case "$+":
                id = Id_PLUS;
                break;
            case "leftContext":
                id = Id_leftContext;
                break;
            case "$`":
                id = Id_BACK_QUOTE;
                break;
            case "rightContext":
                id = Id_rightContext;
                break;
            case "$'":
                id = Id_QUOTE;
                break;
            case "$1":
                id = Id_DOLLAR_1;
                break;
            case "$2":
                id = Id_DOLLAR_2;
                break;
            case "$3":
                id = Id_DOLLAR_3;
                break;
            case "$4":
                id = Id_DOLLAR_4;
                break;
            case "$5":
                id = Id_DOLLAR_5;
                break;
            case "$6":
                id = Id_DOLLAR_6;
                break;
            case "$7":
                id = Id_DOLLAR_7;
                break;
            case "$8":
                id = Id_DOLLAR_8;
                break;
            case "$9":
                id = Id_DOLLAR_9;
                break;
            default:
                id = 0;
                break;
        }

        if (id == 0) return super.findInstanceIdInfo(s);

        int attr;
        switch (id) {
            case Id_multiline:
                attr = multilineAttr;
                break;
            case Id_STAR:
                attr = starAttr;
                break;
            case Id_input:
                attr = inputAttr;
                break;
            case Id_UNDERSCORE:
                attr = underscoreAttr;
                break;
            default:
                attr = PERMANENT | READONLY;
                break;
        }

        return instanceIdInfo(attr, super.getMaxInstanceId() + id);
    }

    @Override
    protected String getInstanceIdName(int id) {
        int shifted = id - super.getMaxInstanceId();
        if (1 <= shifted && shifted <= MAX_INSTANCE_ID) {
            switch (shifted) {
                case Id_multiline:
                    return "multiline";
                case Id_STAR:
                    return "$*";

                case Id_input:
                    return "input";
                case Id_UNDERSCORE:
                    return "$_";

                case Id_lastMatch:
                    return "lastMatch";
                case Id_AMPERSAND:
                    return "$&";

                case Id_lastParen:
                    return "lastParen";
                case Id_PLUS:
                    return "$+";

                case Id_leftContext:
                    return "leftContext";
                case Id_BACK_QUOTE:
                    return "$`";

                case Id_rightContext:
                    return "rightContext";
                case Id_QUOTE:
                    return "$'";
            }
            // Must be one of $1..$9, convert to 0..8
            int substring_number = shifted - DOLLAR_ID_BASE - 1;
            char[] buf = {'$', (char) ('1' + substring_number)};
            return new String(buf);
        }
        return super.getInstanceIdName(id);
    }

    @Override
    protected Object getInstanceIdValue(int id) {
        int shifted = id - super.getMaxInstanceId();
        if (1 <= shifted && shifted <= MAX_INSTANCE_ID) {
            RegExpImpl impl = getImpl();
            Object stringResult;
            switch (shifted) {
                case Id_multiline:
                case Id_STAR:
                    return ScriptRuntime.wrapBoolean(impl.multiline);

                case Id_input:
                case Id_UNDERSCORE:
                    stringResult = impl.input;
                    break;

                case Id_lastMatch:
                case Id_AMPERSAND:
                    stringResult = impl.lastMatch;
                    break;

                case Id_lastParen:
                case Id_PLUS:
                    stringResult = impl.lastParen;
                    break;

                case Id_leftContext:
                case Id_BACK_QUOTE:
                    stringResult = impl.leftContext;
                    break;

                case Id_rightContext:
                case Id_QUOTE:
                    stringResult = impl.rightContext;
                    break;

                default:
                    {
                        // Must be one of $1..$9, convert to 0..8
                        int substring_number = shifted - DOLLAR_ID_BASE - 1;
                        stringResult = impl.getParenSubString(substring_number);
                        break;
                    }
            }
            return (stringResult == null) ? "" : stringResult.toString();
        }
        return super.getInstanceIdValue(id);
    }

    @Override
    protected void setInstanceIdValue(int id, Object value) {
        int shifted = id - super.getMaxInstanceId();
        switch (shifted) {
            case Id_multiline:
            case Id_STAR:
                getImpl().multiline = ScriptRuntime.toBoolean(value);
                return;

            case Id_input:
            case Id_UNDERSCORE:
                getImpl().input = ScriptRuntime.toString(value);
                return;

            case Id_lastMatch:
            case Id_AMPERSAND:
            case Id_lastParen:
            case Id_PLUS:
            case Id_leftContext:
            case Id_BACK_QUOTE:
            case Id_rightContext:
            case Id_QUOTE:
                return;
            default:
                int substring_number = shifted - DOLLAR_ID_BASE - 1;
                if (0 <= substring_number && substring_number <= 8) {
                    return;
                }
        }
        super.setInstanceIdValue(id, value);
    }

    @Override
    protected void setInstanceIdAttributes(int id, int attr) {
        int shifted = id - super.getMaxInstanceId();
        switch (shifted) {
            case Id_multiline:
                multilineAttr = attr;
                return;
            case Id_STAR:
                starAttr = attr;
                return;
            case Id_input:
                inputAttr = attr;
                return;
            case Id_UNDERSCORE:
                underscoreAttr = attr;
                return;

            case Id_lastMatch:
            case Id_AMPERSAND:
            case Id_lastParen:
            case Id_PLUS:
            case Id_leftContext:
            case Id_BACK_QUOTE:
            case Id_rightContext:
            case Id_QUOTE:
                // non-configurable + non-writable
                return;
            default:
                int substring_number = shifted - DOLLAR_ID_BASE - 1;
                if (0 <= substring_number && substring_number <= 8) {
                    // non-configurable + non-writable
                    return;
                }
        }
        super.setInstanceIdAttributes(id, attr);
    }

    private int multilineAttr = PERMANENT;
    private int starAttr = PERMANENT;
    private int inputAttr = PERMANENT;
    private int underscoreAttr = PERMANENT;
}
