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
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Tom Beauvais
 *   Norris Boyd
 *   Mike McCabe
 *   Cameron McCormack
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

package org.mozilla.javascript;

import java.text.Collator;

/**
 * This class implements the String native object.
 *
 * See ECMA 15.5.
 *
 * String methods for dealing with regular expressions are
 * ported directly from C. Latest port is from version 1.40.12.19
 * in the JSFUN13_BRANCH.
 *
 * @author Mike McCabe
 * @author Norris Boyd
 */
final class NativeString extends ScriptableObject implements IdFunctionCall
{
    static final long serialVersionUID = 920268368584188687L;

    private static final Object STRING_TAG = "String";

    static void init(Scriptable scope, boolean sealed)
    {
        NativeString proto = new NativeString("");
        proto.setParentScope(scope);
        proto.setPrototype(getObjectPrototype(scope));
        IdFunctionObject ctor = null;
        for (Methods method : Methods.values()) {
            IdFunctionObject idfun = new IdFunctionObject(proto, method,
                    0, method.name(), method.arity, scope);
            idfun.addAsProperty(proto);
            if (method == Methods.constructor) {
                ctor = idfun;
                ctor.initFunction(proto.getClassName(), scope);
                ctor.markAsConstructor(proto);
                ctor.exportAsScopeProperty();
            }
        }
        for (StaticMethods method : StaticMethods.values()) {
            IdFunctionObject idfun = new IdFunctionObject(proto, method,
                    0, method.name(),  method.arity, scope);
            idfun.addAsProperty(ctor);
        }
        if (sealed) {
            proto.sealObject();
            ctor.sealObject();
        }
    }

    NativeString(CharSequence s) {
        string = s;
    }

    @Override
    public String getClassName() {
        return "String";
    }

    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        Object tag = f.getTag();
        Methods method = null;

        if (tag instanceof Methods) {
            method = (Methods) tag;
        } else if (tag instanceof StaticMethods) {
            StaticMethods staticMethod = (StaticMethods) tag;
            switch ((StaticMethods) tag) {
              case charAt:
              case charCodeAt:
              case indexOf:
              case lastIndexOf:
              case split:
              case substring:
              case toLowerCase:
              case toUpperCase:
              case substr:
              case concat:
              case slice:
              case equalsIgnoreCase:
              case match:
              case search:
              case replace:
              case localeCompare:
              case toLocaleLowerCase: {
                if (args.length > 0) {
                    thisObj = ScriptRuntime.toObject(scope,
                            ScriptRuntime.toCharSequence(args[0]));
                    Object[] newArgs = new Object[args.length-1];
                    for (int i=0; i < newArgs.length; i++)
                        newArgs[i] = args[i+1];
                    args = newArgs;
                } else {
                    thisObj = ScriptRuntime.toObject(scope,
                            ScriptRuntime.toCharSequence(thisObj));
                }
                method = staticMethod.instanceMethod;
                break;
              }

              case fromCharCode: {
                int N = args.length;
                if (N < 1)
                    return "";
                StringBuilder sb = new StringBuilder(N);
                for (int i = 0; i != N; ++i) {
                    sb.append(ScriptRuntime.toUint16(args[i]));
                }
                return sb.toString();
              }
            }
        }

