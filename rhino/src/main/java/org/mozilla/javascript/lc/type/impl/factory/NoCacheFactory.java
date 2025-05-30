package org.mozilla.javascript.lc.type.impl.factory;

import java.lang.reflect.TypeVariable;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.impl.BasicClassTypeInfo;
import org.mozilla.javascript.lc.type.impl.EnumTypeInfo;
import org.mozilla.javascript.lc.type.impl.InterfaceTypeInfo;
import org.mozilla.javascript.lc.type.impl.VariableTypeInfoImpl;

/**
 * {@link TypeInfoFactory} implementation with no cache.
 *
 * <p>This factory will not cache {@link TypeInfo}. But passing a type to this factory multiple
 * times might still return the exact same object, they are predefined static {@link TypeInfo}.
 *
 * <p>Resolving a type will not prevent it from getting reclaimed by JVM, obviously.
 *
 * <p>This factory is thread-safe, as it's stateless.
 *
 * @author ZZZank
 * @see ConcurrentFactory factory with a strong-reference, high performance cache
 * @see WeakReferenceFactory factory with a weak-reference cache
 */
public enum NoCacheFactory implements FactoryBase {
    INSTANCE;

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
