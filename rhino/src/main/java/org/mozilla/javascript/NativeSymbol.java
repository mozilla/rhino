/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.SymbolKey.TO_PRIMITIVE;
import static org.mozilla.javascript.SymbolKey.TO_STRING_TAG;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This is an implementation of the standard "Symbol" type that implements all of its weird
 * properties. One of them is that some objects can have an "internal data slot" that makes them a
 * Symbol and others cannot.
 */
public class NativeSymbol extends ScriptableObject implements Symbol {
    private static final long serialVersionUID = -589539749749830003L;

    public static final String CLASS_NAME = "Symbol";
    public static final String TYPE_NAME = "symbol";

    private final SymbolKey key;

    public static void init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor ctor =
                new LambdaConstructor(scope, CLASS_NAME, 0, NativeSymbol::js_constructorCall, null);

        ctor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

        ctor.defineConstructorMethod(scope, "for", 1, NativeSymbol::js_for);
        ctor.defineConstructorMethod(scope, "keyFor", 1, NativeSymbol::js_keyFor);

        ctor.definePrototypeMethod(scope, "toString", 0, NativeSymbol::js_toString);
        ctor.definePrototypeMethod(scope, "valueOf", 0, NativeSymbol::js_valueOf);
        ctor.definePrototypeMethod(
                scope, TO_PRIMITIVE, 1, NativeSymbol::js_valueOf, DONTENUM | READONLY);
        ctor.definePrototypeProperty(TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);
        ctor.definePrototypeProperty(cx, "description", NativeSymbol::js_description);

        ScriptableObject.defineProperty(scope, CLASS_NAME, ctor, DONTENUM);

        // Create all the predefined symbols and bind them to the scope.
        createStandardSymbol(scope, ctor, "iterator", SymbolKey.ITERATOR);
        createStandardSymbol(scope, ctor, "species", SymbolKey.SPECIES);
        createStandardSymbol(scope, ctor, "toStringTag", SymbolKey.TO_STRING_TAG);
        createStandardSymbol(scope, ctor, "hasInstance", SymbolKey.HAS_INSTANCE);
        createStandardSymbol(scope, ctor, "isConcatSpreadable", SymbolKey.IS_CONCAT_SPREADABLE);
        createStandardSymbol(scope, ctor, "isRegExp", SymbolKey.IS_REGEXP);
        createStandardSymbol(scope, ctor, "toPrimitive", SymbolKey.TO_PRIMITIVE);
        createStandardSymbol(scope, ctor, "match", SymbolKey.MATCH);
        createStandardSymbol(scope, ctor, "matchAll", SymbolKey.MATCH_ALL);
        createStandardSymbol(scope, ctor, "replace", SymbolKey.REPLACE);
        createStandardSymbol(scope, ctor, "search", SymbolKey.SEARCH);
        createStandardSymbol(scope, ctor, "split", SymbolKey.SPLIT);
        createStandardSymbol(scope, ctor, "unscopables", SymbolKey.UNSCOPABLES);

        if (sealed) {
            // Can't seal until we have created all the stuff above!
            ctor.sealObject();
        }
    }

    NativeSymbol(SymbolKey key) {
        this.key = key;
    }

    @Override
    public Symbol.Kind getKind() {
        return key.getKind();
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static void createStandardSymbol(
            Scriptable scope, LambdaConstructor ctor, String name, SymbolKey key) {
        ctor.defineProperty(name, key, DONTENUM | READONLY | PERMANENT);
    }

    private static NativeSymbol getSelf(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeSymbol.class);
    }

    private static SymbolKey js_constructorCall(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String desc;
        if (args.length > 0 && !Undefined.isUndefined(args[0])) {
            desc = ScriptRuntime.toString(args[0]);
        } else {
            desc = null;
        }
        return new SymbolKey(desc, Symbol.Kind.REGULAR);
    }

    private static String js_toString(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return getSelf(thisObj).toString();
    }

    private static Object js_valueOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return getSelf(thisObj).key;
    }

    private static Object js_description(Scriptable thisObj) {
        return getSelf(thisObj).getKey().getDescription();
    }

    private static Object js_for(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        String name =
                (args.length > 0
                        ? ScriptRuntime.toString(args[0])
                        : ScriptRuntime.toString(Undefined.instance));

        Map<String, SymbolKey> table = getGlobalMap();
        return table.computeIfAbsent(name, (k) -> new SymbolKey(k, Symbol.Kind.REGISTERED));
    }

    @SuppressWarnings("ReferenceEquality")
    private static Object js_keyFor(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object s = (args.length > 0 ? args[0] : Undefined.instance);
        SymbolKey sym;
        if (s instanceof NativeSymbol) {
            sym = ((NativeSymbol) s).key;
        } else if (s instanceof SymbolKey) {
            sym = (SymbolKey) s;
        } else {
            throw ScriptRuntime.throwCustomError(cx, scope, "TypeError", "Not a Symbol");
        }

        Map<String, SymbolKey> table = getGlobalMap();
        for (var e : table.entrySet()) {
            if (e.getValue() == sym) {
                return e.getKey();
            }
        }
        return Undefined.instance;
    }

    @Override
    public String toString() {
        return key.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return key.getName();
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
        return false;
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

    private static final Map<String, SymbolKey> globalMap =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Return the Map that stores global symbols for the 'for' and 'keyFor' operations. It must work
     * across "realms" in the same top-level Rhino scope, so we store it there as an associated
     * property.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, SymbolKey> getGlobalMap() {
        return globalMap;
    }
}
