package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class NativeJavaMethodTest {

    @Test
    public void toStringOfJS() throws Exception {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    final String expected = "function valueOf() {\n\t[native code]\n}\n";
                    final String script = "java.math.BigInteger.valueOf.toString()";

                    final String result =
                            (String) cx.evaluateString(scope, script, "myScript", 1, null);

                    assertEquals(expected, result);

                    return null;
                });
    }
}
