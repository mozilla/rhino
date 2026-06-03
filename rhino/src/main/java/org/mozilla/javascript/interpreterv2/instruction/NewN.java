package org.mozilla.javascript.interpreterv2.instruction;

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
        countInvocation(cx, frame);
        Object[] args = frame.getArguments(cx, arguments);
        doNew(cx, frame, args);
        frame.pc += 1;
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
}
