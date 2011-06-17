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
 *   Norris Boyd
 *   Igor Bukanov
 *   Bob Jervis
 *   Mike McCabe
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

import org.mozilla.javascript.classy.ClassyScriptable;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class implements the Object native object.
 * See ECMA 15.2.
 * @author Norris Boyd
 */
public class NativeObject extends ClassyScriptable implements IdFunctionCall, Map
{
    static final long serialVersionUID = 1;

    private static final Object OBJECT_TAG = "Object.prototype";
    private static final Object OBJECT_STATIC_TAG = "Object";

    static void init(Scriptable scope, boolean sealed)
    {
        NativeObject obj = new NativeObject();
        IdFunctionObject ctor = null;
        for (Methods m : Methods.values()) {
            IdFunctionObject idfun = new IdFunctionObject(obj, OBJECT_TAG,
                    m.ordinal(), m.name(), m.arity, scope);
            if (m == Methods.constructor) {
                ctor = idfun;
                ctor.initFunction(obj.getClassName(), scope);
                ctor.markAsConstructor(obj);
                ctor.exportAsScopeProperty();
            } else {
                idfun.addAsProperty(obj);
            }
        }
        for (StaticMethods m : StaticMethods.values()) {
            IdFunctionObject idfun = new IdFunctionObject(obj, OBJECT_STATIC_TAG,
                    m.ordinal(), m.name(),  m.arity, scope);
            idfun.addAsProperty(ctor);
        }
    }

    @Override
    public String getClassName()
    {
        return "Object";
    }

    @Override
    public String toString()
    {
        return ScriptRuntime.defaultObjectToString(this);
    }

    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        int id = f.methodId();

