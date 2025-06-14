package org.mozilla.javascript.lc.type;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.lc.ByteAsBool;
import org.mozilla.javascript.lc.type.impl.ArrayTypeInfo;
import org.mozilla.javascript.lc.type.impl.BasicClassTypeInfo;
import org.mozilla.javascript.lc.type.impl.InterfaceTypeInfo;
import org.mozilla.javascript.lc.type.impl.NoTypeInfo;
import org.mozilla.javascript.lc.type.impl.PrimitiveClassTypeInfo;

/**
 * A representation of Java type, aiming at preserving more type information than what a {@link
 * Class} can provide
 *
 * <p>Note: using {@code null} is not recommended, {@link #NONE} should be used instead
 *
 * @see #asClass() : how to convert TypeInfo back to Class
 * @see #is(Class) : how to determine whether a TypeInfo represents a specific class
 */
public interface TypeInfo {
    TypeInfo NONE = NoTypeInfo.INSTANCE;

    /**
     * use {@link TypeInfo#isObjectExact()} to determine whether a type represents a {@link Object}
     * class, using `typeInfo == TypeInfo.OBJECT` might cause problem with {@link VariableTypeInfo}
     */
    TypeInfo OBJECT = new BasicClassTypeInfo(Object.class);

    TypeInfo OBJECT_ARRAY = new ArrayTypeInfo(OBJECT);

    TypeInfo PRIMITIVE_VOID = new PrimitiveClassTypeInfo(Void.TYPE, null);
    TypeInfo PRIMITIVE_BOOLEAN = new PrimitiveClassTypeInfo(Boolean.TYPE, false);
    TypeInfo PRIMITIVE_BYTE = new PrimitiveClassTypeInfo(Byte.TYPE, (byte) 0);
    TypeInfo PRIMITIVE_SHORT = new PrimitiveClassTypeInfo(Short.TYPE, (short) 0);
    TypeInfo PRIMITIVE_INT = new PrimitiveClassTypeInfo(Integer.TYPE, 0);
    TypeInfo PRIMITIVE_LONG = new PrimitiveClassTypeInfo(Long.TYPE, 0L);
    TypeInfo PRIMITIVE_FLOAT = new PrimitiveClassTypeInfo(Float.TYPE, 0F);
    TypeInfo PRIMITIVE_DOUBLE = new PrimitiveClassTypeInfo(Double.TYPE, 0D);
    TypeInfo PRIMITIVE_CHARACTER = new PrimitiveClassTypeInfo(Character.TYPE, (char) 0);

    TypeInfo VOID = new BasicClassTypeInfo(Void.class);
    TypeInfo BOOLEAN = new BasicClassTypeInfo(Boolean.class);
    TypeInfo BYTE = new BasicClassTypeInfo(Byte.class);
    TypeInfo SHORT = new BasicClassTypeInfo(Short.class);
    TypeInfo INT = new BasicClassTypeInfo(Integer.class);
    TypeInfo LONG = new BasicClassTypeInfo(Long.class);
    TypeInfo FLOAT = new BasicClassTypeInfo(Float.class);
    TypeInfo DOUBLE = new BasicClassTypeInfo(Double.class);
    TypeInfo CHARACTER = new BasicClassTypeInfo(Character.class);

    TypeInfo NUMBER = new BasicClassTypeInfo(Number.class);
    TypeInfo STRING = new BasicClassTypeInfo(String.class);
    TypeInfo STRING_ARRAY = new ArrayTypeInfo(STRING);
    TypeInfo RAW_CLASS = new BasicClassTypeInfo(Class.class);
    TypeInfo DATE = new BasicClassTypeInfo(Date.class);

    TypeInfo RUNNABLE = new InterfaceTypeInfo(Runnable.class, ByteAsBool.TRUE);
    TypeInfo RAW_CONSUMER = new InterfaceTypeInfo(Consumer.class, ByteAsBool.TRUE);
    TypeInfo RAW_SUPPLIER = new InterfaceTypeInfo(Supplier.class, ByteAsBool.TRUE);
    TypeInfo RAW_FUNCTION = new InterfaceTypeInfo(Function.class, ByteAsBool.TRUE);
    TypeInfo RAW_PREDICATE = new InterfaceTypeInfo(Predicate.class, ByteAsBool.TRUE);

    TypeInfo RAW_LIST = new InterfaceTypeInfo(List.class, ByteAsBool.FALSE);
    TypeInfo RAW_SET = new InterfaceTypeInfo(Set.class, ByteAsBool.FALSE);
    TypeInfo RAW_MAP = new InterfaceTypeInfo(Map.class, ByteAsBool.FALSE);
    TypeInfo RAW_OPTIONAL = new BasicClassTypeInfo(Optional.class);
    TypeInfo RAW_ENUM_SET = new BasicClassTypeInfo(EnumSet.class);
    TypeInfo BIG_INT = new BasicClassTypeInfo(BigInteger.class);

    /**
     * Get the {@link Class} object represented by this {@link TypeInfo}. For a {@link
     * java.lang.reflect.Type} object, the TypeInfo object created from the Type object should
     * return the exact same class as the one after Java erased its generic type info
     */
    Class<?> asClass();

