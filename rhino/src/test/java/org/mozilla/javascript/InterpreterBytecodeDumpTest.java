/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
