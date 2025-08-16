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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void test(boolean interpretMode) {
        try (var cx = ContextFactory.getGlobal().enterContext()) {
            cx.setInterpretedMode(interpretMode);
            var scope = cx.initStandardObjects();

            var bridge = new StringImpl();
            ScriptableObject.putProperty(scope, "bridge", Context.javaToJS(bridge, scope));

            var script = "bridge.set(3.45);"; // number should be wrapped to string automatically
            cx.evaluateString(scope, script, "TypeConsolidationApplyTest", 1, null);

            var got = bridge.value;
            Assertions.assertInstanceOf(String.class, got);
            Assertions.assertEquals("3.45", got);
        }
    }

    public abstract static class GenericBase<T> {
        protected Object value;

        public T get() {
            return (T) value;
        }

        public void set(T value) {
            this.value = value;
        }
    }

    public static class StringImpl extends GenericBase<String> {}
}
