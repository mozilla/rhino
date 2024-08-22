/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.serialize.ScriptableInputStream;
import org.mozilla.javascript.serialize.ScriptableOutputStream;

/*
 * This testcase tests the basic access to java List and Map with []
 */
public class GenericAccessTest extends TestCase {

    @Test
    public void testBeanAccess() {
        String js =
                "bean.integers[0] = 3;\n"
                        + "bean.doubles[0] = 3;"
                        + "bean.doubles[0].getClass().getSimpleName() + ' ' "
                        + "+ bean.integers[0].getClass().getSimpleName()\n";
        testIt(js, "Double Integer");
    }

    @Test
    public void testListAccess() {
        String js =
                "intList[0] = 3;\n"
                        + "dblList[0] = 3;"
                        + "dblList[0].getClass().getSimpleName() + ' ' "
                        + "+ intList[0].getClass().getSimpleName()\n";
        testIt(js, "Double Integer");
    }

    @Test
    public void testIntListIncrement() {
        String js =
                "intList[0] = 3.5;\n"
                        + "intList[0]++;\n"
                        + "intList[0].getClass().getSimpleName() + ' ' + intList[0]\n";
        testIt(js, "Integer 4");
    }

    @Test
    public void testDblListIncrement() {
        String js =
                "dblList[0] = 3.5;\n"
                        + "dblList[0]++;\n"
                        + "dblList[0].getClass().getSimpleName() + ' ' + dblList[0]\n";
        testIt(js, "Double 4.5");
    }

    @Test
    public void testListAdd() {
        String js =
                "intList.add(3);\n"
                        + "dblList.add(3);"
                        + "dblList[0].getClass().getSimpleName() + ' ' "
                        + "+ intList[0].getClass().getSimpleName()\n";
        testIt(js, "Double Integer");
    }

    @Test
    public void testBeanListAdd() {
        String js =
                "bean.integers.add(3);\n"
                        + "bean.doubles.add(3);"
                        + "bean.doubles[0].getClass().getSimpleName() + ' ' "
                        + "+ bean.integers[0].getClass().getSimpleName()\n";
        testIt(js, "Double Integer");
    }

    @Test
    public void testGenericProperty() {
        String js =
                "bean.intBean1.value = 3;\n"
                        + "bean.dblBean1.value = 3;\n"
                        + "bean.intBean2.value = 3;\n"
                        + "bean.dblBean2.value = 3;\n"
                        + "bean.intBean1.value.getClass().getSimpleName() + ' ' + "
                        + "bean.dblBean1.value.getClass().getSimpleName() + ' ' + "
                        + "bean.intBean2.value.getClass().getSimpleName() + ' ' + "
                        + "bean.dblBean2.value.getClass().getSimpleName()";
        testIt(js, "Integer Double Integer Double");
    }

    @Test
    public void testGenericPropertyNoSetter() {
        String js =
                "bean.intBean1.publicValue = 3;\n"
                        + "bean.dblBean1.publicValue = 3;\n"
                        + "bean.intBean2.publicValue = 3;\n"
                        + "bean.dblBean2.publicValue = 3;\n"
                        + "bean.intBean1.publicValue.getClass().getSimpleName() + ' ' + "
                        + "bean.dblBean1.publicValue.getClass().getSimpleName() + ' ' + "
                        + "bean.intBean2.publicValue.getClass().getSimpleName() + ' ' + "
                        + "bean.dblBean2.publicValue.getClass().getSimpleName()";
        testIt(js, "Integer Double Integer Double");
    }

    @Test
    public void testGenericMultipleSetters() {
        String js =
                "bean.intBean1.valueMultipleSetters = 3;\n"
                        + "bean.dblBean1.valueMultipleSetters = 3;\n"
                        + "bean.intBean2.valueMultipleSetters = 3;\n"
                        + "bean.dblBean2.valueMultipleSetters = 3;\n"
                        + "bean.intBean1.valueMultipleSetters.getClass().getSimpleName() + ' ' + "
                        + "bean.dblBean1.valueMultipleSetters.getClass().getSimpleName() + ' ' + "
                        + "bean.intBean2.valueMultipleSetters.getClass().getSimpleName() + ' ' + "
                        + "bean.dblBean2.valueMultipleSetters.getClass().getSimpleName()";
        testIt(js, "Integer Double Integer Double");
    }

