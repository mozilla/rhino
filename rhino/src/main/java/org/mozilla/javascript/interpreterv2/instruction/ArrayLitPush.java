package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NewLiteralStorage;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Appends one element to the array {@link NewLiteralStorage} on top of the stack. */
public class ArrayLitPush extends Instruction {
    private final Operand valueOp;

    public ArrayLitPush(Operand valueOp) {
        this.valueOp = valueOp;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        Object value = valueOp.retrieveAndWrap(cx, frame);
        var storage = (NewLiteralStorage) frame.peek(0);
        storage.pushValue(value);
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return valueOp.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "value", valueOp);
    }
}
