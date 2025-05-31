package org.mozilla.javascript.nat;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.VariableTypeInfo;

/**
 * @see TypeInfo#consolidate(Map)
 * @author ZZZank
 */
public final class TypeConsolidator {
    private static final IdentityHashMap<Class<?>, Map<VariableTypeInfo, TypeInfo>> MAPPINGS =
            new IdentityHashMap<>();

    private static final boolean DEBUG = false;

    private TypeConsolidator() {}

    /** create a mapping for usage in {@link TypeInfo#consolidate(Map)} */
    public static Map<VariableTypeInfo, TypeInfo> getMapping(Class<?> type) {
        if (DEBUG) {
            System.out.println("getting mapping from: " + type);
        }
        var got = getMappingImpl(type);
        return got == null ? Collections.emptyMap() : got;
    }

    public static TypeInfo consolidateOrNone(
            VariableTypeInfo variable, Map<VariableTypeInfo, TypeInfo> mapping) {
        return mapping.getOrDefault(variable, TypeInfo.NONE);
    }

    public static List<TypeInfo> consolidateOrNull(
            List<TypeInfo> original, Map<VariableTypeInfo, TypeInfo> mapping) {
        var len = original.size();
        if (DEBUG) {
            System.out.println("consolidating" + original);
        }

        if (len == 0) {
            return original;
        } else if (len == 1) {
            var consolidated = original.get(0).consolidate(mapping);
            return consolidated != original.get(0) ? List.of(consolidated) : original;
        }

        var different = false;
        var consolidated = new ArrayList<TypeInfo>();
        for (var typeInfo : original) {
            var cons = typeInfo.consolidate(mapping);
            different |= cons != typeInfo;
            consolidated.add(cons);
        }

        return different ? consolidated : null;
    }

    public static TypeInfo[] consolidateAll(
            TypeInfo[] original, Map<VariableTypeInfo, TypeInfo> mapping) {
        var len = original.length;
        if (DEBUG) {
            System.out.println("consolidating" + Arrays.toString(original));
        }
        if (len == 0) {
            return original;
        } else if (len == 1) {
            var consolidated = original[0].consolidate(mapping);
            return consolidated != original[0] ? new TypeInfo[] {consolidated} : original;
        }
        TypeInfo[] consolidatedAll = null;
        for (int i = 0; i < len; i++) {
            var type = original[i];
            var consolidated = type.consolidate(mapping);
            if (consolidated != type) {
                if (consolidatedAll == null) {
                    consolidatedAll = new TypeInfo[len];
                    System.arraycopy(original, 0, consolidatedAll, 0, i);
                }
                consolidatedAll[i] = consolidated;
            } else if (consolidatedAll != null) {
                consolidatedAll[i] = consolidated;
            }
        }
        return consolidatedAll == null ? original : consolidatedAll;
    }

    private static Map<VariableTypeInfo, TypeInfo> getMappingImpl(Class<?> type) {
        if (type == null || type.isPrimitive() || type == Object.class) {
            return null;
        }
        synchronized (MAPPINGS) {
            return MAPPINGS.computeIfAbsent(type, TypeConsolidator::collect);
        }
    }

    private static Map<VariableTypeInfo, TypeInfo> collect(Class<?> type) {
        var mapping = new IdentityHashMap<VariableTypeInfo, TypeInfo>();

        /*
         * (classes are named as 'XXX': A, B, C, ...)
         * (type variables are named as 'Tx': Ta, Tb, Tc, ...)
         *
         * let's consider a rather extreme case:
         *
         * class A<Ta> {}
         * interface B<Tb> {}
         * class C<Tc> extends A<Tc> {}
         * class D<Td> extends C<Td> implements B<A<Td>> {}
         *
         * assuming that input 'type' is D.class
         */

        // in our D.class example, this will collect mapping from C<Td>, forming Tc -> Td
        extractSuperMapping(type.getGenericSuperclass(), mapping);

        // in our D.class example, this will collect mapping from B<A<Td>>, forming Tb -> A<Td>
        for (var genericInterface : type.getGenericInterfaces()) {
            extractSuperMapping(genericInterface, mapping);
        }

        // mapping from super
        // in our D.class example, super mapping will only include Ta -> Tc
        var superMapping = getMappingImpl(type.getSuperclass());

        if (superMapping == null || superMapping.isEmpty()) {
            return postMapping(mapping);
        }

        // transform super mapping to make it able to directly map a type to types used by D.class
        var merged = new IdentityHashMap<>(superMapping);
        for (var entry : merged.entrySet()) {
            // in our D.class example, super mapping Ta -> Tc will be transformed to Ta -> Td
            entry.setValue(entry.getValue().consolidate(mapping));
        }
        // merge two mapping
        merged.putAll(mapping);

        // in our D.class example, our mapping will include Ta -> Td, Tb -> A<Td>, Tc -> Td.
        // This means that all related type (Ta, Tb, Tc) can be directly mapped to
        // the type used by D.class (Td), so we only need to apply the mapping ONCE, which will be
        // important for performance
        return postMapping(merged);
    }

    private static void extractSuperMapping(
            Type superType, IdentityHashMap<VariableTypeInfo, TypeInfo> pushTo) {
        if (superType instanceof ParameterizedType) {
            final var parameterized = (ParameterizedType) superType;
            if (parameterized.getRawType() instanceof Class<?>) {
                final var parent = (Class<?>) parameterized.getRawType();
                final var params = parent.getTypeParameters(); // T
                final var args = parameterized.getActualTypeArguments(); // T is mapped to
                for (int i = 0; i < args.length; i++) {
                    pushTo.put(TypeInfo.of(params[i]), TypeInfo.of(args[i]));
                }
            }
        }
    }

    private static Map<VariableTypeInfo, TypeInfo> postMapping(
            Map<VariableTypeInfo, TypeInfo> mapping) {
        switch (mapping.size()) {
            case 0:
                if (DEBUG) {
                    System.out.println("collected empty mapping");
                }
                return Collections.emptyMap();
            case 1:
                var entry = mapping.entrySet().iterator().next();
                if (DEBUG) {
                    System.out.println(
                            "collected singleton mapping: "
                                    + entry.getKey()
                                    + " -> "
                                    + entry.getValue());
                }
                return Collections.singletonMap(entry.getKey(), entry.getValue());
            default:
                if (DEBUG) {
                    System.out.println("collected mapping with size: " + mapping.size());
                }
                return Collections.unmodifiableMap(mapping);
        }
    }
}
