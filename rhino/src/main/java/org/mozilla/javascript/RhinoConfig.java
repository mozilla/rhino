package org.mozilla.javascript;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

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
 * The config files are in UTF-8 format and all keys in this configuration are case-insensitive and
 * dot/underscore-insensitive.
 *
 * <p>This means, "rhino.use_java_policy_security" is equvalent to "RHINO_USE_JAVA_POLICY_SECURITY"
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class RhinoConfig {
    private static final Map<String, String> PROPERTIES =
            AccessController.doPrivileged((PrivilegedAction<Map<String, String>>) () -> init());

    private static Map<String, String> init() {
        // we have to add a comparator at least for environment: TODO: can this done simpler
        Comparator<String> comparator =
                (s1, s2) -> {
                    s1 = s1.toLowerCase(Locale.ROOT).replace('_', '.');
                    s2 = s2.toLowerCase(Locale.ROOT).replace('_', '.');
                    return s1.compareTo(s2);
                };
        Map<String, String> map = new TreeMap<>(comparator);

        // load from classpaths
        map.putAll(loadFromClasspath(RhinoConfig.class.getClassLoader()));
        map.putAll(loadFromClasspath(Thread.currentThread().getContextClassLoader()));
        map.putAll(loadFromFile(new File("rhino.config")));
        copyMap(System.getProperties(), map);
        copyMap(System.getenv(), map);
		System.out.println("Current config: " + map);
        return map;
    }

    /** Copies all rhino relevant properties. */
    private static void copyMap(Map<?, ?> src, Map<String, String> dst) {
        for (Map.Entry<?, ?> entry : src.entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                String key = (String) entry.getKey();
                if (key.startsWith("rhino.") || key.startsWith("RHINO_")) {
                    dst.put(key, (String) entry.getValue());
                }
            }
        }
    }

	@SuppressWarnings("unchecked")
    private static Map<String, String> loadFromFile(File config) {
        if (config.exists()) {
            try (InputStream in = new FileInputStream(config)) {
                if (in != null) {
                    Properties props = new Properties();
                    props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                    System.out.println(
                            "Loaded rhino.config from "
                                    + config.getAbsolutePath()); // TODO: remove these prints
                    return (Map) props;
                }
            } catch (IOException e) {
                System.err.println(
                        "Error loading rhino.config from "
                                + config.getAbsolutePath()
                                + ": "
                                + e.getMessage());
            }
        }
        return Collections.emptyMap();
    }

	@SuppressWarnings("unchecked")
    private static Map<String, String> loadFromClasspath(ClassLoader cl) {
        if (cl != null) {
            try (InputStream in = cl.getResourceAsStream("rhino.config")) {
                if (in != null) {
                    Properties props = new Properties();
                    props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                    System.out.println(
                            "Loaded "
                                    + props.size()
                                    + " proerties from rhino.config in classpath"); // TODO: remove
                    // these prints
                    return (Map) props;
                }
            } catch (IOException e) {
                System.err.println("Error loading rhino.config from classpath: " + e.getMessage());
            }
        }
        return Collections.emptyMap();
    }

    /** Replacement for {@link System#getProperty(String)}. */
    public static String getProperty(String key) {
        return PROPERTIES.get(key);
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
}
