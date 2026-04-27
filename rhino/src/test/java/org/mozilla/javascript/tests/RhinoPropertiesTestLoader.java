package org.mozilla.javascript.tests;

import java.util.Map;
import org.mozilla.javascript.config.RhinoProperties;
import org.mozilla.javascript.config.RhinoPropertiesLoader;

/** This loader is used for all tests in this maven module! */
public class RhinoPropertiesTestLoader implements RhinoPropertiesLoader {

    /**
     * this loader will add some explicitly typed values to the configuration. After that, it will
     * also load the defaults.
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
