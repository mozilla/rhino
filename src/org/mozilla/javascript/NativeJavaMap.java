/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class NativeJavaMap extends NativeJavaObject {
    
    private static final long serialVersionUID = 46513864372878618L;
    
    private Map<Object, Object> map;
    private Class<?> keyType;
    private Class<?> valueType;
    private transient Map<String, Object> keyTranslationMap;

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
        if (map.containsKey(toKey(name, false))) {
            return true;
        }
        return super.has(name, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        if (map.containsKey(toKey(index, false))) {
            return true;
        }
        return super.has(index, start);
    }

    @Override
    public Object get(String name, Scriptable start) {
        Object key = toKey(name, false);
        if (map.containsKey(key)) {
            Context cx = Context.getContext();
            Object obj = map.get(key);
            if (obj == null) {
                return null;
            }
            return cx.getWrapFactory().wrap(cx, this, obj, obj.getClass());
        }
        return super.get(name, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
      Object key = toKey(Integer.valueOf(index), false);
        if (map.containsKey(key)) {
            Context cx = Context.getContext();
            Object obj = map.get(key);
            if (obj == null) {
                return null;
            }
            return cx.getWrapFactory().wrap(cx, this, obj, obj.getClass());
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
    public void put(String name, Scriptable start, Object value) {
        map.put(toKey(name, true), toValue(value));
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        map.put(toKey(index, true), toValue(value));
    }

    @Override
    public Object unwrap() {
        // clear keyTranslationMap on unwrap, as native java code may modify the object now
        keyTranslationMap = null;
        return super.unwrap();
    }
    
    @Override
    public Object[] getIds() {
        Object[] ids = new Object[map.size()];
        int i = 0;
        for (Object key : map.keySet()) {
            if (key instanceof Number) {
                ids[i++] = (Number)key;
            } else {
                ids[i++] = ScriptRuntime.toString(key);
            }
        }
        return ids;
    }

}
