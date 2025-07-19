package org.mozilla.javascript.lc.type.impl;

import java.util.Objects;
import java.util.function.Consumer;
import org.mozilla.javascript.lc.java.FunctionObject;
import org.mozilla.javascript.lc.type.TypeFormatContext;
import org.mozilla.javascript.lc.type.TypeInfo;

public final class ArrayTypeInfo extends TypeInfoBase {
    private final TypeInfo component;
    private Class<?> asClass;

    public ArrayTypeInfo(TypeInfo component) {
        this.component = Objects.requireNonNull(component);
    }

    @Override
    public boolean is(Class<?> c) {
        return c.isArray() && asClass() == c;
    }

    @Override
    public Class<?> asClass() {
        if (asClass == null) {
            asClass = component.newArray(0).getClass();
        }

        return asClass;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj == this)
                || ((obj instanceof ArrayTypeInfo)
                        && component.equals(((ArrayTypeInfo) obj).component));
    }

    @Override
    public int hashCode() {
        return component.hashCode();
    }

    @Override
    public void append(TypeFormatContext ctx, StringBuilder builder) {
        ctx.formatArray(builder, this);
    }

    @Override
    public TypeInfo getComponentType() {
        return component;
    }

    @Override
    public void collectComponentClass(Consumer<Class<?>> collector) {
        component.collectComponentClass(collector);
    }

    @Override
    public boolean isArray() {
        return true;
    }

    /**
     * array type is not any of the base types
     *
     * @see TypeInfo#getTypeTag()
     */
    @Override
    public int getTypeTag() {
        return FunctionObject.JAVA_UNSUPPORTED_TYPE;
    }
}
