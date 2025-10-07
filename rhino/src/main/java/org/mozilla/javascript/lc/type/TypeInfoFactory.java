package org.mozilla.javascript.lc.type;

import java.io.Serializable;
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
import java.util.HashMap;
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
     * Get consolidation mapping from the input class.
     *
     * <p>Example (for factory implemented by Rhino):
     *
     * <pre>
     * class {@code A<Ta>} {}
     * class {@code B<Tb>} extends {@code A<Tb>} {}
     *
     * interface {@code C<Tc>} {}
     * interface {@code D<Td>} extends {@code C<Td>} {}
     *
     * class {@code E<Te>} extends {@code B<Te>} implements {@code D<String>} {}
     * </pre>
     *
     * and input class is {@code E.class}. The result mapping will then be: {@code Ta -> Te}, {@code
     * Tb -> Te}, {@code Tc -> String}, {@code Td -> String}
     *
     * @see TypeInfo#consolidate(Map)
     */
    default Map<VariableTypeInfo, TypeInfo> getConsolidationMapping(Class<?> from) {
        return Map.of();
    }

    /// helpers

    /**
     * consolidate a type using the mapping extracted from {@code consolidateHint}
     *
     * <p>Example: {@code type} is {@code E in List<E>}, {@code consolidateHint} is {@code
     * ArrayList<SomeRandomType>}, result (for factories that support Java Generic) should then be
     * {@code SomeRandomType}
     *
     * @see TypeInfo#consolidate(Map)
     * @see #getConsolidationMapping(Class)
     * @see ParameterizedTypeInfo#extractConsolidationMapping(TypeInfoFactory)
     */
    default TypeInfo consolidateType(TypeInfo type, TypeInfo consolidateHint) {
        type = type.consolidate(getConsolidationMapping(consolidateHint.asClass()));
        if (consolidateHint instanceof ParameterizedTypeInfo) {
            type =
                    type.consolidate(
                            ((ParameterizedTypeInfo) consolidateHint)
                                    .extractConsolidationMapping(this));
        }
        return type;
    }

    /**
     * Transform provided {@code types} by applying {@link TypeInfo#consolidate(Map)} on its
     * elements
     *
     * <p>Example: types is {@code [int, E]}, mapping is {@code E -> String}, return value will then
     * be {@code [int, String]}
     */
    static List<TypeInfo> consolidateAll(
            List<TypeInfo> types, Map<VariableTypeInfo, TypeInfo> mapping) {
        if (mapping.isEmpty()) { // implicit null check
            return types;
        }

        var size = types.size();

        if (size == 0) {
            return List.of();
        }

        if (size == 1) {
            var type = types.get(0);
            var consolidated = type.consolidate(mapping);
            return type == consolidated ? types : List.of(consolidated);
        }

        var consolidatedTypes = new ArrayList<TypeInfo>(types.size());
        for (var type : types) {
            consolidatedTypes.add(type.consolidate(mapping));
        }
        return consolidatedTypes;
    }

    /**
     * Transform a mapping by applying {@link TypeInfo#consolidate(Map)} on its values.
     *
     * <p>Example: mapping is {@code K -> V}, transformer is {@code V -> String}, return value will
     * then be {@code K -> String}
     */
    static Map<VariableTypeInfo, TypeInfo> transformMapping(
            Map<VariableTypeInfo, TypeInfo> mapping, Map<VariableTypeInfo, TypeInfo> transformer) {
        if (mapping.isEmpty()) {
            return Map.of();
        } else if (mapping.size() == 1) {
            var entry = mapping.entrySet().iterator().next();
            return Map.of(entry.getKey(), entry.getValue().consolidate(transformer));
        }
        var transformed = new HashMap<>(mapping);
        for (var entry : transformed.entrySet()) {
            entry.setValue(entry.getValue().consolidate(transformer));
        }
        return transformed;
    }

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
     * <p>NOTE: If you're about associate a custom TypeInfoFactory to a scope, call this method
     * before {@code initStandardObjects(...)} or {@code initSafeStandardObjects(...)}
     *
     * @param topScope scope to associate this TypeInfoFactory object with.
     * @return {@code this} if no previous TypeInfoFactory object was associated with the scope and
     *     this TypeInfoFactory is successfully associated, or the old associated factory otherwise.
     * @throws IllegalArgumentException if provided scope is not top scope
     * @see #get(Scriptable scope)
     */
    default TypeInfoFactory associate(ScriptableObject topScope) {
        if (topScope.getParentScope() != null) {
            throw new IllegalArgumentException("provided scope not top scope");
        }
        return (TypeInfoFactory) topScope.associateValue("TypeInfoFactory", this);
    }

    /**
     * Search for TypeInfoFactory in the given scope.
     *
     * @param scope scope to search for TypeInfoFactory object.
     * @return previously associated TypeInfoFactory object.
     * @throws IllegalArgumentException if the top scope of provided scope have no associated
     *     TypeInfoFactory.
     * @see #associate(ScriptableObject topScope)
     */
    static TypeInfoFactory get(Scriptable scope) {
        var got = getOrElse(scope, null);
        if (got == null) {
            throw new IllegalArgumentException("top scope have no associated TypeInfoFactory");
        }
        return got;
    }

    /**
     * Search for TypeInfoFactory in the given scope. When none was found, {@code fallback} is
     * returned instead
     *
     * @param scope scope to search for TypeInfoFactory object.
     * @return previously associated TypeInfoFactory object, or {@code fallback} if none was found
     * @see #get(Scriptable)
     * @see #associate(ScriptableObject topScope)
     */
    static TypeInfoFactory getOrElse(Scriptable scope, TypeInfoFactory fallback) {
        var got = (TypeInfoFactory) ScriptableObject.getTopScopeValue(scope, "TypeInfoFactory");
        if (got == null) {
            return fallback;
        }
        return got;
    }
}
