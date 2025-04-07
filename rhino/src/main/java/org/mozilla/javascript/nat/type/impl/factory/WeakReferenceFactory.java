package org.mozilla.javascript.nat.type.impl.factory;

import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.WeakHashMap;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.impl.BasicClassTypeInfo;
import org.mozilla.javascript.nat.type.impl.EnumTypeInfo;
import org.mozilla.javascript.nat.type.impl.InterfaceTypeInfo;
import org.mozilla.javascript.nat.type.impl.VariableTypeInfoImpl;

/**
 * @author ZZZank
 */
public final class WeakReferenceFactory implements FactoryBase {

    private final Map<TypeVariable<?>, VariableTypeInfoImpl> variableCache = new WeakHashMap<>();
    private final Map<Class<?>, BasicClassTypeInfo> basicClassCache = new WeakHashMap<>();
    private final Map<Class<?>, InterfaceTypeInfo> interfaceCache = new WeakHashMap<>();
    private final Map<Class<?>, EnumTypeInfo> enumCache = new WeakHashMap<>();

    @Override
    public TypeInfo create(Class<?> clazz) {
        final var predefined = matchPredefined(clazz);
        if (predefined != null) {
            return predefined;
        } else if (clazz.isArray()) {
            return toArray(create(clazz.getComponentType()));
        } else if (clazz.isEnum()) {
            final var got = enumCache.get(clazz);
            if (got != null) {
                return got;
            }
            synchronized (enumCache) {
                return enumCache.computeIfAbsent(clazz, EnumTypeInfo::new);
            }
        } else if (clazz.isInterface()) {
            final var got = interfaceCache.get(clazz);
            if (got != null) {
                return got;
            }
            synchronized (interfaceCache) {
                return interfaceCache.computeIfAbsent(clazz, InterfaceTypeInfo::new);
            }
        }
        final var got = basicClassCache.get(clazz);
        if (got != null) {
            return got;
        }
        synchronized (basicClassCache) {
            return basicClassCache.computeIfAbsent(clazz, BasicClassTypeInfo::new);
        }
    }

    @Override
    public TypeInfo create(TypeVariable<?> typeVariable) {
        final var got = variableCache.get(typeVariable);
        if (got != null) {
            return got;
        }
        synchronized (variableCache) {
            return variableCache.computeIfAbsent(
                    typeVariable, raw -> new VariableTypeInfoImpl(raw, this));
        }
    }
}
