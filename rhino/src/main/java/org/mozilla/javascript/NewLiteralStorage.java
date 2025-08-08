package org.mozilla.javascript;

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

    private void ensureCapacity() {
        int minLen = index + 1;
        int curLen = values.length;
        if (minLen > curLen) {
            int newLen = Math.max(curLen * 2, minLen);
            if (keys != null) keys = java.util.Arrays.copyOf(keys, newLen);
            getterSetters = java.util.Arrays.copyOf(getterSetters, newLen);
            values = java.util.Arrays.copyOf(values, newLen);
        }
    }

    public void pushValue(Object value) {
        ensureCapacity();
        values[index++] = value;
    }

    public void pushGetter(Object value) {
        ensureCapacity();
        getterSetters[index] = -1;
        pushValue(value);
    }

    public void pushSetter(Object value) {
        ensureCapacity();
        getterSetters[index] = +1;
        pushValue(value);
    }

    public void pushKey(Object key) {
        ensureCapacity();
        keys[index] = key;
    }

    public void setGetterSetterFlagAt(int index, int flag) {
        this.getterSetters[index] = flag;
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
