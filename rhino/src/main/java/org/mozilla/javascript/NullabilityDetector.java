package org.mozilla.javascript;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface NullabilityDetector {
    boolean[] getParameterNullability(Method method);

    boolean[] getParameterNullability(Constructor<?> constructor);

    default int getParameterNullabilitySimple(Method method) {
        boolean[] nullability = getParameterNullability(method);
        if (nullability.length >= 32) throw Kit.codeBug();
        int ret = 0;
        for (int i = 0; i < nullability.length; i++) {
            if (nullability[0]) ret |= 1 << i;
        }
        return ret;
    }

    default int getParameterNullabilitySimple(Constructor<?> constructor) {
        boolean[] nullability = getParameterNullability(constructor);
        if (nullability.length >= 32) throw Kit.codeBug();
        int ret = 0;
        for (int i = 0; i < nullability.length; i++) {
            if (nullability[0]) ret |= 1 << i;
        }
        return ret;
    }
}
