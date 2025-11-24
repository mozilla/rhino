package org.mozilla.javascript.lc.type;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    default TypeInfo param(int index) {
        final var params = params();
        return index >= 0 && index < params.size() ? params.get(index) : TypeInfo.NONE;
    }

    /**
     * Extract consolidation mapping based on {@link #params()} and {@link
     * Class#getTypeParameters()}
     *
     * <p>Example: {@code type} is {@code List<String>}, calling {@code ((ParameterizedTypeInfo)
     * type).extractConsolidationMapping(TypeInfoFactory.GLOBAL) } will give {@code E -> String},
     * where the {@code E} the type variable declared by {@link List}
     */
    default Map<VariableTypeInfo, TypeInfo> extractConsolidationMapping(TypeInfoFactory factory) {
        var typeVariables = this.asClass().getTypeParameters();
        var actualParams = this.params();

        var len = typeVariables.length;
        if (len != actualParams.size()) {
            throw new IllegalStateException(
                    String.format(
                            "Expecting %s type params for class '%s', but got %s",
                            len, this.asClass().getName(), actualParams.size()));
        } else if (len == 0) {
            throw new IllegalStateException(
                    String.format(
                            "Base class '%s' is not a generic class", this.asClass().getName()));
        }

        if (len == 1) {
            return Map.of((VariableTypeInfo) factory.create(typeVariables[0]), actualParams.get(0));
        }

        var mapping = new HashMap<VariableTypeInfo, TypeInfo>();
        for (int i = 0; i < len; i++) {
            mapping.put((VariableTypeInfo) factory.create(typeVariables[i]), actualParams.get(i));
        }
        return mapping;
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