    @Test
    public void testGenericSetter() {
        String js =
                "bean.intBean1.setValue(3);\n"
                        + "bean.dblBean1.setValue(3);\n"
                        + "bean.intBean2.setValue(3);\n"
                        + "bean.dblBean2.setValue(3);\n"
                        + "bean.intBean1.value.getClass().getSimpleName() + ' ' + "
                        + "bean.dblBean1.value.getClass().getSimpleName() + ' ' + "
                        + "bean.intBean2.value.getClass().getSimpleName() + ' ' + "
                        + "bean.dblBean2.value.getClass().getSimpleName()";
        testIt(js, "Integer Double Integer Double");
    }

    // Test what happens, when writing and reading to map
    private static final String TEST_MAP_INT =
            "m[4] = 2;\n"
                    + "var key = m.keySet().iterator().next();\n"
                    + "var value = m.values().iterator().next();\n"
                    + "key.getClass().getSimpleName() + ' ' + key + ' ' + value.getClass().getSimpleName() + ' ' + value + ' ' + m[4]";

    private static final String TEST_MAP_STRING =
            "m['foo'] = 'bar';\n"
                    + "var key = m.keySet().iterator().next();\n"
                    + "var value = m.values().iterator().next();\n"
                    + "key.getClass().getSimpleName() + ' ' + key + ' ' + value.getClass().getSimpleName() + ' ' + value + ' ' + m['foo']";

    @Test
    public void testStringString() {
        String js = "var m = bean.stringStringMap;\n" + TEST_MAP_INT;
        testIt(js, "String 4 String 2 2");
        js = "var m = bean.stringStringMap;\n" + TEST_MAP_STRING;
        testIt(js, "String foo String bar bar");
    }

    @Test
    public void testIntStringMap1() {
        String js = "var m = bean.intStringMap;\n" + TEST_MAP_INT;
        testIt(js, "Integer 4 String 2 2");
    }

    @Test
    public void testIntStringMapWriteStringKey() {
        String js =
                "var m = bean.intStringMap;\n"
                        + "try { "
                        + TEST_MAP_STRING
                        + "} catch (e) { e.toString() }";
        testIt(
                js,
                "InternalError: Cannot convert foo to java.lang.Integer (GenericAccessTest.js#2)");
    }

    @Test
    public void testIntIntMap() {
        String js = "var m = bean.intIntMap;\n" + TEST_MAP_INT;
        testIt(js, "Integer 4 Integer 2 2");
    }

    @Test
    public void testIntLongMap() {
        String js = "var m = bean.intLongMap;\n" + TEST_MAP_INT;
        testIt(js, "Integer 4 Long 2 2");
    }

    public static class Bean implements Serializable {
        private static final long serialVersionUID = 1L;

        public List<Integer> integers = new ArrayList<>();
        private List<Double> doubles = new ArrayList<>();

        public List<Double> getDoubles() {
            return doubles;
        }

        public List<Number> numbers = new ArrayList<>();

        public Map<String, String> stringStringMap = new HashMap<>();
        public Map<Integer, String> intStringMap = new HashMap<>();
        public Map<Integer, Integer> intIntMap = new HashMap<>();
        public Map<Integer, Long> intLongMap = new HashMap<>();
        // beans with typeInfo in the static type
        public GenericBean<Integer> intBean1 = new GenericBean<>();
        public GenericBean<Double> dblBean1 = new GenericBean<>();
        // beans with typeInfo in the dynamic type
        public GenericBean<? extends Number> intBean2 = new IntegerGenericBean();
        public GenericBean<? extends Number> dblBean2 = new DoubleGenericBean();
    }

    public static class GenericBean<M extends Number> implements Serializable {
        private static final long serialVersionUID = 1L;

        private M value;

        public M publicValue;

        private M valueMultipleSetters;

        public M getValue() {
            return value;
        }

        public void setValue(M value) {
            this.value = value;
        }

        public M getValueMultipleSetters() {
            return valueMultipleSetters;
        }

        public void setValueMultipleSetters(M valueMultipleSetters) {
            this.valueMultipleSetters = valueMultipleSetters;
        }

        public void setValueMultipleSetters(String s) {
            throw new UnsupportedOperationException("Should not be called");
        }
    }

    static class IntegerArrayList extends ArrayList<Integer> {
        private static final long serialVersionUID = 1L;
    }

    static class DoubleArrayList extends ArrayList<Double> {
        private static final long serialVersionUID = 1L;
    }

    static class NumberArrayList extends ArrayList<Number> {
        private static final long serialVersionUID = 1L;
    }

    static class IntegerGenericBean extends GenericBean<Integer> {
        private static final long serialVersionUID = 1L;
    }

