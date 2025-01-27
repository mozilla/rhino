package org.mozilla.javascript;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

public class TestNullabilityDetector implements NullabilityDetector {
    @Override
    public boolean[] getParameterNullability(Method method) {
        int paramCount = method.getParameters().length;
        boolean[] result = new boolean[paramCount];
        // All arguments are nullable
        Arrays.fill(result, true);
        return result;
    }

    @Override
    public boolean[] getParameterNullability(Constructor<?> constructor) {
        int paramCount = constructor.getParameters().length;
        boolean[] result = new boolean[paramCount];
        for (int i = 0; i < paramCount; i++) {
            // Even arguments are nullable
            result[i] = i % 2 == 0;
        }
        return result;
    }
}
