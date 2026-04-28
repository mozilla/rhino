/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;

/**
 * Test-only helper that captures the interpreter icode dump produced while {@code action} runs.
 * Lives in {@link org.mozilla.javascript} so it can flip the package-private switches on {@link
 * Interpreter} and {@link Token}.
 */
final class InterpreterIcodeCapture {

    private InterpreterIcodeCapture() {}

    static String capture(Runnable action) {
        PrintStream oldStream = Interpreter.interpreterBytecodePrintStream;
        boolean oldPrintICode = Token.printICode;
        boolean oldPrintNames = Token.printNames;
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            Interpreter.interpreterBytecodePrintStream = new PrintStream(buffer);
            Token.printICode = true;
            Token.printNames = true;
            action.run();
            return buffer.toString(UTF_8);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Token.printNames = oldPrintNames;
            Token.printICode = oldPrintICode;
            Interpreter.interpreterBytecodePrintStream = oldStream;
        }
    }
}
