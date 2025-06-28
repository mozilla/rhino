package org.mozilla.javascript.lc.type.impl;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import zzzank.probejs.lang.java.type.impl.VariableType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ZZZank
 */
public final class TypeReplacementCollector {
    private final Map<Class<?>, Map<VariableType, TypeDescriptor>> replacementByClass = new ConcurrentHashMap<>();

    public Map<VariableType, TypeDescriptor> transformMapping(
        Map<VariableType, TypeDescriptor> mapping,
        Map<VariableType, TypeDescriptor> transformer
    ) {
        if (mapping.isEmpty()) {
            return Map.of();
        } else if (mapping.size() == 1) {
            val entry = mapping.entrySet().iterator().next();
            return Map.of(entry.getKey(), entry.getValue().consolidate(transformer));
        }
        val transformed = new HashMap<>(mapping);
        for (var entry : transformed.entrySet()) {
            entry.setValue(entry.getValue().consolidate(transformer));
        }
        return transformed;
    }

    @NotNull
    public Map<VariableType, TypeDescriptor> getTypeReplacement(Class<?> type) {
        if (type == null || type == Object.class || type.isPrimitive()) {
            return Map.of();
        }
        // no computeIfAbsent because `computeTypeReplacement(...)` will recursively call this method again
        var got = replacementByClass.get(type);
        if (got == null) {
            got = computeTypeReplacement(type);
            replacementByClass.put(type, got);
        }
        return got;
    }

    private Map<VariableType, TypeDescriptor> computeTypeReplacement(Class<?> type) {
        var mapping = new HashMap<VariableType, TypeDescriptor>();

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
        var superMapping = getTypeReplacement(type.getSuperclass());

        var interfaces = type.getInterfaces();
        var interfaceMappings = new ArrayList<Map<VariableType, TypeDescriptor>>(interfaces.length);
        for (var interface_ : interfaces) {
            interfaceMappings.add(getTypeReplacement(interface_));
        }

        if (superMapping.isEmpty() && interfaceMappings.stream().allMatch(Map::isEmpty)) {
            return Map.copyOf(mapping);
        }

        // transform super mapping to make it able to directly map a type to types used by D.class
        // then merge them together
        var merged = new HashMap<>(transformMapping(superMapping, mapping));
        for (var interfaceMapping : interfaceMappings) {
            merged.putAll(transformMapping(interfaceMapping, mapping));
        }
        merged.putAll(mapping);

        // in our D.class example, our mapping will include Ta -> Td, Tb -> A<Td>, Tc -> Td.
        // This means that all related type (Ta, Tb, Tc) can be directly mapped to
        // the type used by D.class (Td), so we only need to apply the mapping ONCE, which will be
        // important for performance
        return Map.copyOf(merged);
    }

    private static void extractSuperMapping(Type superType, HashMap<VariableType, TypeDescriptor> pushTo) {
        if (!(superType instanceof ParameterizedType parameterized)
            || !(parameterized.getRawType() instanceof Class<?> parent)) {
            return;
        }

        final var params = parent.getTypeParameters(); // T
        final var args = parameterized.getActualTypeArguments(); // T is mapped to

        if (params.length != args.length) {
            throw new IllegalArgumentException(String.format(
                "typeParameters.length != actualTypeArguments.length (%s != %s)",
                params.length,
                args.length
            ));
        }

        for (int i = 0; i < args.length; i++) {
            pushTo.put(new VariableType(params[i]), TypeAdapter.getTypeDescription(args[i]));
        }
    }
}