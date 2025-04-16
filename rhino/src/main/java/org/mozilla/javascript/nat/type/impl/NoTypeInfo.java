package org.mozilla.javascript.nat.type.impl;

import java.util.function.Consumer;
import org.mozilla.javascript.nat.type.TypeFormatContext;
import org.mozilla.javascript.nat.type.TypeInfo;

/**
 * @see TypeInfo#NONE
 */
public enum NoTypeInfo implements TypeInfo {
    INSTANCE;

    @Override
    public Class<?> asClass() {
        return Object.class;
    }

    @Override
    public boolean shouldConvert() {
        return false;
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
