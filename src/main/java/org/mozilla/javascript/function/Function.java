package org.mozilla.javascript.function;

public interface Function<T, R> {
    R apply(T t);
}
