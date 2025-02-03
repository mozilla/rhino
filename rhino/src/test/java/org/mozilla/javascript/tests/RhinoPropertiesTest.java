package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

    /** This loader is used for all tests in this maven module! */
    public static class Loader implements RhinoPropertiesLoader {

        public static enum TestEnum {
            valueA,
            VALUEB,
            valuec
        }

        @Override
        public void load(RhinoProperties properties) {

            properties.addConfig(Map.of("foo.bar", "baz1"));
            properties.addConfig(
                    Map.of(
                            "foo.bar", "baz2",
                            "foo.someBoolean1", "true",
                            "foo.someBoolean2", "TRUE",
                            "foo.someBoolean3", "1",
                            "foo.someInteger1", "42",
                            "foo.someInteger2", 42,
                            "foo.someAValue", true,
                            "FOO_SOME_BVALUE", 42,
                            "FOO_ENUM1", "valuea"));
            properties.addConfig(
                    Map.of(
                            "FOO_ENUM1", "valuea",
                            "FOO_ENUM2", "VALUEA",
                            "FOO_ENUM3", "VALUEB",
                            "FOO_ENUM4", "VALUEC",
                            "FOO_ENUM5", "VALUED"));
            // load module independent file.
            properties.loadFromFile(new File("rhino-config-for-this-module.config"));
        }
    }

    /** Tests the loader initialization and all methods in RhinoConfig. */
    @Test
    void testLoaderInit() {
        // override test
        assertEquals("baz2", RhinoConfig.get("foo.bar"));

        // normal getters (with correct type)
        assertEquals(true, RhinoConfig.get("foo.someBoolean1", false));
        assertEquals(true, RhinoConfig.get("foo.someBoolean2", false));
        assertEquals(false, RhinoConfig.get("foo.someBoolean3", false));
        assertEquals(false, RhinoConfig.get("foo.someOtherBoolean", false));

        // integer test
        assertEquals(42, RhinoConfig.get("foo.someInteger1", 21));
        assertEquals(42, RhinoConfig.get("foo.someInteger2", 21));
        assertEquals(21, RhinoConfig.get("foo.someOtherInteger", 21));

        // camelCase-Tests
        assertEquals(true, RhinoConfig.get("foo.someAValue", false));
        assertEquals(42, RhinoConfig.get("foo.someBValue", 21));

        // enums
        assertEquals(Loader.TestEnum.valueA, RhinoConfig.get("foo.enum1", Loader.TestEnum.VALUEB));
        assertEquals(Loader.TestEnum.valueA, RhinoConfig.get("foo.enum2", Loader.TestEnum.VALUEB));
        assertEquals(Loader.TestEnum.VALUEB, RhinoConfig.get("foo.enum3", Loader.TestEnum.valueA));
        assertEquals(Loader.TestEnum.valuec, RhinoConfig.get("foo.enum4", Loader.TestEnum.valueA));
        assertEquals(Loader.TestEnum.valueA, RhinoConfig.get("foo.enum5", Loader.TestEnum.valueA));
        assertEquals(Loader.TestEnum.valueA, RhinoConfig.get("foo.enum6", Loader.TestEnum.valueA));
    }

    /** Tests explicit loading a file from classpath. */
    @Test
    void testClasspathLoad() {
        RhinoProperties properties = new RhinoProperties();
        properties.loadFromClasspath(getClass().getClassLoader(), "rhino-testcase.config");
        assertEquals("value1", properties.get("testconfig.foo"));
        assertEquals("value2", properties.get("testconfig.bar"));
    }

    /** Tests explicit loading a file. */
    @Test
    void testFileLoad() {
        RhinoProperties properties = new RhinoProperties();
        properties.loadFromFile(new File("src/test/resources/rhino-testcase.config"));
        assertEquals("value1", properties.get("testconfig.foo"));
        assertEquals("value2", properties.get("testconfig.bar"));
    }

    @Test
    void testDefaultLoad() {
        System.setProperty("some.system.value", "value6");
        try {
            RhinoProperties properties = new RhinoProperties();
            properties.loadDefaults();
            assertEquals("value3", properties.get("testconfig.foo"));
            assertEquals("value4-mod", properties.get("testconfig.bar"));
            assertEquals("value5", properties.get("testconfig.baz"));
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
