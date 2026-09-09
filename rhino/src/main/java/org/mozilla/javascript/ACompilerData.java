package org.mozilla.javascript;

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

    /**
     * @return the one-based column, or zero when this backend does not track columns.
     */
    public int getColumnNumberFromPc(int pc, int pcLineStart) {
        return 0;
    }

    /**
     * @return the original source path for this position, or null to use the source name of the
     *     enclosing script or function.
     */
    public String getSourceNameFromPc(int pc, int pcLineStart) {
        return null;
    }
}
