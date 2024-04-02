package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.tests.Utils;

public class GeneratorToStringTest {
    private Context cx;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
    }

    @After
    public void tearDown() {
        Context.exit();
    }

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
