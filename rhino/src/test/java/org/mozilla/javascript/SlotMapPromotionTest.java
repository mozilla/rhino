package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Ensures that slot map promotion */
class SlotMapPromotionTest {
    @Test
    void nonThreadSafe() {
        assertCanPutAndGetManyPropertiesInAnObject();
    }

    @Test
    void threadSafe() {
        ContextFactory contextFactory =
                Utils.contextFactoryWithFeatures(Context.FEATURE_THREAD_SAFE_OBJECTS);
        try (Context unused = contextFactory.enterContext()) {
            assertCanPutAndGetManyPropertiesInAnObject();
        }
    }

    private void assertCanPutAndGetManyPropertiesInAnObject() {
        NativeObject o = new NativeObject();
        int a_LARGE_SIZE = SlotMapOwner.LARGE_HASH_SIZE * 2;
        for (int i = 0; i < a_LARGE_SIZE; ++i) {
            o.put(String.valueOf(i), o, i);
        }
        for (int i = 0; i < a_LARGE_SIZE; ++i) {
            assertEquals(i, o.get(i, o));
        }
    }
}
