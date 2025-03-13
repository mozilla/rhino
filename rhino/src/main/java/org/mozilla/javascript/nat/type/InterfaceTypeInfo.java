package org.mozilla.javascript.nat.type;

import java.util.IdentityHashMap;
import java.util.Map;
import org.mozilla.javascript.nat.ByteAsBool;

public class InterfaceTypeInfo extends ClassTypeInfo {
    static final Map<Class<?>, InterfaceTypeInfo> CACHE = new IdentityHashMap<>();

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
                if (asClass().isAnnotationPresent(FunctionalInterface.class)) {
                    functional = ByteAsBool.TRUE;
                } else {
                    int count = 0;

                    for (var method : asClass().getMethods()) {
                        if (!method.isDefault() && !method.isSynthetic() && !method.isBridge()) {
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
