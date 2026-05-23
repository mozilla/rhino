package org.mozilla.javascript;

import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;
import org.mozilla.javascript.sourcemap.SourceMapper;

/**
 * Parameters for compiling a JavaScript script (i.e. top-level program source). Build instances via
 * {@link #fromSource(String)} or {@link #fromReader(Reader)} and pass them to {@link
 * Context#compileScript(ScriptCompileSpec)} or {@link Context#evaluateScript(ScriptCompileSpec,
 * VarScope)}.
 *
 * @see FunctionCompileSpec
 */
public final class ScriptCompileSpec {
    private final String source;
    private final String sourceName;
    private final int lineno;
    private final Object securityDomain;
    private final Evaluator compiler;
    private final ErrorReporter compilationErrorReporter;
    private final Consumer<CompilerEnvirons> compilerEnvironsProcessor;
    private final SourceMapper sourceMapper;

    public ScriptCompileSpec(
            String source,
            String sourceName,
            int lineno,
            Object securityDomain,
            Evaluator compiler,
            ErrorReporter compilationErrorReporter,
            Consumer<CompilerEnvirons> compilerEnvironsProcessor,
            SourceMapper sourceMapper) {
        this.source = source;
        this.sourceName = sourceName;
        this.lineno = lineno;
        this.securityDomain = securityDomain;
        this.compiler = compiler;
        this.compilationErrorReporter = compilationErrorReporter;
        this.compilerEnvironsProcessor = compilerEnvironsProcessor;
        this.sourceMapper = sourceMapper;
    }

    public static Builder fromSource(String source) {
        return new Builder(source);
    }

    public static Builder fromReader(Reader reader) throws IOException {
        return new Builder(Kit.readReader(reader));
    }

    public String getSource() {
        return source;
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

    public static final class Builder {
        private final String source;
        private String sourceName;
        private int lineno = 0;
        private Object securityDomain;
        private Evaluator compiler;
        private ErrorReporter compilationErrorReporter;
        private Consumer<CompilerEnvirons> compilerEnvironsProcessor;
        private SourceMapper sourceMapper;

        private Builder(String source) {
            this.source = source;
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

        public ScriptCompileSpec build() {
            int normalizedLineno = Math.max(lineno, 0);
            return new ScriptCompileSpec(
                    source,
                    sourceName,
                    normalizedLineno,
                    securityDomain,
                    compiler,
                    compilationErrorReporter,
                    compilerEnvironsProcessor,
                    sourceMapper);
        }
    }
}
