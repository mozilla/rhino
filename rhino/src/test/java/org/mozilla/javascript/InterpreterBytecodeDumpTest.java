/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

// Simple smoke tests for dumping of the bytecode generated in interpreter mode
class InterpreterBytecodeDumpTest {
    @Test
    void basicSmokeTest() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 7",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [2] SHORTNUMBER 42",
                        " [5] POP_RESULT",
                        " [6] RETURN_RESULT",
                        ""),
                getByteCodeFrom("42"));
    }

    @Test
    void functionNamesArePrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for f, length = 3",
                        "MaxStack = 0",
                        " [0] LINE : 1:14",
                        " [2] RETUNDEF",
                        "ICode dump, for null, length = 1",
                        "MaxStack = 0",
                        " [0] RETURN_RESULT",
                        ""),
                getByteCodeFrom("function f() {}"));
    }

    @Test
    void methodsArePrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for f, length = 3",
                        "MaxStack = 0",
                        " [0] LINE : 1:11",
                        " [2] RETUNDEF",
                        "ICode dump, for null, length = 15",
                        "MaxStack = 4",
                        " [0] LINE : 1:1",
                        " [2] REG_STR_C0 \"o\"",
                        " [3] BINDNAME",
                        " [4] REG_IND_C0",
                        " [5] LITERAL_NEW_OBJECT [f] false",
                        " [7] REG_IND_C0",
                        " [8] METHOD_EXPR #0",
                        " [9] LITERAL_SET",
                        " [10] OBJECTLIT",
                        " [11] REG_STR_C0 \"o\"",
                        " [12] SETNAME",
                        " [13] POP_RESULT",
                        " [14] RETURN_RESULT",
                        ""),
                getByteCodeFrom("o = { f() {} }"));
    }

    @Test
    void bigIntsArePrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 19",
                        "MaxStack = 2",
                        " [0] LINE : 1:1",
                        " [2] REG_BIGINT_C0 1n",
                        " [3] BIGINT",
                        " [4] REG_BIGINT_C1 2n",
                        " [5] BIGINT",
                        " [6] ADD",
                        " [7] REG_BIGINT_C2 3n",
                        " [8] BIGINT",
                        " [9] ADD",
                        " [10] REG_BIGINT_C3 4n",
                        " [11] BIGINT",
                        " [12] ADD",
                        " [13] LOAD_BIGINT1 5n",
                        " [15] BIGINT",
                        " [16] ADD",
                        " [17] POP_RESULT",
                        " [18] RETURN_RESULT",
                        ""),
                getByteCodeFrom("1n + 2n + 3n + 4n + 5n"));
    }

    @Test
    void branchInstructionsArePrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 18",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [2] REG_STR_C0 \"x\"",
                        " [3] NAME",
                        " [4] IFNE 13",
                        " [7] SHORTNUMBER 42",
                        " [10] GOTO 16",
                        " [13] SHORTNUMBER 43",
                        " [16] POP_RESULT",
                        " [17] RETURN_RESULT",
                        ""),
                getByteCodeFrom("x ? 42 : 43"));
    }

    @Test
    void nullishCoalescingBranchIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 13",
                        "MaxStack = 2",
                        " [0] LINE : 1:1",
                        " [2] REG_STR_C0 \"x\"",
                        " [3] NAME",
                        " [4] DUP",
                        " [5] IF_NOT_NULL_UNDEF 11",
                        " [8] POP",
                        " [9] REG_STR_C1 \"y\"",
                        " [10] NAME",
                        " [11] POP_RESULT",
                        " [12] RETURN_RESULT",
                        ""),
                getByteCodeFrom("x ?? y"));
    }

    @Test
    void optionalChainingBranchIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 19",
                        "MaxStack = 2",
                        " [0] LINE : 1:1",
                        " [2] REG_STR_C0 \"x\"",
                        " [3] NAME",
                        " [4] POS : 1:4",
                        " [6] DUP",
                        " [7] IF_NULL_UNDEF 15",
                        " [10] REG_STR_C1 \"y\"",
                        " [11] GETPROP",
                        " [12] GOTO 17",
                        " [15] POP",
                        " [16] UNDEF",
                        " [17] POP_RESULT",
                        " [18] RETURN_RESULT",
                        ""),
                getByteCodeFrom("x?.y"));
    }

    @Test
    void nameIncDecIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 7",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [2] REG_STR_C0 \"x\"",
                        " [3] NAME_INC_DEC 2",
                        " [5] POP_RESULT",
                        " [6] RETURN_RESULT",
                        ""),
                getByteCodeFrom("x++"));
    }

    @Test
    void callSpecialIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 14",
                        "MaxStack = 3",
                        " [0] LINE : 1:1",
                        " [2] REG_STR_C0 \"eval\"",
                        " [3] NAME_AND_THIS",
                        " [4] REG_STR_C1 \"1\"",
                        " [5] STRING",
                        " [6] REG_IND_C1",
                        " [7] CALLSPECIAL 1 false 1 1",
                        " [12] POP_RESULT",
                        " [13] RETURN_RESULT",
                        ""),
                getByteCodeFrom("eval('1')"));
    }

    @Test
    void catchScopeAndExceptionTableArePrinted() throws IOException {
        var output = getByteCodeFrom("try { x } catch(e) { e }");
        assertTrue(output.contains("CATCH_SCOPE "));
        assertTrue(output.contains("Exception handlers: "));
        assertTrue(output.contains("type=catch"));
    }

    @Test
    void finallyExceptionTableIsPrinted() throws IOException {
        var output = getByteCodeFrom("try { x } finally { y }");
        assertTrue(output.contains("GOSUB "));
        assertTrue(output.contains("type=finally"));
    }

    @Test
    void regexpIsPrinted() throws IOException {
        var output = getByteCodeFrom("/abc/g");
        assertTrue(output.contains("REGEXP "));
    }

    @Test
    void sparseArrayLitIsPrinted() throws IOException {
        var output = getByteCodeFrom("[1,,2]");
        assertTrue(output.contains("SPARE_ARRAYLIT "));
    }

    @Test
    void closureExprIsPrinted() throws IOException {
        var output = getByteCodeFrom("(function() {})");
        assertTrue(output.contains("CLOSURE_EXPR #0"));
    }

    @Test
    void functionCallIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 12",
                        "MaxStack = 4",
                        " [0] LINE : 1:1",
                        " [2] REG_STR_C0 \"f\"",
                        " [3] NAME_AND_THIS",
                        " [4] ONE",
                        " [5] SHORTNUMBER 2",
                        " [8] REG_IND_C2",
                        " [9] CALL 2",
                        " [10] POP_RESULT",
                        " [11] RETURN_RESULT",
                        ""),
                getByteCodeFrom("f(1, 2)"));
    }

    @Test
    void newExpressionIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 8",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [2] REG_STR_C0 \"F\"",
                        " [3] NAME",
                        " [4] REG_IND_C0",
                        " [5] NEW 0",
                        " [6] POP_RESULT",
                        " [7] RETURN_RESULT",
                        ""),
                getByteCodeFrom("new F()"));
    }

    @Test
    void throwIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 7",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [2] ONE",
                        " [3] THROW : 1",
                        " [6] RETURN_RESULT",
                        ""),
                getByteCodeFrom("throw 1"));
    }

    @Test
    void generatorICodeIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for g, length = 13",
                        "MaxStack = 1",
                        " [0] GENERATOR : 1",
                        " [3] LINE : 1:17",
                        " [5] ONE",
                        " [6] YIELD : 1",
                        " [9] POP",
                        " [10] GENERATOR_END : 1",
                        "ICode dump, for null, length = 1",
                        "MaxStack = 0",
                        " [0] RETURN_RESULT",
                        ""),
                getByteCodeFrom("function* g() { yield 1; }"));
    }

    @Test
    void yieldStarIsPrinted() throws IOException {
        var output = getByteCodeFrom("function* g() { yield* [1]; }");
        assertTrue(output.contains("YIELD_STAR : "));
    }

    @Test
    void generatorReturnIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for g, length = 9",
                        "MaxStack = 1",
                        " [0] GENERATOR : 1",
                        " [3] LINE : 1:17",
                        " [5] ONE",
                        " [6] GENERATOR_RETURN : 1",
                        "ICode dump, for null, length = 1",
                        "MaxStack = 0",
                        " [0] RETURN_RESULT",
                        ""),
                getByteCodeFrom("function* g() { return 1; }"));
    }

    @Test
    void intNumberIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 9",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [2] INTNUMBER 100000",
                        " [7] POP_RESULT",
                        " [8] RETURN_RESULT",
                        ""),
                getByteCodeFrom("100000"));
    }

    @Test
    void doubleNumberIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 6",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [2] REG_IND_C0",
                        " [3] NUMBER 1.5",
                        " [4] POP_RESULT",
                        " [5] RETURN_RESULT",
                        ""),
                getByteCodeFrom("1.5"));
    }

    @Test
    void multipleStringRegistersArePrinted() throws IOException {
        var output = getByteCodeFrom("a + b + c + d + e");
        assertTrue(output.contains("REG_STR_C0 \"a\""));
        assertTrue(output.contains("REG_STR_C1 \"b\""));
        assertTrue(output.contains("REG_STR_C2 \"c\""));
        assertTrue(output.contains("REG_STR_C3 \"d\""));
        assertTrue(output.contains("LOAD_STR1 \"e\""));
    }

    @Test
    void regIndConstantsArePrinted() throws IOException {
        var output =
                getByteCodeFrom(
                        "var a=function(){};var b=function(){};var c=function(){};"
                                + "var d=function(){};var e=function(){};var f=function(){}");
        assertTrue(output.contains("REG_IND_C0"));
        assertTrue(output.contains("REG_IND_C1"));
        assertTrue(output.contains("REG_IND_C2"));
        assertTrue(output.contains("REG_IND_C3"));
        assertTrue(output.contains("REG_IND_C4"));
        assertTrue(output.contains("REG_IND_C5"));
    }

    @Test
    void regInd1IsPrinted() throws IOException {
        var output =
                getByteCodeFrom(
                        "var a=function(){};var b=function(){};var c=function(){};"
                                + "var d=function(){};var e=function(){};var f=function(){};"
                                + "var g=function(){}");
        assertTrue(output.contains("LOAD_IND1 6"));
    }

    @Test
    void getvar1Setvar1ArePrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for f, length = 11",
                        "MaxStack = 1",
                        " [0] LINE : 1:16",
                        " [2] ONE",
                        " [3] SETVAR1 0",
                        " [5] POP",
                        " [6] POS : 1:27",
                        " [8] GETVAR1 0",
                        " [10] RETURN",
                        "ICode dump, for null, length = 1",
                        "MaxStack = 0",
                        " [0] RETURN_RESULT",
                        ""),
                getByteCodeFrom("function f() { var x = 1; return x; }"));
    }

    @Test
    void setConstVar1IsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for f, length = 7",
                        "MaxStack = 1",
                        " [0] LINE : 1:16",
                        " [2] ONE",
                        " [3] SETCONSTVAR1 0",
                        " [5] POP",
                        " [6] RETUNDEF",
                        "ICode dump, for null, length = 1",
                        "MaxStack = 0",
                        " [0] RETURN_RESULT",
                        ""),
                getByteCodeFrom("function f() { const x = 1; }"));
    }

    @Test
    void switchIfeqPopIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 18",
                        "MaxStack = 3",
                        " [0] LINE : 1:1",
                        " [2] REG_STR_C0 \"x\"",
                        " [3] NAME",
                        " [4] DUP",
                        " [5] ONE",
                        " [6] SHEQ",
                        " [7] IFEQ_POP 14",
                        " [10] POP",
                        " [11] GOTO 17",
                        " [14] GOTO 17",
                        " [17] RETURN_RESULT",
                        ""),
                getByteCodeFrom("switch(x) { case 1: break; }"));
    }

    @Test
    void objectLiteralWithSpreadIsPrinted() throws IOException {
        var output = getByteCodeFrom("({...x})");
        assertTrue(output.contains("LITERAL_NEW_OBJECT "));
    }

    @Test
    void objectRestIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 34",
                        "MaxStack = 3",
                        " [0] LINE : 1:1",
                        " [2] REG_IND_C0",
                        " [3] LITERAL_NEW_OBJECT [$0] false",
                        " [5] REG_STR_C0 \"obj\"",
                        " [6] NAME",
                        " [7] LITERAL_SET",
                        " [8] OBJECTLIT",
                        " [9] ENTERWITH",
                        " [10] REG_STR_C1 \"a\"",
                        " [11] BINDNAME",
                        " [12] REG_STR_C2 \"$0\"",
                        " [13] NAME",
                        " [14] REG_STR_C1 \"a\"",
                        " [15] GETPROP",
                        " [16] REG_STR_C1 \"a\"",
                        " [17] SETNAME",
                        " [18] POP",
                        " [19] REG_STR_C3 \"rest\"",
                        " [20] BINDNAME",
                        " [21] REG_STR_C2 \"$0\"",
                        " [22] NAME",
                        " [23] REG_IND_C1",
                        " [24] OBJECT_REST excluding [static: \"a\"; computed: 0]",
                        " [26] REG_STR_C3 \"rest\"",
                        " [27] SETNAME",
                        " [28] POP",
                        " [29] REG_STR_C2 \"$0\"",
                        " [30] NAME",
                        " [31] LEAVEWITH",
                        " [32] POP",
                        " [33] RETURN_RESULT",
                        ""),
                getByteCodeFrom("var {a, ...rest} = obj"));
    }

    private static String getByteCodeFrom(String source) throws IOException {
        return InterpreterIcodeCapture.capture(
                () -> {
                    try (Context cx = Context.enter()) {
                        cx.setInterpretedMode(true);
                        cx.compileString(source, "test", 1, null);
                    }
                });
    }
}
