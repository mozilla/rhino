package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Regression test for addUint8 overflow in CodeGenerator.visitArrayLiteral. When a script contains
 * more than 255 object/array literals, the literalIds table grows beyond 255, and skipIndexesId
 * (encoded as uint8) overflows.
 */
public class ArrayLiteralOverflowTest {

    /**
     * Each object literal {k:v} adds one entry to literalIds. After 256 object literals, the next
     * sparse array literal (with elisions) gets a skipIndexesId > 255, which overflows the uint8
     * encoding.
     */
    @Test
    public void testLiteralIdsOverflowWithSparseArray() {
        StringBuilder sb = new StringBuilder();

        // Create 260 object literals to push literalIds past 255
        for (int i = 0; i < 260; i++) {
            sb.append("var o").append(i).append(" = {k: ").append(i).append("};\n");
        }

        // Now create a sparse array (with elision) that needs skipIndexes
        // [1, , 3] has a "hole" at index 1 which triggers skipIndexes
        sb.append("var sparse = [1, , 3];\n").append("sparse[0] + sparse[2];\n");

        Utils.assertWithAllModes(4.0d, sb.toString());
    }

    /** Tests spread with sparse arrays when literalIds has grown past 255. */
    @Test
    public void testManySpreadArrayLiterals() {
        StringBuilder sb = new StringBuilder();
        sb.append("var base = [0];\n");

        // 260 object literals to fill literalIds
        for (int i = 0; i < 260; i++) {
            sb.append("var o").append(i).append(" = {k: ").append(i).append("};\n");
        }

        // Sparse array with spread: triggers both skipIndexesId and sourcePositions overflow
        sb.append("var result = [...base, , 42];\n").append("result[2];\n");

        Utils.assertWithAllModes(42.0d, sb.toString());
    }
}
