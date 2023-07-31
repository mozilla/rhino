/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>NativeJavaList</code> is a wrapper for java objects implementing <code>java.util.List
 * </code> interface. This wrapper delegates index based access in javascript (like <code>
 * value[x] = 3</code>) to the according {@link List#get(int)}, {@link List#set(int, Object)} and
 * {@link List#add(Object)} methods. This allows you to use java lists in many places like a
 * javascript <code>Array</code>.
 *
 * <p>Supported functions:
 *
 * <ul>
 *   <li>index based access is delegated to List.get/set/add. If <code>index &gt;= length</code>,
 *       the skipped elements will be filled with <code>null</code> values
 *   <li>iterator support with <code>for...of</code> (provided by NativeJavaObject for all
 *       iterables)
 *   <li>when iterating with <code>for .. in</code> (or <code>for each .. in</code>) then <code>
 *       getIds
 *       </code> + index based access is used.
 *   <li>reading and setting <code>length</code> property. When modifying the length property, the
 *       list is either truncated or will be filled with <code>null</code> values up to <code>length
 *       </code>
 *   <li>deleting entries: <code>delete value[index]</code> will be equivalent with <code>
 *       value[index] = null</code> and is implemented to provide array compatibility.
 * </ul>
 *
 * <b>Important:</b> JavaList does not support sparse arrays. So setting the length property to a
 * high value or writing to a high index may allocate a lot of memory.
 *
 * <p><b>Note:</b> Although <code>JavaList</code> looks like a javascript-<code>Array</code>, it is
 * not an <code>
 * Array</code>. Some methods behave very similar like <code>Array.indexOf</code> and <code>
 * java.util.List.indexOf</code>, others are named differently like <code>Array.includes</code> vs.
 * <code>java.util.List.contains</code>. Especially <code>forEach</code> is different in <code>Array
 * </code> and <code>java.util.List</code>. Also deleting entries will set entries to <code>null
 * </code> instead to <code>Undefined</code>
 */
public class NativeJavaList extends NativeJavaObject {

    private static final long serialVersionUID = 660285467829047519L;

    private List<Object> list;

    @SuppressWarnings("unchecked")
    public NativeJavaList(Scriptable scope, Object list) {
        super(scope, list, list.getClass());
        assert list instanceof List;
        this.list = (List<Object>) list;
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

    @Override
    public void delete(int index) {
        if (isWithValidIndex(index)) {
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
            Context cx = Context.getCurrentContext();
            Object obj = list.get(index);
            if (cx != null) {
                return cx.getWrapFactory().wrap(cx, this, obj, obj == null ? null : obj.getClass());
            }
            return obj;
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
            Object javaValue = Context.jsToJava(value, Object.class);
            if (index == list.size()) {
                list.add(javaValue); // use "add" at the end of list.
            } else {
                ensureCapacity(index + 1);
                list.set(index, javaValue);
            }
            return;
        }
        super.put(index, start, value);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if (list != null && "length".equals(name)) {
            setLength(value);
            return;
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

    private void setLength(Object val) {
        double d = ScriptRuntime.toNumber(val);
        long longVal = ScriptRuntime.toUint32(d);
        if (longVal != d || longVal > Integer.MAX_VALUE) {
            String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
            throw ScriptRuntime.rangeError(msg);
        }
        if (longVal < list.size()) {
            list.subList((int) longVal, list.size()).clear();
        } else {
            ensureCapacity((int) longVal);
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
        return index >= 0 && index < list.size();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
