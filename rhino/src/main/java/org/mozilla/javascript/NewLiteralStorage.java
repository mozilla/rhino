package org.mozilla.javascript;

/**
 * Used to store the support structures for a literal object (or array) being built.
 */
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

    public NewLiteralStorage(int length) {
        this.keys = null;
        this.getterSetters = new int[length];
        this.values = new Object[length];
    }

    public void pushValue(Object value) {
        this.values[this.index] = value;
        this.index++;
    }

    public void pushGetter(Object value) {
        this.getterSetters[this.index] = -1;
        this.pushValue(value);
    }

    public void pushSetter(Object value) {
        this.getterSetters[this.index] = +1;
        this.pushValue(value);
    }

    public void pushKey(Object key) {
        this.keys[this.index] = key;
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