        if (f.hasTag(OBJECT_TAG)) {
            Methods method = Methods.values()[id];

            switch (method) {
                case constructor: {
                    if (thisObj != null) {
                        // BaseFunction.construct will set up parent, proto
                        return f.construct(cx, scope, args);
                    }
                    if (args.length == 0 || args[0] == null
                            || args[0] == Undefined.instance)
                    {
                        return new NativeObject();
                    }
                    return ScriptRuntime.toObject(cx, scope, args[0]);
                }

                case toLocaleString: // For now just alias toString
                case toString: {
                    if (cx.hasFeature(Context.FEATURE_TO_STRING_AS_SOURCE)) {
                        String s = ScriptRuntime.defaultObjectToSource(cx, scope,
                                thisObj, args);
                        int L = s.length();
                        if (L != 0 && s.charAt(0) == '(' && s.charAt(L - 1) == ')') {
                            // Strip () that surrounds toSource
                            s = s.substring(1, L - 1);
                        }
                        return s;
                    }
                    return ScriptRuntime.defaultObjectToString(thisObj);
                }

                case valueOf:
                    return thisObj;

                case hasOwnProperty: {
                    boolean result;
                    if (args.length == 0) {
                        result = false;
                    } else {
                        String s = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
                        if (s == null) {
                            int index = ScriptRuntime.lastIndexResult(cx);
                            result = thisObj.has(index, thisObj);
                        } else {
                            result = thisObj.has(s, thisObj);
                        }
                    }
                    return ScriptRuntime.wrapBoolean(result);
                }

                case propertyIsEnumerable: {
                    boolean result;
                    if (args.length == 0) {
                        result = false;
                    } else {
                        String s = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
                        if (s == null) {
                            int index = ScriptRuntime.lastIndexResult(cx);
                            result = thisObj.has(index, thisObj);
                            if (result && thisObj instanceof ScriptableObject) {
                                ScriptableObject so = (ScriptableObject)thisObj;
                                int attrs = so.getAttributes(index);
                                result = ((attrs & ScriptableObject.DONTENUM) == 0);
                            }
                        } else {
                            result = thisObj.has(s, thisObj);
                            if (result && thisObj instanceof ScriptableObject) {
                                ScriptableObject so = (ScriptableObject)thisObj;
                                int attrs = so.getAttributes(s);
                                result = ((attrs & ScriptableObject.DONTENUM) == 0);
                            }
                        }
                    }
                    return ScriptRuntime.wrapBoolean(result);
                }

                case isPrototypeOf: {
                    boolean result = false;
                    if (args.length != 0 && args[0] instanceof Scriptable) {
                        Scriptable v = (Scriptable) args[0];
                        do {
                            v = v.getPrototype();
                            if (v == thisObj) {
                                result = true;
                                break;
                            }
                        } while (v != null);
                    }
                    return ScriptRuntime.wrapBoolean(result);
                }

                case toSource:
                    return ScriptRuntime.defaultObjectToSource(cx, scope, thisObj,
                            args);
                case __defineGetter__:
                case __defineSetter__:
                {
                    if (args.length < 2 || !(args[1] instanceof Callable)) {
                        Object badArg = (args.length >= 2 ? args[1]
                                : Undefined.instance);
                        throw ScriptRuntime.notFunctionError(badArg);
                    }
                    if (!(thisObj instanceof ScriptableObject)) {
                        throw Context.reportRuntimeError2(
                                "msg.extend.scriptable",
                                thisObj.getClass().getName(),
                                String.valueOf(args[0]));
                    }
                    ScriptableObject so = (ScriptableObject)thisObj;
                    String name = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
                    int index = (name != null ? 0
                            : ScriptRuntime.lastIndexResult(cx));
                    Callable getterOrSetter = (Callable)args[1];
                    boolean isSetter = (method == Methods.__defineSetter__);
                    so.setGetterOrSetter(name, index, getterOrSetter, isSetter);
                    if (so instanceof NativeArray)
                        ((NativeArray)so).setDenseOnly(false);
                }
                return Undefined.instance;

                case __lookupGetter__:
                case __lookupSetter__:
                {
                    if (args.length < 1 ||
                            !(thisObj instanceof ScriptableObject))
                        return Undefined.instance;

                    ScriptableObject so = (ScriptableObject)thisObj;
                    String name = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
                    int index = (name != null ? 0
                            : ScriptRuntime.lastIndexResult(cx));
                    boolean isSetter = (method == Methods.__lookupSetter__);
                    Object gs;
                    for (;;) {
                        gs = so.getGetterOrSetter(name, index, isSetter);
                        if (gs != null)
                            break;
                        // If there is no getter or setter for the object itself,
                        // how about the prototype?
                        Scriptable v = so.getPrototype();
                        if (v == null)
                            break;
                        if (v instanceof ScriptableObject)
                            so = (ScriptableObject)v;
                        else
                            break;
                    }
                    if (gs != null)
                        return gs;
                }
                return Undefined.instance;
            }
        }

