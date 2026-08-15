package org.mozilla.javascript;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import org.mozilla.classfile.DynamicConstant;

/** Stores a reference to the original source code and returns it as necessary. */
public class EagerSourceCodeProvider implements SourceCodeProvider, Serializable, DynamicConstant {
    private static final long serialVersionUID = 1L;
    private final String rawSource;

    public EagerSourceCodeProvider(String rawSource) {
        Objects.requireNonNull(rawSource);
        this.rawSource = rawSource;
    }

    @Override
    public String getSource(JSDescriptor<?> desc, int start, int end) {
        return rawSource.substring(start, end);
    }

    @Override
    public String getRawSource() {
        return rawSource;
    }

    /**
     * Bootstrap method for the dynamic constant described by {@link
     * org.mozilla.javascript.optimizer.EagerSourceCodeProviderDescriber}. The raw source is a
     * bootstrap argument rather than the constant name, since a name may not contain the "[" and
     * ";" characters that ordinary JavaScript source is full of.
     */
    public static EagerSourceCodeProvider sourceConstant(
            MethodHandles.Lookup lookup, String name, Class<?> type, String rawSource) {
        return new EagerSourceCodeProvider(rawSource);
    }
}
