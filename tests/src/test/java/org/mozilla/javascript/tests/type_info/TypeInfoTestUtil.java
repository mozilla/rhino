package org.mozilla.javascript.tests.type_info;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author ZZZank
 */
public class TypeInfoTestUtil {

    public static <K, V> Iterator<Map.Entry<K, V>> zip(Iterator<K> iter1, Iterator<V> iter2) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iter1.hasNext() && iter2.hasNext();
            }

            @Override
            public Map.Entry<K, V> next() {
                return new AbstractMap.SimpleImmutableEntry<>(iter1.next(), iter2.next());
            }
        };
    }

    public static <T> Stream<T> streamIterator(Iterator<T> iter) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED), false);
    }

    public static <K, V> Stream<Map.Entry<K, V>> zip(Stream<K> iter1, Stream<V> iter2) {
        return streamIterator(zip(iter1.iterator(), iter2.iterator()));
    }
}
