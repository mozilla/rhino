package org.mozilla.javascript.nat.type;

public class PrimitiveClassTypeInfo extends ClassTypeInfo {
    private final Object defaultValue;

    public PrimitiveClassTypeInfo(Class<?> type, Object defaultValue) {
        super(type);
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean is(Class<?> c) {
        return c.isPrimitive() && asClass() == c;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public Object createDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isVoid() {
        return asClass() == Void.TYPE;
    }

    @Override
    public boolean isBoolean() {
        return asClass() == Boolean.TYPE;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isByte() {
        return asClass() == Byte.TYPE;
    }

    @Override
    public boolean isShort() {
        return asClass() == Short.TYPE;
    }

    @Override
    public boolean isInt() {
        return asClass() == Integer.TYPE;
    }

    @Override
    public boolean isLong() {
        return asClass() == Long.TYPE;
    }

    @Override
    public boolean isFloat() {
        return asClass() == Float.TYPE;
    }

    @Override
    public boolean isDouble() {
        return asClass() == Double.TYPE;
    }

    @Override
    public boolean isCharacter() {
        return asClass() == Character.TYPE;
    }

    @Override
    public boolean isAssignableFrom(TypeInfo another) {
        return this == another;
    }
}
