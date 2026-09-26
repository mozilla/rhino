package org.mozilla.javascript;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;

/** Can provide source code for a built in Function */
public class BuiltInSourceCodeProvider implements SourceCodeProvider, Serializable {
    private static final long serialVersionUID = 1L;
    public static final BuiltInSourceCodeProvider INSTANCE = new BuiltInSourceCodeProvider();

    private BuiltInSourceCodeProvider() {}

    @Override
    public String getSource(JSDescriptor<?> desc, int start, int end) {
        return String.format("function %s() {\n\t[native code]\n}", desc.getFunctionName());
    }

    @Override
    public String getRawSource() {
        return String.format("/*[native code]*/\n}");
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
