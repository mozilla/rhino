package org.mozilla.javascript;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import org.mozilla.javascript.nat.TypeConsolidator;
import org.mozilla.javascript.nat.type.TypeInfo;

/**
 * holder for {@link java.lang.reflect.Executable} parameter information
 *
 * @see MemberBox
 * @author ZZZank
 */
class JavaParameters {
    private static final NullabilityDetector NULLABILITY_DETECTOR =
            ScriptRuntime.loadOneServiceImplementation(NullabilityDetector.class);

    private final List<TypeInfo> types;
    private final boolean[] nullabilities;
    private final boolean varArg;

    public JavaParameters(Method method, Class<?> from) {
        this.types =
                List.of(
                        TypeConsolidator.consolidateAll(
                                TypeInfo.ofArray(method.getGenericParameterTypes()),
                                TypeConsolidator.getMapping(from)));
        this.nullabilities =
                NULLABILITY_DETECTOR == null
                        ? new boolean[types.size()]
                        : NULLABILITY_DETECTOR.getParameterNullability(method);
        this.varArg = method.isVarArgs();
    }

    public JavaParameters(Constructor<?> constructor, Class<?> from) {
        this.types =
                List.of(
                        TypeConsolidator.consolidateAll(
                                TypeInfo.ofArray(constructor.getGenericParameterTypes()),
                                TypeConsolidator.getMapping(from)));
        this.nullabilities =
                NULLABILITY_DETECTOR == null
                        ? new boolean[types.size()]
                        : NULLABILITY_DETECTOR.getParameterNullability(constructor);
        this.varArg = constructor.isVarArgs();
    }

    public List<TypeInfo> getTypes() {
        return types;
    }

    public boolean[] getNullabilities() {
        return nullabilities;
    }

    public boolean isVarArg() {
        return varArg;
    }
}
