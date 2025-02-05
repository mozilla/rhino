package org.mozilla.javascript.tests;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.config.RhinoConfig;
import org.mozilla.javascript.config.RhinoProperties;
import org.mozilla.javascript.config.RhinoPropertiesLoader;

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

    /** This loader is used for all tests in this maven module! */
    public static class Loader implements RhinoPropertiesLoader {

        /**
         * this loader will add some explicitly typed values to the configuration. After that, it
         * will also load the defaults.
         */
        @Override
        public void load(RhinoProperties properties) {

            properties.addConfig(Map.of("test.foo.bar", "baz1"));
            properties.addConfig(
                    Map.of(
                            "test.foo.bar", "baz2",
                            "test.foo.someBoolean1", "true",
                            "test.foo.someBoolean2", "TRUE",
                            "test.foo.someBoolean3", "1",
                            "test.foo.someInteger1", "42",
                            "test.foo.someInteger2", 42,
                            "test.foo.someAValue", true,
                            "TEST_FOO_SOME_BVALUE", 42,
                            "TEST_FOO_ENUM1", "valuea"));
            properties.addConfig(
                    Map.of(
                            "TEST_FOO_ENUM1", "valuea",
                            "TEST_FOO_ENUM2", "VALUEA",
                            "TEST_FOO_ENUM3", "VALUEB",
                            "TEST_FOO_ENUM4", "VALUEC",
                            "TEST_FOO_ENUM5", "VALUED"));
            // also load defaults, so that users can use rhino-test.config here!
            properties.loadDefaults();
        }
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
        properties.loadFromFile(new File("src/test/resources/rhino-explicit.config"));
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
        assertNotNull(properties.get("path"));
    }
}
