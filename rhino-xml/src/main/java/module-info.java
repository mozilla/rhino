module org.mozilla.javascript.xml {
    exports org.mozilla.javascript.xmlimpl;

    requires transitive org.mozilla.rhino;
    requires transitive java.xml;

    provides org.mozilla.javascript.Plugin with
            org.mozilla.javascript.xmlimpl.XmlPlugin;
}
