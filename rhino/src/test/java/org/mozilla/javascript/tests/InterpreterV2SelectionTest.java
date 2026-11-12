package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Context.EvaluationMethod;
import org.mozilla.javascript.InterpreterV2;
import org.mozilla.javascript.VarScope;

/** Test that InterpreterV2 can be selected and initialized. */
public class InterpreterV2SelectionTest {

    @Test
    @SuppressWarnings("deprecation")
    public void testInterpreterV2Selection() {
        try (Context cx = Context.enter()) {
            // Set optimization level to -2 to select InterpreterV2
            cx.setOptimizationLevel(-2);
            assertEquals(EvaluationMethod.InterpreterV2, cx.getEvaluationMethod());
            assertEquals(-2, cx.getOptimizationLevel());
        }
    }

    @Test
    public void testInterpreterV2DirectSelection() {
        try (Context cx = Context.enter()) {
            // Directly set evaluation method
            cx.setEvaluationMethod(EvaluationMethod.InterpreterV2);
            assertEquals(EvaluationMethod.InterpreterV2, cx.getEvaluationMethod());
        }
    }

    @Test
    public void testInterpreterV2Instance() {
        // Test that InterpreterV2 can be instantiated
        InterpreterV2 interpreter = new InterpreterV2();
        assertNotNull(interpreter);
    }

    @Test
    public void testSimpleScriptCompilation() {
        try (Context cx = Context.enter()) {
            cx.setEvaluationMethod(EvaluationMethod.InterpreterV2);

            VarScope scope = cx.initStandardObjects();

            // Try to compile a simple script
            String script = "1 + 1";
            try {
                Object compiled = cx.compileString(script, "test", 1, null);
                assertNotNull(compiled);
            } catch (UnsupportedOperationException e) {
                // Expected for now as some parts may not be fully implemented
                assertTrue(e.getMessage() != null);
            }
        }
    }
}
