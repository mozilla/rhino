module org.mozilla.rhino {
    uses org.mozilla.javascript.NullabilityDetector;

    exports org.mozilla.classfile;
    exports org.mozilla.javascript;
    exports org.mozilla.javascript.annotations;
    exports org.mozilla.javascript.ast;
    exports org.mozilla.javascript.commonjs.module;
    exports org.mozilla.javascript.commonjs.module.provider;
    exports org.mozilla.javascript.debug;
    exports org.mozilla.javascript.optimizer;
    exports org.mozilla.javascript.serialize;
    exports org.mozilla.javascript.typedarrays;
    exports org.mozilla.javascript.xml;

    uses org.mozilla.javascript.RegExpProxy;
    uses org.mozilla.javascript.xml.XMLLoader;

    provides org.mozilla.javascript.RegExpProxy with
            org.mozilla.javascript.regexp.RegExpImpl;

    requires java.compiler;
    requires jdk.dynalink;
    requires transitive java.desktop;

    uses org.mozilla.javascript.Plugin;
}
