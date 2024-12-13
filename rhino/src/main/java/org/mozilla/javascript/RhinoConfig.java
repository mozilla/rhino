package org.mozilla.javascript;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class to read current rhino configuration.
 *
 * <p>Rhino properties typically begins with "rhino." (properties) or "RHINO_" (env)
 *
 * <p>The configuration is read from these locations:
 *
 * <ol>
 *   <li>rhino.config file from current class' classpath
 *   <li>rhino.config file from current threas's classpath
 *   <li>rhino.config file from current directory
 *   <li>System-properties starting with "rhino."
 *   <li>env variables starting with "RHINO_" (underscores are replaced by '.' and string is
 * </ol>
 *
 * <p>The config files are in UTF-8 format and all keys in this configuration are case-insensitive
 * and dot/underscore-insensitive.
 *
 * <p>This means, "rhino.use_java_policy_security" is equvalent to "RHINO_USE_JAVA_POLICY_SECURITY"
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class RhinoConfig {

    public static final RhinoConfig DEFAULT =
            AccessController.doPrivileged((PrivilegedAction<RhinoConfig>) () -> init());

    private static RhinoConfig init() {
        RhinoConfig config = new RhinoConfig();
        // we parse the following locations
        config.loadFromClasspath(RhinoConfig.class.getClassLoader());
        config.loadFromClasspath(Thread.currentThread().getContextClassLoader());
        config.loadFromFile(new File("rhino.config"));
        config.load(System.getProperties(), "System.properties");
        config.load(System.getenv(), "System.env");
        return config;
    }

    private void loadFromFile(File config) {
        if (!config.exists()) {
            return;
        }
        try (InputStream in = new FileInputStream(config)) {
            Properties props = new Properties();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            load(props, config.getAbsolutePath());
        } catch (IOException e) {
            System.err.println(
                    "Error loading rhino.config from "
                            + config.getAbsolutePath()
                            + ": "
                            + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFromClasspath(ClassLoader cl) {
        if (cl == null) {
            return;
        }
        URL resource = cl.getResource("rhino.config");
        if (resource == null) {
            return;
        }
        try (InputStream in = resource.openStream()) {
            Properties props = new Properties();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            load(props, resource.toString());
        } catch (IOException e) {
            System.err.println(
                    "Error loading rhino.config from " + resource + ": " + e.getMessage());
        }
    }

    /** Replacement for {@link System#getProperty(String)}. */
    public static String getProperty(String key) {
        return null;
    }

    /** Replacement for {@link System#getProperty(String, String)}. */
    public static String getProperty(String key, String defaultValue) {
        String val = getProperty(key);
        return (val == null) ? defaultValue : val;
    }

    /** Replacement for {@link Boolean#getBoolean(String)}. */
    public static boolean getBoolean(String name) {
        boolean result = false;
        try {
            result = Boolean.parseBoolean(getProperty(name));
        } catch (IllegalArgumentException | NullPointerException e) {
        }
        return result;
    }

    /** Replacement for {@link Integer#getInteger(String, Integer)}. */
    public static Integer getInteger(String nm, Integer val) {
        String v = null;
        try {
            v = getProperty(nm);
        } catch (IllegalArgumentException | NullPointerException e) {
        }
        if (v != null) {
            try {
                return Integer.decode(v);
            } catch (NumberFormatException e) {
            }
        }
        return val;
    }

    /** Replacement for {@link Integer#getInteger(String, int)}. */
    public static Integer getInteger(String nm, int val) {
        Integer result = getInteger(nm, null);
        return (result == null) ? Integer.valueOf(val) : result;
    }

    /** Replacement for {@link Integer#getInteger(String)}. */
    public static Integer getInteger(String nm) {
        return getInteger(nm, null);
    }

    /** Returns the property as string. */
    private String get(Map map, String property, String defaultValue) {
        Object ret = find(map, property);
        if (ret == null) {
            return defaultValue;
        } else {
            return ret.toString();
        }
    }

    /** Returns the property as enum. */
    private <T extends Enum<T>> T get(Map map, String property, T defaultValue) {
        Object ret = map.get(property);
        if (ret == null) {
            return defaultValue;
        } else {
            Class<T> enumType = (Class<T>) defaultValue.getClass();
            return Enum.valueOf(enumType, ret.toString().toUpperCase(Locale.ROOT));
        }
    }

    /** Returns the property as boolean. */
    private boolean get(Map map, String property, boolean defaultValue) {
        Object ret = map.get(property);
        if (ret == null) {
            return defaultValue;
        } else if (ret instanceof Boolean) {
            return (Boolean) ret;
        } else {
            return "1".equals(ret) || "true".equals(ret);
        }
    }

    /**
     * Tries to find the property in the map. It tries the property first, then it tries the camel
     * upper version.
     */
    private Object find(Map map, String property) {
        Object ret = map.get(property);
        if (ret != null) {
            return ret;
        }
        return map.get(toCamelUpper(property));
    }

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

    private void load(Map map, String location) {
        stackStyle = get(map, "rhino.stack.style", stackStyle);
        useJavaPolicySecurity = get(map, "rhino.use_java_policy_security", useJavaPolicySecurity);
        printTrees = get(map, "rhino.printTrees", printTrees);
        printICodes = get(map, "rhino.printICodes", printICodes);
        debugStack = get(map, "rhino.debugStack", debugStack);
        debugLinker = get(map, "rhino.debugLinker", debugStack);
    }

    /** "rhino.stack.style" */
    private StackStyle stackStyle;

    private boolean useJavaPolicySecurity;
    private boolean printTrees = false;
    private boolean printICodes = false;
    private boolean debugStack = false;
    private boolean debugLinker = false;

    public StackStyle stackStyle() {
        return stackStyle;
    }

    public boolean useJavaPolicySecurity() {
        return useJavaPolicySecurity;
    }

    public boolean printTrees() {
        return printTrees;
    }

    public boolean printICodes() {
        return printICodes;
    }

    public boolean debugStack() {
        return debugStack;
    }

    public boolean debugLinker() {
        return debugLinker;
    }
}
