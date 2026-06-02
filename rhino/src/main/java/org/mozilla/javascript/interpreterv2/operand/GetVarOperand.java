package org.mozilla.javascript.interpreterv2.operand;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;

public final class GetVarOperand extends Operand {
    private final int index;

    private static final GetVarOperand[] CACHE = new GetVarOperand[16];

    static {
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new GetVarOperand(i);
        }
    }

    public static GetVarOperand createOperand(int index) {
        if (index >= 0 && index < CACHE.length) {
            return CACHE[index];
        }
        return new GetVarOperand(index);
    }

    private GetVarOperand(int index) {
        this.index = index;
    }

    @Override
    public Object retrieve(Context cx, CallFrameV2 frame) {
        return frame.getVar(index);
    }

    @Override
    public Object retrieveAndWrap(Context cx, CallFrameV2 frame) {
        return frame.getVarAndWrap(index);
    }

    @Override
    public double retrieveDouble(CallFrameV2 frame) {
        return frame.getVarDouble(index);
    }

    @Override
    public boolean isDouble(CallFrameV2 frame) {
        return frame.isVarDouble(index);
    }

    @Override
    public Object viewValue(Context cx, CallFrameV2 frame, int offset) {
        return frame.getVarAndWrap(index);
    }

    @Override
    public void appendDebugString(StringBuilder sb) {
        sb.append("var.").append(index);
    }
}
