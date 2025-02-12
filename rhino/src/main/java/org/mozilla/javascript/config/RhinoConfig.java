package org.mozilla.javascript.config;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;

/**
 * RhinoConfig provides typesafe and static access methods to a {@link RhinoProperties} default
 * instance.
 *
 * <p>Note: Reading a config value only once is a good practice to avoid affecting performance. For
 * example:
 *
 * <pre>
 *     private static final boolean XYZ_ENABLED = RhinoConfig.get("rhino.xyz.enabled", false);
 * </pre>
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class RhinoConfig {

    private static final RhinoProperties INSTANCE =
            // We still assume, that a security manager could be in place!
            AccessController.doPrivileged(
                    (PrivilegedAction<RhinoProperties>) RhinoProperties::init);

    /**
     * Returns the property as string.
     *
     * <p>If the value is not set, <code>defaultVaule</code> is returned.
     */
    private static String get(String property, String defaultValue) {
        Object ret = INSTANCE.get(property);
        if (ret != null) {
            return ret.toString();
        }
        return defaultValue;
    }

    /** Returns the property as string with null as default. */
    public static String get(String property) {
        return get(property, (String) null);
    }

    /**
     * Returns the property as enum.
     *
     * <p>If the property is set to any of the enum names (case-insensitive), this enum value is
     * returned, otherwise <code>defaultValue</code> is returned.
     *
     * <p>Note: <code>defaultValue</code> must be specified to derive the enum class and its values.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T get(String property, T defaultValue) {
        Objects.requireNonNull(defaultValue, "defaultValue must not be null");
        Object ret = INSTANCE.get(property);
        if (ret != null) {
            Class<T> enumType = (Class<T>) defaultValue.getDeclaringClass();
            // We make a case insentive lookup here.
            for (T enm : enumType.getEnumConstants()) {
                if (enm.name().equalsIgnoreCase(ret.toString())) {
                    return enm;
                }
            }
        }
        return defaultValue;
    }

    /**
     * Returns the property as boolean.
     *
     * <p>A property is true, if it is either <code>Boolean.TRUE</code> or if and only if the string
     * representation is equal to {@code "true"} (case-insensitive). If the property is not set,
     * <code>defaultValue</code> is returned
     *
     * <p>Same behaviour as {@link Boolean#getBoolean(String)}
     */
    public static boolean get(String property, boolean defaultValue) {
        Object ret = INSTANCE.get(property);
        if (ret instanceof Boolean) {
            return (Boolean) ret;
        } else if (ret == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(ret.toString());
    }

    /**
     * Returns the property as integer.
     *
     * <p>if the property is not set or not a valid integer value, <code>defaultValue</code> is
     * returned.
     */
    public static int get(String property, int defaultValue) {
        Object ret = INSTANCE.get(property);
        if (ret instanceof Number) {
            return ((Number) ret).intValue();
        } else if (ret != null) {
            try {
                return Integer.decode(ret.toString());
            } catch (NumberFormatException e) {
                // ignore invalid values. See Integer.getInteger
            }
        }
        return defaultValue;
    }
}
