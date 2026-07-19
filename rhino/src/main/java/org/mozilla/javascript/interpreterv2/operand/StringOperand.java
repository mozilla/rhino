package org.mozilla.javascript.interpreterv2.operand;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.InstructionSimplification;
import org.mozilla.javascript.interpreterv2.KnownType;

public final class StringOperand extends Operand {
    private final String value;

    public StringOperand(String value) {
        this.value = value;
    }

    @Override
    public String retrieve(Context cx, CallFrameV2 frame) {
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

    public String getString() {
        return value;
    }

    @Override
    public KnownType getKnownType(InstructionSimplification simplifier) {
        return KnownType.STRING;
    }

    @Override
    public void appendDebugString(StringBuilder sb) {
        InstructionFormatter.appendString(sb, value);
    }

    @Override
    public boolean isValidJumpTableKey() {
        return true;
    }
}
