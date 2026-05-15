package org.mozilla.javascript.tests.es6;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.*;
import org.mozilla.javascript.testutils.Utils;

/**
 * Regression tests for fixed-width Icode_SPREAD encoding in sparse array literals.
 *
 * <p>Before the fix, Icode_SPREAD inside a sparse array literal emitted a conditional 2-byte
 * operand — present only when skipIndexes != null. bytecodeSpan(Icode_SPREAD) returned 1, so
 * getLineNumbers() and dumpICode() were out of sync for any script containing a spread inside a
 * sparse array.
 *
 * <p>The desync is tested indirectly: if bytecodeSpan is wrong, instructions after the spread are
 * misread, causing wrong values or incorrect line numbers on exceptions.
 *
 * <p>Note: cases with multiple spread elements inside a single sparse array expose a separate
 * pre-existing bug in NewLiteralStorage.spreadAdjustments sizing and are not tested here.
 */
public class SpreadSparseArrayBytecodeTest {

    // ------------------------------------------------------------------
    // 1. Basic correctness: spread inside a sparse array works at all.
    // ------------------------------------------------------------------

    @Test
    public void testSpreadInSparseArray() {
        // [...[1,2], , 99] → [1, 2, <hole>, 99], length 4
        Utils.assertWithAllModes_ES6(
                "1/2/absent/99/4",
                "var a = [...[1,2], , 99];\n"
                        + "a[0] + '/' + a[1] + '/'"
                        + " + ((2 in a) ? 'present' : 'absent') + '/'"
                        + " + a[3] + '/' + a.length;\n");
    }

    @Test
    public void testSpreadAtEndOfSparseArray() {
        // [, , ...[3,4,5]] → [<hole>, <hole>, 3, 4, 5]
        Utils.assertWithAllModes_ES6(
                "absent/absent/3/4/5",
                "var a = [, , ...[3,4,5]];\n"
                        + "((0 in a) ? 'present' : 'absent') + '/'"
                        + " + ((1 in a) ? 'present' : 'absent') + '/'"
                        + " + a[2] + '/' + a[3] + '/' + a[4];\n");
    }

    // ------------------------------------------------------------------
    // 2. Code *after* the spread must execute correctly.
    //    If bytecodeSpan(Icode_SPREAD) returns 1 instead of 3, the 2
    //    operand bytes are misread as the next opcode(s), breaking any
    //    computation that follows.
    // ------------------------------------------------------------------

    @Test
    public void testCodeAfterSparseSpreadExecutesCorrectly() {
        Utils.assertWithAllModes_ES6("1/9", "var a = [...[1], , 2];\n" + "a[0] + '/' + 9;\n");
    }

    @Test
    public void testConditionalAfterSparseSpreadIsCorrect() {
        Utils.assertWithAllModes_ES6(
                "true/yes",
                "var a = [...[true], , false];\n" + "a[0] + '/' + (a[0] ? 'yes' : 'no');\n");
    }

    @Test
    public void testLoopAfterSparseSpreadIsCorrect() {
        Utils.assertWithAllModes_ES6(
                "1/10",
                "var a = [...[1], , 2];\n"
                        + "var s = 0;\n"
                        + "for (var i = 0; i < 10; i++) s += a[0];\n"
                        + "a[0] + '/' + s;\n");
    }

    @Test
    public void testFunctionCallAfterSparseSpreadIsCorrect() {
        Utils.assertWithAllModes_ES6(
                "2/3",
                "function add(p,q){return p+q;}\n"
                        + "var a = [...[2], , 9];\n"
                        + "a[0] + '/' + add(a[0], 1);\n");
    }

    @Test
    public void testTwoSparseSpreadsBytecodeConsistent() {
        // Two Icode_SPREAD instructions: 4 extra bytes if span == 1.
        Utils.assertWithAllModes_ES6(
                "10/absent/20",
                "var a = [...[10], , ...[20]];\n"
                        + "a[0] + '/'"
                        + " + ((1 in a) ? 'present' : 'absent') + '/'"
                        + " + a[2];\n");
    }

    // ------------------------------------------------------------------
    // 3. Error line numbers: if bytecodeSpan is wrong, pcSourceLineStart
    //    drifts and exceptions report the wrong line.
    // ------------------------------------------------------------------

