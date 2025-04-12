package org.mozilla.javascript.nat.type;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.nat.type.impl.factory.NoCacheFactory;
import org.mozilla.javascript.nat.type.impl.factory.WeakReferenceFactory;

/**
 * @author ZZZank
 */
public interface TypeInfoFactory {

    /**
     * non-global factory is attached to {@link ClassCache}, which is attached to scope (see {@link
     * ClassCache#get(Scriptable)}), so cached {@link TypeInfo} will be cleared when the scope
     * itself is reclaimed, thus not requiring weak-reference
     */
    TypeInfoFactory GLOBAL = new WeakReferenceFactory();

    TypeInfoFactory NO_CACHE = new NoCacheFactory();

    TypeInfo[] EMPTY_ARRAY = new TypeInfo[0];

    /// creating types

    default TypeInfo none() {
        return TypeInfo.NONE;
    }

    TypeInfo create(Class<?> clazz);

    TypeInfo create(GenericArrayType genericArrayType);

    TypeInfo create(TypeVariable<?> typeVariable);

    TypeInfo create(ParameterizedType parameterizedType);

    TypeInfo create(WildcardType wildcardType);

    /// mutating types

    TypeInfo toArray(TypeInfo component);

    TypeInfo attachParam(TypeInfo base, List<TypeInfo> params);

    default TypeInfo attachParam(TypeInfo base, TypeInfo... params) {
        return attachParam(base, Arrays.asList(params));
    }

    /**
     * @see TypeInfo#consolidate(Map)
     */
    default Map<VariableTypeInfo, TypeInfo> getConsolidationMapping(Class<?> from) {
        return Map.of();
    }

    /// helpers

    /**
     * create a {@link TypeInfo} from {@link Type}.
     *
     * <p>Implementations are recommended, but not required, to maintain the uniqueness of simple
     * types like {@link Class}, to reduce allocation
     *
     * <p>There's no guarantee that created {@link TypeInfo} will keep all their original
     * information, for example, {@link WildcardType} might be mapped to one of its bounds, instead
     * of a {@link WildcardTypeInfo}
     *
     * <p>Implementations should return {@link #none()} if it's unable to parse the {@link Type} it
     * received
     */
    default TypeInfo create(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clz = (Class<?>) type;
            return create(clz);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            return create(paramType);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrType = (GenericArrayType) type;
            return create(arrType);
        } else if (type instanceof TypeVariable<?>) {
            TypeVariable<?> variable = (TypeVariable<?>) type;
            return create(variable);
        } else if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            return create(wildcard);
        }
        return none();
    }

    default <T extends Type> TypeInfo[] createArray(T[] types) {
        if (types.length == 0) {
            return EMPTY_ARRAY;
        }
        final var len = types.length;
        final var arr = new TypeInfo[len];
        for (int i = 0; i < len; i++) {
            arr[i] = create(types[i]);
        }
        return arr;
    }

    default <T extends Type> List<TypeInfo> createList(T[] types) {
        switch (types.length) {
            case 0:
                return List.of();
            case 1:
                return List.of(create(types[0]));
            case 2:
                return List.of(create(types[0]), create(types[1]));
            default:
                final var list = new ArrayList<TypeInfo>();
                for (var type : types) {
                    list.add(create(type));
                }
                return Collections.unmodifiableList(list);
        }
    }

    default TypeInfo matchPredefined(Class<?> clazz) {
        if (clazz == null) {
            return TypeInfo.NONE;
        } else if (clazz == Object.class) {
            return TypeInfo.OBJECT;
        }
        if (clazz.isPrimitive()) {
            if (clazz == Void.TYPE) {
                return TypeInfo.PRIMITIVE_VOID;
            } else if (clazz == Boolean.TYPE) {
                return TypeInfo.PRIMITIVE_BOOLEAN;
            } else if (clazz == Byte.TYPE) {
                return TypeInfo.PRIMITIVE_BYTE;
            } else if (clazz == Short.TYPE) {
                return TypeInfo.PRIMITIVE_SHORT;
            } else if (clazz == Integer.TYPE) {
                return TypeInfo.PRIMITIVE_INT;
            } else if (clazz == Long.TYPE) {
                return TypeInfo.PRIMITIVE_LONG;
            } else if (clazz == Float.TYPE) {
                return TypeInfo.PRIMITIVE_FLOAT;
            } else if (clazz == Double.TYPE) {
                return TypeInfo.PRIMITIVE_DOUBLE;
            } else if (clazz == Character.TYPE) {
                return TypeInfo.PRIMITIVE_CHARACTER;
            }
        }
        if (clazz == Void.class) {
            return TypeInfo.VOID;
        } else if (clazz == Boolean.class) {
            return TypeInfo.BOOLEAN;
        } else if (clazz == Byte.class) {
            return TypeInfo.BYTE;
        } else if (clazz == Short.class) {
            return TypeInfo.SHORT;
        } else if (clazz == Integer.class) {
            return TypeInfo.INT;
        } else if (clazz == Long.class) {
            return TypeInfo.LONG;
        } else if (clazz == Float.class) {
            return TypeInfo.FLOAT;
        } else if (clazz == Double.class) {
            return TypeInfo.DOUBLE;
        } else if (clazz == Character.class) {
            return TypeInfo.CHARACTER;
        } else if (clazz == Number.class) {
            return TypeInfo.NUMBER;
        } else if (clazz == String.class) {
            return TypeInfo.STRING;
        } else if (clazz == Class.class) {
            return TypeInfo.RAW_CLASS;
        } else if (clazz == Date.class) {
            return TypeInfo.DATE;
        } else if (clazz == Optional.class) {
            return TypeInfo.RAW_OPTIONAL;
        } else if (clazz == EnumSet.class) {
            return TypeInfo.RAW_ENUM_SET;
        } else if (clazz == Runnable.class) {
            return TypeInfo.RUNNABLE;
        } else if (clazz == Consumer.class) {
            return TypeInfo.RAW_CONSUMER;
        } else if (clazz == Supplier.class) {
            return TypeInfo.RAW_SUPPLIER;
        } else if (clazz == Function.class) {
            return TypeInfo.RAW_FUNCTION;
        } else if (clazz == Predicate.class) {
            return TypeInfo.RAW_PREDICATE;
        } else if (clazz == List.class) {
            return TypeInfo.RAW_LIST;
        } else if (clazz == Set.class) {
            return TypeInfo.RAW_SET;
        } else if (clazz == Map.class) {
            return TypeInfo.RAW_MAP;
        } else if (clazz == Object[].class) {
            return TypeInfo.OBJECT_ARRAY;
        } else if (clazz == String[].class) {
            return TypeInfo.STRING_ARRAY;
        } else if (clazz == BigInteger.class) {
            return TypeInfo.BIG_INT;
        }
        return null;
    }
}
