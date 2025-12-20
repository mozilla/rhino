package org.mozilla.javascript.lc.type.impl;

import java.util.Map;
import java.util.Objects;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.VariableTypeInfo;

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
        // prevent hash collision with component type
        return component.hashCode() + 1;
    }

    @Override
    public TypeInfo getComponentType() {
        return component;
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

    @Override
    public TypeInfo consolidate(Map<VariableTypeInfo, TypeInfo> mapping) {
        var component = this.component;
        var consolidated = component.consolidate(mapping);
        return component == consolidated ? this : new ArrayTypeInfo(consolidated);
    }
}
