package org.mozilla.javascript.nat.type.impl;

import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.mozilla.javascript.nat.type.TypeFormatContext;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.TypeInfoFactory;

/**
 * @author ZZZank
 */
public class VariableTypeInfo extends TypeInfoBase
        implements org.mozilla.javascript.nat.type.VariableTypeInfo {

    private final TypeVariable<?> raw;
    private final TypeInfo mainBound;

    public VariableTypeInfo(TypeVariable<?> raw) {
        this.raw = raw;
        this.mainBound = TypeInfoFactory.GLOBAL.create(raw.getBounds()[0]);
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
        return mainBound;
    }

    /**
     * @see #mainBound()
     */
    @Override
    public Class<?> asClass() {
        return mainBound().asClass();
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
    public boolean isObjectExact() {
        return mainBound().isObjectExact();
    }

    @Override
    public int getTypeTag() {
        return mainBound().getTypeTag();
    }

    @Override
    public TypeInfo consolidate(
            Map<org.mozilla.javascript.nat.type.VariableTypeInfo, TypeInfo> mapping) {
        return mapping.getOrDefault(this, this);
    }
}
