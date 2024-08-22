/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.JavaTypeResolver;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptableObject;

/**
 * Test the java type info resolver.
 *
 * @author Roland Praml, FOCONIS AG
 */
@SuppressWarnings({"rawtypes", "serial"})
public class JavaTypeInfoTest {

    private Type getType(String name) throws Exception {
        return getClass().getDeclaredField(name).getGenericType();
    }

    private JavaTypeResolver check(String name) throws Exception {
        Object value = getClass().getDeclaredField(name).get(this);
        ScriptableObject scope = new NativeObject();
        new ClassCache().associate(scope);
        return new JavaTypeResolver(scope, value == null ? null : value.getClass(), getType(name));
    }

    List<String> list1;
    /** Tests, if we can read type arguments from a normal List&lt;String&gt;. */
    @Test
    public void testList1() throws Exception {
        assertEquals(String.class, check("list1").resolve(List.class, 0));
        assertEquals(String.class, check("list1").resolve(Collection.class, 0));
        assertEquals(null, check("list1").resolve(ArrayList.class, 0));
    }

    List<List<String>> list2;
    /** Tests, if we can read special generic type arguments like List&lt;List&lt;String&gt;&gt;. */
    @Test
    public void testList2() throws Exception {
        assertEquals(List.class, check("list2").resolve(List.class, 0));
        assertEquals(List.class, check("list2").resolve(Collection.class, 0));
        assertEquals(null, check("list2").resolve(ArrayList.class, 0));
    }

    ArrayList<String> list3;

    /** Tests, if we can read type arguments if it is a class instead of an interface. */
    @Test
    public void testList3() throws Exception {
        assertEquals(String.class, check("list3").resolve(List.class, 0));
    }

