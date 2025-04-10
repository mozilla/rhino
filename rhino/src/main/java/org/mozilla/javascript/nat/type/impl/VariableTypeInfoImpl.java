package org.mozilla.javascript.nat.type.impl;

import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.mozilla.javascript.nat.type.TypeFormatContext;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.TypeInfoFactory;
import org.mozilla.javascript.nat.type.VariableTypeInfo;

/**
 * @author ZZZank
 */
public class VariableTypeInfoImpl extends TypeInfoBase implements VariableTypeInfo {

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
    public void append(TypeFormatContext ctx, StringBuilder builder) {
        builder.append(raw.getName());
    }

    @Override
    public void collectComponentClass(Consumer<Class<?>> collector) {
        for (var bound : this.bounds(TypeInfoFactory.GLOBAL)) {
            bound.collectComponentClass(collector);
        }
    }

    @Override
    public TypeInfo consolidate(Map<VariableTypeInfo, TypeInfo> mapping) {
        return mapping.getOrDefault(this, this);
    }
}
