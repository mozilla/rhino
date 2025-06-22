package org.mozilla.javascript.lc.type;

import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigInteger;
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
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.lc.type.impl.factory.ConcurrentFactory;
import org.mozilla.javascript.lc.type.impl.factory.NoCacheFactory;
import org.mozilla.javascript.lc.type.impl.factory.WeakReferenceFactory;

/**
 * Factory for {@link TypeInfo}
 *
 * <p>For getting TypeInfo representing a {@link Type}, use {@link #create(Type)} and its overloads
 *
 * <p>For mutating an existed TypeInfo, use {@link #toArray(TypeInfo)}, {@link
 * #attachParam(TypeInfo, TypeInfo...)}. These methods will not modify the original TypeInfo
 *
 * <p>This class is serializable, but cached TypeInfo (if implementation has a cache) will not be
 * de/serialized
 *
 * @author ZZZank
 */
public interface TypeInfoFactory extends Serializable {

    /**
     * TypeInfoFactory used by scope independent actions.
     *
     * <p>This factory will cache {@link TypeInfo}. TypeInfo created from simple types like {@link
     * TypeInfoFactory}.class will be kept in a cache, so that when the same type is passed to this
     * factory, no new TypeInfo is created.
     *
     * <p>This factory is weakly referencing cached types. It holds a {@link
     * java.lang.ref.WeakReference} to the type for each cached type, and will not prevent cached
     * types from being unloaded
     *
     * <p>For actions with scope available, the TypeInfoFactory can be obtained via {@link
     * #get(Scriptable)}.
     */
    TypeInfoFactory GLOBAL =
            new WeakReferenceFactory() {
                private Object readResolve() {
                    return GLOBAL;
                }
            };

    /**
     * TypeInfoFactory used by very few actions with the intention of not caching any used types
     *
     * <p>This factory does not cache {@link TypeInfo}. If the same type is passed to this factory
     * multiple times, the return result may or may not be the exact same object
     */
    TypeInfoFactory NO_CACHE = NoCacheFactory.INSTANCE;

    TypeInfo[] EMPTY_ARRAY = new TypeInfo[0];

    /// creating types

    TypeInfo create(Class<?> clazz);

    TypeInfo create(GenericArrayType genericArrayType);

    TypeInfo create(TypeVariable<?> typeVariable);

    TypeInfo create(ParameterizedType parameterizedType);

    TypeInfo create(WildcardType wildcardType);

    /// mutating types

    /**
     * @return a TypeInfo representing an array type whose component type is the provided {@code
     *     component} param
     * @see TypeInfo#isArray()
     * @see TypeInfo#getComponentType()
     */
    TypeInfo toArray(TypeInfo component);

    /**
     * In general, implementations are recommended, but not required, to return a {@link
     * ParameterizedTypeInfo}.
     *
     * <p>Default implementations in Rhino will return a TypeInfo representing a parameterized type
     * with raw type being {@code base} param, and parameters being {@code params} param. If the
     * {@code base} param itself is already a {@link ParameterizedTypeInfo}, the raw type will then
     * be {@link ParameterizedTypeInfo#rawType()} of {@code base}
     *
     * @see ParameterizedTypeInfo
     */
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
     * information. For example, {@link WildcardType} might be mapped to one of its bounds, instead
     * of a {@link WildcardTypeInfo}
     *
     * <p>Implementations should return {@link TypeInfo#NONE} if it's unable to parse the {@link
     * Type} it received
     */
    default TypeInfo create(Type type) {
        if (type instanceof Class<?>) {
            return create((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            return create((ParameterizedType) type);
        } else if (type instanceof GenericArrayType) {
            return create((GenericArrayType) type);
        } else if (type instanceof TypeVariable<?>) {
            return create((TypeVariable<?>) type);
        } else if (type instanceof WildcardType) {
            return create((WildcardType) type);
        }
        return TypeInfo.NONE;
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
                // List.of(createArray(types)) will cause one more array copying
                return Collections.unmodifiableList(Arrays.asList(createArray(types)));
        }
    }

    static TypeInfo matchPredefined(Class<?> clazz) {
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

    /**
     * Associate this TypeInfoFactory object with the given top-level scope.
     *
     * @param topScope scope to associate this TypeInfoFactory object with.
     * @return {@code true} if no previous TypeInfoFactory object were associated with the scope and
     *     this TypeInfoFactory were successfully associated, false otherwise.
     * @throws IllegalArgumentException if provided scope is not top scope
     * @see #get(Scriptable scope)
     */
    default boolean associate(ScriptableObject topScope) {
        if (topScope.getParentScope() != null) {
            throw new IllegalArgumentException("provided scope not top scope");
        }
        return this == topScope.associateValue("TypeInfoFactory", this);
    }

    /**
     * Search for TypeInfoFactory in the given scope.If none was found, it will try to associate a
     * new ClassCache object to the top scope.
     *
     * @param scope scope to search for TypeInfoFactory object.
     * @return previously associated TypeInfoFactory object, or a new instance of TypeInfoFactory if
     *     none was found
     * @throws IllegalArgumentException if the top scope of provided scope have no associated
     *     TypeInfoFactory, and cannot have TypeInfoFactory associated due to the top scope not
     *     being a {@link ScriptableObject}
     * @see #associate(ScriptableObject topScope)
     */
    static TypeInfoFactory get(Scriptable scope) {
        TypeInfoFactory got =
                (TypeInfoFactory) ScriptableObject.getTopScopeValue(scope, "TypeInfoFactory");
        if (got == null) {
            // we expect this to not happen frequently, so computing top scope twice is acceptable
            var topScope = ScriptableObject.getTopLevelScope(scope);
            if (!(topScope instanceof ScriptableObject)) {
                // Note: it's originally a RuntimeException, the super class of
                // IllegalArgumentException, so this will not break error catching
                throw new IllegalArgumentException(
                        "top scope have no associated TypeInfoFactory and cannot have TypeInfoFactory associated due to not being a ScriptableObject");
            }
            got = new ConcurrentFactory();
            got.associate(((ScriptableObject) topScope));
        }
        return got;
    }
}
