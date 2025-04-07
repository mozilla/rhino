package org.mozilla.javascript.nat.type.impl.factory;

import org.mozilla.javascript.nat.type.ParameterizedTypeInfo;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.TypeInfoFactory;
import org.mozilla.javascript.nat.type.impl.ArrayTypeInfo;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.List;

/**
 * @author ZZZank
 */
public interface FactoryBase extends TypeInfoFactory {

    @Override
    default TypeInfo create(GenericArrayType genericArrayType) {
        return toArray(create(genericArrayType.getGenericComponentType()));
    }

    @Override
    default TypeInfo create(ParameterizedType parameterizedType) {
        return attachParam(
            create(parameterizedType.getRawType()),
            createList(parameterizedType.getActualTypeArguments())
        );
    }

    @Override
    default TypeInfo create(WildcardType wildcardType) {
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
    default TypeInfo toArray(TypeInfo component) {
        return new ArrayTypeInfo(component);
    }

    @Override
    default TypeInfo attachParam(TypeInfo base, List<TypeInfo> params) {
        var rawType = base instanceof ParameterizedTypeInfo ? ((ParameterizedTypeInfo) base).rawType() : base;
        return new org.mozilla.javascript.nat.type.impl.ParameterizedTypeInfo(rawType, params);
    }
}
