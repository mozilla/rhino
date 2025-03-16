package org.mozilla.javascript.nat.type;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
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
import org.mozilla.javascript.nat.ByteAsBool;

public interface TypeInfo {
    TypeInfo NONE = NoTypeInfo.INSTANCE;

    TypeInfo[] EMPTY_ARRAY = new TypeInfo[0];

    TypeInfo OBJECT = new BasicClassTypeInfo(Object.class);
    TypeInfo OBJECT_ARRAY = OBJECT.asArray();

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
    TypeInfo STRING_ARRAY = STRING.asArray();
    TypeInfo CLASS = new BasicClassTypeInfo(Class.class);
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

    Class<?> asClass();

    default TypeInfo param(int index) {
        return NONE;
    }

    default boolean is(Class<?> c) {
        return asClass() == c;
    }

    default boolean isPrimitive() {
        return false;
    }

    default boolean shouldConvert() {
        return true;
    }

    static TypeInfo of(Class<?> c) {
        if (c == null) {
            return NONE;
        } else if (c == Object.class) {
            return OBJECT;
        }
        if (c.isPrimitive()) {
            if (c == Void.TYPE) {
                return PRIMITIVE_VOID;
            } else if (c == Boolean.TYPE) {
                return PRIMITIVE_BOOLEAN;
            } else if (c == Byte.TYPE) {
                return PRIMITIVE_BYTE;
            } else if (c == Short.TYPE) {
                return PRIMITIVE_SHORT;
            } else if (c == Integer.TYPE) {
                return PRIMITIVE_INT;
            } else if (c == Long.TYPE) {
                return PRIMITIVE_LONG;
            } else if (c == Float.TYPE) {
                return PRIMITIVE_FLOAT;
            } else if (c == Double.TYPE) {
                return PRIMITIVE_DOUBLE;
            } else if (c == Character.TYPE) {
                return PRIMITIVE_CHARACTER;
            }
        }
        if (c == Void.class) {
            return VOID;
        } else if (c == Boolean.class) {
            return BOOLEAN;
        } else if (c == Byte.class) {
            return BYTE;
        } else if (c == Short.class) {
            return SHORT;
        } else if (c == Integer.class) {
            return INT;
        } else if (c == Long.class) {
            return LONG;
        } else if (c == Float.class) {
            return FLOAT;
        } else if (c == Double.class) {
            return DOUBLE;
        } else if (c == Character.class) {
            return CHARACTER;
        } else if (c == Number.class) {
            return NUMBER;
        } else if (c == String.class) {
            return STRING;
        } else if (c == Class.class) {
            return CLASS;
        } else if (c == Date.class) {
            return DATE;
        } else if (c == Optional.class) {
            return RAW_OPTIONAL;
        } else if (c == EnumSet.class) {
            return RAW_ENUM_SET;
        } else if (c == Runnable.class) {
            return RUNNABLE;
        } else if (c == Consumer.class) {
            return RAW_CONSUMER;
        } else if (c == Supplier.class) {
            return RAW_SUPPLIER;
        } else if (c == Function.class) {
            return RAW_FUNCTION;
        } else if (c == Predicate.class) {
            return RAW_PREDICATE;
        } else if (c == List.class) {
            return RAW_LIST;
        } else if (c == Set.class) {
            return RAW_SET;
        } else if (c == Map.class) {
            return RAW_MAP;
        } else if (c == Object[].class) {
            return OBJECT_ARRAY;
        } else if (c == String[].class) {
            return STRING_ARRAY;
        } else if (c.isArray()) {
            return of(c.getComponentType()).asArray();
        } else if (c.isEnum()) {
            synchronized (EnumTypeInfo.CACHE) {
                return EnumTypeInfo.CACHE.computeIfAbsent(c, EnumTypeInfo::new);
            }
        } else if (c.isInterface()) {
            synchronized (InterfaceTypeInfo.CACHE) {
                return InterfaceTypeInfo.CACHE.computeIfAbsent(c, InterfaceTypeInfo::new);
            }
        }
        synchronized (BasicClassTypeInfo.CACHE) {
            return BasicClassTypeInfo.CACHE.computeIfAbsent(c, BasicClassTypeInfo::new);
        }
    }

    static VariableTypeInfo of(TypeVariable<?> variable) {
        synchronized (VariableTypeInfo.CACHE) {
            return VariableTypeInfo.CACHE.computeIfAbsent(variable, VariableTypeInfo::new);
        }
    }

    static TypeInfo of(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clz = (Class<?>) type;
            return of(clz);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            return of(paramType.getRawType())
                    .withParams(ofArray(paramType.getActualTypeArguments()));
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrType = (GenericArrayType) type;
            return of(arrType.getGenericComponentType()).asArray();
        } else if (type instanceof TypeVariable<?>) {
            TypeVariable<?> variable = (TypeVariable<?>) type;
            return of(variable);
        } else if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            var upper = wildcard.getUpperBounds();
            if (upper.length != 0 && upper[0] != Object.class) {
                return of(upper[0]);
            }

            var lower = wildcard.getLowerBounds();
            if (lower.length != 0) {
                return of(lower[0]);
            }
        }
        return NONE;
    }

    static TypeInfo[] ofArray(Type[] array) {
        if (array.length == 0) {
            return EMPTY_ARRAY;
        }
        var len = array.length;
        var arr = new TypeInfo[len];
        for (int i = 0; i < len; i++) {
            arr[i] = of(array[i]);
        }
        return arr;
    }

    default String signature() {
        return toString();
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
     * get an array TypeInfo whose component type is the caller TypeInfo
     *
     * @see #getComponentType()
     */
    default TypeInfo asArray() {
        return new ArrayTypeInfo(this);
    }

    /**
     * get a parameterized TypeInfo whose raw type is the caller TypeInfo, with parameters provided
     * by the `params` arg
     *
     * @see #param(int)
     */
    default TypeInfo withParams(TypeInfo... params) {
        if (params.length == 0) {
            return this;
        }

        return new ParameterizedTypeInfo(this, params);
    }

    /**
     * @see FunctionalInterface
     */
    default boolean isFunctionalInterface() {
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
        return false;
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
     * @return true if this TypeInfo represents {@link Object} class
     */
    default boolean isObjectExact() {
        return false;
    }

    default void collectComponentClass(Consumer<Class<?>> collector) {
        collector.accept(asClass());
    }

    /**
     * @see Class#isInterface()
     */
    default boolean isInterface() {
        return false;
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

        // Note that the long type is not supported; see the javadoc for
        // the constructor for this class

        return FunctionObject.JAVA_UNSUPPORTED_TYPE;
    }

    default TypeInfo consolidate(Map<VariableTypeInfo, TypeInfo> mapping) {
        return this;
    }
}
