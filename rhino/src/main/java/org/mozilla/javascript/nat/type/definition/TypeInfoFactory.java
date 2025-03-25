package org.mozilla.javascript.nat.type.definition;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.impl.DefaultFactory;

/**
 * @author ZZZank
 */
public interface TypeInfoFactory {

    TypeInfoFactory GLOBAL = new DefaultFactory();

    TypeInfo[] EMPTY_ARRAY = new TypeInfo[0];

    /// creating types

    default TypeInfo none() {
        return TypeInfo.NONE;
    }

    TypeInfo create(Class<?> clazz);

    TypeInfo create(GenericArrayType genericArrayType);

    TypeInfo create(TypeVariable<?> typeVariable);

    TypeInfo create(ParameterizedType parameterizedType);

    TypeInfo create(WildcardType wildcardType);

    /// mutating types

    TypeInfo toArray(TypeInfo component);

    TypeInfo attachParam(TypeInfo base, List<TypeInfo> params);

    default TypeInfo attachParam(TypeInfo base, TypeInfo... params) {
        return attachParam(base, Arrays.asList(params));
    }

    /**
     * @see TypeInfo#consolidate(Map)
     */
    default Map<TypeInfo, TypeInfo> getConsolidationMapping(Class<?> from) {
        return Map.of();
    }

    /// helpers

    /**
     * create a {@link TypeInfo} from {@link Type}.
     *
     * <p>Implementations are recommended, but not required, to maintain the uniqueness of simple
     * types like {@link Class}, to reduce allocation
     *
     * <p>There's no guarantee that created {@link TypeInfo} will keep all their original
     * information, for example, {@link WildcardType} might be mapped to one of its bounds, instead
     * of another {@code "WildcardTypeInfo"}
     *
     * <p>Implementations should return {@link #none()} if it's unable to parse the {@link Type} it
     * received
     */
    default TypeInfo create(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clz = (Class<?>) type;
            return create(clz);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            return create(paramType);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrType = (GenericArrayType) type;
            return create(arrType);
        } else if (type instanceof TypeVariable<?>) {
            TypeVariable<?> variable = (TypeVariable<?>) type;
            return create(variable);
        } else if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            return create(wildcard);
        }
        return none();
    }

    default <T extends Type> TypeInfo[] createArray(T[] types) {
        if (types.length == 0) {
            return EMPTY_ARRAY;
        }
        final var len = types.length;
        final var arr = new TypeInfo[len];
        for (int i = 0; i < len; i++) {
            arr[i] = create(types[i]);
        }
        return arr;
    }

    default <T extends Type> List<TypeInfo> createList(T[] types) {
        return List.of(createArray(types));
    }
}
