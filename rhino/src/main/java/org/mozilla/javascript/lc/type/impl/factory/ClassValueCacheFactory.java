package org.mozilla.javascript.lc.type.impl.factory;

import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.impl.BasicClassTypeInfo;
import org.mozilla.javascript.lc.type.impl.EnumTypeInfo;
import org.mozilla.javascript.lc.type.impl.InterfaceTypeInfo;

/**
 * {@link ClassValue} is really performant for caching per-class data. However, it's not always
 * available on Android. When {@link ClassValue} is not available, {@link LegacyCacheFactory} should
 * be used instead.
 *
 * @see LegacyCacheFactory
 * @author ZZZank
 */
public abstract class ClassValueCacheFactory extends AbstractCacheFactory {
    private static final ClassValue<TypeInfo> CLASS_TYPE =
            new ClassValue<>() {
                @Override
                protected TypeInfo computeValue(Class<?> type) {
                    if (type.isEnum()) {
                        return new EnumTypeInfo(type);
                    } else if (type.isInterface()) {
                        return new InterfaceTypeInfo(type);
                    }
                    return new BasicClassTypeInfo(type);
                }
            };

    @Override
    public TypeInfo create(Class<?> clazz) {
        final var predefined = TypeInfoFactory.matchPredefined(clazz);
        if (predefined != null) {
            return predefined;
        } else if (clazz.isArray()) {
            return toArray(create(clazz.getComponentType()));
        }
        return CLASS_TYPE.get(clazz);
    }

    public static class Concurrent extends ClassValueCacheFactory
            implements CachedFactory.Concurrent {}

    public static class WeakReference extends ClassValueCacheFactory
            implements CachedFactory.WeakReference {}
}
