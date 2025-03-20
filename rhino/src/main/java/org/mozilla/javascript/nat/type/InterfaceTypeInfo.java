package org.mozilla.javascript.nat.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.nat.ByteAsBool;

public class InterfaceTypeInfo extends ClassTypeInfo {
    /** Android device might not have {@link FunctionalInterface} class */
    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation> FN_INTERFACE =
            (Class<? extends Annotation>) Kit.classOrNull("java.lang.FunctionalInterface");

    static final Map<Class<?>, InterfaceTypeInfo> CACHE = new ConcurrentHashMap<>();

    private byte functional;

    InterfaceTypeInfo(Class<?> type) {
        this(type, ByteAsBool.UNKNOWN);
    }

    InterfaceTypeInfo(Class<?> type, byte functional) {
        super(type);
        this.functional = functional;
    }

    @Override
    public boolean isFunctionalInterface() {
        if (ByteAsBool.isUnknown(functional)) {
            try {
                if (FN_INTERFACE != null && asClass().isAnnotationPresent(FN_INTERFACE)) {
                    functional = ByteAsBool.TRUE;
                } else {
                    int count = 0;

                    for (var method : asClass().getMethods()) {
                        if (Modifier.isAbstract(method.getModifiers())
                                && !method.isSynthetic()
                                && !method.isBridge()) {
                            count++;
                        }

                        if (count > 1) {
                            break;
                        }
                    }

                    functional = ByteAsBool.fromBool(count == 1);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                functional = ByteAsBool.FALSE;
            }
        }

        return ByteAsBool.isTrue(functional);
    }

    @Override
    public boolean isInterface() {
        return true;
    }

    @Override
    public boolean isVoid() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isByte() {
        return false;
    }

    @Override
    public boolean isShort() {
        return false;
    }

    @Override
    public boolean isInt() {
        return false;
    }

    @Override
    public boolean isLong() {
        return false;
    }

    @Override
    public boolean isFloat() {
        return false;
    }

    @Override
    public boolean isDouble() {
        return false;
    }

    @Override
    public boolean isCharacter() {
        return false;
    }

    @Override
    public boolean isObjectExact() {
        return false;
    }
}
