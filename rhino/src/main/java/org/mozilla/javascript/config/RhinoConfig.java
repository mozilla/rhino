package org.mozilla.javascript.config;

import java.util.Locale;

/**
 * With RhinoConfig, you can access the {@link RhinoProperties} in a typesafe way.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class RhinoConfig {

    /** Returns the property as string. */
    private static String get(String property, String defaultValue) {
        Object ret = RhinoProperties.get(property);
        if (ret != null) {
            return ret.toString();
        }
        return defaultValue;
    }

    /** Returns the property as string with null as default. */
    public static String get(String property) {
        return get(property, (String) null);
    }

    /** Returns the property as enum. Note: default value must be specified */
    public static <T extends Enum<T>> T get(String property, T defaultValue) {
        Object ret = RhinoProperties.get(property);
        if (ret != null) {
            Class<T> enumType = (Class<T>) defaultValue.getClass();
            // We assume, that enums all are in UPPERCASES
            return Enum.valueOf(enumType, ret.toString().toUpperCase(Locale.ROOT));
        }
        return defaultValue;
    }

    /** Returns the property as boolean. */
    public static boolean get(String property, boolean defaultValue) {
        Object ret = RhinoProperties.get(property);
        if (ret instanceof Boolean) {
            return (Boolean) ret;
        } else {
            return "1".equals(ret) || "true".equals(ret);
        }
    }

    /** Returns the property as integer. */
    public static int get(String property, int defaultValue) {
        Object ret = RhinoProperties.get(property);
        if (ret instanceof Number) {
            return ((Number) ret).intValue();
        } else {
            try {
                return Integer.decode(ret.toString());
            } catch (NumberFormatException e) {
            }
        }
        return defaultValue;
    }
}
