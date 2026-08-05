package org.mozilla.javascript;

/** An object that can provide the source code for the given function. Internal usage only. */
public interface SourceCodeProvider {
    /** Return the source for this descriptor, betweeen the start and end offsets */
    String getSource(JSDescriptor<?> descriptor, int start, int end);

    /** Return the raw source for this descriptor usually the entire source file if availab le. */
    String getRawSource();

    static SourceCodeProvider make(
            boolean generatingSource, SourceCodeSupplier sourceCodeSupplier, String rawSource) {
        if (!generatingSource) {
            return NullSourceCodeProvider.NULL_PROVIDER;
        } else if (sourceCodeSupplier != null) {
            return new LazySourceCodeProvider(sourceCodeSupplier);
        } else {
            return new EagerSourceCodeProvider(rawSource);
        }
    }
}
