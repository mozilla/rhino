package org.mozilla.javascript.lc;

import java.util.List;
import org.mozilla.javascript.lc.type.TypeInfo;

/**
 * @author ZZZank
 */
public abstract class ReflectUtils {

    /**
     * {@code true} if we are on a "modular" version of Java (Java 11 or up, excluding Android). It
     * does not use the SourceVersion class because this is not present on Android.
     */
    public static final boolean IS_MODULAR_JAVA;

    static {
        boolean isModularJava;
        try {
            Class.class.getMethod("getModule");
            isModularJava = true;
        } catch (NoSuchMethodException e) {
            isModularJava = false;
        }
        IS_MODULAR_JAVA = isModularJava;
    }

    public static String javaSignature(Class<?> type) {
        int arrayDimension = 0;
        while (type.isArray()) {
            arrayDimension++;
            type = type.getComponentType();
        }

        if (arrayDimension == 0) {
            return type.getName();
        }

        return type.getName() + "[]".repeat(arrayDimension);
    }

    public static String liveConnectSignature(List<TypeInfo> argTypes) {
        if (argTypes.isEmpty()) {
            return "()";
        }

        var builder = new StringBuilder();

        builder.append('(');
        var iter = argTypes.iterator();
        if (iter.hasNext()) {
            builder.append(javaSignature(iter.next().asClass()));
            while (iter.hasNext()) {
                builder.append(',').append(javaSignature(iter.next().asClass()));
            }
        }
        builder.append(')');

        return builder.toString();
    }
}
