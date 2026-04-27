package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.config.RhinoConfig;
import org.mozilla.javascript.config.RhinoProperties;
import org.mozilla.javascript.testutils.TestSource;

/**
 * Testcase for various property loading mechanism. <br>
 * Note: Testing is a bit difficult, so to test the serviceLoader mechanism, we have to register a
 * RhinoPropertiesLoader in this maven module. Unfortunately, this means, that all other tests in
 * this module will also use this loader. <br>
 * The loader does NOT load the properties from the default locations as mentioned in the
 * documentation. So, do not wonder, if setting config values in 'rhino.config' or
 * 'rhino-test.config' do not take effect. Instead use `rhino-config-for-this-module.config`.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class RhinoPropertiesTest {

    public enum TestEnum {
        valueA,
        VALUEB,
        valuec
    }

    /** Tests the loader initialization and all methods in RhinoConfig. */
    @Test
    void testLoaderInit() {
        // test if correct files are loaded:
        assertFalse(RhinoConfig.get("test.rhino-explicit.loaded", false));
        assertFalse(RhinoConfig.get("test.file.rhino-config.loaded", false));

        assertTrue(RhinoConfig.get("test.file.rhino-test-config.loaded", false));
        assertTrue(RhinoConfig.get("test.cp.rhino-config.loaded", false));
        assertTrue(RhinoConfig.get("test.cp.rhino-config.loaded", false));

        // override test
        assertEquals("baz2", RhinoConfig.get("test.foo.bar"));

        // normal getters (with correct type)
        assertEquals(true, RhinoConfig.get("test.foo.someBoolean1", false));
        assertEquals(true, RhinoConfig.get("test.foo.someBoolean2", false));
        assertEquals(false, RhinoConfig.get("test.foo.someBoolean3", false));
        assertEquals(false, RhinoConfig.get("test.foo.someOtherBoolean", false));

        // integer test
        assertEquals(42, RhinoConfig.get("test.foo.someInteger1", 21));
        assertEquals(42, RhinoConfig.get("test.foo.someInteger2", 21));
        assertEquals(21, RhinoConfig.get("test.foo.someOtherInteger", 21));

        // camelCase-Tests
        assertEquals(true, RhinoConfig.get("test.foo.someAValue", false));
        assertEquals(42, RhinoConfig.get("test.foo.someBValue", 21));

        // enums
        assertEquals(TestEnum.valueA, RhinoConfig.get("test.foo.enum1", TestEnum.VALUEB));
        assertEquals(TestEnum.valueA, RhinoConfig.get("test.foo.enum2", TestEnum.VALUEB));
        assertEquals(TestEnum.VALUEB, RhinoConfig.get("test.foo.enum3", TestEnum.valueA));
        assertEquals(TestEnum.valuec, RhinoConfig.get("test.foo.enum4", TestEnum.valueA));
        assertEquals(TestEnum.valueA, RhinoConfig.get("test.foo.enum5", TestEnum.valueA));
        assertEquals(TestEnum.valueA, RhinoConfig.get("test.foo.enum6", TestEnum.valueA));
    }

    /** Tests explicit loading a file from classpath. */
    @Test
    void testClasspathLoad() {
        RhinoProperties properties = new RhinoProperties();
        properties.loadFromClasspath(getClass().getClassLoader(), "rhino-explicit.config");
        assertEquals("true", properties.get("test.rhino-explicit.loaded"));
        assertEquals("value1", properties.get("test.config.foo"));
        assertEquals("value2", properties.get("test.config.bar"));
    }

    /** Tests explicit loading a file. */
    @Test
    void testFileLoad() {
        RhinoProperties properties = new RhinoProperties();
        properties.loadFromFile(new File(TestSource.resolve("testsrc/rhino-explicit.config")));
        assertEquals("value1", properties.get("test.config.foo"));
        assertEquals("value2", properties.get("test.config.bar"));
    }

    @Test
    void testDefaultLoad() {
        System.setProperty("some.system.value", "value6");
        try {
            RhinoProperties properties = new RhinoProperties();
            properties.loadDefaults();
            assertEquals("value3", properties.get("test.config.foo"));
            assertEquals("value4-mod", properties.get("test.config.bar"));
            assertEquals("value5", properties.get("test.config.baz"));
            assertEquals("value6", properties.get("some.system.value"));
        } finally {
            System.clearProperty("some.system.value");
        }
    }

    /** System properties */
    @Test
    void testSystemOverride() {
        System.setProperty("testconfig.foo", "system-wins");
        try {
            RhinoProperties properties = new RhinoProperties();
            properties.loadDefaults();
            assertEquals("system-wins", properties.get("testconfig.foo"));
        } finally {
            System.clearProperty("testconfig.foo");
        }
    }

    @Test
    void testEnv() {
        RhinoProperties properties = new RhinoProperties();
        properties.loadDefaults();
        // TODO: can/shoud we set an environment value. so check, if we can read PATH
        // for now, we check for Path (and PATH)
        assertNotNull(properties.get("Path"));
    }
}
