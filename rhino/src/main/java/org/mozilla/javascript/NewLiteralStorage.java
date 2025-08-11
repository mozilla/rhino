package org.mozilla.javascript;

import java.util.Arrays;

/** Used to store the support structures for a literal object (or array) being built. */
public final class NewLiteralStorage {
    private Object[] keys;
    private int[] getterSetters;
    private Object[] values;
    private int index = 0;

    public NewLiteralStorage(Object[] ids) {
        this.keys = ids;
        this.getterSetters = new int[ids.length];
        this.values = new Object[ids.length];
    }

    public NewLiteralStorage(int length, boolean createKeys) {
        this.keys = createKeys ? new Object[length] : null;
        this.getterSetters = new int[length];
        this.values = new Object[length];
    }

    public void pushValue(Object value) {
        values[index++] = value;
    }

    public void pushGetter(Object value) {
        getterSetters[index] = -1;
        pushValue(value);
    }

    public void pushSetter(Object value) {
        getterSetters[index] = +1;
        pushValue(value);
    }

    public void pushKey(Object key) {
        keys[index] = key;
    }

    public void spread(Context cx, Scriptable scope, Object source) {
        // See ECMAScript 13.2.5.5
        if (source != null && !Undefined.isUndefined(source)) {
            Scriptable src = ScriptRuntime.toObject(cx, scope, source);
            Object[] ids;
            if (src instanceof ScriptableObject) {
                ids = ((ScriptableObject) src).getIds(false, true);
            } else {
                ids = src.getIds();
            }

            // Resize all the arrays
            int newLen = values.length + ids.length;
            keys = Arrays.copyOf(keys, newLen);
            getterSetters = Arrays.copyOf(getterSetters, newLen);
            values = Arrays.copyOf(values, newLen);

            // getIds() can only return a string, int or a symbol
            for (Object id : ids) {
                Object value;
                if (id instanceof String) {
                    value = ScriptableObject.getProperty(src, (String) id);
                } else if (id instanceof Integer) {
                    value = ScriptableObject.getProperty(src, (int) id);
                } else if (ScriptRuntime.isSymbol(id)) {
                    value = ScriptableObject.getProperty(src, (Symbol) id);
                } else {
                    throw Kit.codeBug();
                }

                pushKey(id);
                pushValue(value);
            }
        }
    }

    public Object[] getKeys() {
        return keys;
    }

    public int[] getGetterSetters() {
        return getterSetters;
    }

    public Object[] getValues() {
        return values;
    }
}
