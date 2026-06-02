package org.mozilla.javascript.interpreterv2.operand;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Undefined;

public final class UndefinedOperand extends Operand {

    public static final UndefinedOperand instance = new UndefinedOperand();

    private UndefinedOperand() {}

    @Override
    public Object retrieve(Context cx, CallFrameV2 frame) {
        return Undefined.instance;
    }

    @Override
    public double retrieveDouble(CallFrameV2 frame) {
        throw new UnsupportedOperationException("Undefined operand has no double value");
    }

    @Override
    public boolean isDouble(CallFrameV2 frame) {
        return false;
    }

    @Override
    public void appendDebugString(StringBuilder sb) {
        sb.append("undefined");
    }

    @Override
    public boolean isValidJumpTableKey() {
        // We're returning false here to avoid confusion when we have SCRIPTABLE_UNDEFINED
        return false;
    }

    @Override
    public boolean coerceToBoolean(Context cx, CallFrameV2 frame) {
        return false;
    }
}
