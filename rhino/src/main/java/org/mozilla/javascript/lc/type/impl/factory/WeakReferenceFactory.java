package org.mozilla.javascript.lc.type.impl.factory;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

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
 * @see ConcurrentFactory factory with a strong-reference, high performance cache
 */
public class WeakReferenceFactory extends WithCacheFactory {

    private static final long serialVersionUID = 7240510556821383410L;

    @Override
    protected final <K, V> Map<K, V> createTypeCache() {
        return Collections.synchronizedMap(new WeakHashMap<>());
    }

    @Override
    protected <K, V> Map<K, V> createConsolidationMappingCache() {
        return Collections.synchronizedMap(new WeakHashMap<>());
    }
}
