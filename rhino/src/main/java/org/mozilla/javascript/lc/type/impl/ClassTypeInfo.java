package org.mozilla.javascript.lc.type.impl;

import org.mozilla.javascript.lc.type.TypeFormatContext;

public abstract class ClassTypeInfo extends TypeInfoBase {
    private final Class<?> type;

    ClassTypeInfo(Class<?> type) {
        this.type = type;
    }

    @Override
    public final Class<?> asClass() {
        return type;
    }

    @Override
    public boolean is(Class<?> c) {
        return type == c;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o == this) || ((o instanceof ClassTypeInfo) && (type == ((ClassTypeInfo) o).type));
    }

    @Override
    public void append(TypeFormatContext ctx, StringBuilder builder) {
        builder.append(ctx.getClassName(this.type));
    }
}
