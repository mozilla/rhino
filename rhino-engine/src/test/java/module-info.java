module org.mozilla.rhino.engine.test {
    exports org.mozilla.javascript.tests.scriptengine;

    requires org.mozilla.rhino.engine;
    requires org.mozilla.rhino;
    requires org.mozilla.rhino.testutils;
    requires junit;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
}
