package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

/**
 * Regression test for addUint8 overflow in CodeGenerator.visitArrayLiteral.
 * When a script contains more than 255 object/array literals, the literalIds
 * table grows beyond 255, and skipIndexesId (encoded as uint8) overflows.
 */
public class ArrayLiteralOverflowTest {

    @Test
    public void testLiteralIdsOverflowWithSparseArrayInterpreted() {
        runLiteralIdsOverflow(-1);
    }

    @Test
    public void testLiteralIdsOverflowWithSparseArrayCompiled() {
        runLiteralIdsOverflow(9);
    }

    @Test
    public void testManySpreadArrayLiteralsInterpreted() {
        runManySpreadArrayLiterals(-1);
    }

    @Test
    public void testManySpreadArrayLiteralsCompiled() {
        runManySpreadArrayLiterals(9);
    }

    /**
     * Each object literal {k:v} adds one entry to literalIds.
     * After 256 object literals, the next sparse array literal (with elisions)
     * gets a skipIndexesId > 255, which overflows the uint8 encoding.
     */
    private void runLiteralIdsOverflow(int optimizationLevel) {
        StringBuilder sb = new StringBuilder();
        // Create 260 object literals to push literalIds past 255
        for (int i = 0; i < 260; i++) {
            sb.append("var o").append(i).append(" = {k: ").append(i).append("};\n");
        }
        // Now create a sparse array (with elision) that needs skipIndexes
        // [1, , 3] has a "hole" at index 1 which triggers skipIndexes
        sb.append("var sparse = [1, , 3];\n");
        sb.append("sparse[0] + sparse[2];\n");
        String script = sb.toString();

        ContextFactory factory = new ContextFactory();
        factory.call(cx -> {
            cx.setOptimizationLevel(optimizationLevel);
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();
            Object result = cx.evaluateString(scope, script, "test.js", 1, null);
            assertEquals(4.0, ((Number) result).doubleValue(), 0.0);
            return null;
        });
    }

    /**
     * Tests spread with sparse arrays when literalIds has grown past 255.
     */
    private void runManySpreadArrayLiterals(int optimizationLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append("var base = [0];\n");
        // 260 object literals to fill literalIds
        for (int i = 0; i < 260; i++) {
            sb.append("var o").append(i).append(" = {k: ").append(i).append("};\n");
        }
        // Sparse array with spread: triggers both skipIndexesId and sourcePositions overflow
        sb.append("var result = [...base, , 42];\n");
        sb.append("result[2];\n");
        String script = sb.toString();

        ContextFactory factory = new ContextFactory();
        factory.call(cx -> {
            cx.setOptimizationLevel(optimizationLevel);
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();
            Object result = cx.evaluateString(scope, script, "test.js", 1, null);
            assertEquals(42.0, ((Number) result).doubleValue(), 0.0);
            return null;
        });
    }
}
