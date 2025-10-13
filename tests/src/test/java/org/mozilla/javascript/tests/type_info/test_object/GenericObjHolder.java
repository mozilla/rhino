package org.mozilla.javascript.tests.type_info.test_object;

/**
 * @author ZZZank
 */
public class GenericObjHolder<T> {
    public Object value; // use 'Object' instead of 'T' to escape runtime typecheck

    public T get() {
        return cast(value);
    }

    public void set(T value) {
        this.value = value;
    }

    public GenericObjHolder<String> forString() {
        return cast(this);
    }

    public GenericObjHolder<Integer> forInt() {
        return cast(this);
    }

    public GenericObjHolder<Double> forDouble() {
        return cast(this);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }
}
