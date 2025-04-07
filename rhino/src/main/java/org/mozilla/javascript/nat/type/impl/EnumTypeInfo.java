package org.mozilla.javascript.nat.type.impl;

import java.util.List;

public class EnumTypeInfo extends ClassTypeInfo {

    public static String getName(Object e) {
        return ((Enum<?>) e).name();
    }

    private List<Object> constants;

    public EnumTypeInfo(Class<?> type) {
        super(type);
    }

    @Override
    public boolean isEnum() {
        return true;
    }

    @Override
    public List<Object> enumConstants() {
        if (constants == null) {
            constants = List.of(asClass().getEnumConstants());
        }

        return constants;
    }
}
