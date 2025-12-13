package org.mozilla.javascript.cli;

import java.io.IOException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.Console;
import org.mozilla.javascript.tools.ConsoleProvider;

public class JLineConsoleProvider implements ConsoleProvider {
    private final Terminal terminal;

    public JLineConsoleProvider() {
        Terminal t;
        try {
            // Initialize JLine so it will try to use a fancy console, but
            // won't print out a warning if it can't.
            t = TerminalBuilder.builder().system(true).dumb(true).build();
        } catch (IOException ioe) {
            t = null;
        }
        terminal = t;
    }

    @Override
    public Console newConsole(Scriptable scope) {
        assert terminal != null;
        return new JLineConsole(terminal, scope);
    }

    @Override
    public boolean isSupported() {
        // Only allow JLine to be used if we have full functionality.
        return (terminal != null
                && !Terminal.TYPE_DUMB.equals(terminal.getType())
                && !Terminal.TYPE_DUMB_COLOR.equals(terminal.getType()));
    }

    @Override
    public boolean supportsEditing() {
        return true;
    }
}
