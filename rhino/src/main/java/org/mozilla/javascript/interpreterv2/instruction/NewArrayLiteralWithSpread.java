package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NewLiteralStorage;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

/**
 * Starts an array literal that contains at least one spread. Pushes a freshly allocated {@link
 * NewLiteralStorage} sized for the non-spread elements. Subsequent {@link ArrayLitPush} / {@link
 * ArrayLitSpread} instructions fill it in source order, and {@link ArrayLitFinish} builds the
 * array.
 */
public class NewArrayLiteralWithSpread extends Instruction {
    private final int[] skipIndices;
    private final int nonSpreadCount;

    public NewArrayLiteralWithSpread(int[] skipIndices, int nonSpreadCount) {
        this.skipIndices = skipIndices;
        this.nonSpreadCount = nonSpreadCount;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        var storage = NewLiteralStorage.create(cx, nonSpreadCount, false);
        if (skipIndices != null) {
            storage.setSkipIndexes(skipIndices);
        }
        frame.push(storage);
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        var skipLen = skipIndices == null ? 0 : skipIndices.length;
        return InstructionFormatter.formatInstruction(
                this, "skipIndices", skipLen, "nonSpreadCount", nonSpreadCount);
    }
}
