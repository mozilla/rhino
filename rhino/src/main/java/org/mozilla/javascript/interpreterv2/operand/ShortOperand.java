package org.mozilla.javascript.interpreterv2.operand;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;

public final class ShortOperand extends Operand {

    // It is implicitly cast to an int in old interpreter
    private final int value;

    public ShortOperand(short value) {
        this.value = value;
    }

    @Override
    public Integer retrieve(Context cx, CallFrameV2 frame) {
        return value;
    }

    @Override
    public double retrieveDouble(CallFrameV2 frame) {
        return value;
    }

    @Override
    public boolean isDouble(CallFrameV2 frame) {
        return value == -1.0;
    }

    @Override
    public void appendDebugString(StringBuilder sb) {
        sb.append(value);
    }

    @Override
    public boolean isValidJumpTableKey() {
        return true;
    }

    @Override
    public boolean coerceToBoolean(Context cx, CallFrameV2 frame) {
        return value != 0;
    }
}
