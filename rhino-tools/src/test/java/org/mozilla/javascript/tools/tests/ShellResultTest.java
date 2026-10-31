package org.mozilla.javascript.tools.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tools.ToolErrorReporter;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.Main;

@ResourceLock("shell-main-global")
public class ShellResultTest {
    public static Stream<Arguments> results() {
        return Stream.of(
                Arguments.of("Symbol('test')", "Symbol(test)"),
                Arguments.of("Symbol()", "Symbol()"),
                Arguments.of("Symbol.iterator", "Symbol(Symbol.iterator)"),
                Arguments.of("Symbol.for('shared')", "Symbol(shared)"),
                Arguments.of("42", "42"),
                Arguments.of("'plain'", "plain"),
                Arguments.of("({ toString() { return 'object'; } })", "object"),
                Arguments.of("undefined", ""));
    }

    @ParameterizedTest
    @MethodSource("results")
    public void displaysResult(String source, String expected) throws Exception {
        Global previousGlobal = Main.global;
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setTrackUnhandledPromiseRejections(true);
            Global global = new Global(cx);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            PrintStream errorStream = new PrintStream(errors, true, StandardCharsets.UTF_8);
            global.setIn(
                    new ByteArrayInputStream((source + "\n").getBytes(StandardCharsets.UTF_8)));
            global.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            global.setErr(errorStream);
            cx.setErrorReporter(new ToolErrorReporter(false, errorStream));
            Main.global = global;
            try {
                Main.processSource(cx, "-");
                assertEquals("", errors.toString(StandardCharsets.UTF_8));
                assertEquals(expected, output.toString(StandardCharsets.UTF_8).trim());
            } finally {
                Main.global = previousGlobal;
                global.cleanup();
            }
        }
    }
}
