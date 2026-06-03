package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.InstructionSimplification;
import org.mozilla.javascript.interpreterv2.KnownType;

public class PushConstant extends Instruction {
    private final Object value;

    public PushConstant(Object value) {
        this.value = value;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.push(value);
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "value", value);
    }

    @Override
    public KnownType getKnownType(InstructionSimplification simplifier) {
        if (value instanceof Number) {
            return KnownType.NUMBER;
        } else if (value instanceof String) {
            return KnownType.STRING;
        } else if (value instanceof Boolean) {
            return KnownType.BOOLEAN;
        } else {
            return KnownType.UNKNOWN;
        }
    }
}
