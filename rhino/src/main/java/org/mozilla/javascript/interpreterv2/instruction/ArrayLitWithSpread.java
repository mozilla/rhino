package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NewLiteralStorage;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Builds an array literal that contains at least one spread ({@code [...expr]}) entry.
 *
 * <p>Uses {@link NewLiteralStorage} to accumulate values in order, calling {@link
 * NewLiteralStorage#spread} for spread entries and {@link NewLiteralStorage#pushValue} for regular
 * entries.
 */
public class ArrayLitWithSpread extends Instruction {
    private final Operand[] elements;
    private final boolean[] isSpread;
    private final int[] skipIndices;
    private final int[] sourcePositions;
    private final int nonSpreadCount;

    public ArrayLitWithSpread(
            Operand[] elements,
            boolean[] isSpread,
            int[] skipIndices,
            int[] sourcePositions,
            int nonSpreadCount) {
        this.elements = elements;
        this.isSpread = isSpread;
        this.skipIndices = skipIndices;
        this.sourcePositions = sourcePositions;
        this.nonSpreadCount = nonSpreadCount;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        Object[] vals = frame.getArguments(cx, elements);

        var storage = NewLiteralStorage.create(cx, nonSpreadCount, false);
        if (skipIndices != null) {
            storage.setSkipIndexes(skipIndices);
        }
        for (int i = 0; i < elements.length; i++) {
            if (isSpread[i]) {
                int sp = sourcePositions != null ? sourcePositions[i] : 0;
                storage.spread(cx, frame.scope, vals[i], sp);
            } else {
                storage.pushValue(vals[i]);
            }
        }

        int[] finalSkip = null;
        if (skipIndices != null) {
            finalSkip = storage.getAdjustedSkipIndexes();
        }
        Object val = ScriptRuntime.newArrayLiteral(storage.getValues(), finalSkip, cx, frame.scope);
        frame.push(val);
    }

    @Override
    public int stackChange() {
        int count = 0;
        for (var e : elements) {
            count += e.stackChange();
        }
        return 1 + count;
    }

    @Override
    public String toDebugString() {
        var skipLen = skipIndices == null ? 0 : skipIndices.length;
        return InstructionFormatter.formatInstruction(
                this, "elements", elements, "spread", isSpread, "skipIndices", skipLen);
    }
}
