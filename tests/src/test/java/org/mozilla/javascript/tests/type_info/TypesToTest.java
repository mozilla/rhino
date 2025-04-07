package org.mozilla.javascript.tests.type_info;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * holds type information in the form of method parameter types and return type, with each method representing one "category"
 *
 * @author ZZZank
 */
public interface TypesToTest {

    Map<String, List<TypePack>> ALL = Arrays
        .stream(TypesToTest.class.getMethods())
        .collect(Collectors.toMap(
            Method::getName,
            m -> TypeInfoTestUtil.zip(
                Stream.concat(Stream.of(m.getGenericReturnType()), Arrays.stream(m.getGenericParameterTypes())),
                Stream.concat(Stream.of(m.getReturnType()), Arrays.stream(m.getParameterTypes()))
            )
                .map(TypePack::new)
                .collect(Collectors.toList())
        ));

    void primitives(
        // float
        float f,
        double d,
        // int
        byte b,
        short s,
        int i,
        long l,
        // char
        char c,
        // bool
        boolean bo
    );

    Void primitiveObjects(
        // float
        Float f,
        Double d,
        // int
        Byte b,
        Short s,
        Integer i,
        Long l,
        // char
        Character c,
        // bool
        Boolean bo
    );

    void commonObjects(
        String s,
        Object o,
        Enum<?> e,
        Class<?> c,
        CharSequence cs
    );

    void objectArrays(
        // primitive
        float[] f,
        double[] d,
        // object
        String[] s,
        Object[] o,
        CharSequence[] cs,
        // array in array
        float[][][] fff,
        String[][][] sss
    );

    <T, TExtend extends CharSequence/*, TSuper super String*/> void genericArrays(
        T[] t,
        TExtend[] tex,
        T[][][] ttt,
        TExtend[][][] texxx
    );

    <T, TExtend extends CharSequence, TExtend3 extends CharSequence & Comparable<T> & Cloneable> T generics(
        T t,
        TExtend tex,
        TExtend3 tex3
    );

    <T, TExtend extends CharSequence> void typeParam(
        // type variable
        Map<T, TExtend> m,
        List<TExtend> l,
        Function<TExtend, T> f,
        // fixed type
        Map<String, Object> m2,
        List<Number> l2,
        Function<Class<?>, int[]> f2
    );

    <T, TExtend extends CharSequence> void wildcard(
        Map<? extends T, ? super TExtend> m,
        List<? extends Object[]> l,
        Function<? extends CharSequence, ? super String> f
    );
}
