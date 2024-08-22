package org.mozilla.javascript;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ThreadSafeScriptableObjectTest {
    @Test
    public void canSealGlobalObjectWithoutDeadlock() {
        ContextFactory.getGlobalSetter()
                .setContextFactoryGlobal(
                        new ContextFactory() {
                            @Override
                            protected boolean hasFeature(Context cx, int featureIndex) {
                                if (featureIndex == Context.FEATURE_THREAD_SAFE_OBJECTS) {
                                    return true;
                                }
                                return super.hasFeature(cx, featureIndex);
                            }
                        });

        try (Context cx = Context.enter()) {
            ScriptableObject global = cx.initStandardObjects();
            global.sealObject();

            // Registered by NativeJavaTopPackage
            assertNotNull(global.get("Packages", global));
        }
    }
}
