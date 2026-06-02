package org.mozilla.javascript.interpreterv2.operand;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;

public final class ZeroOperand extends Operand {
    public static final ZeroOperand instance = new ZeroOperand();

    private ZeroOperand() {}

    @Override
    public Integer retrieve(Context cx, CallFrameV2 frame) {
        return Integer.valueOf(0);
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
        sb.append("0");
    }

    @Override
    public boolean isValidJumpTableKey() {
        return true;
    }
}
