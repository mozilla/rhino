package org.mozilla.javascript.tests.type_info;

import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.VariableTypeInfo;
import org.mozilla.javascript.lc.type.impl.BasicClassTypeInfo;
import org.mozilla.javascript.lc.type.impl.EnumTypeInfo;
import org.mozilla.javascript.lc.type.impl.InterfaceTypeInfo;
import org.mozilla.javascript.lc.type.impl.factory.FactoryBase;

/**
 * {@link org.mozilla.javascript.lc.type.TypeInfoFactory} implementation with no cache, and no
 * generic support, as an example usage of custom type factory via {@link
 * org.mozilla.javascript.ContextFactory#setTypeFactoryProvider(Function)}
 *
 * @author ZZZank
 */
class NoGenericFactory implements FactoryBase {

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
        return create(typeVariable.getBounds()[0]);
    }

    @Override
    public TypeInfo attachParam(TypeInfo base, List<TypeInfo> params) {
        return base;
    }

    @Override
    public Map<VariableTypeInfo, TypeInfo> getConsolidationMapping(Class<?> from) {
        return Map.of();
    }
}
