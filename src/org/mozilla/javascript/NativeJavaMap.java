/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * <code>NativeJavaMap</code> is a wrapper for java objects implementing <code>java.util.Map
 * </code> interface. When {@link Context#FEATURE_ENABLE_JAVA_MAP_ACCESS} is enabled, property based
 * access like <code>map[key]</code> is delegated to {@link Map#get(Object)} or {@link
 * Map#put(Object, Object)} operations so that a <code>JavaMap</code> acts very similar to a
 * javascript <code>Object</code> There is also an iterator to iterate over entries with <code>
 * for .. of</code>.
 *
 * <p><b>Limitations:</b> The wrapped map should have <code>String</code> or <code>Integer</code> as
 * key. Otherwise, property based access may not work properly.
 */
public class NativeJavaMap extends NativeJavaObject {

    private static final long serialVersionUID = 46513864372878618L;

    private final Map<Object, Object> map;

    private final Class<?> keyType;

    private final Class<?> valueType;

    static void init(ScriptableObject scope, boolean sealed) {
        NativeJavaMapIterator.init(scope, sealed);
    }

    @SuppressWarnings("unchecked")
    public NativeJavaMap(Scriptable scope, Object map, Type staticType) {
        super(scope, map, staticType);
        assert map instanceof Map;
        this.map = (Map<Object, Object>) map;
        Type[] types = JavaTypes.lookupType(scope, map.getClass(), staticType, Map.class);
        this.keyType = types == null ? Object.class : JavaTypes.getRawType(types[0]);
        this.valueType = types == null ? Object.class : JavaTypes.getRawType(types[1]);
    }

    @Override
    public String getClassName() {
        return "JavaMap";
    }

    @Override
    public boolean has(String name, Scriptable start) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            Object key = toKey(name);
            if (key != null && map.containsKey(key)) {
                return true;
            }
        }
        return super.has(name, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            Object key = toKey(index);
            if (key != null && map.containsKey(key)) {
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
            Object key = toKey(name);
            if (key != null && map.containsKey(key)) {
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
            Object key = toKey(index);
            if (key != null && map.containsKey(key)) {
                Object obj = map.get(key);
                return cx.getWrapFactory().wrap(cx, this, obj, obj == null ? null : obj.getClass());
            }
            return Scriptable.NOT_FOUND; // do not report "memberNotFound" in this case.
        }
        return super.get(index, start);
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
            Object key = toKey(name);
            if (key == null) {
                reportConversionError(name, keyType);
            }
            map.put(key, Context.jsToJava(value, valueType));
        } else {
            super.put(name, start, value);
        }
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        Context cx = Context.getContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            Object key = toKey(Integer.valueOf(index));
            if (key == null) {
                reportConversionError(Integer.valueOf(index), keyType);
            }
            map.put(key, Context.jsToJava(value, valueType));
        } else {
            super.put(index, start, value);
        }
    }

    @Override
    public Object[] getIds() {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            List<Object> ids = new ArrayList<>(map.size());
            for (Object key : map.keySet()) {
                if (key instanceof Integer) {
                    ids.add(key);
                } else {
                    ids.add(ScriptRuntime.toString(key));
                }
            }
            return ids.toArray();
        }
        return super.getIds();
    }

    public static final Map<Class<?>, Function<String, ? extends Object>> STRING_CONVERTERS =
            new LinkedHashMap<>();
    public static final Map<Class<?>, IntFunction<? extends Object>> INT_CONVERTERS =
            new LinkedHashMap<>();

    {
        STRING_CONVERTERS.put(String.class, Function.identity());
        STRING_CONVERTERS.put(Integer.class, Integer::valueOf);
        STRING_CONVERTERS.put(Long.class, Long::valueOf);
        STRING_CONVERTERS.put(Double.class, Double::valueOf);

        INT_CONVERTERS.put(String.class, Integer::toString);
        INT_CONVERTERS.put(Integer.class, Integer::valueOf);
        INT_CONVERTERS.put(Long.class, Long::valueOf);
        INT_CONVERTERS.put(Double.class, Double::valueOf);
    }
    /**
     * Converts the key, which is either as String or an Integer to the `keyType`.
     *
     * <p>When accessing java lists with javascript notation like <code>var x = map[42]</code> or
     * <code>var x = map['key']</code>, the key could be either a string or an integer. There are
     * cases where you do not have a <code>Map&lt;String,  ?&gt;></code> or <code>
     * Map&lt;Integer,  ?&gt;></code> but a <code>Map&lt;Long,  ?&gt;></code>. In this case, it is
     * impossible to access the map value with index based access.
     *
     * <p>The default implementation can handle maps, when key is either an Enum (EnumMap), String,
     * Integer, Long or Double. You may add additional converters, e.g. with <code>
     * STRING_CONVERTERS.put(UUID.class, UUID::fromString)</code>, then you can also use maps, that
     * has UUIDs as key.
     *
     * <p>Note 1: Adding new converters is not synchronized
     *
     * <p>Note 2: This conversion takes only place, when <code>FEATURE_ENABLE_JAVA_MAP_ACCESS</code>
     * is set in context.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    protected Object toKey(Object key) {
        try {
            if (key instanceof String) {
                // 1. if we have an enum, try to convert the value
                if (keyType.isEnum()) return Enum.valueOf((Class) keyType, (String) key);

                Function<String, ? extends Object> converter = STRING_CONVERTERS.get(keyType);
                if (converter != null) {
                    return converter.apply((String) key);
                } else {
                    return findStringKey(key);
                }

            } else { // could be either String or Integer
                int index = ((Integer) key).intValue();
                IntFunction<? extends Object> converter = INT_CONVERTERS.get(keyType);
                if (converter != null) {
                    return converter.apply(index);
                } else {
                    return findIndexKey(index);
                }
            }
        } catch (IllegalArgumentException ex) {
            return null; // cannot convert key
        }
    }

    protected Object findStringKey(Object key) {
        for (Function<String, ? extends Object> converter : STRING_CONVERTERS.values()) {
            try {
                Object testKey = converter.apply((String) key);
                if (map.containsKey(testKey)) return testKey;
            } catch (IllegalArgumentException ex) {
            }
        }
        return key;
    }

    protected Object findIndexKey(int index) {
        for (IntFunction<? extends Object> converter : INT_CONVERTERS.values()) {
            try {
                Object testKey = converter.apply(index);
                if (map.containsKey(testKey)) return testKey;
            } catch (IllegalArgumentException ex) {
            }
        }
        return Integer.valueOf(index);
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
