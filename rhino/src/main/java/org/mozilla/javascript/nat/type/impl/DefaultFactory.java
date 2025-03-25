package org.mozilla.javascript.nat.type.impl;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.definition.ParameterizedTypeInfo;
import org.mozilla.javascript.nat.type.definition.TypeInfoFactory;

/**
 * @author ZZZank
 */
public class DefaultFactory implements TypeInfoFactory {

    private final Map<TypeVariable<?>, VariableTypeInfo> CACHE_VARIABLE = new ConcurrentHashMap<>();
    private final Map<Class<?>, BasicClassTypeInfo> CACHE_BASIC_CLASS = new ConcurrentHashMap<>();
    private final Map<Class<?>, InterfaceTypeInfo> CACHE_INTERFACE = new ConcurrentHashMap<>();
    private final Map<Class<?>, EnumTypeInfo> CACHE_ENUM = new ConcurrentHashMap<>();

    @Override
    public TypeInfo create(Class<?> clazz) {
        final var predefined = matchPredefined(clazz);
        if (predefined != null) {
            return predefined;
        } else if (clazz.isArray()) {
            return toArray(create(clazz.getComponentType()));
        } else if (clazz.isEnum()) {
            return CACHE_ENUM.computeIfAbsent(clazz, EnumTypeInfo::new);
        } else if (clazz.isInterface()) {
            return CACHE_INTERFACE.computeIfAbsent(clazz, InterfaceTypeInfo::new);
        }
        return CACHE_BASIC_CLASS.computeIfAbsent(clazz, BasicClassTypeInfo::new);
    }

    @Override
    public TypeInfo create(GenericArrayType genericArrayType) {
        return toArray(create(genericArrayType.getGenericComponentType()));
    }

    @Override
    public TypeInfo create(TypeVariable<?> typeVariable) {
        return CACHE_VARIABLE.computeIfAbsent(typeVariable, VariableTypeInfo::new);
    }

    @Override
    public TypeInfo create(ParameterizedType parameterizedType) {
        return attachParam(
            create(parameterizedType.getRawType()),
            createList(parameterizedType.getActualTypeArguments())
        );
    }

    @Override
    public TypeInfo create(WildcardType wildcardType) {
        var upper = wildcardType.getUpperBounds();
        if (upper.length != 0 && upper[0] != Object.class) {
            return create(upper[0]);
        }

        var lower = wildcardType.getLowerBounds();
        if (lower.length != 0) {
            return create(lower[0]);
        }
        return none();
    }

    @Override
    public TypeInfo toArray(TypeInfo component) {
        return new ArrayTypeInfo(component);
    }

    @Override
    public TypeInfo attachParam(TypeInfo base, List<TypeInfo> params) {
        var rawType = base instanceof ParameterizedTypeInfo ? ((ParameterizedTypeInfo) base).rawType() : base;
        return new org.mozilla.javascript.nat.type.impl.ParameterizedTypeInfo(rawType, params);
    }
}
