/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NativeJavaMap extends NativeJavaObject {

    private static final long serialVersionUID = 46513864372878618L;

    private Map<Object, Object> map;
    private Class<?> keyType;
    private Class<?> valueType;
    private transient Map<String, Object> keyTranslationMap;

    static void init(ScriptableObject scope, boolean sealed) {
        NativeJavaMapIterator.init(scope, sealed);
    }

    @SuppressWarnings("unchecked")
    public NativeJavaMap(Scriptable scope, Object map, Type staticType) {
        super(scope, map, staticType);
        assert map instanceof Map;
        this.map = (Map<Object, Object>) map;
        if (staticType == null) {
            staticType = map.getClass().getGenericSuperclass();
        }
        if (staticType instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType) staticType).getActualTypeArguments();
            this.keyType = ScriptRuntime.getRawType(types[0]);
            this.valueType = ScriptRuntime.getRawType(types[1]);
        } else {
            this.keyType = Object.class;
            this.valueType = Object.class;
        }
    }

    @Override
    public String getClassName() {
        return "JavaMap";
    }

    @Override
    public boolean has(String name, Scriptable start) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            if (map.containsKey(toKey(name, false))) {
                return true;
            }
        }
        return super.has(name, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            if (map.containsKey(toKey(index, false))) {
                return true;
            }
        }
        return super.has(index, start);
    }

    @Override
    public boolean has(Symbol key, Scriptable start) {
        if (SymbolKey.ITERATOR.equals(key)) {
            return true;
        }
        return false;
    }

    @Override
    public Object get(String name, Scriptable start) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            Object key = toKey(name, false);
            if (map.containsKey(key)) {
                Object obj = map.get(key);
                return cx.getWrapFactory().wrap(cx, this, obj, obj == null ? null : obj.getClass());
            }
        }
        return super.get(name, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            Object key = toKey(Integer.valueOf(index), false);
            if (map.containsKey(key)) {
                Object obj = map.get(key);
                return cx.getWrapFactory().wrap(cx, this, obj, obj == null ? null : obj.getClass());
            }
        }
        return super.get(index, start);
    }

    @SuppressWarnings("unchecked")
    private Object toKey(Object key, boolean translateNew) {
        if (keyType == String.class || map.containsKey(key)) {
            // fast exit, if we know, that there are only string keys in the map o
            return key;
        }
        String strKey = ScriptRuntime.toString(key);
        if (map.containsKey(strKey)) {
            // second fast exit, if the key is present as string.
            return strKey;
        }

        // TODO: There is no change detection yet. The keys in the wrapped map could theoretically
        // change though other java code. To reduce this risk, we clear the keyTranslationMap on
        // unwrap. An approach to track if the underlying map was changed may be to read the
        // 'modCount' property of HashMap, but this is not part of the Map interface.
        // So for now, wrapped maps must not be changed by external code.
        if (keyTranslationMap == null) {
            keyTranslationMap = new HashMap<>();
            map.keySet().forEach(k -> keyTranslationMap.put(ScriptRuntime.toString(k), k));
        }
        Object ret = keyTranslationMap.get(strKey);
        if (ret == null) {
            if (translateNew) {
                // we do not have the key, and we need a new one, (due PUT operation e.g.)
                if (keyType == Object.class) {
                    // if we do not know the keyType, just pass through the key
                    ret = key;
                } else if (Enum.class.isAssignableFrom(keyType)) {
                    // for enums use "valueOf" method
                    ret = Enum.valueOf((Class) keyType, strKey);
                } else {
                    // for all other use jsToJava (which might run into a conversionError)
                    ret = Context.jsToJava(key, keyType);
                }
                keyTranslationMap.put(strKey, ret);
            } else {
                ret = key;
            }
        }
        return ret;
    }

    private Object toValue(Object value) {
        if (valueType == Object.class) {
            return value;
        } else {
            return Context.jsToJava(value, valueType);
        }
    }

    @Override
    public Object get(Symbol key, Scriptable start) {
        if (SymbolKey.ITERATOR.equals(key)) {
            return symbol_iterator;
        }
        return super.get(key, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            map.put(toKey(name, true), toValue(value));
        } else {
            super.put(name, start, value);
        }
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        Context cx = Context.getContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            map.put(toKey(index, true), toValue(value));
        } else {
            super.put(index, start, value);
        }
    }

    @Override
    public Object unwrap() {
        // clear keyTranslationMap on unwrap, as native java code may modify the object now
        keyTranslationMap = null;
        return super.unwrap();
    }

    @Override
    public Object[] getIds() {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            List<Object> ids = new ArrayList<>(map.size());
            for (Object key : map.keySet()) {
                if (key instanceof Integer) {
                    ids.add((Integer) key);
                } else {
                    ids.add(ScriptRuntime.toString(key));
                }
            }
            return ids.toArray();
        }
        return super.getIds();
    }

    private static Callable symbol_iterator =
            (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> {
                if (!(thisObj instanceof NativeJavaMap)) {
                    throw ScriptRuntime.typeErrorById("msg.incompat.call", SymbolKey.ITERATOR);
                }
                return new NativeJavaMapIterator(scope, ((NativeJavaMap) thisObj).map);
            };

    private static final class NativeJavaMapIterator extends ES6Iterator {
        private static final long serialVersionUID = 1L;
        private static final String ITERATOR_TAG = "JavaMapIterator";

        static void init(ScriptableObject scope, boolean sealed) {
            ES6Iterator.init(scope, sealed, new NativeJavaMapIterator(), ITERATOR_TAG);
        }

        /** Only for constructing the prototype object. */
        private NativeJavaMapIterator() {
            super();
        }

        NativeJavaMapIterator(Scriptable scope, Map<Object, Object> map) {
            super(scope, ITERATOR_TAG);
            this.iterator = map.entrySet().iterator();
        }

        @Override
        public String getClassName() {
            return "Java Map Iterator";
        }

        @Override
        protected boolean isDone(Context cx, Scriptable scope) {
            return !iterator.hasNext();
        }

        @Override
        protected Object nextValue(Context cx, Scriptable scope) {
            if (!iterator.hasNext()) {
                return cx.newArray(scope, new Object[] {Undefined.instance, Undefined.instance});
            }
            Map.Entry e = iterator.next();
            return cx.newArray(scope, new Object[] {e.getKey(), e.getValue()});
        }

        @Override
        protected String getTag() {
            return ITERATOR_TAG;
        }

        private Iterator<Map.Entry<Object, Object>> iterator;
    }
}
