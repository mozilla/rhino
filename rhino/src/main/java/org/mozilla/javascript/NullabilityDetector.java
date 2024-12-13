package org.mozilla.javascript;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface NullabilityDetector {
    boolean[] getParameterNullability(Method method);

    boolean[] getParameterNullability(Constructor<?> constructor);
}
