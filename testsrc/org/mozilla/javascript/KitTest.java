package org.mozilla.javascript.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Kit;

import static org.mozilla.javascript.Kit.*;
import static junit.framework.TestCase.*;

public class KitTest {

    @BeforeClass
    public static void e

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
        Kit.reinitNonClassCache();
    }
}
