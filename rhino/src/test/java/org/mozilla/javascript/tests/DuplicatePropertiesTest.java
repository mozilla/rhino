package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class DuplicatePropertiesTest {
    @Test
    public void duplicateProperty() {
        String script =
                "(function() {\n"
                        + "'use strict';\n"
                        + "const o = {foo: 1, foo: 2};\n"
                        + "return o.foo\n"
                        + "})();";

        Utils.assertEvaluatorException_1_8(
                "Property \"foo\" already defined in this object literal", script);

        Utils.assertWithAllModes_ES6(2, script);
    }

    @Test
    public void duplicatePropertyFunction() {
        String script =
                "(function() {\n"
                        + "'use strict';\n"
                        + "var o = {\n"
                        + "  foo: 1,\n"
                        + "  foo() {return 'Hello';},\n"
                        + "  foo: 2,\n"
                        + "  foo() {return 'Hello2';},\n"
                        + "};\n"
                        + "return o.foo;\n"
                        + "})()();";

        Utils.assertEvaluatorException_1_8(
                "Property \"foo\" already defined in this object literal", script);

        Utils.assertWithAllModes_ES6("Hello2", script);
    }

    @Test
    public void duplicatePropertyFunctionGetter() {
        String script =
                "(function() {\n"
                        + "'use strict';\n"
                        + "var o = {\n"
                        + "  foo: 1,\n"
                        + "  get foo() {return 'Hello2';},\n"
                        + "  get foo() {return 'Hello2';},\n"
                        + "};\n"
                        + "return o.foo;\n"
                        + "})();";

        Utils.assertEvaluatorException_1_8(
                "Property \"foo\" already defined in this object literal", script);

        Utils.assertWithAllModes_ES6("Hello2", script);
    }

    @Test
    public void duplicatePropertyFunctionSetter() {
        String script =
                "(function() {\n"
                        + "'use strict';\n"
                        + "var o = {\n"
                        + "  foo: 1,\n"
                        + "  set foo(val) {return val},\n"
                        + "  set foo(val) {return val},\n"
                        + "};\n"
                        + "return o.foo = 'Hello2';\n"
                        + "})();";

        Utils.assertEvaluatorException_1_8(
                "Property \"foo\" already defined in this object literal", script);

        Utils.assertWithAllModes_ES6("Hello2", script);
    }
}
