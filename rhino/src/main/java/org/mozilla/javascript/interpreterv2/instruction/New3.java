package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Arity-specialized {@link New} with three arguments. */
final class New3 extends New {
    private final Operand arg0;
    private final Operand arg1;
    private final Operand arg2;

    New3(Operand fun, Operand arg0, Operand arg1, Operand arg2) {
        super(fun);
        this.arg0 = arg0;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        countInvocation(cx, frame);
        // Retrieve in reverse order: arguments sit on the stack with the last on top.
        Object[] args = new Object[3];
        args[2] = arg2.retrieveAndWrap(cx, frame);
        args[1] = arg1.retrieveAndWrap(cx, frame);
        args[0] = arg0.retrieveAndWrap(cx, frame);
        doNew(cx, frame, args);
    }

    @Override
    public int stackChange() {
        return 1 + fun.stackChange() + arg0.stackChange() + arg1.stackChange() + arg2.stackChange();
    }

    @Override
    public String toDebugString() {
        return formatDebug(new Operand[] {arg0, arg1, arg2});
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof New3)) {
            return false;
        }
        New3 other = (New3) o;
        return Objects.equals(fun, other.fun)
                && Objects.equals(arg0, other.arg0)
                && Objects.equals(arg1, other.arg1)
                && Objects.equals(arg2, other.arg2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fun, arg0, arg1, arg2);
    }
}
