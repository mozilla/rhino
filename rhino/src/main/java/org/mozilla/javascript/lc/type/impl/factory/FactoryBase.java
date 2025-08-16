package org.mozilla.javascript.lc.type.impl.factory;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.lc.type.ParameterizedTypeInfo;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.VariableTypeInfo;
import org.mozilla.javascript.lc.type.impl.ArrayTypeInfo;
import org.mozilla.javascript.lc.type.impl.ParameterizedTypeInfoImpl;

/**
 * @author ZZZank
 */
public interface FactoryBase extends TypeInfoFactory {

    @Override
    default TypeInfo create(GenericArrayType genericArrayType) {
        return toArray(create(genericArrayType.getGenericComponentType()));
    }

    @Override
    default TypeInfo create(ParameterizedType parameterizedType) {
        return attachParam(
                create(parameterizedType.getRawType()),
                createList(parameterizedType.getActualTypeArguments()));
    }

    @Override
    default TypeInfo create(WildcardType wildcardType) {
        var upper = wildcardType.getUpperBounds();
        if (upper.length != 0 && upper[0] != Object.class) {
            return create(upper[0]);
        }

        var lower = wildcardType.getLowerBounds();
        if (lower.length != 0) {
            return create(lower[0]);
        }
        return TypeInfo.NONE;
    }

    @Override
    default TypeInfo toArray(TypeInfo component) {
        return new ArrayTypeInfo(component);
    }

    @Override
    default TypeInfo attachParam(TypeInfo base, List<TypeInfo> params) {
        if (base instanceof ParameterizedTypeInfo) {
            base = ((ParameterizedTypeInfo) base).rawType();
        }
        return new ParameterizedTypeInfoImpl(base, params);
    }

    private static Map<VariableTypeInfo, TypeInfo> transformMapping(
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

    /** Used by {@link #getConsolidationMapping(java.lang.Class)} */
    default Map<VariableTypeInfo, TypeInfo> computeConsolidationMapping(Class<?> type) {
        var mapping = new HashMap<VariableTypeInfo, TypeInfo>();

        // in our E.class example, this will collect mapping from B<Te>, forming Tb -> Te
        extractSuperMapping(type.getGenericSuperclass(), mapping);

        // in our E.class example, this will collect mapping from D<String>, forming Td -> String
        for (var genericInterface : type.getGenericInterfaces()) {
            extractSuperMapping(genericInterface, mapping);
        }

        // extract mappings for superclasses/interfaces
        // in our E.class example, super mapping will include Ta -> Tb
        var superMapping = getConsolidationMapping(type.getSuperclass());

        // in our E.class example, interface mapping will include Tc -> Td
        var interfaces = type.getInterfaces();
        var interfaceMappings = new ArrayList<Map<VariableTypeInfo, TypeInfo>>(interfaces.length);
        for (var interface_ : interfaces) {
            interfaceMappings.add(getConsolidationMapping(interface_));
        }

        if (superMapping.isEmpty() && interfaceMappings.stream().allMatch(Map::isEmpty)) {
            return Map.copyOf(mapping);
        }

        // transform super mapping to make it able to directly map a type to types used by E.class,
        // then merge them together
        // Example: Ta -> Tb (from `superMapping`) will be transformed by Tb -> Te (from `mapping`),
        // forming Ta -> Te
        var merged = new HashMap<>(transformMapping(superMapping, mapping));
        for (var interfaceMapping : interfaceMappings) {
            merged.putAll(transformMapping(interfaceMapping, mapping));
        }
        merged.putAll(mapping);

        // Result: `Ta -> Te`, `Tb -> Te`, `Tc -> String`, `Td -> String`
        // This means that all type variables from superclass / interface (Ta, Tb, Tc, Td) can be
        // eliminated by applying the mapping ONCE, which will be important for performance
        return Map.copyOf(merged);
    }

    private void extractSuperMapping(Type superType, HashMap<VariableTypeInfo, TypeInfo> pushTo) {
        if (!(superType instanceof ParameterizedType)) {
            return;
        }
        var parameterized = (ParameterizedType) superType;
        if (!(parameterized.getRawType() instanceof Class<?>)) {
            return;
        }
        var parent = (Class<?>) parameterized.getRawType();

        final var params = parent.getTypeParameters(); // T
        final var args = parameterized.getActualTypeArguments(); // T is mapped to

        if (params.length != args.length) {
            throw new IllegalArgumentException(
                    String.format(
                            "typeParameters.length != actualTypeArguments.length (%s != %s)",
                            params.length, args.length));
        }

        for (int i = 0; i < args.length; i++) {
            pushTo.put((VariableTypeInfo) create(params[i]), create(args[i]));
        }
    }
}
