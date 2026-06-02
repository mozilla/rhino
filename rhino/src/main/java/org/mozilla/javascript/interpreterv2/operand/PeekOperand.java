package org.mozilla.javascript.interpreterv2.operand;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionSimplification;
import org.mozilla.javascript.interpreterv2.KnownType;

public final class PeekOperand extends Operand {
    public static final PeekOperand instance = new PeekOperand();

    private final int offset;

    public PeekOperand() {
        offset = 0;
    }

    public PeekOperand(int offset) {
        this.offset = offset;
    }

    @Override
    public Object retrieve(Context cx, CallFrameV2 frame) {
        return frame.peek(offset);
    }

    @Override
    public double retrieveDouble(CallFrameV2 frame) {
        return frame.peekDouble(offset);
    }

    @Override
    public boolean isDouble(CallFrameV2 frame) {
        return frame.peek(offset) == DOUBLE_MARK;
    }

    @Override
    public Operand convertToConsume() {
        return PopOperand.instance;
    }

    @Override
    public void cleanup(CallFrameV2 frame) {
        frame.pop();
    }

    @Override
    public KnownType getKnownType(InstructionSimplification simplifier) {
        return simplifier.getStackValueType(offset);
    }

    @Override
    public void appendDebugString(StringBuilder sb) {
        sb.append("peek(offset=").append(offset).append(")");
    }
}
