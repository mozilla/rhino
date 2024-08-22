/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ScriptRuntime.rangeError;
import static org.mozilla.javascript.ScriptRuntimeES6.requireObjectCoercible;

import java.text.Collator;
import java.text.Normalizer;
import java.util.Locale;
import org.mozilla.javascript.ScriptRuntime.StringIdOrIndex;
import org.mozilla.javascript.regexp.NativeRegExp;

/**
 * This class implements the String native object.
 *
 * <p>See ECMA 15.5.
 *
 * <p>String methods for dealing with regular expressions are ported directly from C. Latest port is
 * from version 1.40.12.19 in the JSFUN13_BRANCH.
 *
 * @author Mike McCabe
 * @author Norris Boyd
 * @author Ronald Brill
 */
final class NativeString extends IdScriptableObject {
    private static final long serialVersionUID = 920268368584188687L;

    private static final Object STRING_TAG = "String";

    static void init(Scriptable scope, boolean sealed) {
        NativeString obj = new NativeString("");
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    NativeString(CharSequence s) {
        string = s;
    }

    @Override
    public String getClassName() {
        return "String";
    }

    private static final int Id_length = 1, MAX_INSTANCE_ID = 1;

    @Override
    protected int getMaxInstanceId() {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected int findInstanceIdInfo(String s) {
        if (s.equals("length")) {
            return instanceIdInfo(DONTENUM | READONLY | PERMANENT, Id_length);
        }
        return super.findInstanceIdInfo(s);
    }

    @Override
    protected String getInstanceIdName(int id) {
        if (id == Id_length) {
            return "length";
        }
        return super.getInstanceIdName(id);
    }

    @Override
    protected Object getInstanceIdValue(int id) {
        if (id == Id_length) {
            return ScriptRuntime.wrapInt(string.length());
        }
        return super.getInstanceIdValue(id);
    }

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor) {
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_fromCharCode, "fromCharCode", 1);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_fromCodePoint, "fromCodePoint", 1);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_raw, "raw", 1);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_charAt, "charAt", 2);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_charCodeAt, "charCodeAt", 2);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_indexOf, "indexOf", 2);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_lastIndexOf, "lastIndexOf", 2);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_split, "split", 3);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_substring, "substring", 3);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_toLowerCase, "toLowerCase", 1);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_toUpperCase, "toUpperCase", 1);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_substr, "substr", 3);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_concat, "concat", 2);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_slice, "slice", 3);
        addIdFunctionProperty(
                ctor, STRING_TAG, ConstructorId_equalsIgnoreCase, "equalsIgnoreCase", 2);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_match, "match", 2);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_search, "search", 2);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_replace, "replace", 2);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_replaceAll, "replaceAll", 2);
        addIdFunctionProperty(ctor, STRING_TAG, ConstructorId_localeCompare, "localeCompare", 2);
        addIdFunctionProperty(
                ctor, STRING_TAG, ConstructorId_toLocaleLowerCase, "toLocaleLowerCase", 1);
        super.fillConstructorProperties(ctor);
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id == SymbolId_iterator) {
            initPrototypeMethod(STRING_TAG, id, SymbolKey.ITERATOR, "[Symbol.iterator]", 0);
            return;
        }

        String s, fnName = null;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 1;
                s = "constructor";
                break;
            case Id_toString:
                arity = 0;
                s = "toString";
                break;
            case Id_toSource:
                arity = 0;
                s = "toSource";
                break;
            case Id_valueOf:
                arity = 0;
                s = "valueOf";
                break;
            case Id_charAt:
                arity = 1;
                s = "charAt";
                break;
            case Id_charCodeAt:
                arity = 1;
                s = "charCodeAt";
                break;
            case Id_indexOf:
                arity = 1;
                s = "indexOf";
                break;
            case Id_lastIndexOf:
                arity = 1;
                s = "lastIndexOf";
                break;
            case Id_split:
                arity = 2;
                s = "split";
                break;
            case Id_substring:
                arity = 2;
                s = "substring";
                break;
            case Id_toLowerCase:
                arity = 0;
                s = "toLowerCase";
                break;
            case Id_toUpperCase:
                arity = 0;
                s = "toUpperCase";
                break;
            case Id_substr:
                arity = 2;
                s = "substr";
                break;
            case Id_concat:
                arity = 1;
                s = "concat";
                break;
            case Id_slice:
                arity = 2;
                s = "slice";
                break;
            case Id_bold:
                arity = 0;
                s = "bold";
                break;
            case Id_italics:
                arity = 0;
                s = "italics";
                break;
            case Id_fixed:
                arity = 0;
                s = "fixed";
                break;
            case Id_strike:
                arity = 0;
                s = "strike";
                break;
            case Id_small:
                arity = 0;
                s = "small";
                break;
            case Id_big:
                arity = 0;
                s = "big";
                break;
            case Id_blink:
                arity = 0;
                s = "blink";
                break;
            case Id_sup:
                arity = 0;
                s = "sup";
                break;
            case Id_sub:
                arity = 0;
                s = "sub";
                break;
            case Id_fontsize:
                arity = 0;
                s = "fontsize";
                break;
            case Id_fontcolor:
                arity = 0;
                s = "fontcolor";
                break;
            case Id_link:
                arity = 0;
                s = "link";
                break;
            case Id_anchor:
                arity = 0;
                s = "anchor";
                break;
            case Id_equals:
                arity = 1;
                s = "equals";
                break;
            case Id_equalsIgnoreCase:
                arity = 1;
                s = "equalsIgnoreCase";
                break;
            case Id_match:
                arity = 1;
                s = "match";
                break;
            case Id_search:
                arity = 1;
                s = "search";
                break;
            case Id_replace:
                arity = 2;
                s = "replace";
                break;
            case Id_replaceAll:
                arity = 2;
                s = "replaceAll";
                break;
            case Id_at:
                arity = 1;
                s = "at";
                break;
            case Id_localeCompare:
                arity = 1;
                s = "localeCompare";
                break;
            case Id_toLocaleLowerCase:
                arity = 0;
                s = "toLocaleLowerCase";
                break;
            case Id_toLocaleUpperCase:
                arity = 0;
                s = "toLocaleUpperCase";
                break;
            case Id_trim:
                arity = 0;
                s = "trim";
                break;
            case Id_trimLeft:
                arity = 0;
                s = "trimLeft";
                break;
            case Id_trimRight:
                arity = 0;
                s = "trimRight";
                break;
            case Id_includes:
                arity = 1;
                s = "includes";
                break;
            case Id_startsWith:
                arity = 1;
                s = "startsWith";
                break;
            case Id_endsWith:
                arity = 1;
                s = "endsWith";
                break;
            case Id_normalize:
                arity = 0;
                s = "normalize";
                break;
            case Id_repeat:
                arity = 1;
                s = "repeat";
                break;
            case Id_codePointAt:
                arity = 1;
                s = "codePointAt";
                break;
            case Id_padStart:
                arity = 1;
                s = "padStart";
                break;
            case Id_padEnd:
                arity = 1;
                s = "padEnd";
                break;
            case Id_trimStart:
                arity = 0;
                s = "trimStart";
                break;
            case Id_trimEnd:
                arity = 0;
                s = "trimEnd";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(STRING_TAG, id, s, fnName, arity);
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(STRING_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        again:
        for (; ; ) {
            switch (id) {
                case ConstructorId_charAt:
                case ConstructorId_charCodeAt:
                case ConstructorId_indexOf:
                case ConstructorId_lastIndexOf:
                case ConstructorId_split:
                case ConstructorId_substring:
                case ConstructorId_toLowerCase:
                case ConstructorId_toUpperCase:
                case ConstructorId_substr:
                case ConstructorId_concat:
                case ConstructorId_slice:
                case ConstructorId_equalsIgnoreCase:
                case ConstructorId_match:
                case ConstructorId_search:
                case ConstructorId_replace:
                case ConstructorId_replaceAll:
                case ConstructorId_localeCompare:
                case ConstructorId_toLocaleLowerCase:
                    {
                        if (args.length > 0) {
                            thisObj =
                                    ScriptRuntime.toObject(
                                            cx, scope, ScriptRuntime.toCharSequence(args[0]));
                            Object[] newArgs = new Object[args.length - 1];
                            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
                            args = newArgs;
                        } else {
                            thisObj =
                                    ScriptRuntime.toObject(
                                            cx, scope, ScriptRuntime.toCharSequence(thisObj));
                        }
                        id = -id;
                        continue again;
                    }

                case ConstructorId_fromCodePoint:
                    {
                        int n = args.length;
                        if (n < 1) {
                            return "";
                        }
                        int[] codePoints = new int[n];
                        for (int i = 0; i != n; i++) {
                            Object arg = args[i];
                            int codePoint = ScriptRuntime.toInt32(arg);
                            double num = ScriptRuntime.toNumber(arg);
                            if (!ScriptRuntime.eqNumber(num, Integer.valueOf(codePoint))
                                    || !Character.isValidCodePoint(codePoint)) {
                                throw rangeError(
                                        "Invalid code point " + ScriptRuntime.toString(arg));
                            }
                            codePoints[i] = codePoint;
                        }
                        return new String(codePoints, 0, n);
                    }

                case ConstructorId_fromCharCode:
                    {
                        int n = args.length;
                        if (n < 1) {
                            return "";
                        }
                        char[] chars = new char[n];
                        for (int i = 0; i != n; ++i) {
                            chars[i] = ScriptRuntime.toUint16(args[i]);
                        }
                        return new String(chars);
                    }

                case ConstructorId_raw:
                    return js_raw(cx, scope, args);

                case Id_constructor:
                    {
                        CharSequence s;
                        if (args.length == 0) {
                            s = "";
                        } else if (ScriptRuntime.isSymbol(args[0]) && (thisObj != null)) {
                            // 19.4.3.2 et.al. Convert a symbol to a string with String() but not
                            // new String()
                            s = args[0].toString();
                        } else {
                            s = ScriptRuntime.toCharSequence(args[0]);
                        }
                        if (thisObj == null) {
                            // new String(val) creates a new String object.
                            return new NativeString(s);
                        }
                        // String(val) converts val to a string value.
                        return s instanceof String ? s : s.toString();
                    }

                case Id_toString:
                case Id_valueOf:
                    // ECMA 15.5.4.2: 'the toString function is not generic.
                    CharSequence cs = realThis(thisObj, f).string;
                    return cs instanceof String ? cs : cs.toString();

                case Id_toSource:
                    {
                        CharSequence s = realThis(thisObj, f).string;
                        return "(new String(\"" + ScriptRuntime.escapeString(s.toString()) + "\"))";
                    }

                case Id_charAt:
                case Id_charCodeAt:
                    {
                        // See ECMA 15.5.4.[4,5]
                        CharSequence target =
                                ScriptRuntime.toCharSequence(
                                        requireObjectCoercible(cx, thisObj, f));
                        double pos = ScriptRuntime.toInteger(args, 0);
                        if (pos < 0 || pos >= target.length()) {
                            if (id == Id_charAt) return "";
                            return ScriptRuntime.NaNobj;
                        }
                        char c = target.charAt((int) pos);
                        if (id == Id_charAt) return String.valueOf(c);
                        return ScriptRuntime.wrapInt(c);
                    }

                case Id_indexOf:
                    {
                        String thisString =
                                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        return ScriptRuntime.wrapInt(js_indexOf(Id_indexOf, thisString, args));
                    }

                case Id_includes:
                case Id_startsWith:
                case Id_endsWith:
                    String thisString =
                            ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                    if (args.length > 0 && args[0] instanceof NativeRegExp) {
                        if (ScriptableObject.isTrue(
                                ScriptableObject.getProperty(
                                        ScriptableObject.ensureScriptable(args[0]),
                                        SymbolKey.MATCH))) {
                            throw ScriptRuntime.typeErrorById(
                                    "msg.first.arg.not.regexp",
                                    String.class.getSimpleName(),
                                    f.getFunctionName());
                        }
                    }

                    int idx = js_indexOf(id, thisString, args);

                    if (id == Id_includes) {
                        return Boolean.valueOf(idx != -1);
                    }
                    if (id == Id_startsWith) {
                        return Boolean.valueOf(idx == 0);
                    }
                    if (id == Id_endsWith) {
                        return Boolean.valueOf(idx != -1);
                    }
                    // fallthrough

                case Id_padStart:
                case Id_padEnd:
                    return js_pad(cx, thisObj, f, args, id == Id_padStart);

                case Id_lastIndexOf:
                    {
                        String thisStr =
                                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        return ScriptRuntime.wrapInt(js_lastIndexOf(thisStr, args));
                    }

                case Id_split:
                    {
                        String thisStr =
                                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        return ScriptRuntime.checkRegExpProxy(cx)
                                .js_split(cx, scope, thisStr, args);
                    }

                case Id_substring:
                    {
                        CharSequence target =
                                ScriptRuntime.toCharSequence(
                                        requireObjectCoercible(cx, thisObj, f));
                        return js_substring(cx, target, args);
                    }

                case Id_toLowerCase:
                    {
                        // See ECMA 15.5.4.11
                        String thisStr =
                                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        return thisStr.toLowerCase(Locale.ROOT);
                    }

                case Id_toUpperCase:
                    {
                        // See ECMA 15.5.4.12
                        String thisStr =
                                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        return thisStr.toUpperCase(Locale.ROOT);
                    }

                case Id_substr:
                    {
                        CharSequence target =
                                ScriptRuntime.toCharSequence(
                                        requireObjectCoercible(cx, thisObj, f));
                        return js_substr(target, args);
                    }

                case Id_concat:
                    {
                        String thisStr =
                                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        return js_concat(thisStr, args);
                    }

                case Id_slice:
                    {
                        CharSequence target =
                                ScriptRuntime.toCharSequence(
                                        requireObjectCoercible(cx, thisObj, f));
                        return js_slice(target, args);
                    }

                case Id_bold:
                    return tagify(cx, thisObj, f, "b", null, null);

                case Id_italics:
                    return tagify(cx, thisObj, f, "i", null, null);

                case Id_fixed:
                    return tagify(cx, thisObj, f, "tt", null, null);

                case Id_strike:
                    return tagify(cx, thisObj, f, "strike", null, null);

                case Id_small:
                    return tagify(cx, thisObj, f, "small", null, null);

                case Id_big:
                    return tagify(cx, thisObj, f, "big", null, null);

                case Id_blink:
                    return tagify(cx, thisObj, f, "blink", null, null);

                case Id_sup:
                    return tagify(cx, thisObj, f, "sup", null, null);

                case Id_sub:
                    return tagify(cx, thisObj, f, "sub", null, null);

                case Id_fontsize:
                    return tagify(cx, thisObj, f, "font", "size", args);

                case Id_fontcolor:
                    return tagify(cx, thisObj, f, "font", "color", args);

                case Id_link:
                    return tagify(cx, thisObj, f, "a", "href", args);

                case Id_anchor:
                    return tagify(cx, thisObj, f, "a", "name", args);

                case Id_equals:
                case Id_equalsIgnoreCase:
                    {
                        String s1 = ScriptRuntime.toString(thisObj);
                        String s2 = ScriptRuntime.toString(args, 0);
                        return ScriptRuntime.wrapBoolean(
                                (id == Id_equals) ? s1.equals(s2) : s1.equalsIgnoreCase(s2));
                    }

                case Id_match:
                case Id_search:
                case Id_replace:
                case Id_replaceAll:
                    {
                        int actionType;
                        if (id == Id_match) {
                            actionType = RegExpProxy.RA_MATCH;
                        } else if (id == Id_search) {
                            actionType = RegExpProxy.RA_SEARCH;
                        } else if (id == Id_replace) {
                            actionType = RegExpProxy.RA_REPLACE;
                        } else {
                            actionType = RegExpProxy.RA_REPLACE_ALL;
                        }

                        requireObjectCoercible(cx, thisObj, f);
                        return ScriptRuntime.checkRegExpProxy(cx)
                                .action(cx, scope, thisObj, args, actionType);
                    }
                    // ECMA-262 1 5.5.4.9
                case Id_localeCompare:
                    {
                        // For now, create and configure a collator instance. I can't
                        // actually imagine that this'd be slower than caching them
                        // a la ClassCache, so we aren't trying to outsmart ourselves
                        // with a caching mechanism for now.
                        String thisStr =
                                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        Collator collator = Collator.getInstance(cx.getLocale());
                        collator.setStrength(Collator.IDENTICAL);
                        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
                        return ScriptRuntime.wrapNumber(
                                collator.compare(thisStr, ScriptRuntime.toString(args, 0)));
                    }
                case Id_toLocaleLowerCase:
                    {
                        String thisStr =
                                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        Locale locale = cx.getLocale();
                        if (args.length > 0 && cx.hasFeature(Context.FEATURE_INTL_402)) {
                            String lang = ScriptRuntime.toString(args[0]);
                            locale = new Locale(lang);
                        }
                        return thisStr.toLowerCase(locale);
                    }
                case Id_toLocaleUpperCase:
                    {
                        String thisStr =
                                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        Locale locale = cx.getLocale();
                        if (args.length > 0 && cx.hasFeature(Context.FEATURE_INTL_402)) {
                            String lang = ScriptRuntime.toString(args[0]);
                            locale = new Locale(lang);
                        }
                        return thisStr.toUpperCase(locale);
                    }
                case Id_trim:
                    {
                        String str = ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        char[] chars = str.toCharArray();

                        int start = 0;
                        while (start < chars.length
                                && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[start])) {
                            start++;
                        }
                        int end = chars.length;
                        while (end > start
                                && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[end - 1])) {
                            end--;
                        }

                        return str.substring(start, end);
                    }
                case Id_trimLeft:
                case Id_trimStart:
                    {
                        String str = ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        char[] chars = str.toCharArray();

                        int start = 0;
                        while (start < chars.length
                                && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[start])) {
                            start++;
                        }
                        int end = chars.length;

                        return str.substring(start, end);
                    }
                case Id_trimRight:
                case Id_trimEnd:
                    {
                        String str = ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        char[] chars = str.toCharArray();

                        int start = 0;

                        int end = chars.length;
                        while (end > start
                                && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[end - 1])) {
                            end--;
                        }

                        return str.substring(start, end);
                    }
                case Id_normalize:
                    {
                        if (args.length == 0 || Undefined.isUndefined(args[0])) {
                            return Normalizer.normalize(
                                    ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f)),
                                    Normalizer.Form.NFC);
                        }

                        final String formStr = ScriptRuntime.toString(args, 0);

                        final Normalizer.Form form;
                        if (Normalizer.Form.NFD.name().equals(formStr)) form = Normalizer.Form.NFD;
                        else if (Normalizer.Form.NFKC.name().equals(formStr))
                            form = Normalizer.Form.NFKC;
                        else if (Normalizer.Form.NFKD.name().equals(formStr))
                            form = Normalizer.Form.NFKD;
                        else if (Normalizer.Form.NFC.name().equals(formStr))
                            form = Normalizer.Form.NFC;
                        else
                            throw rangeError(
                                    "The normalization form should be one of 'NFC', 'NFD', 'NFKC', 'NFKD'.");

                        return Normalizer.normalize(
                                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f)),
                                form);
                    }

                case Id_repeat:
                    {
                        return js_repeat(cx, thisObj, f, args);
                    }
                case Id_codePointAt:
                    {
                        String str = ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        double cnt = ScriptRuntime.toInteger(args, 0);

                        return (cnt < 0 || cnt >= str.length())
                                ? Undefined.instance
                                : Integer.valueOf(str.codePointAt((int) cnt));
                    }
                case Id_at:
                    {
                        String str = ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
                        Object targetArg = (args.length >= 1) ? args[0] : Undefined.instance;
                        int len = str.length();
                        int relativeIndex = (int) ScriptRuntime.toInteger(targetArg);

                        int k = (relativeIndex >= 0) ? relativeIndex : len + relativeIndex;

                        if ((k < 0) || (k >= len)) {
                            return Undefined.instance;
                        }

                        return str.substring(k, k + 1);
                    }

                case SymbolId_iterator:
                    return new NativeStringIterator(scope, requireObjectCoercible(cx, thisObj, f));
            }
            throw new IllegalArgumentException(
                    "String.prototype has no method: " + f.getFunctionName());
        }
    }

    private static NativeString realThis(Scriptable thisObj, IdFunctionObject f) {
        return ensureType(thisObj, NativeString.class, f);
    }

    /*
     * HTML composition aids.
     */
    private static String tagify(
            Context cx,
            Scriptable thisObj,
            IdFunctionObject f,
            String tag,
            String attribute,
            Object[] args) {
        String str = ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
        StringBuilder result = new StringBuilder();
        result.append('<').append(tag);

        if (attribute != null && attribute.length() > 0) {
            String attributeValue = ScriptRuntime.toString(args, 0);
            attributeValue = attributeValue.replace("\"", "&quot;");
            result.append(' ').append(attribute).append("=\"").append(attributeValue).append('"');
        }
        result.append('>').append(str).append("</").append(tag).append('>');
        return result.toString();
    }

    public CharSequence toCharSequence() {
        return string;
    }

    @Override
    public String toString() {
        return string instanceof String ? (String) string : string.toString();
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
    public boolean has(int index, Scriptable start) {
        if (0 <= index && index < string.length()) {
            return true;
        }
        return super.has(index, start);
    }

    @Override
    public int getAttributes(int index) {
        if (0 <= index && index < string.length()) {
            int attribs = READONLY | PERMANENT;
            if (Context.getContext().getLanguageVersion() < Context.VERSION_ES6) {
                attribs |= DONTENUM;
            }
            return attribs;
        }
        return super.getAttributes(index);
    }

    @Override
    protected Object[] getIds(boolean nonEnumerable, boolean getSymbols) {
        // In ES6, Strings have entries in the property map for each character.
        Context cx = Context.getCurrentContext();
        if ((cx != null) && (cx.getLanguageVersion() >= Context.VERSION_ES6)) {
            Object[] sids = super.getIds(nonEnumerable, getSymbols);
            Object[] a = new Object[sids.length + string.length()];
            int i;
            for (i = 0; i < string.length(); i++) {
                a[i] = Integer.valueOf(i);
            }
            System.arraycopy(sids, 0, a, i, sids.length);
            return a;
        }
        return super.getIds(nonEnumerable, getSymbols);
    }

    @Override
    protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
        if (!(id instanceof Symbol)
                && (cx != null)
                && (cx.getLanguageVersion() >= Context.VERSION_ES6)) {
            StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(id);
            if (s.stringId == null && 0 <= s.index && s.index < string.length()) {
                String value = String.valueOf(string.charAt(s.index));
                return defaultIndexPropertyDescriptor(value);
            }
        }
        return super.getOwnPropertyDescriptor(cx, id);
    }

    private ScriptableObject defaultIndexPropertyDescriptor(Object value) {
        Scriptable scope = getParentScope();
        if (scope == null) scope = this;
        ScriptableObject desc = new NativeObject();
        ScriptRuntime.setBuiltinProtoAndParent(desc, scope, TopLevel.Builtins.Object);
        desc.defineProperty("value", value, EMPTY);
        desc.defineProperty("writable", Boolean.FALSE, EMPTY);
        desc.defineProperty("enumerable", Boolean.TRUE, EMPTY);
        desc.defineProperty("configurable", Boolean.FALSE, EMPTY);
        return desc;
    }

    /*
     *
     * See ECMA 15.5.4.6.  Uses Java String.indexOf()
     * OPT to add - BMH searching from jsstr.c.
     */
    private static int js_indexOf(int methodId, String target, Object[] args) {
        String searchStr = ScriptRuntime.toString(args, 0);
        double position = ScriptRuntime.toInteger(args, 1);

        if (methodId != Id_startsWith && methodId != Id_endsWith && searchStr.length() == 0) {
            return position > target.length() ? target.length() : (int) position;
        }

        if (methodId != Id_startsWith && methodId != Id_endsWith && position > target.length()) {
            return -1;
        }

        if (position < 0) position = 0;
        else if (position > target.length()) position = target.length();
        else if (methodId == Id_endsWith && (Double.isNaN(position) || position > target.length()))
            position = target.length();

        if (Id_endsWith == methodId) {
            if (args.length == 0
                    || args.length == 1
                    || (args.length == 2 && args[1] == Undefined.instance))
                position = target.length();
            return target.substring(0, (int) position).endsWith(searchStr) ? 0 : -1;
        }
        return methodId == Id_startsWith
                ? target.startsWith(searchStr, (int) position) ? 0 : -1
                : target.indexOf(searchStr, (int) position);
    }

    /*
     *
     * See ECMA 15.5.4.7
     *
     */
    private static int js_lastIndexOf(String target, Object[] args) {
        String search = ScriptRuntime.toString(args, 0);
        double end = ScriptRuntime.toNumber(args, 1);

        if (Double.isNaN(end) || end > target.length()) end = target.length();
        else if (end < 0) end = 0;

        return target.lastIndexOf(search, (int) end);
    }

    /*
     * See ECMA 15.5.4.15
     */
    private static CharSequence js_substring(Context cx, CharSequence target, Object[] args) {
        int length = target.length();
        double start = ScriptRuntime.toInteger(args, 0);
        double end;

        if (start < 0) start = 0;
        else if (start > length) start = length;

        if (args.length <= 1 || args[1] == Undefined.instance) {
            end = length;
        } else {
            end = ScriptRuntime.toInteger(args[1]);
            if (end < 0) end = 0;
            else if (end > length) end = length;

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
        return target.subSequence((int) start, (int) end);
    }

    int getLength() {
        return string.length();
    }

    /*
     * Non-ECMA methods.
     */
    private static CharSequence js_substr(CharSequence target, Object[] args) {
        if (args.length < 1) {
            return target;
        }

        double begin = ScriptRuntime.toInteger(args[0]);
        double end;
        int length = target.length();

        if (begin < 0) {
            begin += length;
            if (begin < 0) begin = 0;
        } else if (begin > length) {
            begin = length;
        }

        end = length;
        if (args.length > 1) {
            Object lengthArg = args[1];

            if (!Undefined.isUndefined(lengthArg)) {
                end = ScriptRuntime.toInteger(lengthArg);
                if (end < 0) {
                    end = 0;
                }
                end += begin;
                if (end > length) {
                    end = length;
                }
            }
        }

        return target.subSequence((int) begin, (int) end);
    }

    /*
     * Python-esque sequence operations.
     */
    private static String js_concat(String target, Object[] args) {
        int N = args.length;
        if (N == 0) {
            return target;
        } else if (N == 1) {
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
        double begin = args.length < 1 ? 0 : ScriptRuntime.toInteger(args[0]);
        double end;
        int length = target.length();
        if (begin < 0) {
            begin += length;
            if (begin < 0) begin = 0;
        } else if (begin > length) {
            begin = length;
        }

        if (args.length < 2 || args[1] == Undefined.instance) {
            end = length;
        } else {
            end = ScriptRuntime.toInteger(args[1]);
            if (end < 0) {
                end += length;
                if (end < 0) end = 0;
            } else if (end > length) {
                end = length;
            }
            if (end < begin) end = begin;
        }
        return target.subSequence((int) begin, (int) end);
    }

    private static String js_repeat(
            Context cx, Scriptable thisObj, IdFunctionObject f, Object[] args) {
        String str = ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
        double cnt = ScriptRuntime.toInteger(args, 0);

        if ((cnt < 0.0) || (cnt == Double.POSITIVE_INFINITY)) {
            throw rangeError("Invalid count value");
        }

        if (cnt == 0.0 || str.length() == 0) {
            return "";
        }

        long size = str.length() * (long) cnt;
        // Check for overflow
        if ((cnt > Integer.MAX_VALUE) || (size > Integer.MAX_VALUE)) {
            throw rangeError("Invalid size or count value");
        }

        StringBuilder retval = new StringBuilder((int) size);
        retval.append(str);

        int i = 1;
        int icnt = (int) cnt;
        while (i <= (icnt / 2)) {
            retval.append(retval);
            i *= 2;
        }
        if (i < icnt) {
            retval.append(retval.substring(0, str.length() * (icnt - i)));
        }

        return retval.toString();
    }

    /**
     * @see https://www.ecma-international.org/ecma-262/8.0/#sec-string.prototype.padstart
     * @see https://www.ecma-international.org/ecma-262/8.0/#sec-string.prototype.padend
     */
    private static String js_pad(
            Context cx, Scriptable thisObj, IdFunctionObject f, Object[] args, boolean atStart) {
        String pad = ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, f));
        long intMaxLength = ScriptRuntime.toLength(args, 0);
        if (intMaxLength <= pad.length()) {
            return pad;
        }

        String filler = " ";
        if (args.length >= 2 && !Undefined.isUndefined(args[1])) {
            filler = ScriptRuntime.toString(args[1]);
            if (filler.length() < 1) {
                return pad;
            }
        }

        // cast is not really correct here
        int fillLen = (int) (intMaxLength - pad.length());
        StringBuilder concat = new StringBuilder();
        do {
            concat.append(filler);
        } while (concat.length() < fillLen);
        concat.setLength(fillLen);

        if (atStart) {
            return concat.append(pad).toString();
        }

        return concat.insert(0, pad).toString();
    }

    @Override
    protected int findPrototypeId(Symbol k) {
        if (SymbolKey.ITERATOR.equals(k)) {
            return SymbolId_iterator;
        }
        return 0;
    }

    /**
     *
     *
     * <h1>String.raw (template, ...substitutions)</h1>
     *
     * <p>22.1.2.4 String.raw [Draft ECMA-262 / April 28, 2021]
     */
    private static CharSequence js_raw(Context cx, Scriptable scope, Object[] args) {
        /* step 1-2 */
        Object arg0 = args.length > 0 ? args[0] : Undefined.instance;
        Scriptable cooked = ScriptRuntime.toObject(cx, scope, arg0);
        /* step 3 */
        Object rawValue = ScriptRuntime.getObjectProp(cooked, "raw", cx);
        Scriptable raw = ScriptRuntime.toObject(cx, scope, rawValue);
        /* step 4-5 */
        long rawLength = NativeArray.getLengthProperty(cx, raw);
        if (rawLength > Integer.MAX_VALUE) {
            throw ScriptRuntime.rangeError("raw.length > " + Integer.MAX_VALUE);
        }
        int literalSegments = (int) rawLength;
        if (literalSegments <= 0) return "";
        /* step 6-7 */
        StringBuilder elements = new StringBuilder();
        int nextIndex = 0;
        for (; ; ) {
            /* step 8 a-i */
            Object next;
            next = ScriptRuntime.getObjectIndex(raw, nextIndex, cx);
            String nextSeg = ScriptRuntime.toString(next);
            elements.append(nextSeg);
            nextIndex += 1;
            if (nextIndex == literalSegments) {
                break;
            }

            if (args.length > nextIndex) {
                next = args[nextIndex];
                String nextSub = ScriptRuntime.toString(next);
                elements.append(nextSub);
            }
        }
        return elements;
    }

    @Override
    protected int findPrototypeId(String s) {
        int id;
        switch (s) {
            case "constructor":
                id = Id_constructor;
                break;
            case "toString":
                id = Id_toString;
                break;
            case "toSource":
                id = Id_toSource;
                break;
            case "valueOf":
                id = Id_valueOf;
                break;
            case "charAt":
                id = Id_charAt;
                break;
            case "charCodeAt":
                id = Id_charCodeAt;
                break;
            case "indexOf":
                id = Id_indexOf;
                break;
            case "lastIndexOf":
                id = Id_lastIndexOf;
                break;
            case "split":
                id = Id_split;
                break;
            case "substring":
                id = Id_substring;
                break;
            case "toLowerCase":
                id = Id_toLowerCase;
                break;
            case "toUpperCase":
                id = Id_toUpperCase;
                break;
            case "substr":
                id = Id_substr;
                break;
            case "concat":
                id = Id_concat;
                break;
            case "slice":
                id = Id_slice;
                break;
            case "bold":
                id = Id_bold;
                break;
            case "italics":
                id = Id_italics;
                break;
            case "fixed":
                id = Id_fixed;
                break;
            case "strike":
                id = Id_strike;
                break;
            case "small":
                id = Id_small;
                break;
            case "big":
                id = Id_big;
                break;
            case "blink":
                id = Id_blink;
                break;
            case "sup":
                id = Id_sup;
                break;
            case "sub":
                id = Id_sub;
                break;
            case "fontsize":
                id = Id_fontsize;
                break;
            case "fontcolor":
                id = Id_fontcolor;
                break;
            case "link":
                id = Id_link;
                break;
            case "anchor":
                id = Id_anchor;
                break;
            case "equals":
                id = Id_equals;
                break;
            case "equalsIgnoreCase":
                id = Id_equalsIgnoreCase;
                break;
            case "match":
                id = Id_match;
                break;
            case "search":
                id = Id_search;
                break;
            case "replace":
                id = Id_replace;
                break;
            case "replaceAll":
                id = Id_replaceAll;
                break;
            case "localeCompare":
                id = Id_localeCompare;
                break;
            case "toLocaleLowerCase":
                id = Id_toLocaleLowerCase;
                break;
            case "toLocaleUpperCase":
                id = Id_toLocaleUpperCase;
                break;
            case "trim":
                id = Id_trim;
                break;
            case "trimLeft":
                id = Id_trimLeft;
                break;
            case "trimRight":
                id = Id_trimRight;
                break;
            case "includes":
                id = Id_includes;
                break;
            case "startsWith":
                id = Id_startsWith;
                break;
            case "endsWith":
                id = Id_endsWith;
                break;
            case "normalize":
                id = Id_normalize;
                break;
            case "repeat":
                id = Id_repeat;
                break;
            case "codePointAt":
                id = Id_codePointAt;
                break;
            case "padStart":
                id = Id_padStart;
                break;
            case "padEnd":
                id = Id_padEnd;
                break;
            case "trimStart":
                id = Id_trimStart;
                break;
            case "trimEnd":
                id = Id_trimEnd;
                break;
            case "at":
                id = Id_at;
                break;
            default:
                id = 0;
                break;
        }
        return id;
    }

    private static final int ConstructorId_fromCharCode = -1,
            ConstructorId_fromCodePoint = -2,
            ConstructorId_raw = -3,
            Id_constructor = 1,
            Id_toString = 2,
            Id_toSource = 3,
            Id_valueOf = 4,
            Id_charAt = 5,
            Id_charCodeAt = 6,
            Id_indexOf = 7,
            Id_lastIndexOf = 8,
            Id_split = 9,
            Id_substring = 10,
            Id_toLowerCase = 11,
            Id_toUpperCase = 12,
            Id_substr = 13,
            Id_concat = 14,
            Id_slice = 15,
            Id_bold = 16,
            Id_italics = 17,
            Id_fixed = 18,
            Id_strike = 19,
            Id_small = 20,
            Id_big = 21,
            Id_blink = 22,
            Id_sup = 23,
            Id_sub = 24,
            Id_fontsize = 25,
            Id_fontcolor = 26,
            Id_link = 27,
            Id_anchor = 28,
            Id_equals = 29,
            Id_equalsIgnoreCase = 30,
            Id_match = 31,
            Id_search = 32,
            Id_replace = 33,
            Id_replaceAll = 34,
            Id_localeCompare = 35,
            Id_toLocaleLowerCase = 36,
            Id_toLocaleUpperCase = 37,
            Id_trim = 38,
            Id_trimLeft = 39,
            Id_trimRight = 40,
            Id_includes = 41,
            Id_startsWith = 42,
            Id_endsWith = 43,
            Id_normalize = 44,
            Id_repeat = 45,
            Id_codePointAt = 46,
            Id_padStart = 47,
            Id_padEnd = 48,
            SymbolId_iterator = 49,
            Id_trimStart = 50,
            Id_trimEnd = 51,
            Id_at = 52,
            MAX_PROTOTYPE_ID = Id_at;
    private static final int ConstructorId_charAt = -Id_charAt,
            ConstructorId_charCodeAt = -Id_charCodeAt,
            ConstructorId_indexOf = -Id_indexOf,
            ConstructorId_lastIndexOf = -Id_lastIndexOf,
            ConstructorId_split = -Id_split,
            ConstructorId_substring = -Id_substring,
            ConstructorId_toLowerCase = -Id_toLowerCase,
            ConstructorId_toUpperCase = -Id_toUpperCase,
            ConstructorId_substr = -Id_substr,
            ConstructorId_concat = -Id_concat,
            ConstructorId_slice = -Id_slice,
            ConstructorId_equalsIgnoreCase = -Id_equalsIgnoreCase,
            ConstructorId_match = -Id_match,
            ConstructorId_search = -Id_search,
            ConstructorId_replace = -Id_replace,
            ConstructorId_replaceAll = -Id_replaceAll,
            ConstructorId_localeCompare = -Id_localeCompare,
            ConstructorId_toLocaleLowerCase = -Id_toLocaleLowerCase;

    private CharSequence string;
}
