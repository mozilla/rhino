package org.mozilla.javascript.interpreterv2.operand;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionSimplification;
import org.mozilla.javascript.interpreterv2.KnownType;

public final class PopOperand extends Operand {
    public static final PopOperand instance = new PopOperand();

    private PopOperand() {}

    @Override
    public Object retrieve(Context cx, CallFrameV2 frame) {
        return frame.pop();
    }

    @Override
    public double retrieveDouble(CallFrameV2 frame) {
        return frame.popDouble();
    }

    @Override
    public boolean isDouble(CallFrameV2 frame) {
        return frame.peek(0) == DOUBLE_MARK;
    }

    @Override
    public int stackChange() {
        return -1;
    }

    @Override
    public Object viewValue(Context cx, CallFrameV2 frame, int offset) {
        var value = frame.peek(offset + 1);
        if (value == DOUBLE_MARK) {
            return frame.peekDouble(offset + 1);
        }
        return value;
    }

    @Override
    public KnownType getKnownType(InstructionSimplification simplifier) {
        KnownType type = simplifier.getStackValueType(0);
        simplifier.consumeStack(1);
        return type;
    }

    @Override
    public void appendDebugString(StringBuilder sb) {
        sb.append("pop");
    }
}
