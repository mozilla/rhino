package org.mozilla.kotlin;

import static kotlin.metadata.Attributes.isNullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import kotlin.Metadata;
import kotlin.metadata.KmClass;
import kotlin.metadata.KmConstructor;
import kotlin.metadata.KmFunction;
import kotlin.metadata.KmValueParameter;
import kotlin.metadata.jvm.KotlinClassMetadata;
import org.mozilla.javascript.NullabilityDetector;

public class KotlinNullabilityDetector implements NullabilityDetector {
    @Override
    public boolean[] getParameterNullability(Method method) {
        int paramCount = method.getParameterTypes().length;
        KmClass kmClass = getKmClassForJavaClass(method.getDeclaringClass());
        return getMethodParameterNullabilityFromKotlinMetadata(
                kmClass, method.getName(), paramCount);
    }

    @Override
    public boolean[] getParameterNullability(Constructor<?> constructor) {
        int paramCount = constructor.getParameterTypes().length;
        KmClass kmClass = getKmClassForJavaClass(constructor.getDeclaringClass());
        return getConstructorParameterNullabilityFromKotlinMetadata(kmClass, paramCount);
    }

    private KmClass getKmClassForJavaClass(Class<?> javaClass) {
        Metadata metadata = javaClass.getAnnotation(Metadata.class);
        if (metadata != null) {
            KotlinClassMetadata.Class kMetadata =
                    (KotlinClassMetadata.Class) KotlinClassMetadata.readLenient(metadata);
            return kMetadata.getKmClass();
        } else {
            return null;
        }
    }

    private boolean[] getMethodParameterNullabilityFromKotlinMetadata(
            KmClass clazz, String methodName, int paramCount) {
        boolean[] fallback = createFallbackNullabilityArray(paramCount);
        if (clazz == null) {
            return fallback;
        }
        List<KmFunction> candidates =
                clazz.getFunctions().stream()
                        .filter(
                                f ->
                                        f.getName().equals(methodName)
                                                && f.getValueParameters().size() == paramCount)
                        .collect(Collectors.toList());
        return candidates.size() == 1
                ? createNullabilityArray(candidates.get(0).getValueParameters())
                : fallback;
    }

    private boolean[] getConstructorParameterNullabilityFromKotlinMetadata(
            KmClass clazz, int paramCount) {
        boolean[] fallback = createFallbackNullabilityArray(paramCount);
        if (clazz == null) {
            return fallback;
        }
        List<KmConstructor> candidates =
                clazz.getConstructors().stream()
                        .filter(c -> c.getValueParameters().size() == paramCount)
                        .collect(Collectors.toList());
        return candidates.size() == 1
                ? createNullabilityArray(candidates.get(0).getValueParameters())
                : fallback;
    }

    private boolean[] createNullabilityArray(List<KmValueParameter> params) {
        boolean[] result = new boolean[params.size()];
        int index = 0;
        for (KmValueParameter parameter : params) {
            result[index++] = isNullable(parameter.getType());
        }
        return result;
    }

    private boolean[] createFallbackNullabilityArray(int paramCount) {
        return new boolean[paramCount];
    }
}