    // some test classes
    static class TestList4 extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
    }

    TestList4 list4;
    /** Tests, if we can read type argument, if class inherits from a generic class. */
    @Test
    public void testList4() throws Exception {
        assertEquals(String.class, check("list4").resolve(List.class, 0));
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
        assertEquals(String.class, check("list5").resolve(List.class, 0));
    }

    static class TestList6<M> extends TestList5A<M, String>
            implements Collection<String>, List<String> {
        private static final long serialVersionUID = 1L;
    }

    TestList6<?> list6;
    /** Tests, if we can read type arguments, if wildcards are involved. */
    @Test
    public void testList6() throws Exception {
        assertEquals(String.class, check("list6").resolve(List.class, 0));
    }

    List<?> list7;
    /** Tests, if we can read type arguments, if wildcards are involved. */
    @Test
    public void testList7() throws Exception {
        assertEquals(Object.class, check("list7").resolve(List.class, 0));
        assertEquals(null, check("list7").resolve(Map.class, 0));
    }

    List list8;
    /** Tests, if we can read type arguments, if raw types are involved. */
    @Test
    public void testList8() throws Exception {
        assertEquals(Object.class, check("list8").resolve(List.class, 0));
        assertEquals(null, check("list8").resolve(Map.class, 0));
    }

    List<? extends Number> list9;
    /** Tests, if we can read wildcard type arguments with lowerBound. */
    @Test
    public void testList9() throws Exception {
        assertEquals(Number.class, check("list9").resolve(List.class, 0));
        assertEquals(null, check("list9").resolve(Map.class, 0));
    }

    List<? super Number> list10;
    /** Tests, if we can read wildcard type arguments with upperBound. */
    @Test
    public void testList10() throws Exception {
        assertEquals(Number.class, check("list10").resolve(List.class, 0));
        assertEquals(null, check("list10").resolve(Map.class, 0));
    }

    List<Number[]> list11;
    /** Tests, if we can read array types. */
    @Test
    public void testList11() throws Exception {
        assertEquals(new Number[0].getClass(), check("list11").resolve(List.class, 0));
    }

    List list12 = new ArrayList<String>() {};
    /** Tests, if we take the "best" type, if there are different possibilities. */
    @Test
    public void testList12() throws Exception {
        assertEquals(String.class, check("list12").resolve(List.class, 0));
    }

    Object list13 = new ArrayList<String>() {};
    /** Tests, if we take the "best" type, if there are different possibilities. */
    @Test
    public void testList13() throws Exception {
        assertEquals(String.class, check("list13").resolve(List.class, 0));
    }

    List<? extends Number> list14 = new ArrayList<Integer>() {};
    /** Tests, if we take the "best" type, if there are different possibilities. */
    @Test
    public void testList14() throws Exception {
        assertEquals(Integer.class, check("list14").resolve(List.class, 0));
    }

    List<? super Integer> list15 = new ArrayList<Number>() {};
    /** Tests, if we take the "best" type, if there are different possibilities. */
    @Test
    public void testList15() throws Exception {
        assertEquals(Integer.class, check("list15").resolve(List.class, 0));
    }

    abstract static class TwoInterfaces implements Iterator<Integer>, Map<String, Double> {}

    TwoInterfaces twoInterfaces1;
    /** Tests, if we can read the type info of each interface. */
    @Test
    public void testTwoInterfaces1() throws Exception {
        assertEquals(Integer.class, check("twoInterfaces1").resolve(Iterator.class, 0));
        assertEquals(String.class, check("twoInterfaces1").resolve(Map.class, 0));
        assertEquals(Double.class, check("twoInterfaces1").resolve(Map.class, 1));
        assertEquals(null, check("twoInterfaces1").resolve(TwoInterfaces.class, 0));
    }

    abstract static class TwoGenericInterfaces<A, B, C> implements Iterator<B>, Map<C, A> {}

    TwoGenericInterfaces<Double, Integer, String> twoInterfaces2;
    /** Tests, if we can read the type info of each generic interface. */
    @Test
    public void testTwoInterfaces2() throws Exception {
        assertEquals(Integer.class, check("twoInterfaces2").resolve(Iterator.class, 0));
        assertEquals(String.class, check("twoInterfaces2").resolve(Map.class, 0));
        assertEquals(Double.class, check("twoInterfaces2").resolve(Map.class, 1));
        assertEquals(Double.class, check("twoInterfaces2").resolve(TwoGenericInterfaces.class, 0));
        assertEquals(Integer.class, check("twoInterfaces2").resolve(TwoGenericInterfaces.class, 1));
        assertEquals(String.class, check("twoInterfaces2").resolve(TwoGenericInterfaces.class, 2));
    }

    TwoGenericInterfaces<? extends Number, Integer, String> twoInterfaces3;
    /** Tests, if we can read the type info of each generic interface. */
    @Test
    public void testTwoInterfaces3() throws Exception {
        assertEquals(Integer.class, check("twoInterfaces3").resolve(Iterator.class, 0));
        assertEquals(String.class, check("twoInterfaces3").resolve(Map.class, 0));
        assertEquals(Number.class, check("twoInterfaces3").resolve(Map.class, 1));
        assertEquals(Number.class, check("twoInterfaces3").resolve(TwoGenericInterfaces.class, 0));
        assertEquals(Integer.class, check("twoInterfaces3").resolve(TwoGenericInterfaces.class, 1));
        assertEquals(String.class, check("twoInterfaces3").resolve(TwoGenericInterfaces.class, 2));
    }

    TwoGenericInterfaces<? extends Number, ?, String> twoInterfaces4;
    /** Tests, if we can read the type info of each generic interface. */
    @Test
    public void testTwoInterfaces4() throws Exception {
        assertEquals(Object.class, check("twoInterfaces4").resolve(Iterator.class, 0));
        assertEquals(String.class, check("twoInterfaces4").resolve(Map.class, 0));
        assertEquals(Number.class, check("twoInterfaces4").resolve(Map.class, 1));
        assertEquals(Number.class, check("twoInterfaces4").resolve(TwoGenericInterfaces.class, 0));
        assertEquals(Object.class, check("twoInterfaces4").resolve(TwoGenericInterfaces.class, 1));
        assertEquals(String.class, check("twoInterfaces4").resolve(TwoGenericInterfaces.class, 2));
    }
}
