package org.mozilla.javascript.tests.type_info;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.BaseStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mozilla.javascript.lc.type.TypeFormatContext;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.impl.NoTypeInfo;

/**
 * @author ZZZank
 */
public class TypeConsolidationMappingTest {

    @Test
    public void testGeneric() {
        Assertions.assertEquals(
                Set.of("Ta -> Te", "Tb -> Te", "Tc -> String", "Td -> String"),
                getAndFormatMapping(E.class));
        Assertions.assertEquals(Set.of("T -> E"), getAndFormatMapping(Collection.class));
        Assertions.assertEquals(
                // T from BaseStream -> T from Stream
                Set.of("S -> Stream<T>", "T -> T"), getAndFormatMapping(Stream.class));
    }

    @Test
    public void testGenericParent() {
        Assertions.assertEquals(
                Set.of(
                        "Ta -> Integer",
                        "Tb -> Integer",
                        "Tc -> String",
                        "Td -> String",
                        "Te -> Integer"),
                getAndFormatMapping(GenericSuperClass.class));
        Assertions.assertEquals(
                Set.of("Tc -> Number", "Td -> Number"),
                getAndFormatMapping(GenericSuperInterface.class));

        Assertions.assertEquals(
                // E from Enum, T from Comparable
                Set.of("T -> NoTypeInfo", "E -> NoTypeInfo"),
                getAndFormatMapping(NoTypeInfo.class));
    }

    @ParameterizedTest
    @ValueSource(classes = {int.class, boolean.class, float.class, void.class})
    public void testPrimitive(Class<?> type) {
        Assertions.assertEquals(Set.of(), getAndFormatMapping(type));
    }

    @ParameterizedTest
    @ValueSource(classes = {Number.class, Object.class, TypeInfo.class})
    public void testNonGeneric(Class<?> type) {
        Assertions.assertEquals(Set.of(), getAndFormatMapping(type));
    }

    @ParameterizedTest
    @ValueSource(classes = {Iterable.class, Iterator.class, BaseStream.class})
    public void testGenericWithNoGenericParent(Class<?> type) {
        Assertions.assertEquals(Set.of(), getAndFormatMapping(type));
    }

    private static Set<String> getAndFormatMapping(Class<?> clazz) {
        var mapping = TypeInfoFactory.GLOBAL.getConsolidationMapping(clazz);

        var formatted = new HashSet<String>();
        mapping.forEach(
                (k, v) -> {
                    var builder = new StringBuilder();

                    k.append(TypeFormatContext.SIMPLE, builder);
                    builder.append(" -> ");
                    v.append(TypeFormatContext.SIMPLE, builder);

                    formatted.add(builder.toString());
                });
        return formatted;
    }

    static class A<Ta> {}

    static class B<Tb> extends A<Tb> {}

    interface C<Tc> {}

    interface D<Td> extends C<Td> {}

    static class E<Te> extends B<Te> implements D<String> {}

    static class GenericSuperClass extends E<Integer> {}

    static class GenericSuperInterface implements D<Number> {}
}
