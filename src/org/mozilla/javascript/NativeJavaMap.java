/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NativeJavaMap extends NativeJavaObject {

    private Map<Object, Object> map;

    @SuppressWarnings("unchecked")
    public NativeJavaMap(Scriptable scope, Object map) {
        super(scope, map, map.getClass());
        assert map instanceof Map;
        this.map = (Map<Object, Object>) map;
    }

    @Override
    public String getClassName() {
        return "JavaMap";
    }


    @Override
    public boolean has(String name, Scriptable start) {
        if (map.containsKey(name)) {
            return true;
        }
        return super.has(name, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        if (map.containsKey(Integer.valueOf(index))) {
            return true;
        }
        return super.has(index, start);
    }

    @Override
    public Object get(String name, Scriptable start) {
        if (map.containsKey(name)) {
            Context cx = Context.getContext();
            Object obj = map.get(name);
            return cx.getWrapFactory().wrap(cx, this, obj, obj.getClass());
        }
        return super.get(name, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
        if (map.containsKey(Integer.valueOf(index))) {
            Context cx = Context.getContext();
            Object obj = map.get(Integer.valueOf(index));
            return cx.getWrapFactory().wrap(cx, this, obj, obj.getClass());
        }
        return super.get(index, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        map.put(name, Context.jsToJava(value, Object.class));
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        map.put(Integer.valueOf(index), Context.jsToJava(value, Object.class));
    }

    @Override
    public Object[] getIds() {
        List<Object> ids = new ArrayList<>(map.size());
        for (Object key : map.keySet()) {
            if (key instanceof Integer) {
                ids.add((Integer)key);
            } else {
                ids.add(ScriptRuntime.toString(key));
            }
        }
        return ids.toArray();
    }
}
