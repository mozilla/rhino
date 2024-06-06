package org.mozilla.javascript.tests.es5;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

public class GeneratorToStringTest {
    @Test
    public void generatorsTest() {
        String code = "  function* f() {\n" + "    yield 1;\n" + "  }; f.toString();";
        Utils.assertWithAllOptimizationLevelsES6(
                "\n" + "function* f() {\n" + "    yield 1;\n" + "}\n", code);
    }
}
