package org.mozilla.javascript.nat.type.impl.factory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.TypeInfoFactory;

/**
 * {@link TypeInfoFactory} implementation with a thread-safe (synchronized), strong-reference cache.
 * <p>
 * This factory will cache {@link TypeInfo} for simple types. Passing simple type ({@link TypeInfoFactory}.class for example) to this factory multiple times will return the exactly same TypeInfo object.
 * <p>
 * This factory uses a strong-reference cache. Resolving a type might cause this factory to hold a strong reference to the type.
 * <p>
 * This factory is thread safe. Multiple threads can safely access the same {@link ConcurrentFactory} object at the same time. It's more performant than {@link WeakReferenceFactory}
 *
 * @author ZZZank
 */
public final class ConcurrentFactory extends WithCacheFactory {

    @Override
    protected <K, V> Map<K, V> createTypeCache() {
        return new ConcurrentHashMap<>();
    }
}
