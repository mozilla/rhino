/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;

public class NativeJavaList extends NativeJavaObject {
    private static final long serialVersionUID = 1L;
    
    private List<Object> list;

    @SuppressWarnings("unchecked")
    public NativeJavaList(Scriptable scope, Object list) {
        super(scope, list, list.getClass());
        assert list instanceof List;
        this.list = (List<Object>) list;
        setPrototype(ScriptableObject.getClassPrototype(scope, "Array"));
    }

    @Override
    public String getClassName() {
        return "JavaList";
    }


    @Override
    public boolean has(String name, Scriptable start) {
        if (name.equals("length")) {
            return true;
        }
        return super.has(name, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        if (isWithValidIndex(index)) {
            return true;
        }
        return super.has(index, start);
    }
    
    public void delete(int index) {
        if (isWithValidIndex(index)) {
            // TODO: what do we with "undefined" values?
            list.set(index, null);
        }
    }

    @Override
    public boolean has(Symbol key, Scriptable start) {
        if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
            return true;
        }
        return super.has(key, start);
    }

    @Override
    public Object get(String name, Scriptable start) {
        if ("length".equals(name)) {
            return Integer.valueOf(list.size());
        }
        return super.get(name, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
        if (isWithValidIndex(index)) {
            Context cx = Context.getContext();
            Object obj = list.get(index);
            if (obj == null) {
                return null;
            }
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
            if (value instanceof Wrapper) {
                value = ((Wrapper) value).unwrap();
            }
            list.set(index, value);
            return;
        }
        super.put(index, start, value);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if ("length".equals(name)) {
            setLength((Integer) Context.jsToJava(value, Integer.class));
        }
        super.put(name, start, value);
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
    
    
    private void setLength(int length) {
        if (length < 0) {
            throw Context.reportRuntimeErrorById(
                    "msg.java.array.index.out.of.bounds",
                    String.valueOf(length),
                    String.valueOf(list.size() - 1));
        }
        if (length < list.size()) {
            list.subList(length, list.size()).clear();
        } else {
            ensureCapacity(length);
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
