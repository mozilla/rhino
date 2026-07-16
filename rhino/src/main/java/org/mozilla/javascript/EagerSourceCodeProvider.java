package org.mozilla.javascript;

import java.io.Serializable;
import java.util.Objects;

/** Stores a reference to the original source code and returns it as necessary. */
public class EagerSourceCodeProvider implements SourceCodeProvider, Serializable {
    private static final long serialVersionUID = 1L;
    private final String rawSource;

    public EagerSourceCodeProvider(String rawSource) {
        Objects.requireNonNull(rawSource);
        this.rawSource = rawSource;
    }

    @Override
    public String getSource(String functionName, int start, int end) {
        return rawSource.substring(start, end);
    }
}
