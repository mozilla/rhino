module org.mozilla.rhino.reflect {
    requires transitive org.mozilla.rhino;

    exports org.mozilla.javascript.reflect;

    provides org.mozilla.javascript.BuiltInLoader with
            org.mozilla.javascript.reflect.ReflectLoaderImpl;
    provides org.mozilla.javascript.LiveConnect with
            org.mozilla.javascript.reflect.LiveConnectImpl;
}
