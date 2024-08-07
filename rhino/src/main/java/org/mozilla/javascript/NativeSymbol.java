/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.HashMap;
import java.util.Map;

/**
 * This is an implementation of the standard "Symbol" type that implements all of its weird
 * properties. One of them is that some objects can have an "internal data slot" that makes them a
 * Symbol and others cannot.
 */
public class NativeSymbol extends ScriptableObject implements Symbol {
    private static final long serialVersionUID = -589539749749830003L;

    public static final String CLASS_NAME = "Symbol";
    public static final String TYPE_NAME = "symbol";

    private static final Object GLOBAL_TABLE_KEY = new Object();
    private static final Object CONSTRUCTOR_SLOT = new Object();

    private final SymbolKey key;
    private final NativeSymbol symbolData;

    public static void init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor ctor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        0,
                        0, // Unused
                        NativeSymbol::js_constructor) {
                    // Calling new Symbol('foo') is not allowed. However, we need to be able to
                    // create
                    // a new symbol instance to register built-in ones and for the implementation of
                    // "for",
                    // so we have this trick of a thread-local variable to allow it.
                    @Override
                    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
                        if (cx.getThreadLocal(CONSTRUCTOR_SLOT) == null) {
                            throw ScriptRuntime.typeErrorById("msg.no.new", getFunctionName());
                        }
                        return (Scriptable) call(cx, scope, null, args);
                    }

                    // Calling Symbol('foo') should act like a constructor call
                    @Override
                    public Object call(
                            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                        Scriptable obj = targetConstructor.construct(cx, scope, args);
                        obj.setPrototype(getClassPrototype());
                        obj.setParentScope(scope);
                        return obj;
                    }
                };

        ctor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

        ctor.defineConstructorMethod(
                scope, "for", 1, NativeSymbol::js_for, DONTENUM, DONTENUM | READONLY);
        ctor.defineConstructorMethod(
                scope, "keyFor", 1, NativeSymbol::js_keyFor, DONTENUM, DONTENUM | READONLY);

        ctor.definePrototypeMethod(
                scope, "toString", 0, NativeSymbol::js_toString, DONTENUM, DONTENUM | READONLY);
        ctor.definePrototypeMethod(
                scope, "valueOf", 0, NativeSymbol::js_valueOf, DONTENUM, DONTENUM | READONLY);
        ctor.definePrototypeMethod(
                scope,
                SymbolKey.TO_PRIMITIVE,
                1,
                NativeSymbol::js_valueOf,
                DONTENUM | READONLY,
                DONTENUM | READONLY);
        ctor.definePrototypeProperty(SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);
        ctor.definePrototypeProperty(
                cx, "description", NativeSymbol::js_description, DONTENUM | READONLY);

        ScriptableObject.defineProperty(scope, CLASS_NAME, ctor, DONTENUM);

        cx.putThreadLocal(CONSTRUCTOR_SLOT, Boolean.TRUE);
        try {
            createStandardSymbol(cx, scope, ctor, "iterator", SymbolKey.ITERATOR);
            createStandardSymbol(cx, scope, ctor, "species", SymbolKey.SPECIES);
            createStandardSymbol(cx, scope, ctor, "toStringTag", SymbolKey.TO_STRING_TAG);
            createStandardSymbol(cx, scope, ctor, "hasInstance", SymbolKey.HAS_INSTANCE);
            createStandardSymbol(
                    cx, scope, ctor, "isConcatSpreadable", SymbolKey.IS_CONCAT_SPREADABLE);
            createStandardSymbol(cx, scope, ctor, "isRegExp", SymbolKey.IS_REGEXP);
            createStandardSymbol(cx, scope, ctor, "toPrimitive", SymbolKey.TO_PRIMITIVE);
            createStandardSymbol(cx, scope, ctor, "match", SymbolKey.MATCH);
            createStandardSymbol(cx, scope, ctor, "replace", SymbolKey.REPLACE);
            createStandardSymbol(cx, scope, ctor, "search", SymbolKey.SEARCH);
            createStandardSymbol(cx, scope, ctor, "split", SymbolKey.SPLIT);
            createStandardSymbol(cx, scope, ctor, "unscopables", SymbolKey.UNSCOPABLES);
        } finally {
            cx.removeThreadLocal(CONSTRUCTOR_SLOT);
        }

        if (sealed) {
            // Can't seal until we have created all the stuff above!
            ctor.sealObject();
        }
    }

    /**
     * This has to be used only for constructing the prototype instance. This sets symbolData to
     * null (see isSymbol() for more).
     *
     * @param desc the description
     */
    private NativeSymbol(String desc) {
        this.key = new SymbolKey(desc);
        this.symbolData = null;
    }

    NativeSymbol(SymbolKey key) {
        this.key = key;
        this.symbolData = this;
    }

    public NativeSymbol(NativeSymbol s) {
        this.key = s.key;
        this.symbolData = s.symbolData;
    }

    /**
     * Use this when we need to create symbols internally because of the convoluted way we have to
     * construct them.
     */
    public static NativeSymbol construct(Context cx, Scriptable scope, Object[] args) {
        cx.putThreadLocal(CONSTRUCTOR_SLOT, Boolean.TRUE);
        try {
            return (NativeSymbol) cx.newObject(scope, CLASS_NAME, args);
        } finally {
            cx.removeThreadLocal(CONSTRUCTOR_SLOT);
        }
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static void createStandardSymbol(
            Context cx, Scriptable scope, LambdaConstructor ctor, String name, SymbolKey key) {
        Scriptable sym = (Scriptable) ctor.call(cx, scope, scope, new Object[] {name, key});
        ctor.defineProperty(name, sym, DONTENUM | READONLY | PERMANENT);
    }

    private static NativeSymbol getSelf(Context cx, Scriptable scope, Object thisObj) {
        try {
            return (NativeSymbol) ScriptRuntime.toObject(cx, scope, thisObj);
        } catch (ClassCastException cce) {
            throw ScriptRuntime.typeErrorById("msg.invalid.type", thisObj.getClass().getName());
        }
    }

    private static NativeSymbol js_constructor(Context cx, Scriptable scope, Object[] args) {
        String desc = null;
        if (args.length > 0 && !Undefined.isUndefined(args[0])) {
            desc = ScriptRuntime.toString(args[0]);
        }

        if (args.length > 1) {
            return new NativeSymbol((SymbolKey) args[1]);
        }

        return new NativeSymbol(new SymbolKey(desc));
    }

    private static String js_toString(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return getSelf(cx, scope, thisObj).toString();
    }

    private static Object js_valueOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // In the case that "Object()" was called we actually have a different "internal slot"
        return getSelf(cx, scope, thisObj).symbolData;
    }

    private static Object js_description(Scriptable thisObj) {
        NativeSymbol self = LambdaConstructor.convertThisObject(thisObj, NativeSymbol.class);
        return self.getKey().getDescription();
    }

    private static Object js_for(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String name =
                (args.length > 0
                        ? ScriptRuntime.toString(args[0])
                        : ScriptRuntime.toString(Undefined.instance));

        Map<String, NativeSymbol> table = getGlobalMap(scope);
        NativeSymbol ret = table.get(name);

        if (ret == null) {
            ret = construct(cx, scope, new Object[] {name});
            table.put(name, ret);
        }
        return ret;
    }

    @SuppressWarnings("ReferenceEquality")
    private static Object js_keyFor(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object s = (args.length > 0 ? args[0] : Undefined.instance);
        if (!(s instanceof NativeSymbol)) {
            throw ScriptRuntime.throwCustomError(cx, scope, "TypeError", "Not a Symbol");
        }
        NativeSymbol sym = (NativeSymbol) s;

        Map<String, NativeSymbol> table = getGlobalMap(scope);
        for (Map.Entry<String, NativeSymbol> e : table.entrySet()) {
            if (e.getValue().key == sym.key) {
                return e.getKey();
            }
        }
        return Undefined.instance;
    }

    @Override
    public String toString() {
        return key.toString();
    }

    // Symbol objects have a special property that one cannot add properties.

    private static boolean isStrictMode() {
        final Context cx = Context.getCurrentContext();
        return (cx != null) && cx.isStrictMode();
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if (!isSymbol()) {
            super.put(name, start, value);
        } else if (isStrictMode()) {
            throw ScriptRuntime.typeErrorById("msg.no.assign.symbol.strict");
        }
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        if (!isSymbol()) {
            super.put(index, start, value);
        } else if (isStrictMode()) {
            throw ScriptRuntime.typeErrorById("msg.no.assign.symbol.strict");
        }
    }

    @Override
    public void put(Symbol key, Scriptable start, Object value) {
        if (!isSymbol()) {
            super.put(key, start, value);
        } else if (isStrictMode()) {
            throw ScriptRuntime.typeErrorById("msg.no.assign.symbol.strict");
        }
    }

    /**
     * Object() on a Symbol constructs an object which is NOT a symbol, but which has an "internal
     * data slot" that is. Furthermore, such an object has the Symbol prototype so this particular
     * object is still used. Account for that here: an "Object" that was created from a Symbol has a
     * different value of the slot.
     */
    @SuppressWarnings("ReferenceEquality")
    public boolean isSymbol() {
        return (symbolData == this);
    }

    @Override
    public String getTypeOf() {
        return (isSymbol() ? TYPE_NAME : super.getTypeOf());
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object x) {
        return key.equals(x);
    }

    SymbolKey getKey() {
        return key;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, NativeSymbol> getGlobalMap(Scriptable scope) {
        ScriptableObject top = (ScriptableObject) getTopLevelScope(scope);
        Map<String, NativeSymbol> map =
                (Map<String, NativeSymbol>) top.getAssociatedValue(GLOBAL_TABLE_KEY);
        if (map == null) {
            map = new HashMap<>();
            top.associateValue(GLOBAL_TABLE_KEY, map);
        }
        return map;
    }
}
