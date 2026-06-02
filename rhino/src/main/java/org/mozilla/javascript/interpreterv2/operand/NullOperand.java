package org.mozilla.javascript.interpreterv2.operand;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;

public final class NullOperand extends Operand {
    public static final NullOperand instance = new NullOperand();

    private NullOperand() {}

    @Override
    public Object retrieve(Context cx, CallFrameV2 frame) {
        return null;
    }

    @Override
    public double retrieveDouble(CallFrameV2 frame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDouble(CallFrameV2 frame) {
        return false;
    }

    @Override
    public void appendDebugString(StringBuilder sb) {
        sb.append("null");
    }

    @Override
    public boolean isValidJumpTableKey() {
        // Null cannot be a key on the jump table
        return false;
    }

    @Override
    public boolean coerceToBoolean(Context cx, CallFrameV2 frame) {
        return false;
    }
}
