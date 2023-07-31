module org.mozilla.rhino.tools {
    requires org.mozilla.rhino.runtime;
    requires java.desktop;

    exports org.mozilla.javascript.tools.debugger;
    exports org.mozilla.javascript.tools.jsc;
    exports org.mozilla.javascript.tools.shell;
}
