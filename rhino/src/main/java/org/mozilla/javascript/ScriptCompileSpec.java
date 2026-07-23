package org.mozilla.javascript;

import java.io.IOException;
import java.io.Reader;

/**
 * Parameters for compiling a JavaScript script (i.e. top-level program source). Build instances via
 * {@link #fromSource(String)} or {@link #fromReader(Reader)} and pass them to {@link
 * Context#compileScript(ScriptCompileSpec)} or {@link Context#evaluateScript(ScriptCompileSpec,
 * VarScope)}.
 *
 * @see FunctionCompileSpec
 */
public final class ScriptCompileSpec extends CompileSpec<JSScript> {

    public ScriptCompileSpec(Builder builder) {
        super(builder);
    }

    public static Builder fromSource(String source) {
        return new Builder(source);
    }

    public static Builder fromReader(Reader reader) throws IOException {
        return new Builder(Kit.readReader(reader));
    }

    @Override
    boolean returnFunction() {
        return false;
    }

    @Override
    CompileFn<JSScript> compilationFunction() {
        return Evaluator::compileScript;
    }

    public static final class Builder extends CompileSpec.Builder<JSScript, Builder> {
        private Builder(String source) {
            super(source);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public ScriptCompileSpec build() {
            return new ScriptCompileSpec(this);
        }
    }
}
