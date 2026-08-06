package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.InstructionSimplification;
import org.mozilla.javascript.interpreterv2.KnownType;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class StringStringAdd extends Instruction {
    private final Operand lhs;
    private final Operand rhs;

    public StringStringAdd(Operand lhs, Operand rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        Object rhsValue = rhs.retrieve(cx, frame);
        Object lhsValue = lhs.retrieve(cx, frame);

        frame.push(new ConsString((CharSequence) lhsValue, (CharSequence) rhsValue));
    }

    @Override
    public int stackChange() {
        return 1 + lhs.stackChange() + rhs.stackChange();
    }

    @Override
    public KnownType getKnownType(InstructionSimplification simplifier) {
        return KnownType.STRING;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "lhs", lhs, "rhs", rhs);
    }
}
