package org.mozilla.javascript;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;

public abstract class ACompilerData<T extends ScriptOrFn<T>, U extends ACompilerData<?, U>>
        extends JSCode<T> implements Serializable {
    private static final long serialVersionUID = -2825944169262416223L;

    public final int[] exceptionTable;

    public final int maxVars;
    public final int maxLocals;
    public final int maxStack;
    public final int maxFrameSize;

    protected ACompilerData(
            int maxVars, int maxLocals, int maxStack, int maxFrameSize, int[] exceptionTable) {
        this.maxVars = maxVars;
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
        this.maxFrameSize = maxFrameSize;
        this.exceptionTable = exceptionTable;
    }

    public abstract int getLineNumberFromPc(int pc, int pcLineStart);

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
