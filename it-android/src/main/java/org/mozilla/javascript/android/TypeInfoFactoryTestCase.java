package org.mozilla.javascript.android;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.impl.factory.ClassValueCacheFactory;

/**
 * @author ZZZank
 */
public class TypeInfoFactoryTestCase extends TestCase {
    public TypeInfoFactoryTestCase(String name, TopLevel global) {
        super(name, global);
    }

    @Override
    protected Object runTest(Context cx, Scriptable scope) {
        TypeInfoFactory typeInfoFactory = TypeInfoFactory.get(scope);
        int apiLevel = android.os.Build.VERSION.SDK_INT;
        if (apiLevel >= 34 != typeInfoFactory instanceof ClassValueCacheFactory) {
            throw new IllegalStateException(
                    String.format(
                            "ClassValueCacheFactory should be used for modern Android, but got %s on API level %s",
                            typeInfoFactory, apiLevel));
        }
        return "success";
    }
}
