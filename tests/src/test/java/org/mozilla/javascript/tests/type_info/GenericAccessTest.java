package org.mozilla.javascript.tests.type_info;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.*;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.tests.type_info.test_object.*;
import org.mozilla.javascript.testutils.Utils;

/**
 * Test for Java member access with generic present:
 *
 * <p>- method call
 *
 * <p>- field access
 *
 * <p>- beaning
 *
 * <p>- list spacial action ({@code list[0]}, {@code list[0] = 42})
 *
 * <p>- map special action ({@code map['foo']}, {@code map['foo'] = 42})
 *
 * @author ZZZank
 */
public class GenericAccessTest {

    @Test
    public void testBeanAccess() {
        String js =
                "bean.integers[0] = 3;\n"
                        + "bean.doubles[0] = 3;"
                        + "classAndValue(bean.doubles[0], bean.integers[0])";
        expect(js, "Double 3.0 Integer 3");
    }

    @Test
    public void testListAccess() {
        String js =
                "intList[0] = 3;\n" + "dblList[0] = 3;\n" + "classAndValue(dblList[0], intList[0])";
        expect(js, "Double 3.0 Integer 3");
    }

    @Test
    public void testIntListIncrement() {
        String js = "intList[0] = 3;\n" + "intList[0]++;\n" + "classAndValue(intList[0])\n";
        expect(js, "Integer 4");
    }

    @Test
    public void testDblListIncrement() {
        String js = "dblList[0] = 3.5;\n" + "dblList[0]++;\n" + "classAndValue(dblList[0])";
        expect(js, "Double 4.5");
    }

    @Test
    public void testListAdd() {
        String js =
                "intList.add(0,3);\n"
                        + "dblList.add(0,3);\n"
                        + "classAndValue(dblList[0], intList[0])";
        expect(js, "Double 3.0 Integer 3");
    }

    @Test
    @Disabled // TODO: https://github.com/mozilla/rhino/pull/2080#issuecomment-3285901869
    public void testBeanListAdd() {
        String js =
                "bean.doubles.add(0, 3);\n"
                        + "bean.integers.add(0, 3);\n"
                        + "classAndValue(bean.doubles[0], bean.integers[0])";
        expect(js, "Double 3.0 Integer 3");
    }

    @Test
    public void testGenericBean() {
        String js =
                "bean.intBean1.value = 3;\n"
                        + "bean.dblBean1.value = 3;\n"
                        + "bean.intBean2.value = 3;\n"
                        + "bean.dblBean2.value = 3;\n"
                        + "classAndValue(bean.intBean1.value, bean.dblBean1.value, bean.intBean2.value, bean.dblBean2.value)";
        expect(js, "Integer 3 Double 3.0 Integer 3 Double 3.0");
    }

    @Test
    @Disabled // TODO: generic support for field
    public void testGenericField() {
        String js =
                "bean.intBean1.publicValue = 3;\n"
                        + "bean.dblBean1.publicValue = 3;\n"
                        + "bean.intBean2.publicValue = 3;\n"
                        + "bean.dblBean2.publicValue = 3;\n"
                        + "classAndValue(bean.intBean1.publicValue, bean.dblBean1.publicValue, bean.intBean2.publicValue, bean.dblBean2.publicValue)";
        expect(js, "Integer Double Integer Double");
    }

    @Test
    public void testGenericMultipleSetters() {
        String js =
                "bean.intBean1.valueMultipleSetters = 3;\n"
                        + "bean.dblBean1.valueMultipleSetters = 3;\n"
                        + "bean.intBean2.valueMultipleSetters = 3;\n"
                        + "bean.dblBean2.valueMultipleSetters = 3;\n"
                        + "classAndValue(bean.intBean1.valueMultipleSetters, bean.dblBean1.valueMultipleSetters, bean.intBean2.valueMultipleSetters, bean.dblBean2.valueMultipleSetters)";
        expect(js, "Integer 3 Double 3.0 Integer 3 Double 3.0");
    }

