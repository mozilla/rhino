module org.mozilla.rhino.engine {
    exports org.mozilla.javascript.engine;

    provides javax.script.ScriptEngineFactory with
            org.mozilla.javascript.engine.RhinoScriptEngineFactory;

    requires transitive org.mozilla.rhino;
    requires transitive java.scripting;
}
