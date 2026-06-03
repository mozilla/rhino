package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.InstructionSimplification;
import org.mozilla.javascript.interpreterv2.KnownType;

public class PushConstant extends Instruction {
    private final Object value;

    public static final PushConstant pushUndefined = new PushConstant(Undefined.instance);
    public static final PushConstant pushNull = new PushConstant(null);
    public static final PushConstant pushTrue = new PushConstant(Boolean.TRUE);
    public static final PushConstant pushFalse = new PushConstant(Boolean.FALSE);
    public static final PushConstant pushZero = new PushConstant(0);
    public static final PushConstant pushOne = new PushConstant(1);
    public static final PushConstant pushNegativeZero =
            new PushConstant(ScriptRuntime.negativeZeroObj);

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
