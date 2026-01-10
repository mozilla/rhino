package org.mozilla.javascript.lc;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.mozilla.javascript.lc.type.TypeInfo;

/**
 * @author ZZZank
 */
public abstract class ReflectUtils {

    public static Iterator<Class<?>> walkSuperClasses(Class<?> start, boolean includeSelf) {
        var c = includeSelf ? start : start.getSuperclass();
        return new Iterator<>() {
            private Class<?> current = c;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public Class<?> next() {
                var result = current;
                if (result == null) {
                    throw new NoSuchElementException();
                }
                current = current.getSuperclass();
                return result;
            }
        };
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
