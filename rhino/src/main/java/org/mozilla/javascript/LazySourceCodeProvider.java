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
            var funcionSrc = source.substring(start, end);
            descriptor.replaceSourceProvider(new ResolvedSourceProvider(funcionSrc));
            return funcionSrc;
        } else {
            source = sourceSupplier.get();
            if (source != null) {
                sourceCodeRef = new WeakReference<>(source);
                var funcionSrc = source.substring(start, end);
                descriptor.replaceSourceProvider(new ResolvedSourceProvider(funcionSrc));
                return funcionSrc;
            } else {
                descriptor.replaceSourceProvider(new ResolvedSourceProvider(""));
                return "";
            }
        }
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
    }
}
