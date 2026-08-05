package org.mozilla.javascript;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Can provide source code from a lazy {@link SourceCodeSupplier}, that for example can retrieve it
 * from the database.
 */
public class LazySourceCodeProvider implements SourceCodeProvider, Serializable {
    private static final long serialVersionUID = 1L;
    private final SourceCodeSupplier sourceSupplier;
    private WeakReference<String> sourceCodeRef;

    public LazySourceCodeProvider(SourceCodeSupplier sourceSupplier) {
        Objects.requireNonNull(sourceSupplier);
        this.sourceSupplier = sourceSupplier;
    }

    @Override
    public String getSource(JSDescriptor<?> descriptor, int start, int end) {
        String source;
        if (sourceCodeRef != null && (source = sourceCodeRef.get()) != null) {
            var functionSrc = source.substring(start, end);
            descriptor.replaceSourceProvider(new ResolvedSourceProvider(functionSrc));
            return functionSrc;
        } else {
            source = sourceSupplier.get();
            if (source != null) {
                sourceCodeRef = new WeakReference<>(source);
                var functionSrc = source.substring(start, end);
                descriptor.replaceSourceProvider(new ResolvedSourceProvider(functionSrc));
                return functionSrc;
            } else {
                descriptor.replaceSourceProvider(new ResolvedSourceProvider(""));
                return "";
            }
        }
    }

    @Override
    public String getRawSource() {
        return sourceSupplier.get();
    }

    static class ResolvedSourceProvider implements SourceCodeProvider {

        private String source;

        private ResolvedSourceProvider(String source) {
            this.source = source;
        }

        @Override
        public String getSource(JSDescriptor<?> functionName, int start, int end) {
            return source;
        }

        @Override
        public String getRawSource() {
            return source;
        }
    }
}
