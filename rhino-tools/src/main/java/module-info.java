module org.mozilla.rhino.tools {
    requires transitive org.mozilla.rhino;
    requires transitive java.desktop;
    requires static org.jline.terminal;
    requires static org.jline.reader;

    exports org.mozilla.javascript.tools;
    exports org.mozilla.javascript.tools.debugger;
    exports org.mozilla.javascript.tools.jsc;
    exports org.mozilla.javascript.tools.shell;

    uses org.mozilla.javascript.tools.ConsoleProvider;

    provides org.mozilla.javascript.tools.ConsoleProvider with
            org.mozilla.javascript.tools.shell.BasicConsoleProvider,
            org.mozilla.javascript.tools.shell.JLineConsoleProvider;
}
