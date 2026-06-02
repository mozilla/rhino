package org.mozilla.javascript.interpreterv2.operand;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionSimplification;
import org.mozilla.javascript.interpreterv2.KnownType;

public final class BooleanOperand extends Operand {
    public static final BooleanOperand TRUE = new BooleanOperand(true);
    public static final BooleanOperand FALSE = new BooleanOperand(false);

    private final boolean value;

    private BooleanOperand(boolean value) {
        this.value = value;
    }

    @Override
    public Boolean retrieve(Context cx, CallFrameV2 frame) {
        return value;
    }

    @Override
    public double retrieveDouble(CallFrameV2 frame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDouble(CallFrameV2 frame) {
        return false;
    }

    public boolean getBoolean() {
        return value;
    }

    @Override
    public KnownType getKnownType(InstructionSimplification simplifier) {
        return KnownType.BOOLEAN;
    }

    @Override
    public void appendDebugString(StringBuilder sb) {
        sb.append(value);
    }

    @Override
    public boolean isValidJumpTableKey() {
        return true;
    }
}