    static class DoubleGenericBean extends GenericBean<Double> {
        private static final long serialVersionUID = 1L;
    }

    private List<Integer> createIntegerList() {
        List<Integer> list = new IntegerArrayList();

        list.add(42);
        list.add(7);
        return list;
    }

    private List<Double> createDoubleList() {
        List<Double> list = new DoubleArrayList();

        list.add(42.5);
        list.add(7.5);
        return list;
    }

    private List<Number> createNumberList() {
        List<Number> list = new NumberArrayList();

        list.add(42);
        list.add(7.5);
        return list;
    }

    private ContextFactory getContextFactory() {
        return new ContextFactory() {
            @Override
            protected boolean hasFeature(Context cx, int featureIndex) {
                switch (featureIndex) {
                    case Context.FEATURE_ENABLE_JAVA_MAP_ACCESS:
                        return true;
                }
                return super.hasFeature(cx, featureIndex);
            }
        };
    }

    private void testIt(String script, String expected) {
        Utils.runWithAllOptimizationLevels(
                getContextFactory(),
                cx -> {
                    final ScriptableObject scope = cx.initStandardObjects();
                    scope.put("intList", scope, createIntegerList());
                    scope.put("dblList", scope, createDoubleList());
                    scope.put("numList", scope, createNumberList());
                    scope.put("bean", scope, new Bean());
                    Object o = cx.evaluateString(scope, script, "GenericAccessTest.js", 1, null);
                    assertEquals(expected, o);

                    return null;
                });
    }

    @Test
    public void testSerDeser() {
        Utils.runWithAllOptimizationLevels(
                getContextFactory(),
                cx -> {

                    // do some tests when we do a deserialization -> serialization roundtrip
                    // while dynamic type info is stored in the wrapped object itself,
                    // we have to do some extra effort to store also static type information
                    ScriptableObject scope = cx.initStandardObjects();
                    scope.put("bean", scope, new Bean());

                    // store 3 into value. We expect that 3 is converted to the generic type.
                    String testCode = "value = 3; value.getClass().getSimpleName()";
                    doTest(cx, scope, testCode, "bean.intBean1", Integer.class);
                    doTest(cx, scope, testCode, "bean.intBean2", Integer.class);
                    doTest(cx, scope, testCode, "bean.dblBean1", Double.class);
                    doTest(cx, scope, testCode, "bean.dblBean2", Double.class);

                    // perform test also for index based list access
                    testCode = "this[0] = 3; this[0].getClass().getSimpleName()";
                    doTest(cx, scope, testCode, "bean.integers", Integer.class);
                    doTest(cx, scope, testCode, "bean.doubles", Double.class);
                    doTest(cx, scope, testCode, "bean.numbers", Double.class);

                    // and for method access
                    testCode = "this.add(0, 3); this[0].getClass().getSimpleName()";
                    doTest(cx, scope, testCode, "bean.integers", Integer.class);
                    doTest(cx, scope, testCode, "bean.doubles", Double.class);
                    doTest(cx, scope, testCode, "bean.numbers", Double.class);

                    // and for maps
                    testCode = "this[0] = 3; this[0].getClass().getSimpleName()";
                    doTest(cx, scope, testCode, "bean.intStringMap", String.class);
                    doTest(cx, scope, testCode, "bean.intIntMap", Integer.class);
                    doTest(cx, scope, testCode, "bean.intLongMap", Long.class);
                    return null;
                });
    }

    private void doTest(
            Context cx,
            ScriptableObject scope,
            String testCode,
            String beanPath,
            Class<?> expectedType) {

        // first step, extract a NativeJavaObject (with type information)
        Object o = cx.evaluateString(scope, beanPath, "testSerDeser", 1, null);
        NativeJavaObject wrappedBean = (NativeJavaObject) o;

        o = cx.evaluateString(wrappedBean, testCode, "testSerDeser", 1, null);
        o = Context.jsToJava(o, String.class);
        assertEquals(expectedType.getSimpleName(), o);

        wrappedBean = serDeser(wrappedBean, scope);

        // try again after deserialization
        o = cx.evaluateString(wrappedBean, testCode, "testSerDeser", 1, null);
        o = Context.jsToJava(o, String.class);

        assertEquals(expectedType.getSimpleName(), o);
    }

    @SuppressWarnings("unchecked")
    private <T> T serDeser(T element, Scriptable scope) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ScriptableOutputStream(baos, scope)) {
            oos.writeObject(element);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // deserialize
        try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                ObjectInputStream ois = new ScriptableInputStream(bais, scope); ) {
            return (T) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
