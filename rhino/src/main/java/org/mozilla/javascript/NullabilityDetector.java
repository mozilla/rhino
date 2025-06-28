package org.mozilla.javascript;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface NullabilityDetector {
    NullabilityAccessor getParameterNullability(Method method);

    NullabilityAccessor getParameterNullability(Constructor<?> constructor);

    @FunctionalInterface
    interface NullabilityAccessor {
        NullabilityAccessor FALSE = (ignored) -> false;
        /**
         * @param index parameter index
         * @return {@code true} if the parameter at specified index is nullable, {@code false} otherwise
         * @throws IndexOutOfBoundsException if index is out of bounds of the method/constructor parameters
         */
        boolean isNullable(int index);
    }
}
