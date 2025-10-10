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

    private static final String SCRIPT_INIT =
            String.format("let obj = new %s();\n", BeaningTestObject.class.getName());

    @Test
    public void testMasked() {
        // 'maskedValue' is private, with getter/setter

        // get
        expect("obj.maskedValue", "_getter");

        // set & get
        expect("obj.maskedValue = '42'; obj.maskedValue", "42_setter_getter");
    }

    @Test
    public void testOverload() {
        // for 'maskedValue', the return type of getter is String, so a setter with type 'String'
        // should be preferred

        // Here there's a setter that accepts number, but the setter that accepts String should be
        // preferred instead. And 42 should be auto-wrapped into string
        expect("obj.maskedValue = 42; obj.maskedValue", "42_setter_getter");
    }

    /**
     * @see BeaningTestObject#value
     * @see BeaningTestObject#getValue()
     * @see BeaningTestObject#setValue(Number)
     */
    @Test
    public void testMaskingPublicField() {
        // public field should not be masked
        expect("obj.value", 42);
        expect("obj.value = 123; obj.value", 123);
    }

    /**
     * @see BeaningTestObject#elementAt(int)
     * @see BeaningTestObject#setElementAt(int)
     */
    @Test
    public void testMaskingMethod() {
        // method should not be masked
        Utils.assertEvaluatorExceptionES6(
                "Java method \"elementAt\" cannot be assigned to",
                SCRIPT_INIT + "obj.elementAt = 42");
    }

    /** 'get' should be preferred over 'is' */
    @Test
    public void testGetterPreference() {
        expect("obj.getInParent", "getGetInParent");
        expect("obj.isInParent", "getIsInParent");
    }

    private static void expect(String script, Object expected) {
        expect(false, script, expected);
        expect(true, script, expected);
    }

    private static void expect(boolean interpreted, String script, Object expected) {
        try (var cx = ContextFactory.getGlobal().enterContext()) {
            cx.setInterpretedMode(interpreted);

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

    public static class BeaningTestObject<N extends Number> extends BeaningTestObjectBase {
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

        public int elementAt(int index) {
            return index;
        }

        public void setElementAt(int value) {
            throw new IllegalStateException(
                    "There's an existed method 'elementAt', no beaning should be applied");
        }

        public String isGetInParent() {
            throw new IllegalStateException("'get' getter should be preferred over 'is'");
        }

        public String getIsInParent() {
            return "getIsInParent";
        }
    }

    /**
     * Methods in parent will be scanned later than those in subclass. We can utilize this to create
     * test cases for order-dependent action
     */
    public static class BeaningTestObjectBase {

        public String getGetInParent() {
            return "getGetInParent";
        }

        public String isIsInParent() {
            throw new IllegalStateException("'get' getter should be preferred over 'is'");
        }
    }
}
