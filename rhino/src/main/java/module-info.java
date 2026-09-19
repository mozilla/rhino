module org.mozilla.rhino {
    exports org.mozilla.classfile;
    exports org.mozilla.javascript;
    exports org.mozilla.javascript.annotations;
    exports org.mozilla.javascript.ast;
    exports org.mozilla.javascript.commonjs.module;
    exports org.mozilla.javascript.commonjs.module.provider;
    exports org.mozilla.javascript.config;
    exports org.mozilla.javascript.debug;
    exports org.mozilla.javascript.interpreterv2;
    exports org.mozilla.javascript.interpreterv2.instruction;
    exports org.mozilla.javascript.interpreterv2.operand;
    exports org.mozilla.javascript.lc;
    exports org.mozilla.javascript.lc.type;
    exports org.mozilla.javascript.optimizer;
    exports org.mozilla.javascript.serialize;
    exports org.mozilla.javascript.sourcemap;
    exports org.mozilla.javascript.typedarrays;
    exports org.mozilla.javascript.xml;

    uses org.mozilla.javascript.BuiltInLoader;
    uses org.mozilla.javascript.LiveConnect;
    uses org.mozilla.javascript.NullabilityDetector;
    uses org.mozilla.javascript.RegExpLoader;
    uses org.mozilla.javascript.xml.XMLLoader;
    uses org.mozilla.javascript.config.RhinoPropertiesLoader;

    provides org.mozilla.javascript.RegExpLoader with
            org.mozilla.javascript.regexp.RegExpLoaderImpl;

    requires java.compiler;
    requires jdk.dynalink;
    requires jdk.jfr;
    requires transitive java.desktop;
    requires java.logging;
}
