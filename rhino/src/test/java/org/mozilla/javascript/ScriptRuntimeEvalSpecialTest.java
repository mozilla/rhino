package org.mozilla.javascript;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class ScriptRuntimeEvalSpecialTest {
    @Test
    public void worksWithAnObject() {
        canUseEvalSpecialWithThisSetTo(new NativeObject());
    }

    @Test
    public void worksWithNull() {
        canUseEvalSpecialWithThisSetTo(null);
    }

    @Test
    public void worksWithUndefined() {
        canUseEvalSpecialWithThisSetTo(Undefined.instance);
    }

    private static void canUseEvalSpecialWithThisSetTo(Object thisArg) {
        Utils.runWithAllModes(
                cx -> {
                    ScriptableObject scope = cx.initStandardObjects();
                    Object o =
                            ScriptRuntime.evalSpecial(
                                    cx, scope, thisArg, new Object[] {"true"}, "", 0);
                    assertEquals(true, o);
                    return null;
                });
    }
}
