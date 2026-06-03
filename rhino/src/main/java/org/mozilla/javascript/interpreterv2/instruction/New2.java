package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Arity-specialized {@link New} with two arguments. */
final class New2 extends New {
    private final Operand arg0;
    private final Operand arg1;

    New2(Operand fun, Operand arg0, Operand arg1) {
        super(fun);
        this.arg0 = arg0;
        this.arg1 = arg1;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        countInvocation(cx, frame);
        // Retrieve in reverse order: arguments sit on the stack with the last on top.
        Object[] args = new Object[2];
        args[1] = arg1.retrieveAndWrap(cx, frame);
        args[0] = arg0.retrieveAndWrap(cx, frame);
        doNew(cx, frame, args);
    }

    @Override
    public int stackChange() {
        return 1 + fun.stackChange() + arg0.stackChange() + arg1.stackChange();
    }

    @Override
    public String toDebugString() {
        return formatDebug(new Operand[] {arg0, arg1});
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof New2)) {
            return false;
        }
        New2 other = (New2) o;
        return Objects.equals(fun, other.fun)
                && Objects.equals(arg0, other.arg0)
                && Objects.equals(arg1, other.arg1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fun, arg0, arg1);
    }
}
