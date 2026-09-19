package org.mozilla.javascript.reflect;

import org.mozilla.javascript.BuiltInLoader;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LazilyLoadedCtor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.TopLevel;

public class ReflectLoaderImpl implements BuiltInLoader {
    private static final String[] TOP_LEVEL_PACKAGES = {
        "java", "javax", "org", "com", "edu", "net"
    };
    private static final String[] TOP_LEVEL_PACKAGES_ANDROID = {
        "java", "javax", "org", "com", "edu", "net", "android"
    };

    @Override
    public void initSafeStandardObjects(Context cx, TopLevel scope, boolean sealed) {
        NativeJavaObject.init(cx, scope, sealed);
        NativeJavaMap.init(cx, scope, sealed);
    }

    @Override
    public void initStandardObjects(Context cx, TopLevel s, boolean sealed) {
        new ClassCache().associate(s);

        // These depend on the legacy initialization behavior of the lazy loading mechanism
        new LazilyLoadedCtor<>(
                s, "Packages", "org.mozilla.javascript.reflect.NativeJavaTopPackage", sealed, true);
        new LazilyLoadedCtor<>(
                s, "getClass", "org.mozilla.javascript.reflect.NativeJavaTopPackage", sealed, true);
        new LazilyLoadedCtor<>(
                s, "JavaAdapter", "org.mozilla.javascript.reflect.JavaAdapter", sealed, true);
        new LazilyLoadedCtor<>(
                s, "JavaImporter", "org.mozilla.javascript.reflect.ImporterTopLevel", sealed, true);

        for (String packageName : getTopPackageNames()) {
            new LazilyLoadedCtor<>(
                    s,
                    packageName,
                    "org.mozilla.javascript.reflect.NativeJavaTopPackage",
                    sealed,
                    true);
        }
    }

    private static String[] getTopPackageNames() {
        // Include "android" top package if running on Android
        return ScriptRuntime.getAndroidApiVersion() > 0
                ? TOP_LEVEL_PACKAGES_ANDROID
                : TOP_LEVEL_PACKAGES;
    }
}
