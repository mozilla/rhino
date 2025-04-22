package org.mozilla.javascript.nat.type;

import java.lang.reflect.WildcardType;
import java.util.List;

/**
 * @see WildcardType
 * @author ZZZank
 */
public interface WildcardTypeInfo extends TypeInfo {

    /**
     * @see WildcardType#getUpperBounds()
     */
    List<TypeInfo> upperBounds(TypeInfoFactory factory);

    /**
     * @see WildcardType#getLowerBounds()
     */
    List<TypeInfo> lowerBounds(TypeInfoFactory factory);

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
