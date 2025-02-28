package org.mozilla.javascript.tests.type_info;

import java.lang.reflect.*;
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
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED), false);
    }

    public static <K, V> Stream<Map.Entry<K, V>> zip(Stream<K> iter1, Stream<V> iter2) {
        return streamIterator(zip(iter1.iterator(), iter2.iterator()));
    }

    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            // type is a normal class.
            return (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            var parameterizedType = (ParameterizedType) type;

            // getRawType() returns Type instead of Class; that seems to be an API mistake,
            // see https://bugs.openjdk.org/browse/JDK-8250659
            var rawType = parameterizedType.getRawType();
            return (Class<?>) rawType;

        } else if (type instanceof GenericArrayType) {
            var componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();

        } else if (type instanceof TypeVariable) {
            // do not return Object.class directly, it's not how Java erase generic type info
            //            return Object.class;
            var typeVariable = (TypeVariable<?>) type;
            return getRawType(typeVariable.getBounds()[0]);

        } else if (type instanceof WildcardType) {
            var bounds = ((WildcardType) type).getUpperBounds();
            // Currently the JLS only permits one bound for wildcards so using first bound is safe
            return getRawType(bounds[0]);
        }
        var className = type == null ? "null" : type.getClass().getName();
        throw new IllegalArgumentException(
                String.format(
                        "Expected a Class, ParameterizedType, or GenericArrayType, but <%s> is of type %s",
                        type, className));
    }
}
