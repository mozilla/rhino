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
                        "ICode dump, for null, length = 6",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [1] SHORTNUMBER 42",
                        " [4] POP_RESULT",
                        " [5] RETURN_RESULT",
                        ""),
                getByteCodeFrom("42"));
    }

    @Test
    void functionNamesArePrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for f, length = 2",
                        "MaxStack = 0",
                        " [0] LINE : 1:14",
                        " [1] RETUNDEF",
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
                        "ICode dump, for f, length = 2",
                        "MaxStack = 0",
                        " [0] LINE : 1:11",
                        " [1] RETUNDEF",
                        "ICode dump, for null, length = 14",
                        "MaxStack = 4",
                        " [0] LINE : 1:1",
                        " [1] REG_STR_C0 \"o\"",
                        " [2] BINDNAME",
                        " [3] REG_IND_C0",
                        " [4] LITERAL_NEW_OBJECT [f] false",
                        " [6] REG_IND_C0",
                        " [7] METHOD_EXPR #0",
                        " [8] LITERAL_SET",
                        " [9] OBJECTLIT",
                        " [10] REG_STR_C0 \"o\"",
                        " [11] SETNAME",
                        " [12] POP_RESULT",
                        " [13] RETURN_RESULT",
                        ""),
                getByteCodeFrom("o = { f() {} }"));
    }

    @Test
    void bigIntsArePrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 18",
                        "MaxStack = 2",
                        " [0] LINE : 1:1",
                        " [1] REG_BIGINT_C0 1n",
                        " [2] BIGINT",
                        " [3] REG_BIGINT_C1 2n",
                        " [4] BIGINT",
                        " [5] ADD",
                        " [6] REG_BIGINT_C2 3n",
                        " [7] BIGINT",
                        " [8] ADD",
                        " [9] REG_BIGINT_C3 4n",
                        " [10] BIGINT",
                        " [11] ADD",
                        " [12] LOAD_BIGINT1 5n",
                        " [14] BIGINT",
                        " [15] ADD",
                        " [16] POP_RESULT",
                        " [17] RETURN_RESULT",
                        ""),
                getByteCodeFrom("1n + 2n + 3n + 4n + 5n"));
    }

    @Test
    void branchInstructionsArePrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 17",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [1] REG_STR_C0 \"x\"",
                        " [2] NAME",
                        " [3] IFNE 12",
                        " [6] SHORTNUMBER 42",
                        " [9] GOTO 15",
                        " [12] SHORTNUMBER 43",
                        " [15] POP_RESULT",
                        " [16] RETURN_RESULT",
                        ""),
                getByteCodeFrom("x ? 42 : 43"));
    }

    @Test
    void nullishCoalescingBranchIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 12",
                        "MaxStack = 2",
                        " [0] LINE : 1:1",
                        " [1] REG_STR_C0 \"x\"",
                        " [2] NAME",
                        " [3] DUP",
                        " [4] IF_NOT_NULL_UNDEF 10",
                        " [7] POP",
                        " [8] REG_STR_C1 \"y\"",
                        " [9] NAME",
                        " [10] POP_RESULT",
                        " [11] RETURN_RESULT",
                        ""),
                getByteCodeFrom("x ?? y"));
    }

    @Test
    void optionalChainingBranchIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 16",
                        "MaxStack = 2",
                        " [0] LINE : 1:1",
                        " [1] REG_STR_C0 \"x\"",
                        " [2] NAME",
                        " [3] DUP",
                        " [4] IF_NULL_UNDEF 12",
                        " [7] REG_STR_C1 \"y\"",
                        " [8] GETPROP",
                        " [9] GOTO 14",
                        " [12] POP",
                        " [13] UNDEF",
                        " [14] POP_RESULT",
                        " [15] RETURN_RESULT",
                        ""),
                getByteCodeFrom("x?.y"));
    }

    @Test
    void nameIncDecIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 6",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [1] REG_STR_C0 \"x\"",
                        " [2] NAME_INC_DEC 2",
                        " [4] POP_RESULT",
                        " [5] RETURN_RESULT",
                        ""),
                getByteCodeFrom("x++"));
    }

    @Test
    void callSpecialIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 11",
                        "MaxStack = 3",
                        " [0] LINE : 1:1",
                        " [1] REG_STR_C0 \"eval\"",
                        " [2] NAME_AND_THIS",
                        " [3] REG_STR_C1 \"1\"",
                        " [4] STRING",
                        " [5] REG_IND_C1",
                        " [6] CALLSPECIAL 1 false 1",
                        " [9] POP_RESULT",
                        " [10] RETURN_RESULT",
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
                        "ICode dump, for null, length = 11",
                        "MaxStack = 4",
                        " [0] LINE : 1:1",
                        " [1] REG_STR_C0 \"f\"",
                        " [2] NAME_AND_THIS",
                        " [3] ONE",
                        " [4] SHORTNUMBER 2",
                        " [7] REG_IND_C2",
                        " [8] CALL 2",
                        " [9] POP_RESULT",
                        " [10] RETURN_RESULT",
                        ""),
                getByteCodeFrom("f(1, 2)"));
    }

    @Test
    void newExpressionIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 7",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [1] REG_STR_C0 \"F\"",
                        " [2] NAME",
                        " [3] REG_IND_C0",
                        " [4] NEW 0",
                        " [5] POP_RESULT",
                        " [6] RETURN_RESULT",
                        ""),
                getByteCodeFrom("new F()"));
    }

    @Test
    void throwIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 4",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [1] ONE",
                        " [2] THROW",
                        " [3] RETURN_RESULT",
                        ""),
                getByteCodeFrom("throw 1"));
    }

    @Test
    void generatorICodeIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for g, length = 6",
                        "MaxStack = 1",
                        " [0] GENERATOR",
                        " [1] LINE : 1:15",
                        " [2] ONE",
                        " [3] YIELD",
                        " [4] POP",
                        " [5] GENERATOR_END",
                        "ICode dump, for null, length = 1",
                        "MaxStack = 0",
                        " [0] RETURN_RESULT",
                        ""),
                getByteCodeFrom("function* g() { yield 1; }"));
    }

    @Test
    void yieldStarIsPrinted() throws IOException {
        var output = getByteCodeFrom("function* g() { yield* [1]; }");
        assertTrue(output.contains("YIELD_STAR"));
    }

    @Test
    void generatorReturnIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for g, length = 4",
                        "MaxStack = 1",
                        " [0] GENERATOR",
                        " [1] LINE : 1:15",
                        " [2] ONE",
                        " [3] GENERATOR_RETURN",
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
                        "ICode dump, for null, length = 8",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [1] INTNUMBER 100000",
                        " [6] POP_RESULT",
                        " [7] RETURN_RESULT",
                        ""),
                getByteCodeFrom("100000"));
    }

    @Test
    void doubleNumberIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 5",
                        "MaxStack = 1",
                        " [0] LINE : 1:1",
                        " [1] REG_IND_C0",
                        " [2] NUMBER 1.5",
                        " [3] POP_RESULT",
                        " [4] RETURN_RESULT",
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
                        "ICode dump, for f, length = 8",
                        "MaxStack = 1",
                        " [0] LINE : 1:14",
                        " [1] ONE",
                        " [2] SETVAR1 0",
                        " [4] POP",
                        " [5] GETVAR1 0",
                        " [7] RETURN",
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
                        "ICode dump, for f, length = 6",
                        "MaxStack = 1",
                        " [0] LINE : 1:14",
                        " [1] ONE",
                        " [2] SETCONSTVAR1 0",
                        " [4] POP",
                        " [5] RETUNDEF",
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
                        "ICode dump, for null, length = 17",
                        "MaxStack = 3",
                        " [0] LINE : 1:1",
                        " [1] REG_STR_C0 \"x\"",
                        " [2] NAME",
                        " [3] DUP",
                        " [4] ONE",
                        " [5] SHEQ",
                        " [6] IFEQ_POP 13",
                        " [9] POP",
                        " [10] GOTO 16",
                        " [13] GOTO 16",
                        " [16] RETURN_RESULT",
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
                        "ICode dump, for null, length = 33",
                        "MaxStack = 3",
                        " [0] LINE : 1:1",
                        " [1] REG_IND_C0",
                        " [2] LITERAL_NEW_OBJECT [$0] false",
                        " [4] REG_STR_C0 \"obj\"",
                        " [5] NAME",
                        " [6] LITERAL_SET",
                        " [7] OBJECTLIT",
                        " [8] ENTERWITH",
                        " [9] REG_STR_C1 \"a\"",
                        " [10] BINDNAME",
                        " [11] REG_STR_C2 \"$0\"",
                        " [12] NAME",
                        " [13] REG_STR_C1 \"a\"",
                        " [14] GETPROP",
                        " [15] REG_STR_C1 \"a\"",
                        " [16] SETNAME",
                        " [17] POP",
                        " [18] REG_STR_C3 \"rest\"",
                        " [19] BINDNAME",
                        " [20] REG_STR_C2 \"$0\"",
                        " [21] NAME",
                        " [22] REG_IND_C1",
                        " [23] OBJECT_REST excluding [static: \"a\"; computed: 0]",
                        " [25] REG_STR_C3 \"rest\"",
                        " [26] SETNAME",
                        " [27] POP",
                        " [28] REG_STR_C2 \"$0\"",
                        " [29] NAME",
                        " [30] LEAVEWITH",
                        " [31] POP",
                        " [32] RETURN_RESULT",
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
