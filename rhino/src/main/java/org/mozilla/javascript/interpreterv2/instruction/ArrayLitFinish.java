package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NewLiteralStorage;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

/**
 * Finalizes an array literal: pops the {@link NewLiteralStorage} and pushes the array built from
 * its accumulated values.
 */
public class ArrayLitFinish extends Instruction {
    public static final ArrayLitFinish instance = new ArrayLitFinish();

    private ArrayLitFinish() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        var storage = (NewLiteralStorage) frame.pop();
        int[] skipIndices = storage.hasSkipIndexes() ? storage.getAdjustedSkipIndexes() : null;
        frame.push(
                ScriptRuntime.newArrayLiteral(storage.getValues(), skipIndices, cx, frame.scope));
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this);
    }
}