        if (method != null) {
            switch (method) {

              case constructor: {
                CharSequence s = (args.length >= 1)
                    ? ScriptRuntime.toCharSequence(args[0]) : "";
                if (thisObj == null) {
                    // new String(val) creates a new String object.
                    return new NativeString(s);
                }
                // String(val) converts val to a string value.
                return s instanceof String ? s : s.toString();
              }

              case toString:
              case valueOf:
                // ECMA 15.5.4.2: 'the toString function is not generic.
                CharSequence cs = realThis(thisObj, f).string;
                return cs instanceof String ? cs : cs.toString();

              case toSource: {
                CharSequence s = realThis(thisObj, f).string;
                return "(new String(\""+ScriptRuntime.escapeString(s.toString())+"\"))";
              }

              case charAt:
              case charCodeAt: {
                 // See ECMA 15.5.4.[4,5]
                CharSequence target = ScriptRuntime.toCharSequence(thisObj);
                double pos = ScriptRuntime.toInteger(args, 0);
                if (pos < 0 || pos >= target.length()) {
                    if (method == Methods.charAt) return "";
                    else return ScriptRuntime.NaNobj;
                }
                char c = target.charAt((int)pos);
                if (method == Methods.charAt) return String.valueOf(c);
                else return ScriptRuntime.wrapInt(c);
              }

              case indexOf:
                return ScriptRuntime.wrapInt(js_indexOf(
                    ScriptRuntime.toString(thisObj), args));

              case lastIndexOf:
                return ScriptRuntime.wrapInt(js_lastIndexOf(
                    ScriptRuntime.toString(thisObj), args));

              case split:
                return ScriptRuntime.checkRegExpProxy(cx).
                  js_split(cx, scope, ScriptRuntime.toString(thisObj),
                        args);

              case substring:
                return js_substring(cx, ScriptRuntime.toCharSequence(thisObj), args);

              case toLowerCase:
                // See ECMA 15.5.4.11
                return ScriptRuntime.toString(thisObj).toLowerCase(
                         ScriptRuntime.ROOT_LOCALE);

              case toUpperCase:
                // See ECMA 15.5.4.12
                return ScriptRuntime.toString(thisObj).toUpperCase(
                         ScriptRuntime.ROOT_LOCALE);

              case substr:
                return js_substr(ScriptRuntime.toCharSequence(thisObj), args);

              case concat:
                return js_concat(ScriptRuntime.toString(thisObj), args);

              case slice:
                return js_slice(ScriptRuntime.toCharSequence(thisObj), args);

              case bold:
                return tagify(thisObj, "b", null, null);

              case italics:
                return tagify(thisObj, "i", null, null);

              case fixed:
                return tagify(thisObj, "tt", null, null);

              case strike:
                return tagify(thisObj, "strike", null, null);

              case small:
                return tagify(thisObj, "small", null, null);

              case big:
                return tagify(thisObj, "big", null, null);

              case blink:
                return tagify(thisObj, "blink", null, null);

              case sup:
                return tagify(thisObj, "sup", null, null);

              case sub:
                return tagify(thisObj, "sub", null, null);

              case fontsize:
                return tagify(thisObj, "font", "size", args);

              case fontcolor:
                return tagify(thisObj, "font", "color", args);

              case link:
                return tagify(thisObj, "a", "href", args);

              case anchor:
                return tagify(thisObj, "a", "name", args);

              case equals:
              case equalsIgnoreCase: {
                String s1 = ScriptRuntime.toString(thisObj);
                String s2 = ScriptRuntime.toString(args, 0);
                return ScriptRuntime.wrapBoolean(
                    (method == Methods.equals) ? s1.equals(s2)
                                      : s1.equalsIgnoreCase(s2));
              }

              case match:
              case search:
              case replace:
                {
                    int actionType;
                    if (method == Methods.match) {
                        actionType = RegExpProxy.RA_MATCH;
                    } else if (method == Methods.search) {
                        actionType = RegExpProxy.RA_SEARCH;
                    } else {
                        actionType = RegExpProxy.RA_REPLACE;
                    }
                    return ScriptRuntime.checkRegExpProxy(cx).
                        action(cx, scope, thisObj, args, actionType);
                }
                // ECMA-262 1 5.5.4.9
              case localeCompare:
                {
                    // For now, create and configure a collator instance. I can't
                    // actually imagine that this'd be slower than caching them
                    // a la ClassCache, so we aren't trying to outsmart ourselves
                    // with a caching mechanism for now.
                    Collator collator = Collator.getInstance(cx.getLocale());
                    collator.setStrength(Collator.IDENTICAL);
                    collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
                    return ScriptRuntime.wrapNumber(collator.compare(
                            ScriptRuntime.toString(thisObj),
                            ScriptRuntime.toString(args, 0)));
                }
              case toLocaleLowerCase:
                {
                    return ScriptRuntime.toString(thisObj)
                            .toLowerCase(cx.getLocale());
                }
              case toLocaleUpperCase:
                {
                    return ScriptRuntime.toString(thisObj)
                            .toUpperCase(cx.getLocale());
                }
              case trim:
                {
                    String str = ScriptRuntime.toString(thisObj);
                    char[] chars = str.toCharArray();

                    int start = 0;
                    while (start < chars.length && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[start])) {
                      start++;
                    }
                    int end = chars.length;
                    while (end > start && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[end-1])) {
                      end--;
                    }

                    return str.substring(start, end);
                }
            }
        }
        return null;
    }

    private static NativeString realThis(Scriptable thisObj, IdFunctionObject f)
    {
        if (!(thisObj instanceof NativeString))
            throw IdScriptableObject.incompatibleCallError(f);
        return (NativeString)thisObj;
    }

    /*
     * HTML composition aids.
     */
    private static String tagify(Object thisObj, String tag,
                                 String attribute, Object[] args)
    {
        String str = ScriptRuntime.toString(thisObj);
        StringBuilder result = new StringBuilder();
        result.append('<');
        result.append(tag);
        if (attribute != null) {
            result.append(' ');
            result.append(attribute);
            result.append("=\"");
            result.append(ScriptRuntime.toString(args, 0));
            result.append('"');
        }
        result.append('>');
        result.append(str);
        result.append("</");
        result.append(tag);
        result.append('>');
        return result.toString();
    }

    public CharSequence toCharSequence() {
        return string;
    }

    @Override
    public String toString() {
        return string instanceof String ? (String)string : string.toString();
    }

    @Override
    public Object get(String name, Scriptable start) {
        if ("length".equals(name)) {
            return ScriptRuntime.wrapInt(string.length());
        }
        return super.get(name, start);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return "length".equals(name) || super.has(name, start);
    }

    /* Make array-style property lookup work for strings.
    * XXX is this ECMA?  A version check is probably needed. In js too.
    */
    @Override
    public Object get(int index, Scriptable start) {
        if (0 <= index && index < string.length()) {
            return String.valueOf(string.charAt(index));
        }
        return super.get(index, start);
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        if (0 <= index && index < string.length()) {
            return;
        }
        super.put(index, start, value);
    }

    @Override
    public int getAttributes(String id) {
        if ("length".equals(id)) {
            return READONLY | DONTENUM | PERMANENT;
        }
        return super.getAttributes(id);
    }

    @Override
    protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
        if ("length".equals(id)) {
            return buildDataDescriptor(getParentScope(),
                    ScriptRuntime.wrapInt(string.length()),
                    READONLY | DONTENUM | PERMANENT);
        }
        return super.getOwnPropertyDescriptor(cx, id);
    }

    /*
     *
     * See ECMA 15.5.4.6.  Uses Java String.indexOf()
     * OPT to add - BMH searching from jsstr.c.
     */
    private static int js_indexOf(String target, Object[] args) {
        String search = ScriptRuntime.toString(args, 0);
        double begin = ScriptRuntime.toInteger(args, 1);

        if (begin > target.length()) {
            return -1;
        } else {
            if (begin < 0)
                begin = 0;
            return target.indexOf(search, (int)begin);
        }
    }

    /*
     *
     * See ECMA 15.5.4.7
     *
     */
    private static int js_lastIndexOf(String target, Object[] args) {
        String search = ScriptRuntime.toString(args, 0);
        double end = ScriptRuntime.toNumber(args, 1);

        if (end != end || end > target.length())
            end = target.length();
        else if (end < 0)
            end = 0;

        return target.lastIndexOf(search, (int)end);
    }


    /*
     * See ECMA 15.5.4.15
     */
    private static CharSequence js_substring(Context cx, CharSequence target,
                                       Object[] args)
    {
        int length = target.length();
        double start = ScriptRuntime.toInteger(args, 0);
        double end;

        if (start < 0)
            start = 0;
        else if (start > length)
            start = length;

        if (args.length <= 1 || args[1] == Undefined.instance) {
            end = length;
        } else {
            end = ScriptRuntime.toInteger(args[1]);
            if (end < 0)
                end = 0;
            else if (end > length)
                end = length;

            // swap if end < start
            if (end < start) {
                if (cx.getLanguageVersion() != Context.VERSION_1_2) {
                    double temp = start;
                    start = end;
                    end = temp;
                } else {
                    // Emulate old JDK1.0 java.lang.String.substring()
                    end = start;
                }
            }
        }
        return target.subSequence((int)start, (int)end);
    }

    int getLength() {
        return string.length();
    }

    /*
     * Non-ECMA methods.
     */
    private static CharSequence js_substr(CharSequence target, Object[] args) {
        if (args.length < 1)
            return target;

        double begin = ScriptRuntime.toInteger(args[0]);
        double end;
        int length = target.length();

        if (begin < 0) {
            begin += length;
            if (begin < 0)
                begin = 0;
        } else if (begin > length) {
            begin = length;
        }

        if (args.length == 1) {
            end = length;
        } else {
            end = ScriptRuntime.toInteger(args[1]);
            if (end < 0)
                end = 0;
            end += begin;
            if (end > length)
                end = length;
        }

        return target.subSequence((int)begin, (int)end);
    }

    /*
     * Python-esque sequence operations.
     */
    private static String js_concat(String target, Object[] args) {
        int N = args.length;
        if (N == 0) { return target; }
        else if (N == 1) {
            String arg = ScriptRuntime.toString(args[0]);
            return target.concat(arg);
        }

        // Find total capacity for the final string to avoid unnecessary
        // re-allocations in StringBuilder
        int size = target.length();
        String[] argsAsStrings = new String[N];
        for (int i = 0; i != N; ++i) {
            String s = ScriptRuntime.toString(args[i]);
            argsAsStrings[i] = s;
            size += s.length();
        }

        StringBuilder result = new StringBuilder(size);
        result.append(target);
        for (int i = 0; i != N; ++i) {
            result.append(argsAsStrings[i]);
        }
        return result.toString();
    }

    private static CharSequence js_slice(CharSequence target, Object[] args) {
        if (args.length != 0) {
            double begin = ScriptRuntime.toInteger(args[0]);
            double end;
            int length = target.length();
            if (begin < 0) {
                begin += length;
                if (begin < 0)
                    begin = 0;
            } else if (begin > length) {
                begin = length;
            }

            if (args.length == 1) {
                end = length;
            } else {
                end = ScriptRuntime.toInteger(args[1]);
                if (end < 0) {
                    end += length;
                    if (end < 0)
                        end = 0;
                } else if (end > length) {
                    end = length;
                }
                if (end < begin)
                    end = begin;
            }
            return target.subSequence((int) begin, (int) end);
        }
        return target;
    }

    enum Methods {
        constructor(1),
        toString(0),
        toSource(0),
        valueOf(0),
        charAt(1),
        charCodeAt(1),
        indexOf(1),
        lastIndexOf(1),
        split(2),
        substring(2),
        toLowerCase(0),
        toUpperCase(0),
        substr(2),
        concat(1),
        slice(2),
        bold(0),
        italics(0),
        fixed(0),
        strike(0),
        small(0),
        big(0),
        blink(0),
        sup(0),
        sub(0),
        fontsize(0),
        fontcolor(0),
        link(0),
        anchor(0),
        equals(1),
        equalsIgnoreCase(1),
        match(1),
        search(1),
        replace(1),
        localeCompare(1),
        toLocaleLowerCase(0),
        toLocaleUpperCase(0),
        trim(0);

        private final int arity;
        Methods(int arity) {
            this.arity = arity;
        }
    }

    enum StaticMethods {
        fromCharCode(1, false),
        charAt(2, true),
        charCodeAt(2, true),
        indexOf(2, true),
        lastIndexOf(2, true),
        split(3, true),
        substring(3, true),
        toLowerCase(1, true),
        toUpperCase(1, true),
        substr(3, true),
        concat(2, true),
        slice(3, true),
        equalsIgnoreCase(2, true),
        match(2, true),
        search(2, true),
        replace(2, true),
        localeCompare(2, true),
        toLocaleLowerCase(1, true);

        private final int arity;
        private final Methods instanceMethod;
        StaticMethods(int arity, boolean callsInstance) {
            this.arity = arity;
            this.instanceMethod = callsInstance ? Methods.valueOf(name()) : null;
        }
    }

    private CharSequence string;
}

