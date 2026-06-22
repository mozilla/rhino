package org.mozilla.javascript.interpreterv2.operand;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionSimplification;
import org.mozilla.javascript.interpreterv2.KnownType;

public abstract class Operand {
    public static final Operand[] EMPTY_ARRAY = new Operand[0];

    public abstract Object retrieve(Context cx, CallFrameV2 frame);

    public abstract double retrieveDouble(CallFrameV2 frame);

    public abstract boolean isDouble(CallFrameV2 frame);

    public int stackChange() {
        return 0;
    }

    public Operand convertToConsume() {
        return this;
    }

    public void cleanup(CallFrameV2 frame) {}

    public Object viewValue(Context cx, CallFrameV2 frame, int offset) {
        return retrieve(cx, frame);
    }

    public boolean isValidJumpTableKey() {
        return false;
    }

    public Object retrieveAndWrap(Context cx, CallFrameV2 frame) {
        if (isDouble(frame)) {
            return ScriptRuntime.wrapNumber(retrieveDouble(frame));
        }
        return retrieve(cx, frame);
    }

    public Number retrieveNumber(Context cx, CallFrameV2 frame) {
        if (isDouble(frame)) {
            return retrieveDouble(frame);
        } else {
            var obj = retrieve(cx, frame);
            return ScriptRuntime.toNumeric(obj);
        }
    }

    public boolean coerceToBoolean(Context cx, CallFrameV2 frame) {
        if (isDouble(frame)) {
            double x = retrieveDouble(frame);
            return !Double.isNaN(x) && x != 0.0;
        }
        return ScriptRuntime.toBoolean(retrieve(cx, frame));
    }

    public double coerceToDouble(Context cx, CallFrameV2 frame) {
        if (this.isDouble(frame)) {
            return retrieveDouble(frame);
        }
        return ScriptRuntime.toNumber(retrieve(cx, frame));
    }

    public void setResult(Context cx, CallFrameV2 frame) {
        if (isDouble(frame)) {
            frame.setResult(retrieveDouble(frame));
        } else {
            frame.setResult(retrieve(cx, frame));
        }
    }

    public KnownType getKnownType(InstructionSimplification simplifier) {
        return KnownType.UNKNOWN;
    }

    public abstract void appendDebugString(StringBuilder sb);
}
