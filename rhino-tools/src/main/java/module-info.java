module org.mozilla.rhino.tools {
    requires transitive org.mozilla.rhino;
    requires transitive java.desktop;

    exports org.mozilla.javascript.tools;
    exports org.mozilla.javascript.tools.debugger;
    exports org.mozilla.javascript.tools.jsc;
    exports org.mozilla.javascript.tools.shell;
}
