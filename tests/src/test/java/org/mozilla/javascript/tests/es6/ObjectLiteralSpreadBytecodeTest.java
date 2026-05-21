package org.mozilla.javascript.tests.es6;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.*;
import org.mozilla.javascript.testutils.Utils;

/**
 * Regression tests for fixed-width Icode.SPREAD encoding in object literals.
 *
 * <p>visitObjectLiteralWithSpread() emitted Icode.SPREAD with no operand bytes. Once
 * bytecodeSpan(Icode.SPREAD) was fixed to return 1+2, the linear bytecode scanners would skip 2
 * extra bytes after every object spread, misreading real opcodes as operand data.
 *
 * <p>The desync is tested indirectly: if bytecodeSpan is wrong, instructions after the spread are
 * misread, causing wrong values or incorrect line numbers on exceptions — all observable without
 * internal APIs.
 */
public class ObjectLiteralSpreadBytecodeTest {

    // ------------------------------------------------------------------
    // 1. Runtime correctness: the unconditional addUint16(0) in
    //    visitObjectLiteralWithSpread must not change observable behaviour.
    // ------------------------------------------------------------------

    @Test
    public void testBasicObjectSpreadStillWorks() {
        Utils.assertWithAllModes_ES6("1/2", "var a={x:1}; var b={...a,y:2}; b.x + '/' + b.y;");
    }

    @Test
    public void testMultipleObjectSpreadsStillWork() {
        Utils.assertWithAllModes_ES6(
                "1/2/3",
                "var a={x:1}; var b={y:2}; var c={z:3};"
                        + "var r={...a,...b,...c}; r.x + '/' + r.y + '/' + r.z;");
    }

    @Test
    public void testObjectSpreadOverridesStillWork() {
        // Later spread wins; first x:1 is overwritten by x:2.
        Utils.assertWithAllModes_ES6(2.0, "var r={x:1,...{x:2}}; r.x;");
    }

    @Test
    public void testObjectSpreadWithNullStillIgnored() {
        Utils.assertWithAllModes_ES6(1, "var r={...null,a:1}; r.a;");
    }

    @Test
    public void testObjectSpreadInsideFunction() {
        Utils.assertWithAllModes_ES6(
                "1/2",
                "function merge(a,b){return {...a,...b};}"
                        + "var r=merge({x:1},{y:2}); r.x + '/' + r.y;");
    }

