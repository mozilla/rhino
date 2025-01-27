package org.mozilla.javascript.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
 * <p>
 *
 * <p>This means, "rhino.use_java_policy_security" is equvalent to "RHINO_USE_JAVA_POLICY_SECURITY"
 *
 * <p>This class contains only the properties. Every plugin etc. can parse its config from there.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class RhinoProperties {

    private static String[] CONFIG_FILES = {"rhino.config", "rhino-test.config"};
    private static final RhinoProperties INSTANCE =
            AccessController.doPrivileged((PrivilegedAction<RhinoProperties>) () -> init());

    static RhinoProperties init() {
        RhinoProperties props = new RhinoProperties();
        Iterator<RhinoPropertiesLoader> loader =
                ServiceLoader.load(RhinoPropertiesLoader.class).iterator();
        if (loader.hasNext()) {
            while (loader.hasNext()) {
                loader.next().load(props);
            }
        } else {
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

        addConfig(System.getenv());
        addConfig(System.getProperties());
    }

    private List<Map<?, ?>> configs = new ArrayList<>();

    /** Loads the configuration from the given file. */
    public void loadFromFile(File config) {
        if (!config.exists()) {
            return;
        }
        try (InputStream in = new FileInputStream(config)) {
            Properties props = new Properties();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            addConfig(props);
        } catch (IOException e) {
            System.err.println(
                    "Error loading configuration from "
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
        try (InputStream in = resource.openStream()) {
            Properties props = new Properties();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            addConfig(props);
        } catch (IOException e) {
            System.err.println(
                    "Error loading configuration from " + resource + ": " + e.getMessage());
        }
    }

    /** Adds a config map */
    public void addConfig(Map<?, ?> config) {
        configs.add(config);
    }

    /**
     * Tries to find the property in the maps. It tries the property first, then it tries the camel
     * upper version.
     */
    public static Object get(String property) {
        for (Map<?, ?> map : INSTANCE.configs) {
            Object ret = map.get(property);
            if (ret != null) {
                return ret;
            }
            ret = map.get(toCamelUpper(property));
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    private static String toCamelUpper(String property) {
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
