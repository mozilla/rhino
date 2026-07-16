package org.mozilla.javascript;

import java.io.IOException;
import java.io.Reader;

/**
 * Parameters for compiling a JavaScript function (a single function definition). The parent scope
 * is required, so it must be supplied to {@link #fromSource(String, VarScope)} or {@link
 * #fromReader(Reader, VarScope)}. Pass instances to {@link
 * Context#compileFunction(FunctionCompileSpec)}.
 *
 * @see ScriptCompileSpec
 */
public final class FunctionCompileSpec extends CompileSpec<JSFunction> {
    private final VarScope scope;

    public FunctionCompileSpec(Builder builder) {
        super(builder);
        if (builder.scope == null) {
            throw new IllegalArgumentException("scope is required for FunctionCompileSpec");
        }
        this.scope = builder.scope;
    }

    public static Builder fromSource(String source, VarScope scope) {
        return new Builder(source, scope);
    }

    public static Builder fromReader(Reader reader, VarScope scope) throws IOException {
        return new Builder(Kit.readReader(reader), scope);
    }

    @Override
    public VarScope getScope() {
        return scope;
    }

    @Override
    boolean returnFunction() {
        return true;
    }

    @Override
    CompileFn<JSFunction> compilationFunction() {
        return Evaluator::compileFunction;
    }

    public static final class Builder extends CompileSpec.Builder<JSFunction, Builder> {
        private final VarScope scope;

        private Builder(String source, VarScope scope) {
            super(source);
            if (scope == null) {
                throw new IllegalArgumentException("scope is required for FunctionCompileSpec");
            }
            this.scope = scope;
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public FunctionCompileSpec build() {
            return new FunctionCompileSpec(this);
        }
    }
}
