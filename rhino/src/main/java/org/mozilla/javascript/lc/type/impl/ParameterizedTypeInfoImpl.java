package org.mozilla.javascript.lc.type.impl;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.mozilla.javascript.lc.type.ParameterizedTypeInfo;
import org.mozilla.javascript.lc.type.TypeFormatContext;
import org.mozilla.javascript.lc.type.TypeInfo;

public final class ParameterizedTypeInfoImpl extends TypeInfoBase implements ParameterizedTypeInfo {
    private final TypeInfo rawType;
    private final List<TypeInfo> params;
    private int hashCode;

    public ParameterizedTypeInfoImpl(TypeInfo rawType, List<TypeInfo> params) {
        this.rawType = rawType;
        this.params = params;
        for (var param : params) { // implicit null check on `params`
            Objects.requireNonNull(param);
        }
    }

    @Override
    public Class<?> asClass() {
        return rawType.asClass();
    }

    @Override
    public boolean is(Class<?> c) {
        return rawType.is(c);
    }

    @Override
    public TypeInfo param(int index) {
        if (index < 0 || index >= params.size()) {
            return TypeInfo.NONE;
        }
        var got = params.get(index);
        return got == TypeInfo.OBJECT ? TypeInfo.NONE : got;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = rawType.hashCode() * 31 + params.hashCode();

            // make sure computed hashcode is never 0 to prevent computing again
            if (hashCode == 0) {
                hashCode = -1;
            }
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object object) {
        return (this == object)
                || ((object instanceof ParameterizedTypeInfoImpl)
                        && rawType.equals(((ParameterizedTypeInfoImpl) object).rawType)
                        && params.equals(((ParameterizedTypeInfoImpl) object).params));
    }

    @Override
    public void append(TypeFormatContext ctx, StringBuilder builder) {
        ctx.formatParameterized(builder, this);
    }

    @Override
    public TypeInfo rawType() {
        return rawType;
    }

    @Override
    public List<TypeInfo> params() {
        return params;
    }

    @Override
    public Object newArray(int length) {
        return rawType.newArray(length);
    }

    @Override
    public void collectComponentClass(Consumer<Class<?>> collector) {
        rawType.collectComponentClass(collector);
        for (var param : params) {
            param.collectComponentClass(collector);
        }
    }
}
