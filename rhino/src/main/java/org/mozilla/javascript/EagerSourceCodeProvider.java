package org.mozilla.javascript;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
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
    public String getSource(JSDescriptor<?> desc, int start, int end) {
        return rawSource.substring(start, end);
    }

    @Override
    public String getRawSource() {
        return rawSource;
    }

    @Serial
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        Context.checkDeserializationEnabled();
        stream.defaultReadObject();
    }

    @Serial
    private void writeObject(ObjectOutputStream stream) throws IOException {
        Context.checkSerializationEnabled();
        stream.defaultWriteObject();
    }

    @Serial
    private void readObjectNoData() throws InvalidObjectException {
        throw new InvalidObjectException("Missing serialized Rhino object data");
    }
}
