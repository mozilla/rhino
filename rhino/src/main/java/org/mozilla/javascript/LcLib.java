package org.mozilla.javascript;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import org.mozilla.javascript.lc.type.TypeInfo;

public interface LcLib {
    LcLib instance =
            Objects.requireNonNull(
                    ScriptRuntime.loadOneServiceImplementation(LcLib.class),
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

    Object jsToJava(Object value, TypeInfo desiredType) throws EvaluatorException;
}
