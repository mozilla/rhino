package org.mozilla.classfile;

import java.lang.constant.ConstantDesc;
import java.util.HashMap;
import java.util.Map;

public class DynamicDescriberRegistry {

    private final Map<Class<?>, DynamicConstantDescriber<?>> registry;

    DynamicDescriberRegistry() {
        registry = new HashMap<>();
    }

    void register(DynamicConstantDescriber<?> describer) {
        registry.put(describer.describedType(), describer);
    }

    public <T> ConstantDesc describe(T obj) {
        if (obj instanceof ConstantDesc cd) {
            return cd;
        }
        @SuppressWarnings("unchecked")
        var type = (Class<T>) obj.getClass();
        @SuppressWarnings("unchecked")
        var describer = (DynamicConstantDescriber<T>) registry.get(type);
        if (describer == null) {
            throw new IllegalArgumentException(String.format("No describer found for %s.", type));
        }
        return describer.describe(obj);
    }
}
