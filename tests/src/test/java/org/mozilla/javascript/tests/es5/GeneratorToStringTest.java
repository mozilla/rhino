package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.tests.Utils;

public class GeneratorToStringTest {
    @Test
    public void generatorsTest() {
        String code = "  function* f() {\n" + "    yield 1;\n" + "  }; f.toString();";
        test("\n" + "function* f() {\n" + "    yield 1;\n" + "}\n", code);
    }

    private static void test(Object expected, String js) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = new TopLevel();
                    cx.initStandardObjects(scope);

                    Object result = cx.evaluateString(scope, js, "test", 1, null);
                    assertEquals(expected, result);

                    return null;
                });
    }
}
