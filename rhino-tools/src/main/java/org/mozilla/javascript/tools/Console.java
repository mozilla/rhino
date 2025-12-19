package org.mozilla.javascript.tools;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;

/**
 * A generic interface for a console that supports input from the user and displays input on the
 * screen.
 */
public interface Console extends Closeable {
    String getImplementation();

    String readLine(String prompt) throws IOException;

    String readLine() throws IOException;

    void print(String msg);

    void println(String msg);

    void println();

    void flush();

    PrintStream getOut();

    PrintStream getErr();
}
