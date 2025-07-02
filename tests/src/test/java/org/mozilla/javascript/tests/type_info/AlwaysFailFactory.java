package org.mozilla.javascript.tests.type_info;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

/**
 * @author ZZZank
 */
enum AlwaysFailFactory implements TypeInfoFactory {
    INSTANCE;

    public static final String MESSAGE = "attempting to create TypeInfo on AlwaysFailFactory";

    @Override
    public TypeInfo create(Class<?> clazz) {
        throw new AssertionError(MESSAGE);
    }

    @Override
    public TypeInfo create(GenericArrayType genericArrayType) {
        throw new AssertionError(MESSAGE);
    }

    @Override
    public TypeInfo create(TypeVariable<?> typeVariable) {
        throw new AssertionError(MESSAGE);
    }

    @Override
    public TypeInfo create(ParameterizedType parameterizedType) {
        throw new AssertionError(MESSAGE);
    }

    @Override
    public TypeInfo create(WildcardType wildcardType) {
        throw new AssertionError(MESSAGE);
    }

    @Override
    public TypeInfo toArray(TypeInfo component) {
        throw new AssertionError(MESSAGE);
    }

    @Override
    public TypeInfo attachParam(TypeInfo base, List<TypeInfo> params) {
        throw new AssertionError(MESSAGE);
    }
}
