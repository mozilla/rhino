package org.mozilla.javascript.tools.shell;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.mozilla.javascript.tools.Console;

public class JLineConsole implements Console {
    private final Terminal terminal;
    private final LineReader reader;

    public JLineConsole(Terminal t) {
        this.terminal = t;
        this.reader = LineReaderBuilder.builder().terminal(t).build();
    }

    @Override
    public void close() throws IOException {
        terminal.close();
    }

    @Override
    public String getImplementation() {
        return "JLine " + terminal.getType();
    }

    @Override
    public String readLine(String prompt) {
        try {
            return reader.readLine(prompt);
        } catch (UserInterruptException | EndOfFileException e) {
            // This will cause the "main" to cleanly exit
            return null;
        }
    }

    @Override
    public String readLine() {
        try {
            return reader.readLine();
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        }
    }

    // JLine only flushes the terminal writer when the terminal is active (during an
    // interactive readLine), so flush explicitly or output is lost in script mode.

    @Override
    public void print(String msg) {
        PrintWriter w = terminal.writer();
        w.print(msg);
        w.flush();
    }

    @Override
    public void println(String msg) {
        PrintWriter w = terminal.writer();
        w.println(msg);
        w.flush();
    }

    @Override
    public void println() {
        PrintWriter w = terminal.writer();
        w.println();
        w.flush();
    }

    @Override
    public void flush() {
        terminal.writer().flush();
    }

    @Override
    public PrintStream getOut() {
        return new PrintStream(terminal.output());
    }

    @Override
    public PrintStream getErr() {
        // Errors (e.g. from the error reporter) must go to stderr, not the terminal.
        return System.err;
    }
}
