package org.mozilla.javascript.lc.type.impl;

import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.VariableTypeInfo;

/**
 * @author ZZZank
 */
public final class VariableTypeInfoImpl extends TypeInfoBase implements VariableTypeInfo {

    private final TypeVariable<?> raw;
    private volatile Object mainBound;

    public VariableTypeInfoImpl(TypeVariable<?> raw, TypeInfoFactory factory) {
        this.raw = raw;
        mainBound = factory;
    }

    @Override
    public String name() {
        return raw.getName();
    }

    @Override
    public List<TypeInfo> bounds(TypeInfoFactory factory) {
        return factory.createList(this.raw.getBounds());
    }

    @Override
    public TypeInfo mainBound() {
        if (mainBound instanceof TypeInfoFactory) {
            synchronized (this) {
                if (mainBound instanceof TypeInfoFactory) {
                    mainBound = ((TypeInfoFactory) mainBound).create(raw.getBounds()[0]);
                }
            }
        }
        return (TypeInfo) mainBound;
    }

    @Override
    public TypeInfo consolidate(Map<VariableTypeInfo, TypeInfo> mapping) {
        return mapping.getOrDefault(this, this);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VariableTypeInfoImpl
                && this.raw.equals(((VariableTypeInfoImpl) obj).raw);
    }
}
