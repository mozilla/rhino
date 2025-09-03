package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

public class ArrowFunctionToStringTest {
    @Test
    public void EvalArrowToString() {
        String source = "eval(\"()=>this\").toString()";
        Utils.assertWithAllModes("()=>this", source);
    }
}
