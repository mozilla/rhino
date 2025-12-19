package org.mozilla.javascript.tools.shell;

import org.mozilla.javascript.tools.Console;
import org.mozilla.javascript.tools.ConsoleProvider;

public class BasicConsoleProvider implements ConsoleProvider {
    @Override
    public Console newConsole() {
        return new BasicConsole();
    }

    /** Return whether the console is possible to use at all */
    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public boolean supportsEditing() {
        return false;
    }
}
