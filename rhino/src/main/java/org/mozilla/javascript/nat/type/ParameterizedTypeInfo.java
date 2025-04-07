package org.mozilla.javascript.nat.type;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;

/**
 * @see ParameterizedType
 * @author ZZZank
 */
public interface ParameterizedTypeInfo extends TypeInfo {

    /**
     * @see ParameterizedType#getRawType()
     */
    TypeInfo rawType();

    /**
     * @see ParameterizedType#getActualTypeArguments()
     */
    List<TypeInfo> params();

    @Override
    default TypeInfo param(int index) {
        final var params = params();
        return index >= 0 && index < params.size() ? params.get(index) : TypeInfo.NONE;
    }

    @Override
    default boolean isInterface() {
        return rawType().isInterface();
    }

    @Override
    default boolean isFunctionalInterface() {
        return rawType().isFunctionalInterface();
    }

    @Override
    default boolean isAssignableFrom(TypeInfo another) {
        return rawType().isAssignableFrom(another);
    }

    @Override
    default boolean isInstance(Object o) {
        return rawType().isInstance(o);
    }

    /**
     * none of the base types is parameterized, unless this object is an implementation of {@link
     * Scriptable} with type variables
     */
    @Override
    default int getTypeTag() {
        if (Scriptable.class.isAssignableFrom(asClass())) {
            return FunctionObject.JAVA_SCRIPTABLE_TYPE;
        }
        return FunctionObject.JAVA_UNSUPPORTED_TYPE;
    }
}
