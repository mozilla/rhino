package org.mozilla.javascript.nat.type;

import java.util.function.Consumer;

public final class NoTypeInfo implements TypeInfo {
    static final NoTypeInfo INSTANCE = new NoTypeInfo();

    private NoTypeInfo() {}

    @Override
    public Class<?> asClass() {
        return Object.class;
    }

    @Override
    public boolean shouldConvert() {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "?";
    }

    @Override
    public void append(TypeFormatContext ctx, StringBuilder builder) {
        builder.append('?');
    }

    @Override
    public TypeInfo asArray() {
        return this;
    }

    @Override
    public TypeInfo withParams(TypeInfo... params) {
        return this;
    }

    @Override
    public void collectComponentClass(Consumer<Class<?>> collector) {}

    /** {@link Object} class is assignable from any class */
    @Override
    public boolean isAssignableFrom(TypeInfo another) {
        return true;
    }

    @Override
    public boolean isInstance(Object o) {
        return o != null;
    }

    @Override
    public boolean isObjectExact() {
        return true;
    }
}
