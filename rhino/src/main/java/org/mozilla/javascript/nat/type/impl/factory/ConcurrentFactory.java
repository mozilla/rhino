package org.mozilla.javascript.nat.type.impl.factory;

import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.impl.BasicClassTypeInfo;
import org.mozilla.javascript.nat.type.impl.EnumTypeInfo;
import org.mozilla.javascript.nat.type.impl.InterfaceTypeInfo;
import org.mozilla.javascript.nat.type.impl.VariableTypeInfoImpl;

/**
 * @author ZZZank
 */
public final class ConcurrentFactory implements FactoryBase {

    private final Map<TypeVariable<?>, VariableTypeInfoImpl> variableCache =
            new ConcurrentHashMap<>();
    private final Map<Class<?>, BasicClassTypeInfo> basicClassCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, InterfaceTypeInfo> interfaceCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, EnumTypeInfo> enumCache = new ConcurrentHashMap<>();

    @Override
    public TypeInfo create(Class<?> clazz) {
        final var predefined = matchPredefined(clazz);
        if (predefined != null) {
            return predefined;
        } else if (clazz.isArray()) {
            return toArray(create(clazz.getComponentType()));
        } else if (clazz.isEnum()) {
            return enumCache.computeIfAbsent(clazz, EnumTypeInfo::new);
        } else if (clazz.isInterface()) {
            return interfaceCache.computeIfAbsent(clazz, InterfaceTypeInfo::new);
        }
        return basicClassCache.computeIfAbsent(clazz, BasicClassTypeInfo::new);
    }

    @Override
    public TypeInfo create(TypeVariable<?> typeVariable) {
        return variableCache.computeIfAbsent(
                typeVariable, raw -> new VariableTypeInfoImpl(raw, this));
    }
}
