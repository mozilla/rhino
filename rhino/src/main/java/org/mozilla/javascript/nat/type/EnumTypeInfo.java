package org.mozilla.javascript.nat.type;

import java.util.IdentityHashMap;
import java.util.List;

public class EnumTypeInfo extends ClassTypeInfo {
    static final IdentityHashMap<Class<?>, EnumTypeInfo> CACHE = new IdentityHashMap<>();

    public static String getName(Object e) {
        return ((Enum<?>) e).name();
    }

    private List<Object> constants;

    EnumTypeInfo(Class<?> type) {
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
