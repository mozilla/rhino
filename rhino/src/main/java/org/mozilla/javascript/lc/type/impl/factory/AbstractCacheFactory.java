package org.mozilla.javascript.lc.type.impl.factory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.VariableTypeInfo;
import org.mozilla.javascript.lc.type.impl.VariableTypeInfoImpl;

/**
 * {@link TypeInfoFactory} implementation with cache. The exact characteristic if the factory
 * depends on the characteristic of map backend created via {@link #initCache()}
 *
 * <p>This factory is serializable, but none of its cached objects will be serialized.
 *
 * @author ZZZank
 */
public abstract class AbstractCacheFactory implements FactoryBase, CacheProvider {
    private static final long serialVersionUID = 4533445095188189419L;

    private transient Map<TypeVariable<?>, VariableTypeInfoImpl> variableCache;
    private transient Map<Class<?>, Map<VariableTypeInfo, TypeInfo>> consolidationMappingCache;

    public AbstractCacheFactory() {
        initCache();
    }

    protected void initCache() {
        variableCache = createCache();
        consolidationMappingCache = createCache();
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
        // no computeIfAbsent because `computeTypeReplacement(...)` will recursively call this
        // method again
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
        initCache();
    }
}
