package org.mozilla.javascript.nat.type;

import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author ZZZank
 */
public class VariableTypeInfo extends TypeInfoBase {
    static final Map<TypeVariable<?>, VariableTypeInfo> CACHE = new IdentityHashMap<>();

    private final TypeVariable<?> raw;
    private TypeInfo mainBound = null;

    VariableTypeInfo(TypeVariable<?> raw) {
        this.raw = raw;
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

    public String getName() {
        return raw.getName();
    }

    /**
     * The main bound is what a {@link TypeVariable} will become when converted to a {@link Class},
     * and what this type should be after Java erased generic type info
     *
     * <p>for {@code T}, the main bound will be {@link Object}, for {@code T extends XXX}, the main
     * bound will be {@code XXX}, for {@code T extends XXX & YYY & ZZZ} the main bound will still be
     * {@code XXX}
     *
     * @see #asClass()
     */
    public TypeInfo getMainBound() {
        if (mainBound == null) {
            mainBound = TypeInfo.of(raw.getBounds()[0]);
        }
        return mainBound;
    }

    /**
     * @see #getMainBound()
     */
    @Override
    public Class<?> asClass() {
        return getMainBound().asClass();
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
    public boolean isObject() {
        return getMainBound().isObject();
    }

    @Override
    public int getTypeTag() {
        return getMainBound().getTypeTag();
    }

    @Override
    public TypeInfo consolidate(Map<VariableTypeInfo, TypeInfo> mapping) {
        return mapping.getOrDefault(this, this);
    }
}