    @Test
    public void testObjectSpreadAfterManyLiterals() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 260; i++) {
            sb.append("var o").append(i).append(" = {k:").append(i).append("};\n");
        }
        sb.append("var r = {...o5, extra:42}; o5.k + '/' + r.extra;");
        Utils.assertWithAllModes_ES6("5/42", sb.toString());
    }

    // ------------------------------------------------------------------
    // 2. Code *after* the spread must execute correctly.
    //    If bytecodeSpan(Icode.SPREAD) returns 1 instead of 3, the 2
    //    operand bytes of the now-unconditional uint16 are misread as
    //    the next opcode(s), breaking any computation that follows.
    // ------------------------------------------------------------------

    @Test
    public void testCodeAfterObjectSpreadExecutesCorrectly() {
        Utils.assertWithAllModes_ES6(
                "1/9", "var a = {x:1};\n" + "var b = {...a};\n" + "b.x + '/' + 9;\n");
    }

    @Test
    public void testConditionalAfterObjectSpreadIsCorrect() {
        Utils.assertWithAllModes_ES6(
                "true/yes",
                "var a = {flag:true};\n"
                        + "var b = {...a};\n"
                        + "b.flag + '/' + (b.flag ? 'yes' : 'no');\n");
    }

    @Test
    public void testLoopAfterObjectSpreadIsCorrect() {
        Utils.assertWithAllModes_ES6(
                "1/10",
                "var a = {x:1};\n"
                        + "var b = {...a};\n"
                        + "var s = 0;\n"
                        + "for (var i=0;i<10;i++) s+=b.x;\n"
                        + "b.x + '/' + s;\n");
    }

    @Test
    public void testFunctionCallAfterObjectSpreadIsCorrect() {
        Utils.assertWithAllModes_ES6(
                "2/3",
                "function add(p,q){return p+q;}\n"
                        + "var a = {x:2};\n"
                        + "var b = {...a};\n"
                        + "b.x + '/' + add(b.x, 1);\n");
    }

    @Test
    public void testMultipleObjectSpreadsCodeAfterIsCorrect() {
        // Two spreads → 4 extra bytes skipped if span == 1.
        Utils.assertWithAllModes_ES6(
                "10/20",
                "var p = {a:10};\n"
                        + "var q = {b:20};\n"
                        + "var r = {...p,...q};\n"
                        + "r.a + '/' + r.b;\n");
    }

    // ------------------------------------------------------------------
    // 3. Error line numbers: pcSourceLineStart is driven by Icode.LINE
    //    instructions found via bytecodeSpan(). If the span for
    //    Icode.SPREAD is wrong, the line counter drifts.
    // ------------------------------------------------------------------

    @Test
    public void testErrorLineAfterObjectSpreadIsCorrect() {
        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.evaluateString(
                    cx.initStandardObjects(),
                    "var a = {x:1};\n" // line 1
                            + "var b = {...a};\n" // line 2 (Icode.SPREAD here)
                            + "undefinedVar;\n", // line 3 ← must be reported
                    "test",
                    1,
                    null);
            fail("Expected ReferenceError");
        } catch (EcmaError e) {
            assertEquals(
                    3,
                    e.lineNumber(),
                    "ReferenceError must be on line 3; "
                            + "wrong line means bytecodeSpan(Icode.SPREAD) is incorrect");
        } finally {
            Context.exit();
        }
    }

    @Test
    public void testErrorLineAfterMultipleObjectSpreadsIsCorrect() {
        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.evaluateString(
                    cx.initStandardObjects(),
                    "var a = {x:1};\n" // line 1
                            + "var b = {y:2};\n" // line 2
                            + "var c = {...a,...b};\n" // line 3 (two Icode.SPREADs)
                            + "undefinedVar;\n", // line 4 ← must be reported
                    "test",
                    1,
                    null);
            fail("Expected ReferenceError");
        } catch (EcmaError e) {
            assertEquals(
                    4, e.lineNumber(), "ReferenceError must be on line 4 after two object spreads");
        } finally {
            Context.exit();
        }
    }

    @Test
    public void testErrorLineWithMixedSpreadsIsCorrect() {
        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.evaluateString(
                    cx.initStandardObjects(),
                    "var obj = {x:1};\n" // line 1
                            + "var arr = [1,2];\n" // line 2
                            + "var o2 = {...obj, y:2};\n" // line 3 (object Icode.SPREAD)
                            + "var a2 = [...arr, 3];\n" // line 4 (array Icode.SPREAD)
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
    // 4. Mixed spread types: object and array spreads must coexist.
    // ------------------------------------------------------------------

    @Test
    public void testObjectSpreadMixedWithArraySpread() {
        Utils.assertWithAllModes_ES6(
                "1/2/3",
                "var obj = {x:1};\n"
                        + "var arr = [1,2];\n"
                        + "var o2  = {...obj, y:2};\n"
                        + "var a2  = [...arr, 3];\n"
                        + "o2.x + '/' + o2.y + '/' + a2[2];\n");
    }

    @Test
    public void testObjectSpreadMixedWithSparseArraySpread() {
        Utils.assertWithAllModes_ES6(
                "1/2/absent/99",
                "var base = {v:1};\n"
                        + "var obj  = {...base, w:2};\n" // object spread
                        + "var arr  = [...[10,20], , 99];\n" // array spread in sparse array
                        + "obj.v + '/' + obj.w + '/'"
                        + " + ((2 in arr) ? 'present' : 'absent') + '/'"
                        + " + arr[3];\n");
    }
}
