package org.mozilla.javascript.tests.type_info;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
        assertMappingMatch(E.class, "Ta -> Te[]", "Tb -> Te", "Tc -> List<String>", "Td -> String");
        assertMappingMatch(Collection.class, "T -> E");

        // T from BaseStream -> T from Stream
        assertMappingMatch(Stream.class, "S -> Stream<T>", "T -> T");

        // TwoGenericInterfaces<A, B, C> implements Iterator<E -> B>, Map<K -> C, V -> A>
        assertMappingMatch(TwoGenericInterfaces.class, "V -> A", "E -> B", "K -> C");

        // E from ArrayList & List & Collection & ..., T from Iterable
        // due to different class inheritance in Java11/17/21, we can't use 'equals' to match
        // mapping
        assertMappingInclude(TestListA.class, "E -> N", "T -> N");
    }

    @Test
    public void testGenericParent() {
        assertMappingMatch(
                GenericSuperClass.class,
                "Ta -> Integer[]",
                "Tb -> Integer",
                "Tc -> List<String>",
                "Td -> String",
                "Te -> Integer");
        assertMappingMatch(GenericSuperInterface.class, "Tc -> List<Number>", "Td -> Number");

        // E from Enum, T from Comparable
        assertMappingMatch(NoTypeInfo.class, "T -> NoTypeInfo", "E -> NoTypeInfo");

        // TwoInterfaces implements Iterator<E -> Integer>, Map<K -> String, V -> Double>
        assertMappingMatch(TwoInterfaces.class, "V -> Double", "K -> String", "E -> Integer");

        // E from ArrayList and List and Collection and ..., T from Iterable
        // M and N from TestListA
        // due to different class inheritance in Java11/17/21, we can't use 'equals' to match
        // mapping
        assertMappingInclude(
                TestListB.class, "E -> String", "M -> Integer", "N -> String", "T -> String");
    }

    @ParameterizedTest
    @ValueSource(classes = {int.class, boolean.class, float.class, void.class})
    public void testPrimitive(Class<?> type) {
        assertMappingMatch(type /* empty mapping */);
    }

    @ParameterizedTest
    @ValueSource(classes = {Number.class, Object.class, TypeInfo.class})
    public void testNonGeneric(Class<?> type) {
        assertMappingMatch(type /* empty mapping */);
    }

    @ParameterizedTest
    @ValueSource(classes = {Iterable.class, Iterator.class, BaseStream.class, Map.class})
    public void testGenericWithNoGenericParent(Class<?> type) {
        assertMappingMatch(type /* empty mapping */);
    }

    private static void assertMappingMatch(Class<?> clazz, String... expected) {
        var formatted = getAndFormatMapping(clazz);

        formatted.sort(null);
        Arrays.sort(expected);

        Assertions.assertEquals(Arrays.asList(expected), formatted);
    }

    private static void assertMappingInclude(Class<?> clazz, String... expected) {
        var formatted = getAndFormatMapping(clazz);
        Assertions.assertTrue(
                Set.copyOf(formatted).containsAll(Arrays.asList(expected)),
                () ->
                        String.format(
                                "Found mapping '%s' does not include all elements in '%s'",
                                Set.copyOf(formatted), Arrays.asList(expected)));
    }

    private static ArrayList<String> getAndFormatMapping(Class<?> clazz) {
        var mapping = TypeInfoFactory.GLOBAL.getConsolidationMapping(clazz);

        var formatted = new ArrayList<String>();
        mapping.forEach(
                (k, v) -> {
                    var builder = new StringBuilder();

                    TypeFormatContext.SIMPLE.append(builder, k);
                    builder.append(" -> ");
                    TypeFormatContext.SIMPLE.append(builder, v);

                    formatted.add(builder.toString());
                });
        return formatted;
    }

    static class A<Ta> {}

    static class B<Tb> extends A<Tb[]> {}

    interface C<Tc> {}

    interface D<Td> extends C<List<Td>> {}

    static class E<Te> extends B<Te> implements D<String> {}

    static class GenericSuperClass extends E<Integer> {}

    static class GenericSuperInterface implements D<Number> {}

    abstract static class TwoInterfaces implements Iterator<Integer>, Map<String, Double> {}

    abstract static class TwoGenericInterfaces<A, B, C> implements Iterator<B>, Map<C, A> {}

    abstract static class TestListA<M, N> extends ArrayList<N> {}

    abstract static class TestListB extends TestListA<Integer, String> {}
}