    @Test
    public void testGenericSetter() {
        String js =
                "bean.intBean1.setValue(3);\n"
                        + "bean.dblBean1.setValue(3);\n"
                        + "bean.intBean2.setValue(3);\n"
                        + "bean.dblBean2.setValue(3);\n"
                        + "classAndValue(bean.intBean1.value, bean.dblBean1.value, bean.intBean2.value, bean.dblBean2.value)";
        expect(js, "Integer 3 Double 3.0 Integer 3 Double 3.0");
    }

    private static final String TEST_MAP_INT =
            "m[4] = 2;\n"
                    + "var key = m.keySet().iterator().next();\n"
                    + "var value = m.values().iterator().next();\n"
                    + "classAndValue(key, value, m[4])";

    private static final String TEST_MAP_STRING =
            "m['foo'] = 'bar';\n"
                    + "var key = m.keySet().iterator().next();\n"
                    + "var value = m.values().iterator().next();\n"
                    + "classAndValue(key, value, m['foo'])";

    @Test
    @Disabled // deciding when to wrap map key is awfully tricky
    public void testStringString() {
        String js = "var m = bean.stringStringMap;\n" + TEST_MAP_INT;
        expect(js, "String 4 String 2 String 2");
        js = "var m = bean.stringStringMap;\n" + TEST_MAP_STRING;
        expect(js, "String foo String bar String bar");
    }

    @Test
    public void testIntStringMap1() {
        String js = "var m = bean.intStringMap;\n" + TEST_MAP_INT;
        expect(js, "Integer 4 String 2 String 2");
    }

    @Test
    public void testIntStringMapWriteStringKey() {
        String js =
                "var m = bean.intStringMap;\n"
                        + "try { "
                        + TEST_MAP_STRING
                        + "} catch (e) { e.toString() }";
        expect(
                js,
                "InternalError: Cannot convert foo to java.lang.Integer (GenericAccessTest.js#2)");
    }

    @Test
    public void testIntIntMap() {
        String js = "var m = bean.intIntMap;\n" + TEST_MAP_INT;
        expect(js, "Integer 4 Integer 2 Integer 2");
    }

    @Test
    public void testIntLongMap() {
        String js = "var m = bean.intLongMap;\n" + TEST_MAP_INT;
        expect(js, "Integer 4 Long 2 Long 2");
    }

    private static void expect(String script, String expected) {
        var bindings = new HashMap<String, Object>();
        bindings.put("intList", IntegerArrayList.createTestObject());
        bindings.put("dblList", DoubleArrayList.createTestObject());
        bindings.put("numList", NumberArrayList.createTestObject());
        bindings.put("classAndValue", (Callable) GenericAccessTest::readClassAndValue);

        // test member access via beaning
        bindings.put("bean", new MethodBasedBean());
        expect(script, expected, bindings);
    }

    private static void expect(String script, String expected, Map<String, Object> bindings) {
        Utils.runWithAllModes(
                CONTEXT_FACTORY,
                cx -> {
                    var scope = cx.initStandardObjects();
                    bindings.forEach((name, value) -> scope.put(name, scope, value));

                    Object o = cx.evaluateString(scope, script, "GenericAccessTest.js", 1, null);
                    Assertions.assertEquals(expected, ScriptRuntime.toString(o));

                    return null;
                });
    }

    private static final ContextFactory CONTEXT_FACTORY =
            new ContextFactory() {
                @Override
                protected boolean hasFeature(Context cx, int featureIndex) {
                    return featureIndex == Context.FEATURE_ENABLE_JAVA_MAP_ACCESS
                            || super.hasFeature(cx, featureIndex);
                }
            };

    private static Object readClassAndValue(
            Context cx, Scriptable scope, Scriptable thiz, Object[] args) {
        return Arrays.stream(args)
                .map(arg -> Context.jsToJava(arg, TypeInfo.OBJECT))
                .map(arg -> arg.getClass().getSimpleName() + ' ' + arg)
                .collect(Collectors.joining(" "));
    }
}
