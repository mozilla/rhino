package org.mozilla.javascript.tools.shell;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
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
        } catch (Throwable e) {
            // Most likely reason for this is that JLine isn't in the path
            t = null;
        }
        terminal = t;
    }

    @Override
    public Console newConsole() {
        assert terminal != null;
        return new JLineConsole(terminal);
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
