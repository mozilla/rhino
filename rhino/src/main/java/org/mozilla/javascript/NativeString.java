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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.mozilla.javascript.AbstractEcmaStringOperations.ReplacementOperation;
import org.mozilla.javascript.ScriptRuntime.StringIdOrIndex;

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
final class NativeString extends ScriptableObject {
    private static final long serialVersionUID = 920268368584188687L;

    private static final String CLASS_NAME = "String";

    private final CharSequence string;

    static void init(Scriptable scope, boolean sealed) {
        LambdaConstructor c =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        1,
                        NativeString::js_constructorFunc,
                        NativeString::js_constructor);
        c.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        c.setPrototypeScriptable(new NativeString(""));

        defConsMethod(c, scope, "fromCharCode", 1, NativeString::js_fromCharCode);
        defConsMethod(c, scope, "fromCodePoint", 1, NativeString::js_fromCodePoint);
        defConsMethod(c, scope, "raw", 1, NativeString::js_raw);

        /*
         * All of the methods below are on the constructor for compatibility with ancient Rhino
         * versions. They are no longer part of ECMAScript. The "wrapConstructor" method is a
         * technique used in the past in Rhino to adapt the instance functions so that they
         * may be called on the constructor directly.
         */
        defConsMethod(c, scope, "charAt", 1, wrapConstructor(NativeString::js_charAt));
        defConsMethod(c, scope, "charCodeAt", 1, wrapConstructor(NativeString::js_charCodeAt));
        defConsMethod(c, scope, "indexOf", 2, wrapConstructor(NativeString::js_indexOf));
        defConsMethod(c, scope, "lastIndexOf", 2, wrapConstructor(NativeString::js_lastIndexOf));
        defConsMethod(c, scope, "split", 3, wrapConstructor(NativeString::js_split));
        defConsMethod(c, scope, "substring", 3, wrapConstructor(NativeString::js_substring));
        defConsMethod(c, scope, "toLowerCase", 1, wrapConstructor(NativeString::js_toLowerCase));
        defConsMethod(c, scope, "toUpperCase", 1, wrapConstructor(NativeString::js_toUpperCase));
        defConsMethod(c, scope, "substr", 3, wrapConstructor(NativeString::js_substr));
        defConsMethod(c, scope, "concat", 2, wrapConstructor(NativeString::js_concat));
        defConsMethod(c, scope, "slice", 3, wrapConstructor(NativeString::js_slice));
        defConsMethod(
                c,
                scope,
                "equalsIgnoreCase",
                2,
                wrapConstructor(NativeString::js_equalsIgnoreCase));
        defConsMethod(c, scope, "match", 2, wrapConstructor(NativeString::js_match));
        defConsMethod(c, scope, "search", 2, wrapConstructor(NativeString::js_search));
        defConsMethod(c, scope, "replace", 2, wrapConstructor(NativeString::js_replace));
        defConsMethod(c, scope, "replaceAll", 2, wrapConstructor(NativeString::js_replaceAll));
        defConsMethod(
                c, scope, "localeCompare", 2, wrapConstructor(NativeString::js_localeCompare));
        defConsMethod(
                c,
                scope,
                "toLocaleLowerCase",
                1,
                wrapConstructor(NativeString::js_toLocaleLowerCase));

