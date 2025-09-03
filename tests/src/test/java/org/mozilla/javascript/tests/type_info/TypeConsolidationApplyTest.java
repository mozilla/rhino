package org.mozilla.javascript.tests.type_info;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;

/**
 * @author ZZZank
 */
public class TypeConsolidationApplyTest {

    /** Test for {@code SomeClass<SomeParamHere>} */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testGeneric(boolean interpretMode) {
        try (var cx = ContextFactory.getGlobal().enterContext()) {
            cx.setInterpretedMode(interpretMode);
            var scope = cx.initStandardObjects();

            var o = new TestObj<>();
            ScriptableObject.putProperty(scope, "bridge", Context.javaToJS(o, scope));

            // Double auto wrapped to String
            cx.evaluateString(scope, "bridge.forString().set(3.45);", "test", 1, null);
            Assertions.assertInstanceOf(String.class, o.value);
            Assertions.assertEquals("3.45", o.value);

            // Double auto wrapped to Integer
            cx.evaluateString(scope, "bridge.forInt().set(3.45);", "test", 1, null);
            Assertions.assertInstanceOf(Integer.class, o.value);
            Assertions.assertEquals(3, o.value);

            // String auto wrapped to Double
            cx.evaluateString(scope, "bridge.forDouble().set('3');", "test", 1, null);
            Assertions.assertInstanceOf(Double.class, o.value);
            Assertions.assertEquals(3.0, o.value);
        }
    }

    /** Test for {@code SomeClassImpl extends SomeClass<SomeParamHere>} */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testGenericParent(boolean interpretMode) {
        try (var cx = ContextFactory.getGlobal().enterContext()) {
            cx.setInterpretedMode(interpretMode);
            var scope = cx.initStandardObjects();

            var o = new TestObjStr();
            ScriptableObject.putProperty(scope, "bridge", Context.javaToJS(o, scope));

            // Double auto wrapped to String
            cx.evaluateString(scope, "bridge.set(3.45);", "test", 1, null);
            Assertions.assertInstanceOf(String.class, o.value);
            Assertions.assertEquals("3.45", o.value);
        }
    }

    public static class TestObj<T> {
        protected Object value;

        public T get() {
            return cast(value);
        }

        public void set(T value) {
            this.value = value;
        }

        public TestObj<String> forString() {
            return cast(this);
        }

        public TestObj<Integer> forInt() {
            return cast(this);
        }

        public TestObj<Double> forDouble() {
            return cast(this);
        }
    }

    public static class TestObjStr extends TestObj<String> {}

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }
}
