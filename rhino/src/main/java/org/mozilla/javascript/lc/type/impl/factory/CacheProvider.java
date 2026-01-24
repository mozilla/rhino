package org.mozilla.javascript.lc.type.impl.factory;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

/**
 * @author ZZZank
 */
interface CacheProvider extends TypeInfoFactory {
    <K, V> Map<K, V> createCache();

    interface Concurrent extends CacheProvider {

        @Override
        default <K, V> Map<K, V> createCache() {
            return new ConcurrentHashMap<>();
        }
    }

    interface WeakReference extends CacheProvider {

        @Override
        default <K, V> Map<K, V> createCache() {
            return Collections.synchronizedMap(new WeakHashMap<>());
        }
    }
}
