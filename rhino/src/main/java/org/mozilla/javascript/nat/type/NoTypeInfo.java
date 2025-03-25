package org.mozilla.javascript.nat.type;

import java.util.function.Consumer;

/**
 * @see TypeInfo#NONE
 */
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
        return TypeFormatContext.DEFAULT.getFormattedNone();
    }

    @Override
    public void append(TypeFormatContext ctx, StringBuilder builder) {
        builder.append(ctx.getFormattedNone());
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
}
