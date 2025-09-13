package org.mozilla.javascript;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.mozilla.javascript.debug.DebuggableScript;

public final class JSDescriptor<T extends ScriptOrFn<T>> implements Serializable, DebuggableScript {
    private static final long seria_ersio_ID = 5067677351589230234L;

    private static final int IS_STRICT_FLAG = 1;
    private static final int IS_SCRIPT_FLAG = 1 << 1;
    private static final int IS_TOP_LEVEL_FLAG = 1 << 2;
    private static final int IS_ES6_GENERATOR_FLAG = 1 << 3;
    private static final int IS_SHORTHAND_FLAG = 1 << 4;
    private static final int HAS_PROTOTYPE_FLAG = 1 << 5;
    private static final int HAS_LEXICAL_THIS_FLAG = 1 << 6;
    private static final int IS_EVAL_FUNCTION_FLAG = 1 << 7;
    private static final int HAS_REST_ARG_FLAG = 1 << 8;
    private static final int HAS_DEFAULT_PARAMETERS_FLAG = 1 << 9;
    private static final int REQUIRES_ACTIVATION_FRAME_FLAG = 1 << 10;
    private static final int REQUIRES_ARGUMENT_OBJECT_FLAG = 1 << 11;
    private static final int DECLARED_AS_FUNCTION_EXPRESSION_FLAG = 1 << 12;

    private final JSCode<T> code;
    private final JSCode<T> constructor;
    private final JSDescriptor<?> parent;
    private List<JSDescriptor<JSFunction>> nestedFunctions;
    private final String[] paramAndVarNames;
    private final boolean[] paramIsConst;
    private final int flags;
    private final String sourceFile;
    private final String rawSource;
    private final int rawSourceStart;
    private final int rawSourceEnd;
    private final String name;
    private final int languageVersion;
    private final int paramAndVarCount;
    private final int paramCount;
    private final SecurityController securityController;
    private final Object securityDomain;
    private final int functionType;

    public JSDescriptor(
            JSCode<T> code,
            JSCode<T> constructor,
            JSDescriptor<?> parent,
            String[] paramAndVarNames,
            boolean[] paramIsConst,
            boolean isStrict,
            boolean isScript,
            boolean isTopLevel,
            boolean isES6Generator,
            boolean isShorthand,
            boolean hasPrototype,
            boolean hasLexicalThis,
            boolean isEvalFunction,
            boolean hasRestArg,
            String sourceFile,
            String rawSource,
            int rawSourceStart,
            int rawSourceEnd,
            String name,
            int languageVersion,
            int paramAndVarCount,
            int paramCount,
            boolean hasDefaultParameters,
            boolean requiresActivationFrame,
            boolean requiresArgumentObject,
            boolean declaredAsFunctionExpression,
            SecurityController securityController,
            Object securityDomain,
            int functionType) {
        this.code = code;
        this.constructor = constructor;
        this.parent = parent;
        this.paramAndVarNames = paramAndVarNames;
        this.paramIsConst = paramIsConst;

        int flags = 0;
        flags = flags | (isStrict ? IS_STRICT_FLAG : 0);
        flags = flags | (isScript ? IS_SCRIPT_FLAG : 0);
        flags = flags | (isTopLevel ? IS_TOP_LEVEL_FLAG : 0);
        flags = flags | (isES6Generator ? IS_ES6_GENERATOR_FLAG : 0);
        flags = flags | (isShorthand ? IS_SHORTHAND_FLAG : 0);
        flags = flags | (hasPrototype ? HAS_PROTOTYPE_FLAG : 0);
        flags = flags | (hasLexicalThis ? HAS_LEXICAL_THIS_FLAG : 0);
        flags = flags | (isEvalFunction ? IS_EVAL_FUNCTION_FLAG : 0);
        flags = flags | (hasRestArg ? HAS_REST_ARG_FLAG : 0);
        flags = flags | (hasDefaultParameters ? HAS_DEFAULT_PARAMETERS_FLAG : 0);
        flags = flags | (requiresActivationFrame ? REQUIRES_ACTIVATION_FRAME_FLAG : 0);
        flags = flags | (requiresArgumentObject ? REQUIRES_ARGUMENT_OBJECT_FLAG : 0);
        flags = flags | (declaredAsFunctionExpression ? DECLARED_AS_FUNCTION_EXPRESSION_FLAG : 0);
        this.flags = flags;

        this.sourceFile = sourceFile;
        this.rawSource = rawSource;
        this.rawSourceStart = rawSourceStart;
        this.rawSourceEnd = rawSourceEnd;
        this.name = name == null ? "" : name;
        this.languageVersion = languageVersion;
        this.paramAndVarCount = paramAndVarCount;
        this.paramCount = paramCount;
        this.securityController = securityController;
        this.securityDomain = securityDomain;
        this.functionType = functionType;
    }

    public JSCode<T> getCode() {
        return code;
    }

    public JSCode<T> getConstructor() {
        return constructor;
    }

    public List<JSDescriptor<JSFunction>> getNestedFunctions() {
        return nestedFunctions;
    }

    public boolean isStrict() {
        return (flags & IS_STRICT_FLAG) != 0;
    }

    public boolean isScript() {
        return (flags & IS_SCRIPT_FLAG) != 0;
    }

    @Override
    public boolean isTopLevel() {
        return (flags & IS_TOP_LEVEL_FLAG) != 0;
    }

    @Override
    public boolean isFunction() {
        return functionType != 0;
    }

    public boolean isES6Generator() {
        return (flags & IS_ES6_GENERATOR_FLAG) != 0;
    }

    public boolean isShorthand() {
        return (flags & IS_SHORTHAND_FLAG) != 0;
    }

    public boolean hasPrototype() {
        return (flags & HAS_PROTOTYPE_FLAG) != 0;
    }

