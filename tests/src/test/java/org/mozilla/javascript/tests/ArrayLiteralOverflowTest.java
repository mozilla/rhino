package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Regression tests for addUint8 overflow in CodeGenerator.visitArrayLiteral.
 *
 * <p>Two fields were encoded as uint8 and overflow when literalIds grows past 255:
 *
 * <ul>
 *   <li>skipIndexesId – index into literalIds for the skip-indexes array of a sparse array literal
 *       (elisions). Fixed by switching to addUint16.
 *   <li>sourcePositions[i] – per-spread source position used to place spread elements at the right
 *       index inside a sparse array. Fixed likewise.
 * </ul>
 *
 * <p>Note: cases with multiple spread elements inside a single sparse array (e.g. [...[1], ,
 * ...[2,3], , ...[4]]) expose a separate pre-existing bug in NewLiteralStorage.spreadAdjustments
 * sizing and are not tested here.
 */
public class ArrayLiteralOverflowTest {

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Returns a script that defines n object literals then appends body. */
    private static String withObjectLiterals(int n, String body) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append("var o").append(i).append(" = {k: ").append(i).append("};\n");
        }
        sb.append(body);
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 1. Original regression: skipIndexesId overflow
    //    (plain sparse array, no spread)
    // ------------------------------------------------------------------

    @Test
    public void testLiteralIdsOverflowWithSparseArray() {
        Utils.assertWithAllModes(
                "1/3", withObjectLiterals(260, "var a = [1, , 3]; a[0] + '/' + a[2];\n"));
    }

    // ------------------------------------------------------------------
    // 2. Boundary values for skipIndexesId overflow
    // ------------------------------------------------------------------

    @Test
    public void testSkipIndexesIdAt254Literals() {
        Utils.assertWithAllModes(
                "1/3", withObjectLiterals(254, "var a = [1, , 3]; a[0] + '/' + a[2];\n"));
    }

    @Test
    public void testSkipIndexesIdAt255Literals() {
        Utils.assertWithAllModes(
                "1/3", withObjectLiterals(255, "var a = [1, , 3]; a[0] + '/' + a[2];\n"));
    }

    @Test
    public void testSkipIndexesIdAt256Literals() {
        Utils.assertWithAllModes(
                "1/3", withObjectLiterals(256, "var a = [1, , 3]; a[0] + '/' + a[2];\n"));
    }

    // ------------------------------------------------------------------
    // 3. Hole positions must be correct after overflow
    // ------------------------------------------------------------------

    @Test
    public void testHoleIsAbsentAfterOverflow() {
        Utils.assertWithAllModes(
                "1/absent/3",
                withObjectLiterals(
                        260,
                        "var a = [1, , 3];\n"
                                + "a[0] + '/'"
                                + " + ((1 in a) ? 'present' : 'absent') + '/'"
                                + " + a[2];\n"));
    }

    @Test
    public void testLengthAfterOverflow() {
        Utils.assertWithAllModes(
                "1/3/3",
                withObjectLiterals(260, "var a = [1, , 3]; a[0] + '/' + a[2] + '/' + a.length;\n"));
    }

    @Test
    public void testMultipleHolesAfterOverflow() {
        // [1, , , 4] — two consecutive holes; both must be absent
        Utils.assertWithAllModes(
                "1/absent/absent/4",
                withObjectLiterals(
                        260,
                        "var a = [1, , , 4];\n"
                                + "a[0] + '/'"
                                + " + ((1 in a) ? 'present' : 'absent') + '/'"
                                + " + ((2 in a) ? 'present' : 'absent') + '/'"
                                + " + a[3];\n"));
    }

    @Test
    public void testLeadingHoleAfterOverflow() {
        Utils.assertWithAllModes(
                "absent/1/2",
                withObjectLiterals(
                        260,
                        "var a = [, 1, 2];\n"
                                + "((0 in a) ? 'present' : 'absent') + '/'"
                                + " + a[1] + '/' + a[2];\n"));
    }

    @Test
    public void testTrailingHoleAfterOverflow() {
        Utils.assertWithAllModes(
                "1/2/3",
                withObjectLiterals(
                        260, "var a = [1, 2, ,];\n" + "a[0] + '/' + a[1] + '/' + a.length;\n"));
    }

    // ------------------------------------------------------------------
    // 4. Multiple sparse arrays in one script
    // ------------------------------------------------------------------

    @Test
    public void testTwoSparseArraysAfterOverflow() {
        Utils.assertWithAllModes(
                "1/3/4/6",
                withObjectLiterals(
                        260,
                        "var a = [1, , 3];\n"
                                + "var b = [4, , 6];\n"
                                + "a[0] + '/' + a[2] + '/' + b[0] + '/' + b[2];\n"));
    }

    @Test
    public void testSparseArrayInsideFunctionAfterOverflow() {
        Utils.assertWithAllModes(
                "1/3",
                withObjectLiterals(
                        260,
                        "function f() { return [1, , 3]; }\n"
                                + "var a = f(); a[0] + '/' + a[2];\n"));
    }

    // ------------------------------------------------------------------
    // 5. sourcePositions overflow: spread inside a sparse array
    // ------------------------------------------------------------------

    /** Original regression test kept for continuity. */
    @Test
    public void testManySpreadArrayLiterals() {
        Utils.assertWithAllModes(
                "0/42",
                withObjectLiterals(
                        260,
                        "var base = [0];\n"
                                + "var result = [...base, , 42];\n"
                                + "result[0] + '/' + result[2];\n"));
    }

    @Test
    public void testSpreadSourcePositionAt255Literals() {
        Utils.assertWithAllModes(
                "10/99", withObjectLiterals(255, "var a = [...[10], , 99]; a[0] + '/' + a[2];\n"));
    }

    @Test
    public void testSpreadElementIsAtCorrectIndexAfterOverflow() {
        // [...[10, 20], , 42] → [10, 20, <hole>, 42]
        Utils.assertWithAllModes(
                "10/20/absent/42",
                withObjectLiterals(
                        260,
                        "var a = [...[10, 20], , 42];\n"
                                + "a[0] + '/' + a[1] + '/'"
                                + " + ((2 in a) ? 'present' : 'absent') + '/'"
                                + " + a[3];\n"));
    }

    @Test
    public void testHoleAfterSpreadIsAbsentAfterOverflow() {
        // [...[1, 2], , 99] → [1, 2, <hole>, 99]
        Utils.assertWithAllModes(
                "1/2/absent/99",
                withObjectLiterals(
                        260,
                        "var a = [...[1, 2], , 99];\n"
                                + "a[0] + '/' + a[1] + '/'"
                                + " + ((2 in a) ? 'present' : 'absent') + '/'"
                                + " + a[3];\n"));
    }

    @Test
    public void testSpreadAtStartOfSparseArrayAfterOverflow() {
        // [...[1, 2], , 3] → [1, 2, <hole>, 3]
        Utils.assertWithAllModes(
                "1/2/absent/3",
                withObjectLiterals(
                        260,
                        "var a = [...[1, 2], , 3];\n"
                                + "a[0] + '/' + a[1] + '/'"
                                + " + ((2 in a) ? 'present' : 'absent') + '/'"
                                + " + a[3];\n"));
    }

    @Test
    public void testSpreadAtEndOfSparseArrayAfterOverflow() {
        // [, 1, ...[2, 3]] → [<hole>, 1, 2, 3]
        Utils.assertWithAllModes(
                "absent/1/2/3",
                withObjectLiterals(
                        260,
                        "var a = [, 1, ...[2, 3]];\n"
                                + "((0 in a) ? 'present' : 'absent') + '/'"
                                + " + a[1] + '/' + a[2] + '/' + a[3];\n"));
    }

    @Test
    public void testSpreadLengthAfterOverflow() {
        // [...[1, 2], , 3] → length 4
        Utils.assertWithAllModes(
                "1/2/absent/3/4",
                withObjectLiterals(
                        260,
                        "var a = [...[1, 2], , 3];\n"
                                + "a[0] + '/' + a[1] + '/'"
                                + " + ((2 in a) ? 'present' : 'absent') + '/'"
                                + " + a[3] + '/' + a.length;\n"));
    }
}
