package org.mozilla.javascript.tools.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.mozilla.javascript.tools.Console;

/**
 * A simple console that always works because it only relies on standard input and output. Works in
 * the case of an interactive shell and also in a non-interactive context.
 */
public class BasicConsole implements Console {
    private BufferedReader reader;
    private PrintStream out;
    private PrintStream err;

    public BasicConsole() {
        setIn(System.in);
        setOut(System.out);
        setErr(System.err);
    }

    @Override
    public void close() throws IOException {}

    @Override
    public String getImplementation() {
        return "Default console";
    }

    @Override
    public String readLine(String prompt) throws IOException {
        if (prompt != null) {
            out.print(prompt);
            out.flush();
        }
        return reader.readLine();
    }

    @Override
    public String readLine() throws IOException {
        return reader.readLine();
    }

    @Override
    public void print(String msg) {
        out.print(msg);
    }

    @Override
    public void println(String msg) {
        out.println(msg);
    }

    @Override
    public void println() {
        out.println();
    }

    @Override
    public void flush() {
        out.flush();
    }

    public void setIn(InputStream in) {
        reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    @Override
    public PrintStream getOut() {
        return out;
    }

    public void setErr(PrintStream err) {
        this.err = err;
    }

    @Override
    public PrintStream getErr() {
        return err;
    }
}
