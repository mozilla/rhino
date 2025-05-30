package org.mozilla.javascript.lc.type.impl;

public class BasicClassTypeInfo extends ClassTypeInfo {

    public BasicClassTypeInfo(Class<?> type) {
        super(type);
    }

    @Override
    public boolean isVoid() {
        return asClass() == Void.class;
    }

    @Override
    public boolean isBoolean() {
        return asClass() == Boolean.class;
    }

    @Override
    public boolean isByte() {
        return asClass() == Byte.class;
    }

    @Override
    public boolean isShort() {
        return asClass() == Short.class;
    }

    @Override
    public boolean isInt() {
        return asClass() == Integer.class;
    }

    @Override
    public boolean isLong() {
        return asClass() == Long.class;
    }

    @Override
    public boolean isFloat() {
        return asClass() == Float.class;
    }

    @Override
    public boolean isDouble() {
        return asClass() == Double.class;
    }

    @Override
    public boolean isCharacter() {
        return asClass() == Character.class;
    }

    @Override
    public boolean isString() {
        return asClass() == String.class;
    }

    @Override
    public boolean isObjectExact() {
        return asClass() == Object.class;
    }
}
