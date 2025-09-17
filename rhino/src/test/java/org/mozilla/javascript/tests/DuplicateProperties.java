package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class DuplicateProperties {
    @Test
    public void unterminatedCharacterClass() {
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
}
