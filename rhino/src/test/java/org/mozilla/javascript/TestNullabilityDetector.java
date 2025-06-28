package org.mozilla.javascript;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class TestNullabilityDetector implements NullabilityDetector {
    @Override
    public NullabilityAccessor getParameterNullability(Method method) {
        // All arguments are nullable
        return i -> true;
    }

    @Override
    public NullabilityAccessor getParameterNullability(Constructor<?> constructor) {
        // Even arguments are nullable
        return i -> i % 2 == 0;
    }
}