        /* Back to prototype methods -- these are all part of ECMAScript */
        defProtoMethod(c, scope, SymbolKey.ITERATOR, 0, NativeString::js_iterator);
        defProtoMethod(c, scope, "toString", 0, NativeString::js_toString);
        defProtoMethod(c, scope, "toSource", 0, NativeString::js_toSource);
        defProtoMethod(c, scope, "valueOf", 0, NativeString::js_toString);
        defProtoMethod(c, scope, "charAt", 1, NativeString::js_charAt);
        defProtoMethod(c, scope, "charCodeAt", 1, NativeString::js_charCodeAt);
        defProtoMethod(c, scope, "indexOf", 1, NativeString::js_indexOf);
        defProtoMethod(c, scope, "lastIndexOf", 1, NativeString::js_lastIndexOf);
        defProtoMethod(c, scope, "split", 2, NativeString::js_split);
        defProtoMethod(c, scope, "substring", 2, NativeString::js_substring);
        defProtoMethod(c, scope, "toLowerCase", 0, NativeString::js_toLowerCase);
        defProtoMethod(c, scope, "toUpperCase", 0, NativeString::js_toUpperCase);
        defProtoMethod(c, scope, "substr", 2, NativeString::js_substr);
        defProtoMethod(c, scope, "concat", 1, NativeString::js_concat);
        defProtoMethod(c, scope, "slice", 2, NativeString::js_slice);
        defProtoMethod(c, scope, "bold", 0, NativeString::js_bold);
        defProtoMethod(c, scope, "italics", 0, NativeString::js_italics);
        defProtoMethod(c, scope, "fixed", 0, NativeString::js_fixed);
        defProtoMethod(c, scope, "strike", 0, NativeString::js_strike);
        defProtoMethod(c, scope, "small", 0, NativeString::js_small);
        defProtoMethod(c, scope, "big", 0, NativeString::js_big);
        defProtoMethod(c, scope, "blink", 0, NativeString::js_blink);
        defProtoMethod(c, scope, "sup", 0, NativeString::js_sup);
        defProtoMethod(c, scope, "sub", 0, NativeString::js_sub);
        defProtoMethod(c, scope, "fontsize", 0, NativeString::js_fontsize);
        defProtoMethod(c, scope, "fontcolor", 0, NativeString::js_fontcolor);
        defProtoMethod(c, scope, "link", 0, NativeString::js_link);
        defProtoMethod(c, scope, "anchor", 0, NativeString::js_anchor);
        defProtoMethod(c, scope, "equals", 1, NativeString::js_equals);
        defProtoMethod(c, scope, "equalsIgnoreCase", 1, NativeString::js_equalsIgnoreCase);
        defProtoMethod(c, scope, "match", 1, NativeString::js_match);
        defProtoMethod(c, scope, "matchAll", 1, NativeString::js_matchAll);
        defProtoMethod(c, scope, "search", 1, NativeString::js_search);
        defProtoMethod(c, scope, "replace", 2, NativeString::js_replace);
        defProtoMethod(c, scope, "replaceAll", 2, NativeString::js_replaceAll);
        defProtoMethod(c, scope, "at", 1, NativeString::js_at);
        defProtoMethod(c, scope, "localeCompare", 1, NativeString::js_localeCompare);
        defProtoMethod(c, scope, "toLocaleLowerCase", 0, NativeString::js_toLocaleLowerCase);
        defProtoMethod(c, scope, "toLocaleUpperCase", 0, NativeString::js_toLocaleUpperCase);
        defProtoMethod(c, scope, "trim", 0, NativeString::js_trim);
        defProtoMethod(c, scope, "trimLeft", 0, NativeString::js_trimLeft);
        defProtoMethod(c, scope, "trimStart", 0, NativeString::js_trimLeft);
        defProtoMethod(c, scope, "trimRight", 0, NativeString::js_trimRight);
        defProtoMethod(c, scope, "trimEnd", 0, NativeString::js_trimRight);
        defProtoMethod(c, scope, "includes", 1, NativeString::js_includes);
        defProtoMethod(c, scope, "startsWith", 1, NativeString::js_startsWith);
        defProtoMethod(c, scope, "endsWith", 1, NativeString::js_endsWith);
        defProtoMethod(c, scope, "normalize", 0, NativeString::js_normalize);
        defProtoMethod(c, scope, "repeat", 1, NativeString::js_repeat);
        defProtoMethod(c, scope, "codePointAt", 1, NativeString::js_codePointAt);
        defProtoMethod(c, scope, "padStart", 1, NativeString::js_padStart);
        defProtoMethod(c, scope, "padEnd", 1, NativeString::js_padEnd);
        defProtoMethod(c, scope, "isWellFormed", 0, NativeString::js_isWellFormed);
        defProtoMethod(c, scope, "toWellFormed", 0, NativeString::js_toWellFormed);

