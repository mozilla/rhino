package org.mozilla.javascript.nat.type.impl.factory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.impl.BasicClassTypeInfo;
import org.mozilla.javascript.nat.type.impl.EnumTypeInfo;
import org.mozilla.javascript.nat.type.impl.InterfaceTypeInfo;
import org.mozilla.javascript.nat.type.impl.VariableTypeInfoImpl;

/**
 * {@link org.mozilla.javascript.nat.type.TypeInfoFactory} implementation with cache. The exact
 * characteristic if the factory depends on the characteristic of map backend created via {@link
 * #createTypeCache()}
 *
 * @author ZZZank
 */
public abstract class WithCacheFactory implements FactoryBase, Serializable {
    private static final long serialVersionUID = 1L;

    private transient Map<TypeVariable<?>, VariableTypeInfoImpl> variableCache = createTypeCache();
    private transient Map<Class<?>, BasicClassTypeInfo> basicClassCache = createTypeCache();
    private transient Map<Class<?>, InterfaceTypeInfo> interfaceCache = createTypeCache();
    private transient Map<Class<?>, EnumTypeInfo> enumCache = createTypeCache();

    protected abstract <K, V> Map<K, V> createTypeCache();

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

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        variableCache = createTypeCache();
        basicClassCache = createTypeCache();
        interfaceCache = createTypeCache();
        enumCache = createTypeCache();
    }
}
