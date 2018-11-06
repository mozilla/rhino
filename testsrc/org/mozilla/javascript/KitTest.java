package org.mozilla.javascript;

import org.junit.*;

import java.lang.reflect.Field;

import static org.mozilla.javascript.Kit.*;
import static junit.framework.TestCase.*;

public class KitTest {
    static Field enabledFld;

    @BeforeClass
    public static void enableCaching() throws Exception {
        enabledFld = Kit.class.getDeclaredField("enabledNonClassCaching");
        enabledFld.set(null, true);
    }

    @Test
    public void testNonClassUsages() {
        System.setProperty(CACHE_SIZE_PROPERTY_NAME, "2");

        classOrNull("java");
        assertEquals(1, getNonClassCacheSize());
        classOrNull("java.lang");
        assertEquals(2, getNonClassCacheSize());
        classOrNull("java.lang.String");
        assertEquals(2, getNonClassCacheSize());

    }

    @After
    public void after() {
        reinitNonClassCache();
    }

    @AfterClass
    public static void disableCaching() throws Exception {
        enabledFld.set(null, false);
    }
}
