package org.mozilla.javascript;

import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;
import org.mozilla.javascript.sourcemap.SourceMapper;

/**
 * Parameters for compiling a JavaScript function (a single function definition). The parent scope
 * is required, so it must be supplied to {@link #fromSource(String, VarScope)} or {@link
 * #fromReader(Reader, VarScope)}. Pass instances to {@link
 * Context#compileFunction(FunctionCompileSpec)}.
 *
 * @see ScriptCompileSpec
 */
public final class FunctionCompileSpec {
    private final String source;
    private final VarScope scope;
    private final String sourceName;
    private final int lineno;
    private final Object securityDomain;
    private final Evaluator compiler;
    private final ErrorReporter compilationErrorReporter;
    private final Consumer<CompilerEnvirons> compilerEnvironsProcessor;
    private final SourceMapper sourceMapper;
    private final SourceCodeSupplier sourceCodeSupplier;

    public FunctionCompileSpec(
            String source,
            VarScope scope,
            String sourceName,
            int lineno,
            Object securityDomain,
            Evaluator compiler,
            ErrorReporter compilationErrorReporter,
            Consumer<CompilerEnvirons> compilerEnvironsProcessor,
            SourceMapper sourceMapper,
            SourceCodeSupplier sourceCodeSupplier) {
        if (scope == null) {
            throw new IllegalArgumentException("scope is required for FunctionCompileSpec");
        }
        this.source = source;
        this.scope = scope;
        this.sourceName = sourceName;
        this.lineno = lineno;
        this.securityDomain = securityDomain;
        this.compiler = compiler;
        this.compilationErrorReporter = compilationErrorReporter;
        this.compilerEnvironsProcessor = compilerEnvironsProcessor;
        this.sourceMapper = sourceMapper;
        this.sourceCodeSupplier = sourceCodeSupplier;
    }

    public static Builder fromSource(String source, VarScope scope) {
        return new Builder(source, scope);
    }

    public static Builder fromReader(Reader reader, VarScope scope) throws IOException {
        return new Builder(Kit.readReader(reader), scope);
    }

    public String getSource() {
        return source;
    }

    public VarScope getScope() {
        return scope;
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

    public static final class Builder {
        private final String source;
        private final VarScope scope;
        private String sourceName;
        private int lineno = 0;
        private Object securityDomain;
        private Evaluator compiler;
        private ErrorReporter compilationErrorReporter;
        private Consumer<CompilerEnvirons> compilerEnvironsProcessor;
        private SourceMapper sourceMapper;
        private SourceCodeSupplier sourceCodeSupplier;

        private Builder(String source, VarScope scope) {
            if (scope == null) {
                throw new IllegalArgumentException("scope is required for FunctionCompileSpec");
            }
            this.source = source;
            this.scope = scope;
        }

        public Builder sourceName(String sourceName) {
            this.sourceName = sourceName;
            return this;
        }

        public Builder lineno(int lineno) {
            this.lineno = lineno;
            return this;
        }

        public Builder securityDomain(Object securityDomain) {
            this.securityDomain = securityDomain;
            return this;
        }

        public Builder compiler(Evaluator compiler) {
            this.compiler = compiler;
            return this;
        }

        public Builder compilationErrorReporter(ErrorReporter compilationErrorReporter) {
            this.compilationErrorReporter = compilationErrorReporter;
            return this;
        }

        public Builder compilerEnvironsProcessor(
                Consumer<CompilerEnvirons> compilerEnvironsProcessor) {
            this.compilerEnvironsProcessor = compilerEnvironsProcessor;
            return this;
        }

        public Builder sourceMapper(SourceMapper sourceMapper) {
            this.sourceMapper = sourceMapper;
            return this;
        }

        /**
         * Can be used to set a lazy source code provider for the script being compiled - for
         * example, something that will lookup the script from a database. If set, Rhino will avoid
         * storing the encoded source code and thus save memory. The trade-off is, of course, that
         * whenever someone does `Function.toString()` we will need to do a database lookup.
         */
        public Builder sourceCodeSupplier(SourceCodeSupplier sourceCodeSupplier) {
            this.sourceCodeSupplier = sourceCodeSupplier;
            return this;
        }

        public FunctionCompileSpec build() {
            int normalizedLineno = Math.max(lineno, 0);
            return new FunctionCompileSpec(
                    source,
                    scope,
                    sourceName,
                    normalizedLineno,
                    securityDomain,
                    compiler,
                    compilationErrorReporter,
                    compilerEnvironsProcessor,
                    sourceMapper,
                    sourceCodeSupplier);
        }
    }
}
