package org.mozilla.javascript.lc.type.impl.factory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.VariableTypeInfo;
import org.mozilla.javascript.lc.type.impl.BasicClassTypeInfo;
import org.mozilla.javascript.lc.type.impl.EnumTypeInfo;
import org.mozilla.javascript.lc.type.impl.InterfaceTypeInfo;
import org.mozilla.javascript.lc.type.impl.VariableTypeInfoImpl;

/**
 * {@link org.mozilla.javascript.lc.type.TypeInfoFactory} implementation with cache. The exact
 * characteristic if the factory depends on the characteristic of map backend created via {@link
 * #createTypeCache()}
 *
 * <p>This factory is serializable, but none of its cached objects will be serialized.
 *
 * @author ZZZank
 */
public abstract class WithCacheFactory implements FactoryBase {
    private static final long serialVersionUID = 4533445095188189419L;

    private transient Map<TypeVariable<?>, VariableTypeInfoImpl> variableCache = createTypeCache();
    private transient Map<Class<?>, BasicClassTypeInfo> basicClassCache = createTypeCache();
    private transient Map<Class<?>, InterfaceTypeInfo> interfaceCache = createTypeCache();
    private transient Map<Class<?>, EnumTypeInfo> enumCache = createTypeCache();

    private transient Map<Class<?>, Map<VariableTypeInfo, TypeInfo>> consolidationMappingCache = createConsolidationMappingCache();

    protected abstract <K, V> Map<K, V> createTypeCache();

    protected abstract <K, V> Map<K, V> createConsolidationMappingCache();

    @Override
    public TypeInfo create(Class<?> clazz) {
        final var predefined = TypeInfoFactory.matchPredefined(clazz);
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

    @Override
    public Map<VariableTypeInfo, TypeInfo> getConsolidationMapping(Class<?> from) {
        if (from == null || from == Object.class || from.isPrimitive()) {
            return Map.of();
        }
        // no computeIfAbsent because `computeTypeReplacement(...)` will recursively call this method again
        var got = consolidationMappingCache.get(from);
        if (got == null) {
            got = computeConsolidationMapping(from);
            consolidationMappingCache.put(from, got);
        }
        return got;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        variableCache = createTypeCache();
        basicClassCache = createTypeCache();
        interfaceCache = createTypeCache();
        enumCache = createTypeCache();
        consolidationMappingCache = createConsolidationMappingCache();
    }
}
