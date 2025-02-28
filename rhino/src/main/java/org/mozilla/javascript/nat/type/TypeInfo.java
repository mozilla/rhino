package org.mozilla.javascript.nat.type;

import org.mozilla.javascript.nat.ByteAsBool;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface TypeInfo {
	TypeInfo NONE = new NoTypeInfo();

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

	default boolean is(TypeInfo info) {
		return this == info;
	}

	default boolean isPrimitive() {
		return false;
	}

	default boolean shouldConvert() {
		return true;
	}

	static TypeInfo of(Class<?> c) {
		if (c == null || c == Object.class) {
			return OBJECT;
		} else if (c == Void.TYPE) {
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
		} else if (c == Void.class) {
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
            return of(paramType.getRawType()).withParams(ofArray(paramType.getActualTypeArguments()));
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

			return NONE;
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

	String toString();

	default TypeInfo componentType() {
		return NONE;
	}

	default Object newArray(int length) {
		return Array.newInstance(asClass(), length);
	}

	default TypeInfo asArray() {
		return new ArrayTypeInfo(this);
	}

	default TypeInfo withParams(TypeInfo... params) {
		if (params.length == 0) {
			return this;
		}

		return new ParameterizedTypeInfo(this, params);
	}

	default boolean isFunctionalInterface() {
		return false;
	}

	default List<Object> enumConstants() {
		return List.of();
	}

	default void append(TypeStringContext ctx, StringBuilder sb) {
		sb.append(this);
	}

	default Object createDefaultValue() {
		return null;
	}

	default boolean isVoid() {
		return false;
	}

	default boolean isBoolean() {
		return false;
	}

	default boolean isNumber() {
		return false;
	}

	default boolean isByte() {
		return false;
	}

	default boolean isShort() {
		return false;
	}

	default boolean isInt() {
		return false;
	}

	default boolean isLong() {
		return false;
	}

	default boolean isFloat() {
		return false;
	}

	default boolean isDouble() {
		return false;
	}

	default boolean isCharacter() {
		return false;
	}

	default void collectContainedComponentClasses(Collection<Class<?>> classes) {
		classes.add(asClass());
	}

	default Set<Class<?>> getContainedComponentClasses() {
		var set = new LinkedHashSet<Class<?>>();
		collectContainedComponentClasses(set);
		return set;
	}

	default boolean isInterface() {
		return false;
	}

	default boolean isArray() {
		return false;
	}

	default TypeInfo consolidate(Map<VariableTypeInfo, TypeInfo> mapping) {
		return this;
	}
}
