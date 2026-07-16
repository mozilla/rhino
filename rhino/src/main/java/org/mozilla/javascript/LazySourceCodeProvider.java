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
    public String getSource(String functionName, int start, int end) {
        if (sourceCodeRef != null && sourceCodeRef.get() != null) {
            String source = sourceCodeRef.get();
            return source.substring(start, end);
        } else {
            String source = sourceSupplier.get();
            sourceCodeRef = new WeakReference<>(source);
            if (source != null) {
                return source.substring(start, end);
            } else {
                return null;
            }
        }
    }
}
