package org.mozilla.javascript;

import java.io.Reader;
import java.util.function.Consumer;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.sourcemap.SourceMapper;

/**
 * Parameters for compiling a JavaScript script (i.e. top-level program source). Build instances via
 * {@link #fromSource(String)} or {@link #fromReader(Reader)} and pass them to {@link
 * Context#compileScript(ScriptCompileSpec)} or {@link Context#evaluateScript(ScriptCompileSpec,
 * VarScope)}.
 *
 * @see FunctionCompileSpec
 */
public abstract class CompileSpec<T extends ScriptOrFn<T>> {
    private final String source;
    private final String sourceName;
    private final int lineno;
    private final Object securityDomain;
    private final Evaluator compiler;
    private final ErrorReporter compilationErrorReporter;
    private final Consumer<CompilerEnvirons> compilerEnvironsProcessor;
    private final SourceMapper sourceMapper;
    private final SourceCodeSupplier sourceCodeSupplier;

    public CompileSpec(Builder<T, ?> builder) {
        this.source = builder.source;
        this.sourceName = builder.sourceName;
        this.lineno = Math.max(builder.lineno, 0);
        this.securityDomain = builder.securityDomain;
        this.compiler = builder.compiler;
        this.compilationErrorReporter = builder.compilationErrorReporter;
        this.compilerEnvironsProcessor = builder.compilerEnvironsProcessor;
        this.sourceMapper = builder.sourceMapper;
        this.sourceCodeSupplier = builder.sourceCodeSupplier;
    }

    public String getSource() {
        return source;
    }

    public VarScope getScope() {
        return null;
    }

    public String getSourceName() {
        return sourceName;
    }

    public int getLineno() {
        return lineno;
    }

    public Object getSecurityDomain() {
        return securityDomain;
    }

    public Evaluator getCompiler() {
        return compiler;
    }

    public ErrorReporter getCompilationErrorReporter() {
        return compilationErrorReporter;
    }

    public Consumer<CompilerEnvirons> getCompilerEnvironsProcessor() {
        return compilerEnvironsProcessor;
    }

    public SourceMapper getSourceMapper() {
        return sourceMapper;
    }

    public SourceCodeSupplier getSourceCodeSupplier() {
        return sourceCodeSupplier;
    }

    abstract boolean returnFunction();

    abstract CompileFn<T> compilationFunction();

    @FunctionalInterface
    interface CompileFn<T extends ScriptOrFn<T>> {
        CompilationResult<T> compile(
                Evaluator evaluator, CompilerEnvirons env, ScriptNode tree, String rawSource);
    }

    public abstract static class Builder<T extends ScriptOrFn<T>, U extends Builder<T, U>> {
        protected final String source;
        protected String sourceName;
        protected int lineno = 0;
        protected Object securityDomain;
        protected Evaluator compiler;
        protected ErrorReporter compilationErrorReporter;
        protected Consumer<CompilerEnvirons> compilerEnvironsProcessor;
        protected SourceMapper sourceMapper;
        protected SourceCodeSupplier sourceCodeSupplier;

        protected Builder(String source) {
            this.source = source;
        }

        public U sourceName(String sourceName) {
            this.sourceName = sourceName;
            return getThis();
        }

        public U lineno(int lineno) {
            this.lineno = lineno;
            return getThis();
        }

        public U securityDomain(Object securityDomain) {
            this.securityDomain = securityDomain;
            return getThis();
        }

        public U compiler(Evaluator compiler) {
            this.compiler = compiler;
            return getThis();
        }

        public U compilationErrorReporter(ErrorReporter compilationErrorReporter) {
            this.compilationErrorReporter = compilationErrorReporter;
            return getThis();
        }

        public U compilerEnvironsProcessor(Consumer<CompilerEnvirons> compilerEnvironsProcessor) {
            this.compilerEnvironsProcessor = compilerEnvironsProcessor;
            return getThis();
        }

        public U sourceMapper(SourceMapper sourceMapper) {
            this.sourceMapper = sourceMapper;
            return getThis();
        }

        /**
         * Can be used to set a lazy source code provider for the script being compiled - for
         * example, something that will lookup the script from a database. If set, Rhino will avoid
         * storing the encoded source code and thus save memory. The trade-off is, of course, that
         * whenever someone does `Function.toString()` we will need to do a database lookup.
         */
        public U sourceCodeSupplier(SourceCodeSupplier sourceCodeSupplier) {
            this.sourceCodeSupplier = sourceCodeSupplier;
            return getThis();
        }

        protected abstract U getThis();

        public abstract CompileSpec<T> build();
    }
}
