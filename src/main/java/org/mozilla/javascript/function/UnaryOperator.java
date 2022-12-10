package org.mozilla.javascript.function;


public interface UnaryOperator<T> extends Function<T, T> {

    static <T> UnaryOperator<T> identity() {
        return t -> t;
    }
}
