package org.mozilla.javascript.cli;

import java.io.PrintStream;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.Console;

public class JLineConsole implements Console {
    private final Terminal terminal;
    private final LineReader reader;

    JLineConsole(Terminal t, Scriptable scope) {
        this.terminal = t;
        this.reader = LineReaderBuilder.builder().terminal(t).build();
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

    @Override
    public void print(String msg) {
        terminal.writer().print(msg);
    }

    @Override
    public void println(String msg) {
        terminal.writer().println(msg);
    }

    @Override
    public void println() {
        terminal.writer().println();
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
        return new PrintStream(terminal.output());
    }
}
