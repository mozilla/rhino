/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    private static final long serialVersionUID = -3786257752907047381L;

    private final Map<Object, Object> map;

    private transient Class<?> keyType;

    private transient Class<?> valueType;

    static void init(ScriptableObject scope, boolean sealed) {
        NativeJavaMapIterator.init(scope, sealed);
    }

    @SuppressWarnings("unchecked")
    public NativeJavaMap(Scriptable scope, Object map, Type staticType) {
        super(scope, map, staticType);
        assert map instanceof Map;
        this.map = (Map<Object, Object>) map;
    }

    @Override
    protected void initMembers() {
        super.initMembers();
        this.keyType = typeResolver.resolve(Map.class, 0);
        this.valueType = typeResolver.resolve(Map.class, 1);
    }

    @Override
    public String getClassName() {
        return "JavaMap";
    }

    @Override
    public boolean has(String name, Scriptable start) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            if (map.containsKey(name)) {
                return true;
            }
        }
        return super.has(name, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            if (map.containsKey(Integer.valueOf(index)) || map.containsKey(String.valueOf(index))) {
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
            if (map.containsKey(name)) {
                Object obj = map.get(name);
                return cx.getWrapFactory().wrap(cx, this, obj, obj == null ? null : obj.getClass());
            }
        }
        return super.get(name, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            Object key = Integer.valueOf(index);
            if (map.containsKey(key)) {
                Object obj = map.get(key);
                return cx.getWrapFactory().wrap(cx, this, obj, obj == null ? null : obj.getClass());
            }
            key = String.valueOf(index); // try again with String
            if (map.containsKey(key)) {
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
            if (keyType.isAssignableFrom(String.class)) {
                map.put(name, Context.jsToJava(value, valueType));
            } else {
                reportConversionError(name, keyType);
            }
        } else {
            super.put(name, start, value);
        }
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        Context cx = Context.getContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            if (keyType.isAssignableFrom(Integer.class)) {
                map.put(Integer.valueOf(index), Context.jsToJava(value, valueType));
            } else if (keyType.isAssignableFrom(String.class)) {
                map.put(String.valueOf(index), Context.jsToJava(value, valueType));
            } else {
                reportConversionError(index, keyType);
            }
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
                } else if (key instanceof String) {
                    ids.add(key);
                } // else skip all other types, as you may not able to access them
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
            Map.Entry<Object, Object> e = iterator.next();
            Object key = e.getKey();
            Object value = e.getValue();
            WrapFactory wrapFactory = cx.getWrapFactory();
            key = wrapFactory.wrap(cx, this, key, key == null ? null : key.getClass());
            value = wrapFactory.wrap(cx, this, value, value == null ? null : value.getClass());

            return cx.newArray(scope, new Object[] {key, value});
        }

        @Override
        protected String getTag() {
            return ITERATOR_TAG;
        }

        private Iterator<Map.Entry<Object, Object>> iterator;
    }
}
