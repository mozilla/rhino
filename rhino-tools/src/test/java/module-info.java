module org.mozilla.rhino.tools.test {
    requires org.mozilla.rhino;
    requires org.mozilla.rhino.testutils;
    requires org.mozilla.rhino.tools;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
    requires org.jline.terminal;

    exports org.mozilla.javascript.tools.tests;
}
