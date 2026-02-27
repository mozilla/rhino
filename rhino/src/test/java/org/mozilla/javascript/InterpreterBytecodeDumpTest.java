/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

// Simple smoke tests for dumping of the bytecode generated in interpreter mode
class InterpreterBytecodeDumpTest {
    @Test
    void basicSmokeTest() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 8",
                        "MaxStack = 1",
                        " [0] LINE : 1",
                        " [3] SHORTNUMBER 42",
                        " [6] POP_RESULT",
                        " [7] RETURN_RESULT",
                        ""),
                getByteCodeFrom("42"));
    }

    @Test
    void functionNamesArePrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for f, length = 4",
                        "MaxStack = 0",
                        " [0] LINE : 1",
                        " [3] RETUNDEF",
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
                        "ICode dump, for f, length = 4",
                        "MaxStack = 0",
                        " [0] LINE : 1",
                        " [3] RETUNDEF",
                        "ICode dump, for null, length = 16",
                        "MaxStack = 4",
                        " [0] LINE : 1",
                        " [3] REG_STR_C0 \"o\"",
                        " [4] BINDNAME",
                        " [5] REG_IND_C0",
                        " [6] LITERAL_NEW_OBJECT [f] false",
                        " [8] REG_IND_C0",
                        " [9] METHOD_EXPR #0",
                        " [10] LITERAL_SET",
                        " [11] OBJECTLIT",
                        " [12] REG_STR_C0 \"o\"",
                        " [13] SETNAME",
                        " [14] POP_RESULT",
                        " [15] RETURN_RESULT",
                        ""),
                getByteCodeFrom("o = { f() {} }"));
    }

    @Test
    void bigIntsArePrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 20",
                        "MaxStack = 2",
                        " [0] LINE : 1",
                        " [3] REG_BIGINT_C0 1n",
                        " [4] BIGINT",
                        " [5] REG_BIGINT_C1 2n",
                        " [6] BIGINT",
                        " [7] ADD",
                        " [8] REG_BIGINT_C2 3n",
                        " [9] BIGINT",
                        " [10] ADD",
                        " [11] REG_BIGINT_C3 4n",
                        " [12] BIGINT",
                        " [13] ADD",
                        " [14] LOAD_BIGINT1 5n",
                        " [16] BIGINT",
                        " [17] ADD",
                        " [18] POP_RESULT",
                        " [19] RETURN_RESULT",
                        ""),
                getByteCodeFrom("1n + 2n + 3n + 4n + 5n"));
    }

    @Test
    void branchInstructionsArePrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 19",
                        "MaxStack = 1",
                        " [0] LINE : 1",
                        " [3] REG_STR_C0 \"x\"",
                        " [4] NAME",
                        " [5] IFNE 14",
                        " [8] SHORTNUMBER 42",
                        " [11] GOTO 17",
                        " [14] SHORTNUMBER 43",
                        " [17] POP_RESULT",
                        " [18] RETURN_RESULT",
                        ""),
                getByteCodeFrom("x ? 42 : 43"));
    }

    @Test
    void nullishCoalescingBranchIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 14",
                        "MaxStack = 2",
                        " [0] LINE : 1",
                        " [3] REG_STR_C0 \"x\"",
                        " [4] NAME",
                        " [5] DUP",
                        " [6] IF_NOT_NULL_UNDEF 12",
                        " [9] POP",
                        " [10] REG_STR_C1 \"y\"",
                        " [11] NAME",
                        " [12] POP_RESULT",
                        " [13] RETURN_RESULT",
                        ""),
                getByteCodeFrom("x ?? y"));
    }

    @Test
    void optionalChainingBranchIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 18",
                        "MaxStack = 2",
                        " [0] LINE : 1",
                        " [3] REG_STR_C0 \"x\"",
                        " [4] NAME",
                        " [5] DUP",
                        " [6] IF_NULL_UNDEF 14",
                        " [9] REG_STR_C1 \"y\"",
                        " [10] GETPROP",
                        " [11] GOTO 16",
                        " [14] POP",
                        " [15] UNDEF",
                        " [16] POP_RESULT",
                        " [17] RETURN_RESULT",
                        ""),
                getByteCodeFrom("x?.y"));
    }

    @Test
    void nameIncDecIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 8",
                        "MaxStack = 1",
                        " [0] LINE : 1",
                        " [3] REG_STR_C0 \"x\"",
                        " [4] NAME_INC_DEC 2",
                        " [6] POP_RESULT",
                        " [7] RETURN_RESULT",
                        ""),
                getByteCodeFrom("x++"));
    }

    @Test
    void callSpecialIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 15",
                        "MaxStack = 3",
                        " [0] LINE : 1",
                        " [3] REG_STR_C0 \"eval\"",
                        " [4] NAME_AND_THIS",
                        " [5] REG_STR_C1 \"1\"",
                        " [6] STRING",
                        " [7] REG_IND_C1",
                        " [8] CALLSPECIAL 1 false 1 1",
                        " [13] POP_RESULT",
                        " [14] RETURN_RESULT",
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
                        "ICode dump, for null, length = 13",
                        "MaxStack = 4",
                        " [0] LINE : 1",
                        " [3] REG_STR_C0 \"f\"",
                        " [4] NAME_AND_THIS",
                        " [5] ONE",
                        " [6] SHORTNUMBER 2",
                        " [9] REG_IND_C2",
                        " [10] CALL 2",
                        " [11] POP_RESULT",
                        " [12] RETURN_RESULT",
                        ""),
                getByteCodeFrom("f(1, 2)"));
    }

    @Test
    void newExpressionIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 9",
                        "MaxStack = 1",
                        " [0] LINE : 1",
                        " [3] REG_STR_C0 \"F\"",
                        " [4] NAME",
                        " [5] REG_IND_C0",
                        " [6] NEW 0",
                        " [7] POP_RESULT",
                        " [8] RETURN_RESULT",
                        ""),
                getByteCodeFrom("new F()"));
    }

    @Test
    void throwIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 8",
                        "MaxStack = 1",
                        " [0] LINE : 1",
                        " [3] ONE",
                        " [4] THROW : 1",
                        " [7] RETURN_RESULT",
                        ""),
                getByteCodeFrom("throw 1"));
    }

    @Test
    void generatorICodeIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for g, length = 14",
                        "MaxStack = 1",
                        " [0] GENERATOR : 1",
                        " [3] LINE : 1",
                        " [6] ONE",
                        " [7] YIELD : 1",
                        " [10] POP",
                        " [11] GENERATOR_END : 1",
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
                        "ICode dump, for g, length = 10",
                        "MaxStack = 1",
                        " [0] GENERATOR : 1",
                        " [3] LINE : 1",
                        " [6] ONE",
                        " [7] GENERATOR_RETURN : 1",
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
                        "ICode dump, for null, length = 10",
                        "MaxStack = 1",
                        " [0] LINE : 1",
                        " [3] INTNUMBER 100000",
                        " [8] POP_RESULT",
                        " [9] RETURN_RESULT",
                        ""),
                getByteCodeFrom("100000"));
    }

    @Test
    void doubleNumberIsPrinted() throws IOException {
        assertEquals(
                Utils.portableLines(
                        "ICode dump, for null, length = 7",
                        "MaxStack = 1",
                        " [0] LINE : 1",
                        " [3] REG_IND_C0",
                        " [4] NUMBER 1.5",
                        " [5] POP_RESULT",
                        " [6] RETURN_RESULT",
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
    void objectRestIsPrinted() throws IOException {
        var output = getByteCodeFrom("var {a, ...rest} = x");
        assertTrue(output.contains("OBJECT_REST excluding"));
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
                        "ICode dump, for f, length = 10",
                        "MaxStack = 1",
                        " [0] LINE : 1",
                        " [3] ONE",
                        " [4] SETVAR1 0",
                        " [6] POP",
                        " [7] GETVAR1 0",
                        " [9] RETURN",
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
                        "ICode dump, for f, length = 8",
                        "MaxStack = 1",
                        " [0] LINE : 1",
                        " [3] ONE",
                        " [4] SETCONSTVAR1 0",
                        " [6] POP",
                        " [7] RETUNDEF",
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
                        "ICode dump, for null, length = 19",
                        "MaxStack = 3",
                        " [0] LINE : 1",
                        " [3] REG_STR_C0 \"x\"",
                        " [4] NAME",
                        " [5] DUP",
                        " [6] ONE",
                        " [7] SHEQ",
                        " [8] IFEQ_POP 15",
                        " [11] POP",
                        " [12] GOTO 18",
                        " [15] GOTO 18",
                        " [18] RETURN_RESULT",
                        ""),
                getByteCodeFrom("switch(x) { case 1: break; }"));
    }

    @Test
    void objectLiteralWithSpreadIsPrinted() throws IOException {
        var output = getByteCodeFrom("({...x})");
        assertTrue(output.contains("LITERAL_NEW_OBJECT "));
    }

    private static String getByteCodeFrom(String source) throws IOException {
        var oldBytecodePrintStream = Interpreter.interpreterBytecodePrintStream;
        var oldTokenPrintICode = Token.printICode;
        var oldTokenPrintNames = Token.printNames;
        try (var buffer = new ByteArrayOutputStream()) {
            Interpreter.interpreterBytecodePrintStream = new PrintStream(buffer);
            Token.printICode = true;
            Token.printNames = true;

            try (Context cx = Context.enter()) {
                cx.setInterpretedMode(true);
                cx.compileString(source, "test", 1, null);
            }

            return buffer.toString(UTF_8);
        } finally {
            Token.printNames = oldTokenPrintNames;
            Token.printICode = oldTokenPrintICode;
            Interpreter.interpreterBytecodePrintStream = oldBytecodePrintStream;
        }
    }
}
