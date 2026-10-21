package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NewLiteralStorage;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Spreads an iterable (ECMAScript 13.2.4.1 {@code ArrayAccumulation}) into the array {@link
 * NewLiteralStorage} on top of the stack. The iterator is consumed immediately, so that it happens
 * in source order relative to the other elements.
 */
public class ArrayLitSpread extends Instruction {
    private final Operand sourceOp;
    private final int sourcePosition;

    public ArrayLitSpread(Operand sourceOp, int sourcePosition) {
        this.sourceOp = sourceOp;
        this.sourcePosition = sourcePosition;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        Object source = sourceOp.retrieveAndWrap(cx, frame);
        var storage = (NewLiteralStorage) frame.peek(0);
        storage.spread(cx, frame.scope, source, sourcePosition);
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return sourceOp.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "source", sourceOp, "sourcePosition", sourcePosition);
    }
}
