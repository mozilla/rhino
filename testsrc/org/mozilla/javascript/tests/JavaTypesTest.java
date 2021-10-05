/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertArrayEquals;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.JavaTypes;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptableObject;

/**
 * Test the type resolver.
 *
 * @author Roland Praml, FOCONIS AG
 */
@SuppressWarnings({"rawtypes", "serial"})
public class JavaTypesTest {

    private Type getType(String name) throws Exception {
        return getClass().getDeclaredField(name).getGenericType();
    }

    private Type[] check(String name, Class<?> interestingClass) throws Exception {
        Object value = getClass().getDeclaredField(name).get(this);
        ScriptableObject scope = new NativeObject();
        new ClassCache().associate(scope);
        return JavaTypes.lookupType(
                scope, value == null ? null : value.getClass(), getType(name), interestingClass);
    }

    List<String> list1;
    /** Tests, if we can read type arguments from a normal List&lt;String&gt;. */
    @Test
    public void testList1() throws Exception {
        assertArrayEquals(new Type[] {String.class}, check("list1", List.class));
        assertArrayEquals(new Type[] {String.class}, check("list1", Collection.class));
        assertArrayEquals(null, check("list1", ArrayList.class));
    }

    List<List<String>> list2;
    /** Tests, if we can read special generic type arguments like List&lt;List&lt;String&gt;&gt;. */
    @Test
    public void testList2() throws Exception {
        assertArrayEquals(new Type[] {getType("list1")}, check("list2", List.class));
        assertArrayEquals(new Type[] {getType("list1")}, check("list2", Collection.class));
        assertArrayEquals(null, check("list2", ArrayList.class));
    }

    ArrayList<String> list3;

    /** Tests, if we can read type arguments if it is a class instead of an interface. */
    @Test
    public void testList3() throws Exception {
        assertArrayEquals(new Type[] {String.class}, check("list3", List.class));
    }

