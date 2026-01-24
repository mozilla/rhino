package org.mozilla.javascript.lc.type.impl.factory;

import java.util.Map;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.impl.BasicClassTypeInfo;
import org.mozilla.javascript.lc.type.impl.EnumTypeInfo;
import org.mozilla.javascript.lc.type.impl.InterfaceTypeInfo;

/**
 * {@link ClassValue} is not present on Android until API level 34. This factory will be used as
 * fallback when {@link ClassValue} is not available
 *
 * @see <a href="https://developer.android.com/reference/java/lang/ClassValue">Android API
 *     reference</a>
 * @see ClassValueCacheFactory
 * @author ZZZank
 */
public abstract class LegacyCacheFactory extends AbstractCacheFactory {
    private transient Map<Class<?>, BasicClassTypeInfo> basicClassCache;
    private transient Map<Class<?>, InterfaceTypeInfo> interfaceCache;
    private transient Map<Class<?>, EnumTypeInfo> enumCache;

    @Override
    protected void initCache() {
        super.initCache();
        basicClassCache = createCache();
        interfaceCache = createCache();
        enumCache = createCache();
    }

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

    public static class Concurrent extends LegacyCacheFactory implements CachedFactory.Concurrent {}

    public static class WeakReference extends LegacyCacheFactory
            implements CachedFactory.WeakReference {}
}
