package org.mozilla.javascript;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface NullabilityDetector {
    NullabilityAccessor getParameterNullability(Method method);

    NullabilityAccessor getParameterNullability(Constructor<?> constructor);

    interface NullabilityAccessor {
        NullabilityAccessor TRUE = (ignored) -> true;
        NullabilityAccessor FALSE = (ignored) -> false;
        NullabilityAccessor INDEX_OUT_OF_BOUNDS =
                (i) -> {
                    throw new IndexOutOfBoundsException(
                            String.format("Index %s out of bounds [0,0)", i));
                };

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

        static NullabilityAccessor compress(boolean[] values) {
            var length = values.length;
            // empty
            if (length == 0) {
                return INDEX_OUT_OF_BOUNDS;
            }
            // single element
            if (length == 1) {
                return values[0] ? TRUE : FALSE;
            }
            // same elements
            Boolean allMatch = values[0];
            for (var value : values) {
                if (allMatch != value) {
                    allMatch = null;
                    break;
                }
            }
            if (allMatch != null) {
                return allMatch ? TRUE : FALSE;
            }
            // use smaller object (int) as backend
            if (length < 32) { // length: [2, 31]
                var compressed = 0;
                for (int i = 0; i < length; i++) {
                    if (values[i]) {
                        compressed |= (1 << i);
                    }
                }
                final var compressedFinal = compressed;
                return i -> ((compressedFinal >> i) & 1) != 0;
            }
            // fallback
            return i -> values[i];
        }
    }
}
