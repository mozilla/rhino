package org.mozilla.javascript;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.nat.ByteAsBool;
import org.mozilla.javascript.nat.TypeConsolidator;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.VariableTypeInfo;

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
    private byte consolidatable = ByteAsBool.UNKNOWN;

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

    public Object[] wrapArgs(final Object[] args) {
        return wrapArgs(args, this.types);
    }

    public Object[] wrapArgs(final Object[] args, final Map<VariableTypeInfo, TypeInfo> mapping) {
        if (ByteAsBool.isTrue(consolidatable)) {
            return wrapArgs(args, TypeConsolidator.consolidateOrNull( this.types, mapping));
        } else if (ByteAsBool.isFalse(consolidatable)) {
            return wrapArgs(args);
        }
        var types = TypeConsolidator.consolidateOrNull( this.types, mapping);
        consolidatable = ByteAsBool.fromBool(types != null);
        return ByteAsBool.isTrue(consolidatable)
            ? wrapArgs(args, types)
            : wrapArgs(args);
    }

    private Object[] wrapArgs(final Object[] args, final List<TypeInfo> types) {
        return varArg ? wrapVarArgs(args, types) : wrapFixedSizeArgs(args, types);
    }

    public static Object[] wrapFixedSizeArgs(final Object[] args, final List<TypeInfo> types) {
        var len = args.length;
        if (len != types.size()) {
            throw new IllegalArgumentException();
        }

        var wrapped = args; // defer new array allocation
        for (int i = 0; i < len; i++) {
            var arg = args[i];
            var wrapTo = Context.jsToJava(arg, types.get(i));
            if (arg != wrapTo) {
                if (wrapped == args) {
                    wrapped = wrapped.clone();
                }
                wrapped[i] = wrapTo;
            }
        }
        return wrapped;
    }

    public static Object[] wrapVarArgs(final Object[] args, final List<TypeInfo> types) {
        var explicitLen = types.size() - 1;
        if (args.length < explicitLen) {
            throw new IllegalArgumentException(
                    String.format(
                            "not enough args for vararg method, expected %s at least, but got %s",
                            explicitLen, args.length));
        }

        // marshall explicit parameters
        var wrapped = new Object[types.size()];
        for (int i = 0; i < explicitLen; i++) {
            wrapped[i] = Context.jsToJava(args[i], types.get(i));
        }

        // Handle special situation where a single variable parameter
        // is given, and it is a Java or ECMA array or is null.
        if (args.length == types.size()) {
            var last = args[explicitLen];
            if (last == null || last instanceof NativeArray || last instanceof NativeJavaArray) {
                // convert the ECMA array into a native array
                wrapped[explicitLen] = Context.jsToJava(last, types.get(explicitLen));
                // all args converted, return
                return wrapped;
            }
        }

        // marshall the variable parameters
        var varArgType = types.get(explicitLen).getComponentType();
        var varArgLen = args.length - explicitLen;
        var varArgs = varArgType.newArray(varArgLen);
        for (int i = 0; i < varArgLen; i++) {
            Array.set(varArgs, i, Context.jsToJava(args[explicitLen + i], varArgType));
        }
        wrapped[explicitLen] = varArgs;

        return wrapped;
    }
}