        if (sealed) {
            c.sealObject();
            ((NativeString) c.getPrototypeProperty()).sealObject();
        }
        ScriptableObject.defineProperty(scope, CLASS_NAME, c, DONTENUM);
    }

    private static void defConsMethod(
            LambdaConstructor c,
            Scriptable scope,
            String name,
            int length,
            SerializableCallable target) {
        c.defineConstructorMethod(scope, name, length, target);
    }

    private static void defProtoMethod(
            LambdaConstructor c,
            Scriptable scope,
            SymbolKey key,
            int length,
            SerializableCallable target) {
        c.definePrototypeMethod(scope, key, length, target);
    }

    private static void defProtoMethod(
            LambdaConstructor c,
            Scriptable scope,
            String name,
            int length,
            SerializableCallable target) {
        c.definePrototypeMethod(scope, name, length, target);
    }

    NativeString(CharSequence s) {
        string = s;
        // This needs to happen right here because ScriptRuntime sometimes
        // constructs strings directly without using the JS constructor.
        defineProperty("length", s::length, null, DONTENUM | READONLY | PERMANENT);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static SerializableCallable wrapConstructor(SerializableCallable target) {
        return (Context cx, Scriptable scope, Scriptable origThis, Object[] origArgs) -> {
            Scriptable thisObj;
            Object[] newArgs;
            if (origArgs.length > 0) {
                thisObj =
                        ScriptRuntime.toObject(
                                cx, scope, ScriptRuntime.toCharSequence(origArgs[0]));
                newArgs = new Object[origArgs.length - 1];
                System.arraycopy(origArgs, 1, newArgs, 0, newArgs.length);
            } else {
                thisObj = ScriptRuntime.toObject(cx, scope, ScriptRuntime.toCharSequence(origThis));
                newArgs = origArgs;
            }
            return target.call(cx, scope, thisObj, newArgs);
        };
    }

    private static Scriptable js_constructor(Context cx, Scriptable scope, Object[] args) {
        CharSequence s;
        if (args.length == 0) {
            s = "";
        } else {
            s = ScriptRuntime.toCharSequence(args[0]);
        }
        return new NativeString(s);
    }

    private static Object js_constructorFunc(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        CharSequence s;
        if (args.length == 0) {
            s = "";
        } else if (ScriptRuntime.isSymbol(args[0])) {
            // 19.4.3.2 et.al. Convert a symbol to a string with String() but not
            // new String()
            s = args[0].toString();
        } else {
            s = ScriptRuntime.toCharSequence(args[0]);
        }
        // String(val) converts val to a string value.
        return s instanceof String ? s : s.toString();
    }

    private static Object js_fromCharCode(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
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

    private static Object js_fromCodePoint(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        int n = args.length;
        if (n < 1) {
            return "";
        }
        int[] codePoints = new int[n];
        for (int i = 0; i != n; i++) {
            Object arg = args[i];
            int codePoint = ScriptRuntime.toInt32(arg);
            double num = ScriptRuntime.toNumber(arg);
            if (!ScriptRuntime.eqNumber(num, codePoint) || !Character.isValidCodePoint(codePoint)) {
                throw rangeError("Invalid code point " + ScriptRuntime.toString(arg));
            }
            codePoints[i] = codePoint;
        }
        return new String(codePoints, 0, n);
    }

    private static Object js_charAt(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return charAt(cx, thisObj, args, false);
    }

    private static Object js_charCodeAt(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return charAt(cx, thisObj, args, true);
    }

    private static Object charAt(Context cx, Scriptable thisObj, Object[] args, boolean getCode) {
        // See ECMA 15.5.4.[4,5]
        CharSequence target =
                ScriptRuntime.toCharSequence(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "charAt"));
        double pos = ScriptRuntime.toInteger(args, 0);
        if (pos < 0 || pos >= target.length()) {
            if (!getCode) return "";
            return ScriptRuntime.NaNobj;
        }
        char c = target.charAt((int) pos);
        if (!getCode) return String.valueOf(c);
        return ScriptRuntime.wrapInt(c);
    }

    private static Object js_indexOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String target =
                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, CLASS_NAME, "indexOf"));
        String searchStr = ScriptRuntime.toString(args, 0);
        double position = ScriptRuntime.toInteger(args, 1);

        if (searchStr.isEmpty()) {
            return position > target.length() ? target.length() : (int) position;
        }
        if (position > target.length()) {
            return -1;
        }
        if (position < 0) position = 0;
        return target.indexOf(searchStr, (int) position);
    }

    private static Object js_startsWith(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String target =
                ScriptRuntime.toString(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "startsWith"));
        checkValidRegex(cx, args, 0, "startsWith");
        String searchStr = ScriptRuntime.toString(args, 0);
        double position = ScriptRuntime.toInteger(args, 1);
        if (position < 0) position = 0;
        else if (position > target.length()) position = target.length();
        return target.startsWith(searchStr, (int) position);
    }

    private static Object js_endsWith(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String target =
                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, CLASS_NAME, "endsWith"));
        checkValidRegex(cx, args, 0, "endsWith");
        String searchStr = ScriptRuntime.toString(args, 0);
        double position = ScriptRuntime.toInteger(args, 1);
        if (position < 0) position = 0;
        else if (Double.isNaN(position) || position > target.length()) position = target.length();
        if (args.length == 0
                || args.length == 1
                || (args.length == 2 && Undefined.isUndefined(args[1]))) position = target.length();
        return target.substring(0, (int) position).endsWith(searchStr);
    }

    private static Object js_includes(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String target =
                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, CLASS_NAME, "includes"));
        String searchStr = ScriptRuntime.toString(args, 0);
        checkValidRegex(cx, args, 0, "includes");
        int position = (int) ScriptRuntime.toInteger(args, 1);
        return target.indexOf(searchStr, position) != -1;
    }

    private static void checkValidRegex(Context cx, Object[] args, int pos, String functionName) {
        if (args.length > pos && args[pos] instanceof Scriptable) {
            RegExpProxy reProxy = ScriptRuntime.getRegExpProxy(cx);
            if (reProxy != null) {
                Scriptable arg = (Scriptable) args[pos];
                if (reProxy.isRegExp(arg)) {
                    if (ScriptableObject.isTrue(
                            ScriptableObject.getProperty(arg, SymbolKey.MATCH))) {
                        throw ScriptRuntime.typeErrorById(
                                "msg.first.arg.not.regexp", CLASS_NAME, functionName);
                    }
                }
            }
        }
    }

    private static Object js_split(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // See ECMAScript spec 22.1.3.23
        Object o = requireObjectCoercible(cx, thisObj, CLASS_NAME, "split");

        if (cx.getLanguageVersion() <= Context.VERSION_1_8) {
            // Use old algorithm for backward compatibility
            return ScriptRuntime.checkRegExpProxy(cx)
                    .js_split(cx, scope, ScriptRuntime.toString(o), args);
        }

        Object separator = args.length > 0 ? args[0] : Undefined.instance;
        Object limit = args.length > 1 ? args[1] : Undefined.instance;
        if (!Undefined.isUndefined(separator) && separator != null) {
            Object splitter = ScriptRuntime.getObjectElem(separator, SymbolKey.SPLIT, cx, scope);
            // If method is not undefined, it should be a Callable
            if (splitter != null && !Undefined.isUndefined(splitter)) {
                if (!(splitter instanceof Callable)) {
                    throw ScriptRuntime.notFunctionError(
                            separator, splitter, SymbolKey.SPLIT.getName());
                }
                return ((Callable) splitter)
                        .call(
                                cx,
                                scope,
                                ScriptRuntime.toObject(scope, separator),
                                new Object[] {
                                    o instanceof NativeString ? ((NativeString) o).string : o,
                                    limit,
                                });
            }
        }

        String s = ScriptRuntime.toString(o);
        long lim;
        if (Undefined.isUndefined(limit)) {
            lim = Integer.MAX_VALUE;
        } else {
            lim = ScriptRuntime.toUint32(limit);
        }
        String r = ScriptRuntime.toString(separator);
        if (lim == 0) {
            return cx.newArray(scope, 0);
        }
        if (Undefined.isUndefined(separator)) {
            return cx.newArray(scope, new Object[] {s});
        }

        int separatorLength = r.length();
        if (separatorLength == 0) {
            int strLen = s.length();
            int outLen = ScriptRuntime.clamp((int) lim, 0, strLen);
            String head = s.substring(0, outLen);

            List<Object> codeUnits = new ArrayList<>();
            for (int i = 0; i < head.length(); ) {
                char c = head.charAt(i);
                codeUnits.add(Character.toString(c));
                i += Character.charCount(c);
            }
            return cx.newArray(scope, codeUnits.toArray());
        }

        if (s.isEmpty()) {
            return cx.newArray(scope, new Object[] {s});
        }

        List<String> substrings = new ArrayList<>();
        int i = 0;
        int j = s.indexOf(r);
        while (j != -1) {
            String t = s.substring(i, j);
            substrings.add(t);
            if (substrings.size() >= lim) {
                return cx.newArray(scope, substrings.toArray());
            }
            i = j + separatorLength;
            j = s.indexOf(r, i);
        }
        String t = s.substring(i);
        substrings.add(t);

        return cx.newArray(scope, substrings.toArray());
    }

    private static NativeString realThis(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeString.class);
    }

    private static Object js_iterator(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return new NativeStringIterator(
                scope, requireObjectCoercible(cx, thisObj, CLASS_NAME, "[Symbol.iterator]"));
    }

    private static Object js_toString(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // ECMA 15.5.4.2: the toString function is not generic.
        CharSequence cs = realThis(thisObj).string;
        return cs instanceof String ? cs : cs.toString();
    }

    private static Object js_toSource(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        CharSequence s = realThis(thisObj).string;
        return "(new String(\"" + ScriptRuntime.escapeString(s.toString()) + "\"))";
    }

    /*
     * HTML composition aids.
     */
    private static String tagify(
            Context cx,
            Scriptable thisObj,
            String functionName,
            String tag,
            String attribute,
            Object[] args) {
        String str =
                ScriptRuntime.toString(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, functionName));
        StringBuilder result = new StringBuilder();
        result.append('<').append(tag);

        if (attribute != null && !attribute.isEmpty()) {
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
    protected Object[] getIds(CompoundOperationMap map, boolean nonEnumerable, boolean getSymbols) {
        // In ES6, Strings have entries in the property map for each character.
        Context cx = Context.getCurrentContext();
        if ((cx != null) && (cx.getLanguageVersion() >= Context.VERSION_ES6)) {
            Object[] sids = super.getIds(map, nonEnumerable, getSymbols);
            Object[] a = new Object[sids.length + string.length()];
            int i;
            for (i = 0; i < string.length(); i++) {
                a[i] = i;
            }
            System.arraycopy(sids, 0, a, i, sids.length);
            return a;
        }
        return super.getIds(map, nonEnumerable, getSymbols);
    }

    @Override
    protected ScriptableObject.DescriptorInfo getOwnPropertyDescriptor(Context cx, Object id) {
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

    private ScriptableObject.DescriptorInfo defaultIndexPropertyDescriptor(Object value) {
        return new ScriptableObject.DescriptorInfo(true, false, false, NOT_FOUND, NOT_FOUND, value);
    }

    /*
     *
     * See ECMA 22.1.3.13
     *
     */
    private static Object js_match(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object o = requireObjectCoercible(cx, thisObj, CLASS_NAME, "match");
        Object regexp = args.length > 0 ? args[0] : Undefined.instance;
        RegExpProxy regExpProxy = ScriptRuntime.checkRegExpProxy(cx);
        if (regexp != null && !Undefined.isUndefined(regexp)) {
            Object matcher = ScriptRuntime.getObjectElem(regexp, SymbolKey.MATCH, cx, scope);
            // If method is not undefined, it should be a Callable
            if (matcher != null && !Undefined.isUndefined(matcher)) {
                if (!(matcher instanceof Callable)) {
                    throw ScriptRuntime.notFunctionError(
                            regexp, matcher, SymbolKey.MATCH.getName());
                }
                return ((Callable) matcher)
                        .call(cx, scope, ScriptRuntime.toObject(scope, regexp), new Object[] {o});
            }
        }

        String s = ScriptRuntime.toString(o);
        String regexpToString = Undefined.isUndefined(regexp) ? "" : ScriptRuntime.toString(regexp);

        String flags = null;

        // Not standard; Done for backward compatibility
        if (cx.getLanguageVersion() < Context.VERSION_1_6 && args.length > 1) {
            flags = ScriptRuntime.toString(args[1]);
        }

        Object compiledRegExp = regExpProxy.compileRegExp(cx, regexpToString, flags);
        Scriptable rx = regExpProxy.wrapRegExp(cx, scope, compiledRegExp);

        Object method = ScriptRuntime.getObjectElem(rx, SymbolKey.MATCH, cx, scope);
        if (!(method instanceof Callable)) {
            throw ScriptRuntime.notFunctionError(rx, method, SymbolKey.MATCH.getName());
        }
        return ((Callable) method).call(cx, scope, rx, new Object[] {s});
    }

    /*
     *
     * See ECMA 15.5.4.7
     *
     */
    private static Object js_lastIndexOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String target =
                ScriptRuntime.toString(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "lastIndexOf"));
        String search = ScriptRuntime.toString(args, 0);
        double end = ScriptRuntime.toNumber(args, 1);

        if (Double.isNaN(end) || end > target.length()) end = target.length();
        else if (end < 0) end = 0;

        return target.lastIndexOf(search, (int) end);
    }

    /*
     * See ECMA 15.5.4.15
     */
    private static Object js_substring(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        CharSequence target =
                ScriptRuntime.toCharSequence(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "substring"));
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

    private static Object js_toLowerCase(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // See ECMA 15.5.4.11
        String thisStr =
                ScriptRuntime.toString(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "toLowerCase"));
        return thisStr.toLowerCase(Locale.ROOT);
    }

    private static Object js_toUpperCase(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // See ECMA 15.5.4.12
        String thisStr =
                ScriptRuntime.toString(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "toUpperCase"));
        return thisStr.toUpperCase(Locale.ROOT);
    }

    int getLength() {
        return string.length();
    }

    /*
     * Non-ECMA methods.
     */
    private static CharSequence js_substr(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        CharSequence target =
                ScriptRuntime.toCharSequence(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "substr"));
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
    private static String js_concat(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String target =
                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, CLASS_NAME, "concat"));
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

    private static Object js_slice(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        CharSequence target =
                ScriptRuntime.toCharSequence(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "slice"));
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

    private static Object js_at(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String str = ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, CLASS_NAME, "at"));
        Object targetArg = (args.length >= 1) ? args[0] : Undefined.instance;
        int len = str.length();
        int relativeIndex = (int) ScriptRuntime.toInteger(targetArg);

        int k = (relativeIndex >= 0) ? relativeIndex : len + relativeIndex;

        if ((k < 0) || (k >= len)) {
            return Undefined.instance;
        }

        return str.substring(k, k + 1);
    }

    private static Object js_equals(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String s1 = ScriptRuntime.toString(thisObj);
        String s2 = ScriptRuntime.toString(args, 0);
        return s1.equals(s2);
    }

    private static Object js_equalsIgnoreCase(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String s1 = ScriptRuntime.toString(thisObj);
        String s2 = ScriptRuntime.toString(args, 0);
        return s1.equalsIgnoreCase(s2);
    }

    private static Object js_search(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object o = requireObjectCoercible(cx, thisObj, CLASS_NAME, "search");
        Object regexp = args.length > 0 ? args[0] : Undefined.instance;
        RegExpProxy regExpProxy = ScriptRuntime.checkRegExpProxy(cx);
        if (regexp != null && !Undefined.isUndefined(regexp)) {
            Object matcher = ScriptRuntime.getObjectElem(regexp, SymbolKey.SEARCH, cx, scope);
            // If method is not undefined, it should be a Callable
            if (matcher != null && !Undefined.isUndefined(matcher)) {
                if (!(matcher instanceof Callable)) {
                    throw ScriptRuntime.notFunctionError(
                            regexp, matcher, SymbolKey.SEARCH.getName());
                }
                return ((Callable) matcher)
                        .call(cx, scope, ScriptRuntime.toObject(scope, regexp), new Object[] {o});
            }
        }

        String s = ScriptRuntime.toString(o);
        String regexpToString = Undefined.isUndefined(regexp) ? "" : ScriptRuntime.toString(regexp);

        String flags = null;
        // Not standard; Done for backward compatibility
        if (cx.getLanguageVersion() < Context.VERSION_1_6 && args.length > 1) {
            flags = ScriptRuntime.toString(args[1]);
        }

        Object compiledRegExp = regExpProxy.compileRegExp(cx, regexpToString, flags);
        Scriptable rx = regExpProxy.wrapRegExp(cx, scope, compiledRegExp);

        Object method = ScriptRuntime.getObjectElem(rx, SymbolKey.SEARCH, cx, scope);
        if (!(method instanceof Callable)) {
            throw ScriptRuntime.notFunctionError(rx, method, SymbolKey.SEARCH.getName());
        }
        return ((Callable) method).call(cx, scope, rx, new Object[] {s});
    }

    private static Object js_replace(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // See ECMAScript spec 22.1.3.19
        Object o = requireObjectCoercible(cx, thisObj, CLASS_NAME, "replace");

        if (cx.getLanguageVersion() <= Context.VERSION_1_8) {
            // Use old algorithm for backward compatibility
            return ScriptRuntime.checkRegExpProxy(cx)
                    .action(cx, scope, thisObj, args, RegExpProxy.RA_REPLACE);
        }

        // Spec-compliant algorithm

        Object searchValue = args.length > 0 ? args[0] : Undefined.instance;
        Object replaceValue = args.length > 1 ? args[1] : Undefined.instance;

        if (!Undefined.isUndefined(searchValue) && searchValue != null) {
            Object replacer =
                    ScriptRuntime.getObjectElem(searchValue, SymbolKey.REPLACE, cx, scope);
            // If method is not undefined, it should be a Callable
            if (replacer != null && !Undefined.isUndefined(replacer)) {
                if (!(replacer instanceof Callable)) {
                    throw ScriptRuntime.notFunctionError(
                            searchValue, replacer, SymbolKey.REPLACE.getName());
                }
                return ((Callable) replacer)
                        .call(
                                cx,
                                scope,
                                ScriptRuntime.toObject(scope, searchValue),
                                new Object[] {
                                    o instanceof NativeString ? ((NativeString) o).string : o,
                                    replaceValue
                                });
            }
        }

        String string = ScriptRuntime.toString(o);
        String searchString = ScriptRuntime.toString(searchValue);
        boolean functionalReplace = replaceValue instanceof Callable;
        List<ReplacementOperation> replaceOps;

        if (!functionalReplace) {
            replaceOps =
                    AbstractEcmaStringOperations.buildReplacementList(
                            ScriptRuntime.toString(replaceValue));
        } else {
            replaceOps = List.of();
        }
        int searchLength = searchString.length();
        int position = string.indexOf(searchString);
        if (position == -1) {
            return string;
        }
        String preceding = string.substring(0, position);
        String following = string.substring(position + searchLength);

        String replacement;
        if (functionalReplace) {
            Scriptable callThis =
                    ScriptRuntime.getApplyOrCallThis(cx, scope, null, 0, (Callable) replaceValue);

            Object replacementObj =
                    ((Callable) replaceValue)
                            .call(
                                    cx,
                                    scope,
                                    callThis,
                                    new Object[] {
                                        searchString, position, string,
                                    });
            replacement = ScriptRuntime.toString(replacementObj);
        } else {
            List<Object> captures = List.of();
            replacement =
                    AbstractEcmaStringOperations.getSubstitution(
                            cx,
                            scope,
                            searchString,
                            string,
                            position,
                            captures,
                            Undefined.SCRIPTABLE_UNDEFINED,
                            replaceOps);
        }
        return preceding + replacement + following;
    }

    private static Object js_replaceAll(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // See ECMAScript spec 22.1.3.20
        Object o = requireObjectCoercible(cx, thisObj, CLASS_NAME, "replaceAll");

        Object searchValue = args.length > 0 ? args[0] : Undefined.instance;
        Object replaceValue = args.length > 1 ? args[1] : Undefined.instance;

        if (searchValue != null && !Undefined.isUndefined(searchValue)) {
            boolean isRegExp =
                    searchValue instanceof Scriptable
                            && AbstractEcmaObjectOperations.isRegExp(cx, scope, searchValue);
            if (isRegExp) {
                Object flags = ScriptRuntime.getObjectProp(searchValue, "flags", cx, scope);
                requireObjectCoercible(cx, flags, CLASS_NAME, "replaceAll");
                String flagsStr = ScriptRuntime.toString(flags);
                if (!flagsStr.contains("g")) {
                    throw ScriptRuntime.typeErrorById("msg.str.replace.all.no.global.flag");
                }
            }

            Object matcher = ScriptRuntime.getObjectElem(searchValue, SymbolKey.REPLACE, cx, scope);
            // If method is not undefined, it should be a Callable
            if (matcher != null && !Undefined.isUndefined(matcher)) {
                if (!(matcher instanceof Callable)) {
                    throw ScriptRuntime.notFunctionError(
                            searchValue, matcher, SymbolKey.REPLACE.getName());
                }
                return ((Callable) matcher)
                        .call(
                                cx,
                                scope,
                                ScriptRuntime.toObject(scope, searchValue),
                                new Object[] {o, replaceValue});
            }
        }

        String string = ScriptRuntime.toString(o);
        String searchString = ScriptRuntime.toString(searchValue);
        boolean functionalReplace = replaceValue instanceof Callable;
        List<ReplacementOperation> replaceOps;
        if (!functionalReplace) {
            replaceOps =
                    AbstractEcmaStringOperations.buildReplacementList(
                            ScriptRuntime.toString(replaceValue));
        } else {
            replaceOps = List.of();
        }
        int searchLength = searchString.length();
        int advanceBy = Math.max(1, searchLength);

        List<Integer> matchPositions = new ArrayList<>();
        int position = string.indexOf(searchString);
        while (position != -1) {
            matchPositions.add(position);
            int newPosition = string.indexOf(searchString, position + advanceBy);
            if (newPosition == position) {
                break;
            }
            position = newPosition;
        }
        int endOfLastMatch = 0;
        StringBuilder result = new StringBuilder();
        for (Integer p : matchPositions) {
            String preserved = string.substring(endOfLastMatch, p);
            String replacement;
            if (functionalReplace) {
                Scriptable callThis =
                        ScriptRuntime.getApplyOrCallThis(
                                cx, scope, null, 0, (Callable) replaceValue);

                Object replacementObj =
                        ((Callable) replaceValue)
                                .call(
                                        cx,
                                        scope,
                                        callThis,
                                        new Object[] {
                                            searchString, p, string,
                                        });
                replacement = ScriptRuntime.toString(replacementObj);
            } else {
                List<Object> captures = List.of();
                replacement =
                        AbstractEcmaStringOperations.getSubstitution(
                                cx,
                                scope,
                                searchString,
                                string,
                                p,
                                captures,
                                Undefined.SCRIPTABLE_UNDEFINED,
                                replaceOps);
            }
            result.append(preserved);
            result.append(replacement);
            endOfLastMatch = p + searchLength;
        }
        if (endOfLastMatch < string.length()) {
            result.append(string.substring(endOfLastMatch));
        }
        return result.toString();
    }

    private static Object js_matchAll(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // See ECMAScript spec 22.1.3.14
        Object o = requireObjectCoercible(cx, thisObj, CLASS_NAME, "matchAll");
        Object regexp = args.length > 0 ? args[0] : Undefined.instance;
        if (regexp != null && !Undefined.isUndefined(regexp)) {
            boolean isRegExp = AbstractEcmaObjectOperations.isRegExp(cx, scope, regexp);
            if (isRegExp) {
                Object flags = ScriptRuntime.getObjectProp(regexp, "flags", cx, scope);
                requireObjectCoercible(cx, flags, CLASS_NAME, "matchAll");
                String flagsStr = ScriptRuntime.toString(flags);
                if (!flagsStr.contains("g")) {
                    throw ScriptRuntime.typeErrorById("msg.str.match.all.no.global.flag");
                }
            }

            Object matcher = ScriptRuntime.getObjectElem(regexp, SymbolKey.MATCH_ALL, cx, scope);
            // If method is not undefined, it should be a Callable
            if (matcher != null && !Undefined.isUndefined(matcher)) {
                if (!(matcher instanceof Callable)) {
                    throw ScriptRuntime.notFunctionError(
                            regexp, matcher, SymbolKey.MATCH_ALL.getName());
                }
                return ((Callable) matcher)
                        .call(cx, scope, ScriptRuntime.toObject(scope, regexp), new Object[] {o});
            }
        }

        String s = ScriptRuntime.toString(o);
        String regexpToString = Undefined.isUndefined(regexp) ? "" : ScriptRuntime.toString(regexp);
        RegExpProxy regExpProxy = ScriptRuntime.checkRegExpProxy(cx);
        Object compiledRegExp = regExpProxy.compileRegExp(cx, regexpToString, "g");
        Scriptable rx = regExpProxy.wrapRegExp(cx, scope, compiledRegExp);

        Object method = ScriptRuntime.getObjectElem(rx, SymbolKey.MATCH_ALL, cx, scope);
        if (!(method instanceof Callable)) {
            throw ScriptRuntime.notFunctionError(rx, method, SymbolKey.MATCH_ALL.getName());
        }
        return ((Callable) method).call(cx, scope, rx, new Object[] {s});
    }

    private static Object js_localeCompare(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // For now, create and configure a collator instance. I can't
        // actually imagine that this'd be slower than caching them
        // a la ClassCache, so we aren't trying to outsmart ourselves
        // with a caching mechanism for now.
        String thisStr =
                ScriptRuntime.toString(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "localeCompare"));
        Collator collator = Collator.getInstance(cx.getLocale());
        collator.setStrength(Collator.IDENTICAL);
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        return collator.compare(thisStr, ScriptRuntime.toString(args, 0));
    }

    private static Object js_toLocaleLowerCase(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String thisStr =
                ScriptRuntime.toString(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "toLocaleLowerCase"));
        Locale locale = cx.getLocale();
        if (args.length > 0 && cx.hasFeature(Context.FEATURE_INTL_402)) {
            String lang = ScriptRuntime.toString(args[0]);
            locale = new Locale(lang);
        }
        return thisStr.toLowerCase(locale);
    }

    private static Object js_toLocaleUpperCase(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String thisStr =
                ScriptRuntime.toString(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "toLocaleUpperCase"));
        Locale locale = cx.getLocale();
        if (args.length > 0 && cx.hasFeature(Context.FEATURE_INTL_402)) {
            String lang = ScriptRuntime.toString(args[0]);
            locale = new Locale(lang);
        }
        return thisStr.toUpperCase(locale);
    }

    private static Object js_trim(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String str =
                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, CLASS_NAME, "trim"));
        char[] chars = str.toCharArray();

        int start = 0;
        while (start < chars.length && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[start])) {
            start++;
        }
        int end = chars.length;
        while (end > start && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[end - 1])) {
            end--;
        }

        return str.substring(start, end);
    }

    private static Object js_trimLeft(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String str =
                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, CLASS_NAME, "trimLeft"));
        char[] chars = str.toCharArray();

        int start = 0;
        while (start < chars.length && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[start])) {
            start++;
        }
        int end = chars.length;

        return str.substring(start, end);
    }

    private static Object js_trimRight(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String str =
                ScriptRuntime.toString(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "trimRight"));
        char[] chars = str.toCharArray();

        int start = 0;

        int end = chars.length;
        while (end > start && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[end - 1])) {
            end--;
        }

        return str.substring(start, end);
    }

    private static Object js_normalize(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length == 0 || Undefined.isUndefined(args[0])) {
            return Normalizer.normalize(
                    ScriptRuntime.toString(
                            requireObjectCoercible(cx, thisObj, CLASS_NAME, "normalize")),
                    Normalizer.Form.NFC);
        }

        final String formStr = ScriptRuntime.toString(args, 0);

        final Normalizer.Form form;
        if (Normalizer.Form.NFD.name().equals(formStr)) form = Normalizer.Form.NFD;
        else if (Normalizer.Form.NFKC.name().equals(formStr)) form = Normalizer.Form.NFKC;
        else if (Normalizer.Form.NFKD.name().equals(formStr)) form = Normalizer.Form.NFKD;
        else if (Normalizer.Form.NFC.name().equals(formStr)) form = Normalizer.Form.NFC;
        else
            throw rangeError(
                    "The normalization form should be one of 'NFC', 'NFD', 'NFKC', 'NFKD'.");

        return Normalizer.normalize(
                ScriptRuntime.toString(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "normalize")),
                form);
    }

    private static String js_repeat(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String str =
                ScriptRuntime.toString(requireObjectCoercible(cx, thisObj, CLASS_NAME, "repeat"));
        double cnt = ScriptRuntime.toInteger(args, 0);

        if ((cnt < 0.0) || (cnt == Double.POSITIVE_INFINITY)) {
            throw rangeError("Invalid count value");
        }

        if (cnt == 0.0 || str.isEmpty()) {
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
            retval.append(retval, 0, str.length() * (icnt - i));
        }

        return retval.toString();
    }

    private static Object js_codePointAt(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String str =
                ScriptRuntime.toString(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "codePointAt"));
        double cnt = ScriptRuntime.toInteger(args, 0);
        return (cnt < 0 || cnt >= str.length()) ? Undefined.instance : str.codePointAt((int) cnt);
    }

    /**
     * @see <a
     *     href="https://www.ecma-international.org/ecma-262/8.0/#sec-string.prototype.padstart">21.1.3.14String.prototype.padStart(maxLength[,
     *     fillString])</a>
     * @see <a
     *     href="https://www.ecma-international.org/ecma-262/8.0/#sec-string.prototype.padend">21.1.3.13String.prototype.padEnd(maxLength[,
     *     fillString])</a>
     */
    private static String pad(
            Context cx, Scriptable thisObj, String functionName, Object[] args, boolean atStart) {
        String pad =
                ScriptRuntime.toString(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, functionName));
        long intMaxLength = ScriptRuntime.toLength(args, 0);
        if (intMaxLength <= pad.length()) {
            return pad;
        }

        String filler = " ";
        if (args.length >= 2 && !Undefined.isUndefined(args[1])) {
            filler = ScriptRuntime.toString(args[1]);
            if (filler.isEmpty()) {
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

    private static Object js_padStart(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return pad(cx, thisObj, "padStart", args, true);
    }

    private static Object js_padEnd(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return pad(cx, thisObj, "padEnd", args, false);
    }

    /**
     *
     *
     * <h1>String.raw (template, ...substitutions)</h1>
     *
     * <p>22.1.2.4 String.raw [Draft ECMA-262 / April 28, 2021]
     */
    private static Object js_raw(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
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

    private static Object js_isWellFormed(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        CharSequence str =
                ScriptRuntime.toCharSequence(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "isWellFormed"));
        int len = str.length();
        boolean foundLeadingSurrogate = false;
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (NativeJSON.isLeadingSurrogate(c)) {
                if (foundLeadingSurrogate) {
                    return false;
                }
                foundLeadingSurrogate = true;
            } else if (NativeJSON.isTrailingSurrogate(c)) {
                if (!foundLeadingSurrogate) {
                    return false;
                }
                foundLeadingSurrogate = false;
            } else if (foundLeadingSurrogate) {
                return false;
            }
        }
        return !foundLeadingSurrogate;
    }

    private static Object js_toWellFormed(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        CharSequence str =
                ScriptRuntime.toCharSequence(
                        requireObjectCoercible(cx, thisObj, CLASS_NAME, "toWellFormed"));
        // true represents a surrogate pair
        // false represents a singular surrogate
        // normal characters aren't present
        Map<Integer, Boolean> surrogates = new HashMap<>();

        int len = str.length();
        char prev = 0;
        int firstSurrogateIndex = -1;
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);

            if (NativeJSON.isLeadingSurrogate(prev) && NativeJSON.isTrailingSurrogate(c)) {
                surrogates.put(i - 1, Boolean.TRUE);
                surrogates.put(i, Boolean.TRUE);
            } else if (NativeJSON.isLeadingSurrogate(c) || NativeJSON.isTrailingSurrogate(c)) {
                surrogates.put(i, Boolean.FALSE);
                if (firstSurrogateIndex == -1) {
                    firstSurrogateIndex = i;
                }
            }

            prev = c;
        }

        if (surrogates.isEmpty()) {
            return str.toString();
        }

        StringBuilder sb = new StringBuilder(str.subSequence(0, firstSurrogateIndex));
        for (int i = firstSurrogateIndex; i < len; i++) {
            char c = str.charAt(i);
            Boolean pairOrNormal = surrogates.get(i);
            if (pairOrNormal == null || pairOrNormal) {
                sb.append(c);
            } else {
                sb.append('\uFFFD');
            }
        }

        return sb.toString();
    }

    private static Object js_bold(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "bold", "b", null, args);
    }

    private static Object js_italics(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "italics", "i", null, args);
    }

    private static Object js_fixed(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "fixed", "tt", null, args);
    }

    private static Object js_strike(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "strike", "strike", null, args);
    }

    private static Object js_small(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "small", "small", null, args);
    }

    private static Object js_big(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "big", "big", null, args);
    }

    private static Object js_blink(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "blink", "blink", null, args);
    }

    private static Object js_sup(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "sup", "sup", null, args);
    }

    private static Object js_sub(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "sub", "sub", null, args);
    }

    private static Object js_fontsize(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "fontsize", "font", "size", args);
    }

    private static Object js_fontcolor(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "fontcolor", "font", "color", args);
    }

    private static Object js_link(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "link", "a", "href", args);
    }

    private static Object js_anchor(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return tagify(cx, thisObj, "anchor", "a", "name", args);
    }
}
