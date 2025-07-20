package org.mozilla.javascript.lc.java;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.LcBridge;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.lc.type.TypeInfo;

public class LcBridgeImpl implements LcBridge {

    @Override
    public <T extends Scriptable> BaseFunction buildClassCtor(
            Scriptable scope, Class<T> clazz, boolean sealed, boolean mapInheritance)
            throws IllegalAccessException, InstantiationException, InvocationTargetException {
        return LiveConnect.buildClassCtor(scope, clazz, sealed, mapInheritance);
    }

    @Override
    public void defineProperty(
            ScriptableObject target, String propertyName, Class<?> clazz, int attributes) {
        LiveConnect.defineProperty(target, propertyName, clazz, attributes);
    }

    @Override
    public void defineProperty(
            ScriptableObject target,
            String propertyName,
            Object delegateTo,
            Method getter,
            Method setter,
            int attributes) {
        LiveConnect.defineProperty(target, propertyName, delegateTo, getter, setter, attributes);
    }

    @Override
    public void defineFunctionProperties(
            ScriptableObject target, String[] names, Class<?> clazz, int attributes) {
        LiveConnect.defineFunctionProperties(target, names, clazz, attributes);
    }

    @Override
    public <T> T jsToJava(Object value, Class<T> desiredType) throws EvaluatorException {
        return (T) LiveConnect.jsToJava(value, desiredType);
    }
    
}
