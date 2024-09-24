module org.mozilla.rhino {
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

    requires java.compiler;
    requires jdk.dynalink;
    requires transitive java.desktop;
}
