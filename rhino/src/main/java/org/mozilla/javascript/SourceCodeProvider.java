package org.mozilla.javascript;

/** An object that can provide the source code for the given function. Internal usage only. */
public interface SourceCodeProvider {
    String getSource(String functionName, int start, int end);

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
