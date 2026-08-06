package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Arrays;
import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Generic {@code new}-expression instruction: an arbitrary number of arguments stored in an array.
 * Used for 4+ argument constructors. The common low-arity case is handled by {@link New0}..{@link
 * New3} instead.
 */
final class NewN extends New {
    private final Operand[] arguments;

    NewN(Operand fun, Operand[] arguments) {
        super(fun);
        this.arguments = arguments;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        countInvocation(cx, frame);
        Object[] args = frame.getArguments(cx, arguments);
        doNew(cx, frame, args);
    }

    @Override
    public int stackChange() {
        int count = 0;
        for (var argument : arguments) {
            count += argument.stackChange();
        }
        return 1 + fun.stackChange() + count;
    }

    @Override
    public String toDebugString() {
        return formatDebug(arguments);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NewN)) {
            return false;
        }
        NewN other = (NewN) o;
        return Objects.equals(fun, other.fun) && Arrays.equals(arguments, other.arguments);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(fun) + Arrays.hashCode(arguments);
    }
}
