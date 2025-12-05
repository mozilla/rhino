package org.mozilla.javascript.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;

class NameDisambiguateTest {
    private static final String SOURCE =
            "(function g(x) {\n"
                    + "  {\n"
                    + "    let x = 'inner';\n"
                    + "  }\n"
                    + "  return x;\n"
                    + "})('outer');";

    // Having a debugger causes an activation frame to be used, which is handled specially in
    // Interpreter, for the GETVAR icode.
    @Test
    void letVariablesAreHandledCorrectlyWithDebugger() {
        try (Context cx = Context.enter()) {
            cx.setDebugger(new NoOpDebugger(), null);
            cx.setGeneratingDebug(true);
            cx.setInterpretedMode(true);

            var scope = cx.initStandardObjects();

            Object value = cx.evaluateString(scope, SOURCE, "test", 1, null);
            assertEquals("outer", value);
        }
    }
}
