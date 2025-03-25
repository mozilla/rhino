package org.mozilla.javascript.nat.type;

import java.lang.reflect.TypeVariable;
import java.util.List;

/**
 * @author ZZZank
 */
public interface VariableTypeInfo extends TypeInfo {

    /**
     * @see TypeVariable#getName()
     */
    String name();

    /**
     * @see TypeVariable#getBounds()
     */
    List<TypeInfo> bounds(TypeInfoFactory factory);

    /**
     * The main bound is what a {@link TypeVariable} will become when converted to a {@link Class},
     * aka what this type should be after Java erased generic type info
     *
     * <p>for {@code T}, the main bound will be {@link Object}, for {@code T extends XXX}, the main
     * bound will be {@code XXX}, for {@code T extends XXX & YYY & ZZZ} the main bound will still be
     * {@code XXX}
     *
     * @see #asClass()
     */
    TypeInfo mainBound();
}
