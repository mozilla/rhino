package org.mozilla.javascript.nat.type.impl;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.definition.TypeInfoFactory;

/**
 * @author ZZZank
 */
public class DefaultFactory implements TypeInfoFactory {

    @Override
    public TypeInfo create(Class<?> clazz) {
        return null;
    }

    @Override
    public TypeInfo create(GenericArrayType genericArrayType) {
        return null;
    }

    @Override
    public TypeInfo create(TypeVariable<?> typeVariable) {
        return null;
    }

    @Override
    public TypeInfo create(ParameterizedType parameterizedType) {
        return null;
    }

    @Override
    public TypeInfo create(WildcardType wildcardType) {
        return null;
    }

    @Override
    public TypeInfo toArray(TypeInfo component) {
        return null;
    }

    @Override
    public TypeInfo attachParam(TypeInfo base, List<TypeInfo> params) {
        return null;
    }
}
