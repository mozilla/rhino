package org.mozilla.javascript.lc.type.impl;

import org.mozilla.javascript.lc.ByteAsBool;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

public class InterfaceTypeInfo extends ClassTypeInfo {
    /** Android device might not have {@link FunctionalInterface} class */
    private static final Class<? extends Annotation> FN_INTERFACE = FunctionalInterface.class;

    /** Not using nullable {@link Boolean} in an attempt of reducing object size. */
    private byte functional;

    public InterfaceTypeInfo(Class<?> type) {
        this(type, ByteAsBool.UNKNOWN);
    }

    public InterfaceTypeInfo(Class<?> type, byte functional) {
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
}
