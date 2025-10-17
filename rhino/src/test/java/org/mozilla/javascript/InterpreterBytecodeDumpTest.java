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

// Simple smoke tests for dumping of the bytecode generated in interpreter mode
class InterpreterBytecodeDumpTest {
    @Test
    void basicSmokeTest() throws IOException {
        assertEquals(
                ""
                        + "ICode dump, for null, length = 8\n"
                        + "MaxStack = 1\n"
                        + " [0] LINE : 1\n"
                        + " [3] SHORTNUMBER 42\n"
                        + " [6] POP_RESULT\n"
                        + " [7] RETURN_RESULT\n",
                getByteCodeFrom("42"));
    }

    @Test
    void functionNamesArePrinted() throws IOException {
        assertEquals(
                ""
                        + "ICode dump, for f, length = 4\n"
                        + "MaxStack = 0\n"
                        + " [0] LINE : 1\n"
                        + " [3] RETUNDEF\n"
                        + "ICode dump, for null, length = 1\n"
                        + "MaxStack = 0\n"
                        + " [0] RETURN_RESULT\n",
                getByteCodeFrom("function f() {}"));
    }

    @Test
    void methodsArePrinted() throws IOException {
        assertEquals(
                ""
                        + "ICode dump, for f, length = 4\n"
                        + "MaxStack = 0\n"
                        + " [0] LINE : 1\n"
                        + " [3] RETUNDEF\n"
                        + "ICode dump, for null, length = 16\n"
                        + "MaxStack = 4\n"
                        + " [0] LINE : 1\n"
                        + " [3] REG_STR_C0 \"o\"\n"
                        + " [4] BINDNAME\n"
                        + " [5] REG_IND_C0\n"
                        + " [6] LITERAL_NEW_OBJECT [f] false\n"
                        + " [8] REG_IND_C0\n"
                        + " [9] METHOD_EXPR #0\n"
                        + " [10] LITERAL_SET\n"
                        + " [11] OBJECTLIT\n"
                        + " [12] REG_STR_C0 \"o\"\n"
                        + " [13] SETNAME\n"
                        + " [14] POP_RESULT\n"
                        + " [15] RETURN_RESULT\n",
                getByteCodeFrom("o = { f() {} }"));
    }

    @Test
    void bigIntsArePrinted() throws IOException {
        assertEquals(
                ""
                        + "ICode dump, for null, length = 20\n"
                        + "MaxStack = 2\n"
                        + " [0] LINE : 1\n"
                        + " [3] REG_BIGINT_C0 1n\n"
                        + " [4] BIGINT\n"
                        + " [5] REG_BIGINT_C1 2n\n"
                        + " [6] BIGINT\n"
                        + " [7] ADD\n"
                        + " [8] REG_BIGINT_C2 3n\n"
                        + " [9] BIGINT\n"
                        + " [10] ADD\n"
                        + " [11] REG_BIGINT_C3 4n\n"
                        + " [12] BIGINT\n"
                        + " [13] ADD\n"
                        + " [14] LOAD_BIGINT1 5n\n"
                        + " [16] BIGINT\n"
                        + " [17] ADD\n"
                        + " [18] POP_RESULT\n"
                        + " [19] RETURN_RESULT\n",
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
