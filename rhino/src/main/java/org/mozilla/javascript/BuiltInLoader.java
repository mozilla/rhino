package org.mozilla.javascript;

public interface BuiltInLoader {
    void initSafeStandardObjects(Context cx, TopLevel scope, boolean sealed);

    void initStandardObjects(Context cx, TopLevel scope, boolean sealed);
}
