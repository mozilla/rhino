/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

/**
 * <code>NativeJavaMap</code> is a wrapper for java objects implementing <code>java.util.Map
 * </code> interface. When {@link Context#FEATURE_ENABLE_JAVA_MAP_ACCESS} is enabled, property-based
 * access like <code>map[key]</code> is delegated to {@link Map#get(Object)} or {@link
 * Map#put(Object, Object)} operations so that a <code>JavaMap</code> acts very similar to a
 * javascript <code>Object</code> There is also an iterator to iterate over entries with <code>
 * for .. of</code>.
 *
 * <p><b>Limitations:</b> The wrapped map should have <code>String</code> or <code>Integer</code> as
 * key. Otherwise, property-based access may not work properly.
 */
public class NativeJavaMap extends NativeJavaObject {

    private static final long serialVersionUID = -3786257752907047381L;

    private final Map<Object, Object> map;
    private final TypeInfo keyType;
    private final TypeInfo valueType;

    static void init(ScriptableObject scope, boolean sealed) {
        NativeJavaMapIterator.init(scope, sealed);
    }

    @SuppressWarnings("unchecked")
    public NativeJavaMap(Scriptable scope, Object map, TypeInfo staticType) {
        super(scope, map, staticType);
        assert map instanceof Map;
        this.map = (Map<Object, Object>) map;

        var typeFactory = TypeInfoFactory.getOrElse(scope, TypeInfoFactory.GLOBAL);
        this.keyType = typeFactory.consolidateType(MapTypeVariables.K, staticType);
        this.valueType = typeFactory.consolidateType(MapTypeVariables.V, staticType);
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
            var key = Integer.valueOf(index);
            if (map.containsKey(key)) {
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
                return cx.getWrapFactory().wrap(cx, this, map.get(name), valueType);
            }
        }
        return super.get(name, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            var key = Integer.valueOf(index);
            if (map.containsKey(key)) {
                return cx.getWrapFactory().wrap(cx, this, map.get(key), valueType);
            }
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
            map.put(name, Context.jsToJava(value, valueType));
        } else {
            super.put(name, start, value);
        }
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        Context cx = Context.getCurrentContext();
        if (cx != null && cx.hasFeature(Context.FEATURE_ENABLE_JAVA_MAP_ACCESS)) {
            map.put(index, Context.jsToJava(value, valueType));
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

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private static Callable symbol_iterator =
            (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> {
                if (!(thisObj instanceof NativeJavaMap)) {
                    throw ScriptRuntime.typeErrorById("msg.incompat.call", SymbolKey.ITERATOR);
                }
                return new NativeJavaMapIterator(scope, (NativeJavaMap) thisObj);
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

        NativeJavaMapIterator(Scriptable scope, NativeJavaMap map) {
            super(scope, ITERATOR_TAG);
            this.iterator = map.map.entrySet().iterator();
            this.keyType = map.keyType;
            this.valueType = map.valueType;
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
            key = wrapFactory.wrap(cx, this, key, keyType);
            value = wrapFactory.wrap(cx, this, value, valueType);

            return cx.newArray(scope, new Object[] {key, value});
        }

        @Override
        protected String getTag() {
            return ITERATOR_TAG;
        }

        private Iterator<Map.Entry<Object, Object>> iterator;
        private TypeInfo keyType;
        private TypeInfo valueType;
    }

    private interface MapTypeVariables {
        TypeInfo K = TypeInfoFactory.GLOBAL.create(Map.class.getTypeParameters()[0]);
        TypeInfo V = TypeInfoFactory.GLOBAL.create(Map.class.getTypeParameters()[1]);
    }
}
