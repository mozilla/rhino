package org.mozilla.javascript.nat.type;

import java.util.List;
import java.util.function.Consumer;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.nat.type.format.TypeFormatContext;

public final class ParameterizedTypeInfo extends TypeInfoBase implements
    org.mozilla.javascript.nat.type.definition.ParameterizedTypeInfo {
    private final TypeInfo rawType;
    private final List<TypeInfo> params;
    private int hashCode;

    ParameterizedTypeInfo(TypeInfo rawType, TypeInfo[] params) {
        this.rawType = rawType;
        this.params = List.of(params);
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
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object object) {
        return (this == object)
                || ((object instanceof ParameterizedTypeInfo)
                        && rawType.equals(((ParameterizedTypeInfo) object).rawType)
                        && params.equals(((ParameterizedTypeInfo) object).params));
    }

    @Override
    public void append(TypeFormatContext ctx, StringBuilder builder) {
        ctx.formatParameterized(builder, this);
    }

    public TypeInfo rawType() {
        return rawType;
    }

    public List<TypeInfo> params() {
        return params;
    }

    @Override
    public Object newArray(int length) {
        return rawType.newArray(length);
    }

    @Override
    public TypeInfo withParams(TypeInfo... params) {
        return rawType.withParams(params);
    }

    @Override
    public boolean isFunctionalInterface() {
        return rawType.isFunctionalInterface();
    }

    @Override
    public List<Object> enumConstants() {
        return rawType.enumConstants();
    }

    @Override
    public void collectComponentClass(Consumer<Class<?>> collector) {
        rawType.collectComponentClass(collector);
        for (var param : params) {
            param.collectComponentClass(collector);
        }
    }

    @Override
    public boolean isInterface() {
        return this.rawType.isInterface();
    }

    @Override
    public boolean isAssignableFrom(TypeInfo another) {
        return this.rawType.isAssignableFrom(another);
    }

    @Override
    public boolean isInstance(Object o) {
        return this.rawType.isInstance(o);
    }

    /**
     * none of the base types is parameterized, unless this object is an implementation of {@link
     * Scriptable} with type variables
     */
    @Override
    public int getTypeTag() {
        if (Scriptable.class.isAssignableFrom(asClass())) {
            return FunctionObject.JAVA_SCRIPTABLE_TYPE;
        }
        return FunctionObject.JAVA_UNSUPPORTED_TYPE;
    }
}
