module org.mozilla.javascript.xml {
    exports org.mozilla.javascript.xmlimpl;

    provides org.mozilla.javascript.xml.XMLLoader with
            org.mozilla.javascript.xmlimpl.XMLLoaderImpl;

    requires transitive org.mozilla.rhino;
    requires transitive java.xml;
}
