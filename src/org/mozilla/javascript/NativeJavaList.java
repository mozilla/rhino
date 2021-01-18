/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class NativeJavaList extends NativeJavaObject {

    private static final long serialVersionUID = 6403865639690547921L;

    private List<Object> list;
    
    private Class<?> valueType;

    @SuppressWarnings("unchecked")
    public NativeJavaList(Scriptable scope, Object list, Type staticType) {
        super(scope, list, staticType);
        assert list instanceof List;
        this.list = (List<Object>) list;
        if (staticType == null) {
            staticType = list.getClass().getGenericSuperclass();
        }
        if (staticType instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType) staticType).getActualTypeArguments();
            // types[0] contains the T of 'List<T>'
            this.valueType = ScriptRuntime.getRawType(types[0]);
        } else {
            this.valueType = Object.class;
        }
    }

    @Override
    public String getClassName() {
        return "JavaList";
    }


    @Override
    public boolean has(int index, Scriptable start) {
        if (isWithValidIndex(index)) {
            return true;
        }
        return super.has(index, start);
    }

    @Override
    public boolean has(Symbol key, Scriptable start) {
        if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
            return true;
        }
        return super.has(key, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
        if (isWithValidIndex(index)) {
            Context cx = Context.getContext();
            Object obj = list.get(index);
            return cx.getWrapFactory().wrap(cx, this, obj, obj.getClass());
        }
        return Undefined.instance;
    }

    @Override
    public Object get(Symbol key, Scriptable start) {
        if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
            return Boolean.TRUE;
        }
        return super.get(key, start);
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        if (index >= 0) {
            ensureCapacity(index + 1);
            list.set(index, Context.jsToJava(value, valueType));
            return;
        }
        super.put(index, start, value);
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > list.size()) {
            if (list instanceof ArrayList) {
                ((ArrayList<?>) list).ensureCapacity(minCapacity);
            }
            while (minCapacity > list.size()) {
              list.add(null);
            }
        }
    }

    @Override
    public Object[] getIds() {
        List<?> list = (List<?>) javaObject;
        Object[] result = new Object[list.size()];
        int i = list.size();
        while (--i >= 0) {
            result[i] = Integer.valueOf(i);
        }
        return result;
    }

    private boolean isWithValidIndex(int index) {
        return index >= 0  && index < list.size();
    }
}
