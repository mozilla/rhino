package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NewLiteralStorage;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Applies a spread (ECMAScript 13.2.5.5 {@code CopyDataProperties}) to the {@link
 * NewLiteralStorage} on top of the stack, grown by {@link NewLiteralStorage#spread}.
 */
public class LitSpread extends Instruction {
    private final Operand sourceOp;

    public LitSpread(Operand sourceOp) {
        this.sourceOp = sourceOp;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        Object source = sourceOp.retrieveAndWrap(cx, frame);
        var storage = (NewLiteralStorage) frame.peek(0);
        // sourcePosition is only consulted for array-spread skip-index adjustments, which
        // object literals never use; pass 0.
        storage.spread(cx, frame.scope, source, 0);
    }

    @Override
    public int stackChange() {
        return sourceOp.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "source", sourceOp);
    }
}
