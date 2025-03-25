package org.mozilla.javascript.nat.type.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EnumTypeInfo extends ClassTypeInfo {
    static final Map<Class<?>, EnumTypeInfo> CACHE = new ConcurrentHashMap<>();

    public static String getName(Object e) {
        return ((Enum<?>) e).name();
    }

    private List<Object> constants;

    public EnumTypeInfo(Class<?> type) {
        super(type);
    }

    @Override
    public List<Object> enumConstants() {
        if (constants == null) {
            constants = List.of(asClass().getEnumConstants());
        }

        return constants;
    }
}
