package org.mozilla.javascript.nat.type.impl.factory;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.TypeInfoFactory;

/**
 * {@link TypeInfoFactory} implementation with a thread-safe (synchronized), weak-reference cache.
 *
 * <p>This factory will cache {@link TypeInfo} for simple types. Passing simple type ({@link
 * TypeInfoFactory}.class for example) to this factory multiple times will return the exactly same
 * TypeInfo object.
 *
 * <p>This factory uses a weak-reference cache. Resolving a type will not prevent it from getting
 * reclaimed by JVM.
 *
 * <p>This factory is thread safe. Multiple threads can safely access the same {@link
 * WeakReferenceFactory} object at the same time, but it's not guaranteed to be performant.
 *
 * <p>This factory is serializable, but none of its cached objects will be serialized.
 *
 * @author ZZZank
 * @see NoCacheFactory factory with no cache
 * @see ConcurrentFactory factory with a strong-reference, high performance cache
 */
public final class WeakReferenceFactory extends WithCacheFactory {

    @Override
    protected <K, V> Map<K, V> createTypeCache() {
        return Collections.synchronizedMap(new WeakHashMap<>());
    }
}
