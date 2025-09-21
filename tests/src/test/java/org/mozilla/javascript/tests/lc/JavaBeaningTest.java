package org.mozilla.javascript.tests.lc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.testutils.Utils;

/**
 * @author ZZZank
 */
public class JavaBeaningTest {

    @Test
    public void testMasked() {
        // 'maskedValue' is private, with getter/setter

        // get
        expect("obj.maskedValue", "_getter");

        // set & get
        expect(Utils.lines("obj.maskedValue = '42'", "obj.maskedValue"), "42_setter_getter");
    }

    @Test
    public void testPublic() {
        // 'value' is public. In this case, no getter/setter should be used
        expect("obj.value", 42);
        expect("obj.value = 123; obj.value", 123);
    }

    @Test
    public void testOverload() {
        // for 'maskedValue', the return type of getter is String, so a setter with type 'String'
        // should be preferred

        // Here there's a setter that accepts number, but the setter that accepts String should be
        // preferred instead. And 42 should be auto-wrapped into string
        expect("obj.maskedValue = 42; obj.maskedValue", "42_setter_getter");
    }

    private static void expect(String script, Object expected) {
        for (int i = 0; i < 2; i++) {
            try (var cx = ContextFactory.getGlobal().enterContext()) {
                cx.setInterpretedMode(i == 0);

                var scope = cx.initStandardObjects();

                var testObject = new BeaningTestObject<>();
                scope.put("obj", scope, testObject);

                var result = cx.evaluateString(scope, script, "test.js", 1, null);
                while (result instanceof Wrapper) {
                    result = ((Wrapper) result).unwrap();
                }
                Assertions.assertEquals(expected, result);
            }
        }
    }

    public static class BeaningTestObject<N extends Number> {
        private String maskedValue = "";
        public Integer value = 42;

        public String getMaskedValue() {
            return maskedValue + "_getter";
        }

        public void setMaskedValue(String maskedValue) {
            this.maskedValue = maskedValue + "_setter";
        }

        public void setMaskedValue(Integer value) {
            throw new IllegalStateException("This setter is not the preferred setter");
        }

        public void setMaskedValue() {
            throw new IllegalStateException("This is not setter");
        }

        public String getValue() {
            throw new IllegalStateException("'value' is public, field access should be preferred");
        }

        public void setValue(Number value) {
            throw new IllegalStateException("'value' is public, field access should be preferred");
        }
    }
}
