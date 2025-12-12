package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.testutils.Utils;

public class DeadlockReproTest {
    @Test
    public void redefinePropertyWithThreadSafeSlotMap() {
        final ContextFactory factory =
                Utils.contextFactoryWithFeatures(Context.FEATURE_THREAD_SAFE_OBJECTS);

        try (Context cx = factory.enterContext()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            ScriptableObject scope = cx.initStandardObjects();

            scope.put("o", scope, new NativeObject());
            final String script =
                    "Object.defineProperty(o, 'test', {value: '1', configurable: !0});"
                            + "Object.defineProperty(o, 'test', {value: 2});"
                            + "o.test";

            var result = cx.evaluateString(scope, script, "myScript", 1, null);

            assertEquals(2, result);
        }
    }
}
