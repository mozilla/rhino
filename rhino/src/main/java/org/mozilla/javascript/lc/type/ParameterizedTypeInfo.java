package org.mozilla.javascript.lc.type;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;

/**
 * a {@link TypeInfo} implementation representing {@link ParameterizedType}
 *
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

    /**
     * Example:
     *
     * <p>1. for {@code Map<String, Integer>}, {@code param(-1)} == {@link TypeInfo#NONE}, {@code
     * param(0)} == {@link TypeInfo#STRING}, {@code param(1)} == {@link TypeInfo#INT}, {@code
     * param(2)} == {@link TypeInfo#NONE}
     *
     * <p>2. for {@code Map} (raw usage of generic class) or {@code Integer} (not generic class),
     * {@code param(anyNumberHere)} == {@link TypeInfo#NONE}
     *
     * <p>{@inheritDoc}
     *
     * @see #params()
     */
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
     * Scriptable} with type variables, which is incredibly rare but still possible
     */
    @Override
    default int getTypeTag() {
        if (Scriptable.class.isAssignableFrom(asClass())) {
            return FunctionObject.JAVA_SCRIPTABLE_TYPE;
        }
        return FunctionObject.JAVA_UNSUPPORTED_TYPE;
    }
}
