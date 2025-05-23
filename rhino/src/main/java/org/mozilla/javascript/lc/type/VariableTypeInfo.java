package org.mozilla.javascript.lc.type;

import java.lang.reflect.TypeVariable;
import java.util.List;

/**
 * @see TypeVariable
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

    @Override
    default boolean isEnum() {
        return mainBound().isEnum();
    }

    @Override
    default boolean isInterface() {
        return mainBound().isInterface();
    }

    @Override
    default Class<?> asClass() {
        return mainBound().asClass();
    }

    @Override
    default boolean isObjectExact() {
        return mainBound().isObjectExact();
    }

    @Override
    default boolean isNumber() {
        return mainBound().isNumber();
    }

    @Override
    default int getTypeTag() {
        return mainBound().getTypeTag();
    }

    @Override
    default boolean isVoid() {
        return mainBound().isVoid();
    }

    @Override
    default boolean isBoolean() {
        return mainBound().isBoolean();
    }

    @Override
    default boolean isByte() {
        return mainBound().isByte();
    }

    @Override
    default boolean isShort() {
        return mainBound().isShort();
    }

    @Override
    default boolean isInt() {
        return mainBound().isInt();
    }

    @Override
    default boolean isLong() {
        return mainBound().isLong();
    }

    @Override
    default boolean isFloat() {
        return mainBound().isFloat();
    }

    @Override
    default boolean isDouble() {
        return mainBound().isDouble();
    }

    @Override
    default boolean isCharacter() {
        return mainBound().isCharacter();
    }

    @Override
    default boolean isString() {
        return mainBound().isString();
    }

    @Override
    default boolean isArray() {
        return mainBound().isArray();
    }
}
