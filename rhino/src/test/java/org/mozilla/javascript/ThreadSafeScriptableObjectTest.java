package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

public class ThreadSafeScriptableObjectTest {
    @Test
    public void canSealGlobalObjectWithoutDeadlock() {
        ContextFactory.getGlobalSetter()
                .setContextFactoryGlobal(
                        Utils.contextFactoryWithFeatures(Context.FEATURE_THREAD_SAFE_OBJECTS));

        try (Context cx = Context.enter()) {
            TopLevel global = cx.initStandardObjects();
            global.sealObject();

            // Registered by NativeJavaTopPackage
            assertNotNull(global.get("Packages", global));
        }
    }
}
