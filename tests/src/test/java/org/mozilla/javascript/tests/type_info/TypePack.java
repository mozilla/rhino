package org.mozilla.javascript.tests.type_info;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.TypeInfoFactory;

/**
 * @author ZZZank
 */
public final class TypePack {
    private final Type raw;
    private final Class<?> clazz;
    private final TypeInfo resolved;

    public TypePack(Type raw, Class<?> clazz, TypeInfo resolved) {
        this.raw = raw;
        this.clazz = clazz;
        this.resolved = resolved;
    }

    public TypePack(Type raw, Class<?> clazz) {
        this(raw, clazz, TypeInfoFactory.GLOBAL.create(raw));
    }

    public TypePack(Map.Entry<Type, Class<?>> entry) {
        this(entry.getKey(), entry.getValue());
    }

    public Type raw() {
        return raw;
    }

    public Class<?> clazz() {
        return clazz;
    }

    public TypeInfo resolved() {
        return resolved;
    }

    public TypePack map(UnaryOperator<Type> typeMapper, UnaryOperator<Class<?>> clazzMapper) {
        return new TypePack(typeMapper.apply(this.raw), clazzMapper.apply(clazz));
    }
}
