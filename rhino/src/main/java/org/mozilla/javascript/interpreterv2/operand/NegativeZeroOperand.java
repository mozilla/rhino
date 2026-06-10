package org.mozilla.javascript.interpreterv2.operand;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;

public final class NegativeZeroOperand extends Operand {
    public static final NegativeZeroOperand instance = new NegativeZeroOperand();

    private NegativeZeroOperand() {}

    @Override
    public Double retrieve(Context cx, CallFrameV2 frame) {
        return ScriptRuntime.negativeZeroObj;
    }

    @Override
    public double retrieveDouble(CallFrameV2 frame) {
        return ScriptRuntime.negativeZeroObj;
    }

    @Override
    public boolean isDouble(CallFrameV2 frame) {
        return true;
    }

    @Override
    public void appendDebugString(StringBuilder sb) {
        sb.append("-0");
    }

    @Override
    public boolean isValidJumpTableKey() {
        return true;
    }
}
