package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NewLiteralStorage;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Fused create instruction for an object literal that contains at least one spread. Pushes the
 * result object followed by a freshly allocated {@link NewLiteralStorage} sized for the non-spread
 * entries, pre-populated with the contiguous leading literal-value prefix. Subsequent {@link
 * LitPush} / {@link LitSpread} instructions fill in the remaining slots in source order.
 */
public class NewObjectLiteralWithSpread extends Instruction {
    /** Keys for the literal prefix only (length == {@code prefixValues.length}). */
    private final Object[] prefixKeys;

    private final Operand[] prefixValues;
    private final int nonSpreadCount;

    public NewObjectLiteralWithSpread(
            Object[] prefixKeys, Operand[] prefixValues, int nonSpreadCount) {
        this.prefixKeys = prefixKeys;
        this.prefixValues = prefixValues;
        this.nonSpreadCount = nonSpreadCount;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var obj = cx.newObject(frame.scope);
        var storage = NewLiteralStorage.create(cx, nonSpreadCount, true);
        for (int i = 0; i < prefixValues.length; i++) {
            storage.pushKey(prefixKeys[i]);
            storage.pushValue(prefixValues[i].retrieveAndWrap(cx, frame));
        }

        frame.push(obj);
        frame.push(storage);
    }

    @Override
    public int stackChange() {
        return 2;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this,
                "prefixKeys",
                prefixKeys,
                "prefixValues",
                prefixValues,
                "nonSpreadCount",
                nonSpreadCount);
    }
}
