/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;


import java.lang.reflect.Array;
import java.util.List;
import java.util.Objects;

/**
 * This class reflects Java Lists into the JavaScript environment.
 *
 * @author Markus Sunela
 * @see NativeJavaClass
 * @see NativeJavaObject
 * @see NativeJavaPackage
 */

public class NativeJavaList
        extends NativeJavaObject
        implements SymbolScriptable {

    private static final long serialVersionUID = -924022554283675333L;

    /**
     * Implementation of the Array.includes method.
     */
    private static class IncludesMethod extends BaseFunction {
        private List list;

        IncludesMethod(List list) {
            this.list = list;
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            Object arg = args[0];
            if (arg instanceof Wrapper)
                arg = ((Wrapper)arg).unwrap();

            int start = 0;
            if (args.length > 1)
                start = ((Number)args[1]).intValue();

            if (start == 0)
                return list.contains(arg);
            else
                return list.subList(start, list.size()).contains(arg);
        }
    }

    @Override
    public String getClassName() {
        return "JavaList";
    }

    public static NativeJavaList wrap(Scriptable scope, Object array) {
        return new NativeJavaList(scope, array);
    }

    @Override
    public Object unwrap() {
        return list;
    }

    public NativeJavaList(Scriptable scope, Object list) {
        super(scope, list, list.getClass());
        Class<?> cl = list.getClass();
        if (!List.class.isAssignableFrom(cl)) {
            throw new RuntimeException("List expected");
        }
        this.list = (List) list;
        this.cls = cl;
    }

    @Override
    public boolean has(String id, Scriptable start) {
        return id.equals("length") || id.equals("includes") || super.has(id, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return 0 <= index && index < list.size();
    }

    @Override
    public boolean has(Symbol key, Scriptable start) {
        return SymbolKey.IS_CONCAT_SPREADABLE.equals(key);
    }

    @Override
    public Object get(String id, Scriptable start) {
        if (id.equals("length"))
            return Integer.valueOf(list.size());
        else if (id.equals("includes"))
            return new IncludesMethod(list);
        Object result = super.get(id, start);
        if (result == NOT_FOUND &&
                !ScriptableObject.hasProperty(getPrototype(), id))
        {
            throw Context.reportRuntimeError2(
                    "msg.java.member.not.found", list.getClass().getName(), id);
        }
        return result;
    }

    @Override
    public Object get(int index, Scriptable start) {
        if (0 <= index && index < list.size()) {
            Context cx = Context.getContext();
            Object obj = list.get(index);
            return cx.getWrapFactory().wrap(cx, this, obj, obj.getClass());
        }
        return Undefined.instance;
    }

    @Override
    public Object get(Symbol key, Scriptable start) {
        if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
            return true;
        }
        return Scriptable.NOT_FOUND;
    }

    @Override
    public void put(String id, Scriptable start, Object value) {
        // Ignore assignments to "length"--it's readonly.
        if (!id.equals("length"))
            throw Context.reportRuntimeError1(
                    "msg.java.array.member.not.found", id);
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        if (0 <= index && index < list.size()) {
            list.remove(index);
            list.add(index, Context.jsToJava(value, Object.class));
        }
        else {
            throw Context.reportRuntimeError2(
                    "msg.java.array.index.out.of.bounds", String.valueOf(index),
                    String.valueOf(list.size() - 1));
        }
    }

    @Override
    public void delete(Symbol key) {
        // All symbols are read-only
    }

    public boolean jsFunction_includes(Object obj) {
        return list.contains(obj);
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == null || hint == ScriptRuntime.StringClass)
            return list.toString();
        if (hint == ScriptRuntime.BooleanClass)
            return Boolean.TRUE;
        if (hint == ScriptRuntime.NumberClass)
            return ScriptRuntime.NaNobj;
        return this;
    }

    @Override
    public Object[] getIds() {
        Object[] result = new Object[list.size()];
        int i = list.size();
        while (--i >= 0)
            result[i] = Integer.valueOf(i);
        return result;
    }

    @Override
    public boolean hasInstance(Scriptable value) {
        if (!(value instanceof Wrapper))
            return false;
        Object instance = ((Wrapper)value).unwrap();
        return cls.isInstance(instance);
    }

    @Override
    public Scriptable getPrototype() {
        if (prototype == null) {
            prototype =
                    ScriptableObject.getArrayPrototype(this.getParentScope());
        }
        return prototype;
    }

    List list;
    Class<?> cls;
}
