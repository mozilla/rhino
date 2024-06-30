module org.mozilla.rhino.engine {
    exports org.mozilla.javascript.engine;

    requires transitive org.mozilla.rhino;
    requires transitive java.scripting;
}
