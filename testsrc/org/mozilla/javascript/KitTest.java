package org.mozilla.javascript;

import org.junit.*;

import java.lang.reflect.Field;
import java.net.URLClassLoader;

import static org.mozilla.javascript.Kit.*;
import static junit.framework.TestCase.*;

public class KitTest {
    private static boolean enabledNonClassCaching;
    private static Field enabledFld;

    @BeforeClass
    public static void enableCaching() throws Exception {
        enabledFld = Kit.class.getDeclaredField("enabledNonClassCaching");
        enabledFld.setAccessible(true);
        enabledNonClassCaching = (Boolean) enabledFld.get(null);
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

    @Test
    public void testWeakReferences() {
        System.setProperty(CACHE_SIZE_PROPERTY_NAME, "2");

        ClassLoader classLoader = new TestClassLoader();
        classOrNull(classLoader, "java");
        assertEquals(1, getNonClassCacheSize());
        classOrNull(classLoader, "java.lang");
        assertEquals(2, getNonClassCacheSize());
        classOrNull(classLoader,"java.lang.String");
        assertEquals(2, getNonClassCacheSize());

        //to activate cleaning of related cache during next GC
        classLoader = null;

        System.gc();

        assertEquals(0, getNonClassCacheSize());
    }

    @After
    public void after() {
        reinitNonClassCache();
    }

    @AfterClass
    public static void disableCaching() throws Exception {
        enabledFld.set(null, enabledNonClassCaching);
    }

    private static class TestClassLoader extends URLClassLoader {
        public TestClassLoader() {
            super(((URLClassLoader)getSystemClassLoader()).getURLs());
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return super.loadClass(name);
        }
    }
}