    // some test classes
    static class TestList4 extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
    }

    TestList4 list4;
    /** Tests, if we can read type argument, if class inherits from a generic class. */
    @Test
    public void testList4() throws Exception {
        assertArrayEquals(new Type[] {String.class}, check("list4", List.class));
    }

    static class TestList5A<M, E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;
    }

    static class TestList5 extends TestList5A<Integer, String> {
        private static final long serialVersionUID = 1L;
    }

    TestList5 list5;
    /** Tests, if we can read type arguments, if inheritance chain introduces new type arguments. */
    @Test
    public void testList5() throws Exception {
        assertArrayEquals(new Type[] {String.class}, check("list5", List.class));
    }

    static class TestList6<M> extends TestList5A<M, String>
            implements Collection<String>, List<String> {
        private static final long serialVersionUID = 1L;
    }

    TestList6<?> list6;
    /** Tests, if we can read type arguments, if wildcards are involved. */
    @Test
    public void testList6() throws Exception {
        assertArrayEquals(new Type[] {String.class}, check("list6", List.class));
    }

    List<?> list7;
    /** Tests, if we can read type arguments, if wildcards are involved. */
    @Test
    public void testList7() throws Exception {
        assertArrayEquals(new Type[] {Object.class}, check("list7", List.class));
        assertArrayEquals(null, check("list7", Map.class));
    }

    List list8;
    /** Tests, if we can read type arguments, if raw types are involved. */
    @Test
    public void testList8() throws Exception {
        assertArrayEquals(new Type[] {Object.class}, check("list8", List.class));
        assertArrayEquals(null, check("list8", Map.class));
    }

    List<? extends Number> list9;
    /** Tests, if we can read wildcard type arguments with lowerBound. */
    @Test
    public void testList9() throws Exception {
        assertArrayEquals(new Type[] {Number.class}, check("list9", List.class));
        assertArrayEquals(null, check("list9", Map.class));
    }

    List<? super Number> list10;
    /** Tests, if we can read wildcard type arguments with upperBound. */
    @Test
    public void testList10() throws Exception {
        assertArrayEquals(new Type[] {Number.class}, check("list10", List.class));
        assertArrayEquals(null, check("list10", Map.class));
    }

    List<Number[]> list11;
    /** Tests, if we can read array types. */
    @Test
    public void testList11() throws Exception {
        assertArrayEquals(new Type[] {new Number[0].getClass()}, check("list11", List.class));
    }

    List list12 = new ArrayList<String>() {};
    /** Tests, if we take the "best" type, if there are different possibilities. */
    @Test
    public void testList12() throws Exception {
        assertArrayEquals(new Type[] {String.class}, check("list12", List.class));
    }

    Object list13 = new ArrayList<String>() {};
    /** Tests, if we take the "best" type, if there are different possibilities. */
    @Test
    public void testList13() throws Exception {
        assertArrayEquals(new Type[] {String.class}, check("list13", List.class));
    }

    List<? extends Number> list14 = new ArrayList<Integer>() {};
    /** Tests, if we take the "best" type, if there are different possibilities. */
    @Test
    public void testList14() throws Exception {
        assertArrayEquals(new Type[] {Integer.class}, check("list14", List.class));
    }

    List<? super Integer> list15 = new ArrayList<Number>() {};
    /** Tests, if we take the "best" type, if there are different possibilities. */
    @Test
    public void testList15() throws Exception {
        assertArrayEquals(new Type[] {Integer.class}, check("list15", List.class));
    }

    abstract static class TwoInterfaces implements Iterator<Integer>, Map<String, Double> {}

    TwoInterfaces twoInterfaces1;
    /** Tests, if we can read the type info of each interface. */
    @Test
    public void testTwoInterfaces1() throws Exception {
        assertArrayEquals(new Type[] {Integer.class}, check("twoInterfaces1", Iterator.class));
        assertArrayEquals(
                new Type[] {String.class, Double.class}, check("twoInterfaces1", Map.class));
        assertArrayEquals(new Type[] {}, check("twoInterfaces1", TwoInterfaces.class));
    }

    abstract static class TwoGenericInterfaces<A, B, C> implements Iterator<B>, Map<C, A> {}

    TwoGenericInterfaces<Double, Integer, String> twoInterfaces2;
    /** Tests, if we can read the type info of each generic interface. */
    @Test
    public void testTwoInterfaces2() throws Exception {
        assertArrayEquals(new Type[] {Integer.class}, check("twoInterfaces2", Iterator.class));
        assertArrayEquals(
                new Type[] {String.class, Double.class}, check("twoInterfaces2", Map.class));
        assertArrayEquals(
                new Type[] {Double.class, Integer.class, String.class},
                check("twoInterfaces2", TwoGenericInterfaces.class));
    }

    TwoGenericInterfaces<? extends Number, Integer, String> twoInterfaces3;
    /** Tests, if we can read the type info of each generic interface. */
    @Test
    public void testTwoInterfaces3() throws Exception {
        assertArrayEquals(new Type[] {Integer.class}, check("twoInterfaces3", Iterator.class));
        assertArrayEquals(
                new Type[] {String.class, Number.class}, check("twoInterfaces3", Map.class));
        assertArrayEquals(
                new Type[] {Number.class, Integer.class, String.class},
                check("twoInterfaces3", TwoGenericInterfaces.class));
    }

    TwoGenericInterfaces<? extends Number, ?, String> twoInterfaces4;
    /** Tests, if we can read the type info of each generic interface. */
    @Test
    public void testTwoInterfaces4() throws Exception {
        assertArrayEquals(new Type[] {Object.class}, check("twoInterfaces4", Iterator.class));
        assertArrayEquals(
                new Type[] {String.class, Number.class}, check("twoInterfaces4", Map.class));
        assertArrayEquals(
                new Type[] {Number.class, Object.class, String.class},
                check("twoInterfaces4", TwoGenericInterfaces.class));
    }
}
