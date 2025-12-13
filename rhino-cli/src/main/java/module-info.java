module org.mozilla.rhino.cli {
    requires org.mozilla.rhino;
    requires org.mozilla.rhino.tools;
    requires org.jline.terminal;
    requires org.jline.reader;

    exports org.mozilla.javascript.cli;

    provides org.mozilla.javascript.tools.ConsoleProvider with
            org.mozilla.javascript.cli.JLineConsoleProvider;
}