        if (f.hasTag(OBJECT_STATIC_TAG)) {

            switch (StaticMethods.values()[id]) {
                case getPrototypeOf:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    Scriptable obj = ensureScriptable(arg);
                    return obj.getPrototype();
                }
                case keys:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    Scriptable obj = ensureScriptable(arg);
                    Object[] ids = obj.getIds();
                    for (int i = 0; i < ids.length; i++) {
                        ids[i] = ScriptRuntime.toString(ids[i]);
                    }
                    return cx.newArray(scope, ids);
                }
                case getOwnPropertyNames:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    ScriptableObject obj = ensureScriptableObject(arg);
                    Object[] ids = obj.getAllIds();
                    for (int i = 0; i < ids.length; i++) {
                        ids[i] = ScriptRuntime.toString(ids[i]);
                    }
                    return cx.newArray(scope, ids);
                }
                case getOwnPropertyDescriptor:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    // TODO(norris): There's a deeper issue here if
                    // arg instanceof Scriptable. Should we create a new
                    // interface to admit the new ECMAScript 5 operations?
                    ScriptableObject obj = ensureScriptableObject(arg);
                    Object nameArg = args.length < 2 ? Undefined.instance : args[1];
                    String name = ScriptRuntime.toString(nameArg);
                    Scriptable desc = obj.getOwnPropertyDescriptor(cx, name);
                    return desc == null ? Undefined.instance : desc;
                }
                case defineProperty:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    ScriptableObject obj = ensureScriptableObject(arg);
                    Object name = args.length < 2 ? Undefined.instance : args[1];
                    Object descArg = args.length < 3 ? Undefined.instance : args[2];
                    ScriptableObject desc = ensureScriptableObject(descArg);
                    obj.defineOwnProperty(cx, name, desc);
                    return obj;
                }
                case isExtensible:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    ScriptableObject obj = ensureScriptableObject(arg);
                    return obj.isExtensible();
                }
                case preventExtensions:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    ScriptableObject obj = ensureScriptableObject(arg);
                    obj.preventExtensions();
                    return obj;
                }
                case defineProperties:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    ScriptableObject obj = ensureScriptableObject(arg);
                    Object propsObj = args.length < 2 ? Undefined.instance : args[1];
                    Scriptable props = Context.toObject(propsObj, getParentScope());
                    obj.defineOwnProperties(cx, ensureScriptableObject(props));
                    return obj;
                }
                case create:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    Scriptable obj = (arg == null) ? null : ensureScriptable(arg);

                    ScriptableObject newObject = new NativeObject();
                    newObject.setParentScope(this.getParentScope());
                    newObject.setPrototype(obj);

                    if (args.length > 1 && args[1] != Undefined.instance) {
                        Scriptable props = Context.toObject(args[1], getParentScope());
                        newObject.defineOwnProperties(cx, ensureScriptableObject(props));
                    }

                    return newObject;
                }

                case isSealed:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    ScriptableObject obj = ensureScriptableObject(arg);

                    if (obj.isExtensible()) return false;

                    for (Object name: obj.getAllIds()) {
                        Object configurable = obj.getOwnPropertyDescriptor(cx, name).get("configurable");
                        if (Boolean.TRUE.equals(configurable))
                            return false;
                    }

                    return true;
                }
                case isFrozen:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    ScriptableObject obj = ensureScriptableObject(arg);

                    if (obj.isExtensible()) return false;

                    for (Object name: obj.getAllIds()) {
                        ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, name);
                        if (Boolean.TRUE.equals(desc.get("configurable")))
                            return false;
                        if (isDataDescriptor(desc) && Boolean.TRUE.equals(desc.get("writable")))
                            return false;
                    }

                    return true;
                }
                case seal:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    ScriptableObject obj = ensureScriptableObject(arg);

                    for (Object name: obj.getAllIds()) {
                        ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, name);
                        if (Boolean.TRUE.equals(desc.get("configurable"))) {
                            desc.put("configurable", desc, false);
                            obj.defineOwnProperty(cx, name, desc);
                        }
                    }
                    obj.preventExtensions();

                    return obj;
                }
                case freeze:
                {
                    Object arg = args.length < 1 ? Undefined.instance : args[0];
                    ScriptableObject obj = ensureScriptableObject(arg);

                    for (Object name: obj.getAllIds()) {
                        ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, name);
                        if (isDataDescriptor(desc) && Boolean.TRUE.equals(desc.get("writable")))
                            desc.put("writable", desc, false);
                        if (Boolean.TRUE.equals(desc.get("configurable")))
                            desc.put("configurable", desc, false);
                        obj.defineOwnProperty(cx, name, desc);
                    }
                    obj.preventExtensions();

                    return obj;
                }
            }
        }

        throw new IllegalArgumentException(String.valueOf(id));
    }

    // methods implementing java.util.Map

    public boolean containsKey(Object key) {
        if (key instanceof String) {
            return has((String) key, this);
        } else if (key instanceof Number) {
            return has(((Number) key).intValue(), this);
        }
        return false;
    }

    public boolean containsValue(Object value) {
        for (Object obj : values()) {
            if (value == obj ||
                    value != null && value.equals(obj)) {
                return true;
            }
        }
        return false;
    }

    public Object remove(Object key) {
        Object value = get(key);
        if (key instanceof String) {
            delete((String) key);
        } else if (key instanceof Number) {
            delete(((Number) key).intValue());
        }
        return value;
    }


    public Set<Object> keySet() {
        return new KeySet();
    }

    public Collection<Object> values() {
        return new ValueCollection();
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return new EntrySet();
    }

    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map m) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }


    class EntrySet extends AbstractSet<Entry<Object, Object>> {
        @Override
        public Iterator<Entry<Object, Object>> iterator() {
            return new Iterator<Map.Entry<Object, Object>>() {
                Object[] ids = getIds();
                Object key = null;
                int index = 0;

                public boolean hasNext() {
                    return index < ids.length;
                }

                public Map.Entry<Object, Object> next() {
                    final Object ekey = key = ids[index++];
                    final Object value = get(key);
                    return new Map.Entry<Object, Object>() {
                        public Object getKey() {
                            return ekey;
                        }

                        public Object getValue() {
                            return value;
                        }

                        public Object setValue(Object value) {
                            throw new UnsupportedOperationException();
                        }

                        public boolean equals(Object other) {
                            if (!(other instanceof Map.Entry)) {
                                return false;
                            }
                            Map.Entry e = (Map.Entry) other;
                            return (ekey == null ? e.getKey() == null : ekey.equals(e.getKey()))
                                && (value == null ? e.getValue() == null : value.equals(e.getValue()));
                        }

                        public int hashCode() {
                            return (ekey == null ? 0 : ekey.hashCode()) ^
                                   (value == null ? 0 : value.hashCode());
                        }

                        public String toString() {
                            return ekey + "=" + value;
                        }
                    };
                }

                public void remove() {
                    if (key == null) {
                        throw new IllegalStateException();
                    }
                    NativeObject.this.remove(key);
                    key = null;
                }
            };
        }

        @Override
        public int size() {
            return NativeObject.this.size();
        }
    }

    class KeySet extends AbstractSet<Object> {

        @Override
        public boolean contains(Object key) {
            return containsKey(key);
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                Object[] ids = getIds();
                Object key;
                int index = 0;

                public boolean hasNext() {
                    return index < ids.length;
                }

                public Object next() {
                    return (key = ids[index++]);
                }

                public void remove() {
                    if (key == null) {
                        throw new IllegalStateException();
                    }
                    NativeObject.this.remove(key);
                    key = null;
                }
           };
        }

        @Override
        public int size() {
            return NativeObject.this.size();
        }
    }

    class ValueCollection extends AbstractCollection<Object> {

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                Object[] ids = getIds();
                Object key;
                int index = 0;

                public boolean hasNext() {
                    return index < ids.length;
                }

                public Object next() {
                    return get((key = ids[index++]));
                }

                public void remove() {
                    if (key == null) {
                        throw new IllegalStateException();
                    }
                    NativeObject.this.remove(key);
                    key = null;
                }
            };
        }

        @Override
        public int size() {
            return NativeObject.this.size();
        }
    }

    enum StaticMethods {
        getPrototypeOf(1),
        keys(1),
        getOwnPropertyNames(1),
        getOwnPropertyDescriptor(2),
        defineProperty(3),
        isExtensible(1),
        preventExtensions(1),
        defineProperties(2),
        create(2),
        isSealed(1),
        isFrozen(1),
        seal(1),
        freeze(1);

        private final int arity;
        StaticMethods(int arity) {
            this.arity = arity;
        }
    }

    enum Methods {
        constructor(1),
        toString(0),
        toLocaleString(0),
        valueOf(0),
        hasOwnProperty(1),
        propertyIsEnumerable(1),
        isPrototypeOf(1),
        toSource(0),
        __defineGetter__(2),
        __defineSetter__(2),
        __lookupGetter__(1),
        __lookupSetter__(1);

        private final int arity;
        Methods(int arity) {
            this.arity = arity;
        }
    }

}