    @Test
    public void testErrorLineAfterSparseSpreadIsCorrect() {
        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.evaluateString(
                    cx.initStandardObjects(),
                    "var a = [...[1], , 2];\n" // line 1 (Icode_SPREAD here)
                            + "undefinedVar;\n", // line 2 ← must be reported
                    "test",
                    1,
                    null);
            fail("Expected ReferenceError");
        } catch (EcmaError e) {
            assertEquals(
                    2,
                    e.lineNumber(),
                    "ReferenceError must be on line 2; "
                            + "wrong line means bytecodeSpan(Icode_SPREAD) is incorrect");
        } finally {
            Context.exit();
        }
    }

    @Test
    public void testErrorLineAfterTwoSparseSpreadIsCorrect() {
        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.evaluateString(
                    cx.initStandardObjects(),
                    "var base = [1, 2];\n" // line 1
                            + "var a = [...base, , 9];\n" // line 2 (first spread)
                            + "var b = [...base, , 8];\n" // line 3 (second spread)
                            + "var c = a[0] + '/' + b[0];\n" // line 4
                            + "undefinedVar;\n", // line 5 ← must be reported
                    "test",
                    1,
                    null);
            fail("Expected ReferenceError");
        } catch (EcmaError e) {
            assertEquals(
                    5,
                    e.lineNumber(),
                    "ReferenceError must be on line 5 after two sparse array spreads");
        } finally {
            Context.exit();
        }
    }

    @Test
    public void testErrorLineWithMixedSpreadTypesIsCorrect() {
        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.evaluateString(
                    cx.initStandardObjects(),
                    "var obj = {x:1};\n" // line 1
                            + "var arr = [1,2];\n" // line 2
                            + "var sparse = [...arr, , 9];\n" // line 3 (array Icode_SPREAD)
                            + "var merged = {...obj, y:2};\n" // line 4 (object Icode_SPREAD)
                            + "undefinedVar;\n", // line 5 ← must be reported
                    "test",
                    1,
                    null);
            fail("Expected ReferenceError");
        } catch (EcmaError e) {
            assertEquals(
                    5, e.lineNumber(), "ReferenceError must be on line 5 after mixed spread types");
        } finally {
            Context.exit();
        }
    }

    // ------------------------------------------------------------------
    // 4. Correctness after >255 literalIds (regression for the uint16
    //    skipIndexesId / sourcePositions overflow fixed in the same PR).
    // ------------------------------------------------------------------

    @Test
    public void testSpreadInSparseArrayAfter260ObjectLiterals() {
        // [...[10, 20], , 30] → [10, 20, <hole>, 30]
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 260; i++) {
            sb.append("var o").append(i).append(" = {k:").append(i).append("};\n");
        }
        sb.append("var a = [...[10, 20], , 30];\n");
        sb.append(
                "a[0] + '/' + a[1] + '/'"
                        + " + ((2 in a) ? 'present' : 'absent') + '/'"
                        + " + a[3];\n");
        Utils.assertWithAllModes_ES6("10/20/absent/30", sb.toString());
    }

    @Test
    public void testErrorLineAfter260LiteralsAndSparseSpread() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 260; i++) {
            sb.append("var o").append(i).append(" = {k:").append(i).append("};\n");
        }
        int errorLine = 262;
        sb.append("var a = [...[1], , 2];\n"); // line 261
        sb.append("undefinedVar;\n"); // line 262

        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.evaluateString(cx.initStandardObjects(), sb.toString(), "test", 1, null);
            fail("Expected ReferenceError");
        } catch (EcmaError e) {
            assertEquals(
                    errorLine,
                    e.lineNumber(),
                    "ReferenceError must be on line " + errorLine + " even with >255 literalIds");
        } finally {
            Context.exit();
        }
    }

    // ------------------------------------------------------------------
    // 5. Object-literal spread must be unaffected.
    // ------------------------------------------------------------------

    @Test
    public void testObjectLiteralSpreadUnaffected() {
        Utils.assertWithAllModes_ES6(
                "1/2/3", "var o = {...{a:1, b:2}, c:3}; o.a + '/' + o.b + '/' + o.c;");
    }

    @Test
    public void testObjectLiteralSpreadAfterManyLiterals() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 260; i++) {
            sb.append("var o").append(i).append(" = {k:").append(i).append("};\n");
        }
        sb.append("var r = {...{x:7, y:8}}; r.x + '/' + r.y;\n");
        Utils.assertWithAllModes_ES6("7/8", sb.toString());
    }
}
