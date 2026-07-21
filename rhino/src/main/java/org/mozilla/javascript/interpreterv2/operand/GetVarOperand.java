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
        if (!frame.useActivation) {
            return frame.getVar(index);
        } else {
            String name = frame.fnOrScript.getDescriptor().getParamOrVarName(index);
            return frame.scope.get(name, frame.scope);
        }
    }

    @Override
    public Object retrieveAndWrap(Context cx, CallFrameV2 frame) {
        if (!frame.useActivation) {
            return frame.getVarAndWrap(index);
        } else {
            String name = frame.fnOrScript.getDescriptor().getParamOrVarName(index);
            Object value = frame.scope.get(name, frame.scope);
            return value != null ? value : frame.scope;
        }
    }

    @Override
    public double retrieveDouble(CallFrameV2 frame) {
        if (!frame.useActivation) {
            return frame.getVarDouble(index);
        } else {
            // When using activation, isDouble() returns false, so this should never be called
            throw new IllegalStateException(
                    "retrieveDouble should not be called when using activation frames");
        }
    }

    @Override
    public boolean isDouble(CallFrameV2 frame) {
        if (!frame.useActivation) {
            return frame.isVarDouble(index);
        } else {
            // When using activation, variables are always objects (not primitives)
            return false;
        }
    }

    @Override
    public Object viewValue(Context cx, CallFrameV2 frame, int offset) {
        if (!frame.useActivation) {
            return frame.getVarAndWrap(index);
        } else {
            String name = frame.fnOrScript.getDescriptor().getParamOrVarName(offset);
            Object value = frame.scope.get(name, frame.scope);
            return value != null ? value : frame.scope;
        }
    }

    @Override
    public void appendDebugString(StringBuilder sb) {
        sb.append("var.").append(index);
    }
}
