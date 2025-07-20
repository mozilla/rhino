package org.mozilla.javascript;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/** This class is intended to keep API compatibility for a while. */
@Deprecated
public interface LcBridge {
    LcBridge instance =
            Objects.requireNonNull(
                    ScriptRuntime.loadOneServiceImplementation(LcBridge.class),
                    "LiveConnect library not in classpath");

    <T extends Scriptable> BaseFunction buildClassCtor(
            Scriptable scope, Class<T> clazz, boolean sealed, boolean mapInheritance)
            throws IllegalAccessException, InstantiationException, InvocationTargetException;

    void defineProperty(
            ScriptableObject target, String propertyName, Class<?> clazz, int attributes);

    void defineProperty(
            ScriptableObject target,
            String propertyName,
            Object delegateTo,
            Method getter,
            Method setter,
            int attributes);

    void defineFunctionProperties(
            ScriptableObject target, String[] names, Class<?> clazz, int attributes);

    <T> T jsToJava(Object value, Class<T> desiredType) throws EvaluatorException;

}
