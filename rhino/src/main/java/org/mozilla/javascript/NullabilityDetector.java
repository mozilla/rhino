package org.mozilla.javascript;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface NullabilityDetector {
    NullabilityAccessor getParameterNullability(Method method);

    NullabilityAccessor getParameterNullability(Constructor<?> constructor);

    interface NullabilityAccessor {
        NullabilityAccessor FALSE = (ignored) -> false;

        /**
         * Note: The return value when providing out-of-bound index is "not defined". There's no
         * guarantee that it will provide a result that makes sense, or throw an {@link
         * IndexOutOfBoundsException} when providing out-of-bound index
         *
         * @param index parameter index
         * @return {@code true} if the parameter at specified index is nullable, {@code false}
         *     otherwise
         * @throws IndexOutOfBoundsException if index is out of bounds of the method/constructor
         *     parameters
         */
        boolean isNullable(int index);
    }
}