    /**
     * Example:
     *
     * <p>1. for {@code Map<String, Integer>}, {@code param(-1)} == {@link TypeInfo#NONE}, {@code
     * param(0)} == {@link TypeInfo#STRING}, {@code param(1)} == {@link TypeInfo#INT}, {@code
     * param(2)} == {@link TypeInfo#NONE}
     *
     * <p>2. for {@code Map} (raw usage of generic class) or {@code Integer} (not generic class),
     * {@code param(anyNumberHere)} == {@link TypeInfo#NONE}
     *
     * @return the actual type parameter at provided index, or {@link #NONE} if:
     *     <p>1. index not in valid range, or
     *     <p>2. the TypeInfo itself is not representing a {@link
     *     java.lang.reflect.ParameterizedType}
     * @see ParameterizedTypeInfo#params()
     */
    default TypeInfo param(int index) {
        return NONE;
    }

    /**
     * @return true if this TypeInfo represents the same class as the {@link Class} parameter, false
     *     otherwise
     */
    default boolean is(Class<?> c) {
        return asClass() == c;
    }

    /**
     * @return {@code true} if this TypeInfo does NOT represent the same class as the {@link Class}
     *     parameter, {@code false} otherwise
     */
    default boolean isNot(Class<?> c) {
        return !is(c);
    }

    /**
     * @see Class#isPrimitive()
     */
    default boolean isPrimitive() {
        return false;
    }

    default boolean shouldConvert() {
        return true;
    }

    /**
     * @see #append(TypeFormatContext, StringBuilder)
     */
    @Override
    String toString();

    void append(TypeFormatContext ctx, StringBuilder builder);

    /**
     * @see Class#getComponentType()
     */
    default TypeInfo getComponentType() {
        return NONE;
    }

    /** get an array whose element type is the caller TypeInfo */
    default Object newArray(int length) {
        return Array.newInstance(asClass(), length);
    }

    /**
     * @see Class#isInterface()
     */
    default boolean isInterface() {
        return false;
    }

    /**
     * @see FunctionalInterface
     */
    default boolean isFunctionalInterface() {
        return false;
    }

    /**
     * @see Class#isEnum()
     */
    default boolean isEnum() {
        return false;
    }

    /**
     * @see Class#getEnumConstants()
     */
    default List<Object> enumConstants() {
        return List.of();
    }

    default Object createDefaultValue() {
        return null;
    }

    /**
     * @return true if this TypeInfo represents {@link Void} class or {@code void} class
     */
    default boolean isVoid() {
        return false;
    }

    /**
     * @return true if this TypeInfo represents {@link Boolean} class or {@code boolean} class
     */
    default boolean isBoolean() {
        return false;
    }

    /**
     * @return true if this TypeInfo represents a type assignable to {@link Number} class
     * @see #isAssignableFrom(TypeInfo)
     */
    default boolean isNumber() {
        // the implementation here does not look like other `isXXX()` method because Number is not a
        // final class, so we cannot match type directly
        return Number.class.isAssignableFrom(asClass());
    }

    /**
     * @return true if this TypeInfo represents {@link Byte} class or {@code byte} class
     */
    default boolean isByte() {
        return false;
    }

    /**
     * @return true if this TypeInfo represents {@link Short} class or {@code short} class
     */
    default boolean isShort() {
        return false;
    }

    /**
     * @return true if this TypeInfo represents {@link Integer} class or {@code int} class
     */
    default boolean isInt() {
        return false;
    }

    /**
     * @return true if this TypeInfo represents {@link Long} class or {@code long} class
     */
    default boolean isLong() {
        return false;
    }

    /**
     * @return true if this TypeInfo represents {@link Float} class or {@code float} class
     */
    default boolean isFloat() {
        return false;
    }

    /**
     * @return true if this TypeInfo represents {@link Double} class or {@code double} class
     */
    default boolean isDouble() {
        return false;
    }

    /**
     * @return true if this TypeInfo represents {@link Character} class or {@code char} class
     */
    default boolean isCharacter() {
        return false;
    }

    /**
     * @return true if this TypeInfo represents {@link String} class
     */
    default boolean isString() {
        return false;
    }

    /**
     * @return true if this TypeInfo represents {@link Object} class
     */
    default boolean isObjectExact() {
        return false;
    }

    default void collectComponentClass(Consumer<Class<?>> collector) {
        collector.accept(asClass());
    }

    /**
     * @see Class#isArray()
     */
    default boolean isArray() {
        return false;
    }

    /**
     * @return {@code true} if the caller TypeInfo represents the super class of the class
     *     represented by {@code another} TypeInfo
     * @see Class#isAssignableFrom(Class)
     */
    default boolean isAssignableFrom(TypeInfo another) {
        return asClass().isAssignableFrom(another.asClass());
    }

    /**
     * @return true if {@code o} is an instance of this class represented by the caller TypeInfo
     * @see Class#isInstance(Object)
     */
    default boolean isInstance(Object o) {
        return asClass().isInstance(o);
    }

    /**
     * @see FunctionObject#getTypeTag(Class)
     */
    default int getTypeTag() {
        if (this == TypeInfo.STRING) {
            return FunctionObject.JAVA_STRING_TYPE;
        } else if (isInt()) {
            return FunctionObject.JAVA_INT_TYPE;
        } else if (isBoolean()) {
            return FunctionObject.JAVA_BOOLEAN_TYPE;
        } else if (isDouble()) {
            return FunctionObject.JAVA_DOUBLE_TYPE;
        } else if (Scriptable.class.isAssignableFrom(asClass())) {
            return FunctionObject.JAVA_SCRIPTABLE_TYPE;
        } else if (isObjectExact()) {
            return FunctionObject.JAVA_OBJECT_TYPE;
        }

        return FunctionObject.JAVA_UNSUPPORTED_TYPE;
    }

    default TypeInfo consolidate(Map<VariableTypeInfo, TypeInfo> mapping) {
        return this;
    }
}
