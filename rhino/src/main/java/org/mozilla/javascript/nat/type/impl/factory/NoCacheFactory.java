package org.mozilla.javascript.nat.type.impl.factory;

import java.lang.reflect.TypeVariable;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.impl.BasicClassTypeInfo;
import org.mozilla.javascript.nat.type.impl.EnumTypeInfo;
import org.mozilla.javascript.nat.type.impl.InterfaceTypeInfo;
import org.mozilla.javascript.nat.type.impl.VariableTypeInfoImpl;

/**
 * @author ZZZank
 */
public final class NoCacheFactory implements FactoryBase {

    @Override
    public TypeInfo create(Class<?> clazz) {
        final var predefined = matchPredefined(clazz);
        if (predefined != null) {
            return predefined;
        } else if (clazz.isArray()) {
            return toArray(create(clazz.getComponentType()));
        } else if (clazz.isEnum()) {
            return new EnumTypeInfo(clazz);
        } else if (clazz.isInterface()) {
            return new InterfaceTypeInfo(clazz);
        }
        return new BasicClassTypeInfo(clazz);
    }

    @Override
    public TypeInfo create(TypeVariable<?> typeVariable) {
        return new VariableTypeInfoImpl(typeVariable, this);
    }
}
