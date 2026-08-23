package org.mozilla.javascript.interpreterv2.operand;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;

public final class NewTargetOperand extends Operand {
    public static final NewTargetOperand instance = new NewTargetOperand();

    private NewTargetOperand() {}

    @Override
    public Object retrieve(Context cx, CallFrameV2 frame) {
        return frame.newTarget;
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
        sb.append("new.target");
    }
}