    public boolean hasLexicalThis() {
        return (flags & HAS_LEXICAL_THIS_FLAG) != 0;
    }

    public boolean isEvalFunction() {
        return (flags & IS_EVAL_FUNCTION_FLAG) != 0;
    }

    public boolean hasRestArg() {
        return (flags & HAS_REST_ARG_FLAG) != 0;
    }

    @Override
    public String getSourceName() {
        return sourceFile;
    }

    public String getRawSource() {
        return rawSource.substring(rawSourceStart, rawSourceEnd);
    }

    public String getName() {
        return name;
    }

    public int getLanguageVersion() {
        return languageVersion;
    }

    @Override
    public int getParamAndVarCount() {
        return paramAndVarCount;
    }

    @Override
    public int getParamCount() {
        return paramCount;
    }

    public boolean getParamOrVarConst(int index) {
        return paramIsConst[index];
    }

    @Override
    public String getParamOrVarName(int index) {
        return paramAndVarNames[index];
    }

    public boolean hasDefaultParameters() {
        return (flags & HAS_DEFAULT_PARAMETERS_FLAG) != 0;
    }

    public boolean hasFunctionNamed(String name) {
        for (int f = 0; f < getFunctionCount(); f++) {
            var functionData = getFunction(f);
            if (!functionData.declaredAsFunctionExpression()
                    && name.equals(functionData.getFunctionName())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getFunctionCount() {
        return (nestedFunctions == null) ? 0 : nestedFunctions.size();
    }

    @Override
    public JSDescriptor<JSFunction> getFunction(int index) {
        return nestedFunctions.get(index);
    }

    @Override
    public String getFunctionName() {
        return name;
    }

    @Override
    public boolean isGeneratedScript() {
        return ScriptRuntime.isGeneratedScript(sourceFile);
    }

    @Override
    public int[] getLineNumbers() {
        return null;
    }

    public boolean requiresActivationFrame() {
        return (flags & REQUIRES_ACTIVATION_FRAME_FLAG) != 0;
    }

    public boolean requiresArgumentObject() {
        return (flags & REQUIRES_ARGUMENT_OBJECT_FLAG) != 0;
    }

    public boolean declaredAsFunctionExpression() {
        return (flags & DECLARED_AS_FUNCTION_EXPRESSION_FLAG) != 0;
    }

    public SecurityController getSecurityController() {
        return securityController;
    }

    public Object getSecurityDomain() {
        return securityDomain;
    }

    public int getFunctionType() {
        return functionType;
    }

    @Override
    public DebuggableScript getParent() {
        return parent;
    }

    public static class Builder<T extends ScriptOrFn<T>> {
        public JSCode.Builder<T> code;
        public JSCode.Builder<T> constructor;
        public Builder<?> parent;
        public final ArrayList<Builder<JSFunction>> nestedFunctions = new ArrayList<>();
        public String[] paramAndVarNames;
        public boolean[] paramIsConst;
        public boolean isStrict;
        public boolean isScript;
        public boolean isTopLevel;
        public boolean isES6Generator;
        public boolean isShorthand;
        public boolean hasPrototype;
        public boolean hasLexicalThis;
        public boolean isEvalFunction;
        public boolean hasRestArg;
        public String sourceFile;
        public String rawSource;
        public int rawSourceStart;
        public int rawSourceEnd;
        public String name;
        public int languageVersion;
        public int paramAndVarCount;
        public int paramCount;
        public boolean hasDefaultParameters;
        public boolean requiresActivationFrame;
        public boolean requiresArgumentObject;
        public boolean declaredAsFunctionExpression;
        public SecurityController securityController;
        public Object securityDomain;
        public int functionType;

        public Builder() {}

        private Builder(Builder<?> parent) {
            this.languageVersion = parent.languageVersion;
            this.rawSource = parent.rawSource;
            this.sourceFile = parent.sourceFile;
            this.isStrict = parent.isStrict;
            this.securityController = parent.securityController;
            this.securityDomain = parent.securityDomain;
        }

        public Builder<JSFunction> createChildBuilder() {
            Builder<JSFunction> child = new Builder<>(this);
            nestedFunctions.add(child);
            return child;
        }

        public JSDescriptor<T> build(Consumer<JSDescriptor<?>> consumer) {
            if (parent != null) {
                throw new Error();
            }
            return build(null, consumer);
        }

        private JSDescriptor<T> build(JSDescriptor<?> parent, Consumer<JSDescriptor<?>> consumer) {
            for (int i = 0; i < paramAndVarNames.length; i++) {
                paramAndVarNames[i] = paramAndVarNames[i].intern();
            }

            var result =
                    new JSDescriptor<T>(
                            code.build(),
                            constructor.build(),
                            parent,
                            paramAndVarNames,
                            paramIsConst,
                            isStrict,
                            isScript,
                            isTopLevel,
                            isES6Generator,
                            isShorthand,
                            hasPrototype,
                            hasLexicalThis,
                            isEvalFunction,
                            hasRestArg,
                            sourceFile,
                            rawSource,
                            rawSourceStart,
                            rawSourceEnd,
                            name == null ? null : name.intern(),
                            languageVersion,
                            paramAndVarCount,
                            paramCount,
                            hasDefaultParameters,
                            requiresActivationFrame,
                            requiresArgumentObject,
                            declaredAsFunctionExpression,
                            securityController,
                            securityDomain,
                            functionType);
            consumer.accept(result);
            result.nestedFunctions =
                    nestedFunctions.stream()
                            .map(x -> x.build(result, consumer))
                            .collect(Collectors.toUnmodifiableList());
            return result;
        }

        public void setCode(JSCode.Builder<T> code) {
            this.code = (JSCode.Builder<T>) code;
        }
    }
}
