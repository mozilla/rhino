package org.mozilla.javascript.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * Utility class to read current rhino configuration from various properties.
 *
 * <p>Rhino properties typically begins with "rhino." (properties) or "RHINO_" (env)
 *
 * <p>You can override this behaviour and implement a {@link RhinoPropertiesLoader} and register it
 * as service. If no loader was found, the configuration is read from these locations by default:
 *
 * <ol>
 *   <li>rhino.config file from current class' classpath
 *   <li>rhino.config file from current threas's classpath
 *   <li>rhino.config file from current directory
 *   <li>rhino-test.config file from current class' classpath
 *   <li>rhino-test.config file from current threas's classpath
 *   <li>rhino-test.config file from current directory
 *   <li>env variables starting with "RHINO_" (underscores are replaced by '.' and string is
 *   <li>System-properties starting with "rhino."
 * </ol>
 *
 * <p>(the later config can override previous ones)
 *
 * <p>The config files are in UTF-8 format and all keys in this configuration are case-insensitive
 * and dot/underscore-insensitive.
 *
 * <p>This means, "rhino.use_java_policy_security=true" is equvalent to
 * "RHINO_USE_JAVA_POLICY_SECURITY=TRUE"
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class RhinoProperties {

    private static String[] CONFIG_FILES = {"rhino.config", "rhino-test.config"};
    private List<Map<?, ?>> configs = new ArrayList<>();
    // this allows debugging via system property "rhino.debugConfig"
    private final boolean debug = Boolean.getBoolean("rhino.debugConfig");

    /**
     * Initializes the default, used in RhinoConfig. If there is RhinoPropertiesLoader present, then
     * this loader has full control. Ohterwise, properties are loaded from default locations.
     */
    static RhinoProperties init() {
        RhinoProperties props = new RhinoProperties();
        Iterator<RhinoPropertiesLoader> loader =
                ServiceLoader.load(RhinoPropertiesLoader.class).iterator();
        if (loader.hasNext()) {
            while (loader.hasNext()) {
                RhinoPropertiesLoader next = loader.next();
                if (props.debug) {
                    System.out.println("Rhino: using loader " + next.getClass().getName());
                }
                next.load(props);
            }
        } else {
            if (props.debug) {
                System.out.println("Rhino: no loader found. Loading defaults");
            }
            props.loadDefaults();
        }
        return props;
    }

    /** Load properties from the default locations. */
    public void loadDefaults() {
        ClassLoader classLoader = RhinoProperties.class.getClassLoader();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        for (String configFile : CONFIG_FILES) {
            loadFromClasspath(classLoader, configFile);
            loadFromClasspath(contextClassLoader, configFile);
            loadFromFile(new File(configFile));
        }
        if (debug) {
            System.out.println("Rhino: loading configuration from System.getEnv()");
        }
        addConfig(System.getenv());
        if (debug) {
            System.out.println("Rhino: loading configuration from System.getProperties()");
        }
        addConfig(System.getProperties());
    }

    /** Loads the configuration from the given file. */
    public void loadFromFile(File config) {
        if (!config.exists()) {
            return;
        }
        if (debug) {
            System.out.println("Rhino: loading configuration from " + config.getAbsolutePath());
        }
        try (InputStream in = new FileInputStream(config)) {
            Properties props = new Properties();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            addConfig(props);
        } catch (IOException e) {
            System.err.println(
                    "Rhino: Error loading configuration from "
                            + config.getAbsolutePath()
                            + ": "
                            + e.getMessage());
        }
    }

    /** Loads the configuration from classpath. */
    public void loadFromClasspath(ClassLoader cl, String location) {
        if (cl != null) {
            loadFromResource(cl.getResource(location));
        }
    }

    /** Loads the configuration from the given resource. */
    public void loadFromResource(URL resource) {
        if (resource == null) {
            return;
        }
        if (debug) {
            System.out.println("Rhino: loading configuration from " + resource);
        }
        try (InputStream in = resource.openStream()) {
            Properties props = new Properties();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            addConfig(props);
        } catch (IOException e) {
            System.err.println(
                    "Rhino: Error loading configuration from " + resource + ": " + e.getMessage());
        }
    }

    /** Adds a config map. Later added configs are overriding previous ones */
    public void addConfig(Map<?, ?> config) {
        if (debug) {
            System.out.println("Rhino: added " + config.size() + " values");
        }
        configs.add(0, config);
    }

    /**
     * Tries to find the property in the maps. It tries the property first, then it tries the camel
     * upper version.
     */
    public Object get(String property) {
        Objects.requireNonNull(property, "property must not be null");
        for (Map<?, ?> map : configs) {
            String key = property;
            for (int i = 0; i < 2; i++) {
                Object ret = map.get(key);
                if (ret != null) {
                    if (debug) {
                        System.out.println("Rhino: get(" + key + ")=" + ret);
                    }
                    return ret;
                }
                key = toCamelUpper(property);
            }
        }
        return null;
    }

    /** converts camelCaseStrings like "rhino.printICode" to "RHINO_PRINT_ICODE". */
    private String toCamelUpper(String property) {
        String s = property.replace('.', '_');
        StringBuilder sb = new StringBuilder(s.length() + 5);
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(s.charAt(i - 1))) {
                sb.append('_');
            }
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }
}
