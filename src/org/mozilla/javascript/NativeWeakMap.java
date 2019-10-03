/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.WeakHashMap;

/**
 * This is an implementation of the ES6 WeakMap class. As per the spec, keys must be
 * ordinary objects. Since there is no defined "equality" for objects, comparisions
 * are done strictly by object equality. Both ES6 and the java.util.WeakHashMap class
 * have the same basic structure -- entries are removed automatically when the sole
 * remaining reference to the key is a weak reference. Therefore, we can use
 * WeakHashMap as the basis of this implementation and preserve the same semantics.
 */
public class NativeWeakMap extends IdScriptableObject {
    private static final long serialVersionUID = 8670434366883930453L;
    
    private static final Object MAP_TAG = "WeakMap";
    
    private boolean instanceOfWeakMap = false;
    
    private transient WeakHashMap<Scriptable, Object> map = new WeakHashMap<>();
    
    private static final Object NULL_VALUE = new Object();
    
    static void init(Scriptable scope, boolean sealed) {
        NativeWeakMap m = new NativeWeakMap();
        m.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    @Override
    public String getClassName() {
        return "WeakMap";
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
        Scriptable thisObj, Object[] args) {

        if (!f.hasTag(MAP_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case Id_constructor:
                if (thisObj == null) {
                    NativeWeakMap nm = new NativeWeakMap();
                    nm.instanceOfWeakMap = true;
                    if (args.length > 0) {
                        NativeMap.loadFromIterable(cx, scope, nm, args[0]);
                    }
                    return nm;
                }
                throw ScriptRuntime.typeError1("msg.no.new", "WeakMap");
            case Id_delete:
                return realThis(thisObj, f).js_delete(args.length > 0 ? args[0] : Undefined.instance);
            case Id_get:
                return realThis(thisObj, f).js_get(args.length > 0 ? args[0] : Undefined.instance);
            case Id_has:
                return realThis(thisObj, f).js_has(args.length > 0 ? args[0] : Undefined.instance);
            case Id_set:
                return realThis(thisObj, f).js_set(
                    args.length > 0 ? args[0] : Undefined.instance,
                    args.length > 1 ? args[1] : Undefined.instance);
        }
        throw new IllegalArgumentException("WeakMap.prototype has no method: " + f.getFunctionName());
    }

    private Object js_delete(Object key) {
        if (!ScriptRuntime.isObject(key)) {
            return false;
        }
        final Object oldVal = map.remove(key);
        return (oldVal != null);
    }

    private Object js_get(Object key) {
        if (!ScriptRuntime.isObject(key)) {
            return Undefined.instance;
        }
        Object result = map.get(key);
        if (result == null) {
            return Undefined.instance;
        } else if (result == NULL_VALUE) {
            return null;
        }
        return result;
    }

    private Object js_has(Object key) {
        if (!ScriptRuntime.isObject(key)) {
            return false;
        }
        return map.containsKey(key);
    }

    private Object js_set(Object key, Object v) {
        // As the spec says, only a true "Object" can be the key to a WeakMap.
        // Use the default object equality here. ScriptableObject does not override
        // equals or hashCode, which means that in effect we are only keying on object identity.
        // This is all correct according to the ECMAscript spec.
        if (!ScriptRuntime.isObject(key)) {
            throw ScriptRuntime.typeError1("msg.arg.not.object", ScriptRuntime.typeof(key));
        }
        // Map.get() does not distinguish between "not found" and a null value. So,
        // replace true null here with a marker so that we can re-convert in "get".
        final Object value = (v == null ? NULL_VALUE : v);
        map.put((Scriptable)key, value);
        return this;
    }

    private NativeWeakMap realThis(Scriptable thisObj, IdFunctionObject f) {
        if (thisObj == null) {
            throw incompatibleCallError(f);
        }
        try {
            final NativeWeakMap nm = (NativeWeakMap)thisObj;
            if (!nm.instanceOfWeakMap) {
                // Check for "Map internal data tag"
                throw incompatibleCallError(f);
            }
            return nm;
        } catch (ClassCastException cce) {
            throw incompatibleCallError(f);
        }
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id == SymbolId_toStringTag) {
            initPrototypeValue(SymbolId_toStringTag, SymbolKey.TO_STRING_TAG,
                getClassName(), DONTENUM | READONLY);
            return;
        }

        String s, fnName = null;
        int arity;
        switch (id) {
            case Id_constructor:  arity = 0; s = "constructor"; break;
            case Id_delete:       arity = 1; s = "delete"; break;
            case Id_get:          arity = 1; s = "get"; break;
            case Id_has:          arity = 1; s = "has"; break;
            case Id_set:          arity = 2; s = "set"; break;
            default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(MAP_TAG, id, s, fnName, arity);
    }

    @Override
    protected int findPrototypeId(Symbol k)
    {
        if (SymbolKey.TO_STRING_TAG.equals(k)) {
            return SymbolId_toStringTag;
        }
        return 0;
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2018-08-24 15:27:45 PDT
        L0: { id = 0; String X = null; int c;
            int s_length = s.length();
            if (s_length==3) {
                c=s.charAt(0);
                if (c=='g') { if (s.charAt(2)=='t' && s.charAt(1)=='e') {id=Id_get; break L0;} }
                else if (c=='h') { if (s.charAt(2)=='s' && s.charAt(1)=='a') {id=Id_has; break L0;} }
                else if (c=='s') { if (s.charAt(2)=='t' && s.charAt(1)=='e') {id=Id_set; break L0;} }
            }
            else if (s_length==6) { X="delete";id=Id_delete; }
            else if (s_length==11) { X="constructor";id=Id_constructor; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_constructor = 1,
        Id_delete = 2,
        Id_get = 3,
        Id_has = 4,
        Id_set = 5,
        SymbolId_toStringTag = 6,
        MAX_PROTOTYPE_ID = SymbolId_toStringTag;

// #/string_id_map#

    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        map = new WeakHashMap<>();
    }
}
