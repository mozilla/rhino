package org.mozilla.javascript.nat.type;

import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.mozilla.javascript.nat.type.definition.TypeInfoFactory;
import org.mozilla.javascript.nat.type.format.TypeFormatContext;

/**
 * @author ZZZank
 */
public class VariableTypeInfo extends TypeInfoBase implements
    org.mozilla.javascript.nat.type.definition.VariableTypeInfo {
    static final Map<TypeVariable<?>, VariableTypeInfo> CACHE = new ConcurrentHashMap<>();

    private final TypeVariable<?> raw;
    private final TypeInfo mainBound;

    VariableTypeInfo(TypeVariable<?> raw) {
        this.raw = raw;
        this.mainBound = TypeInfo.of(raw.getBounds()[0]);
    }

    /**
     * If no upper bound is explicitly declared, this method will return an empty array, instead of
     * using Java default behaviour (return a length 1 array holding {@code Object.class})
     */
    public TypeInfo[] getBounds() {
        var rawBounds = raw.getBounds();
        if (rawBounds.length == 1 && rawBounds[0] == Object.class) {
            // shortcut for most variable types with no bounds
            return TypeInfo.EMPTY_ARRAY;
        }
        return TypeInfo.ofArray(rawBounds);
    }

    @Override
    public String name() {
        return raw.getName();
    }

    @Override
    public List<TypeInfo> bounds(TypeInfoFactory factory) {
        return Arrays.asList(getBounds());
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
        for (var bound : this.getBounds()) {
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
    public TypeInfo consolidate(Map<VariableTypeInfo, TypeInfo> mapping) {
        return mapping.getOrDefault(this, this);
    }
}
