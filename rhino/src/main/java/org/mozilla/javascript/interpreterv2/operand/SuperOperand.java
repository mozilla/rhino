package org.mozilla.javascript.interpreterv2.operand;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Undefined;

public final class SuperOperand extends Operand {
    public static final SuperOperand instance = new SuperOperand();

    private SuperOperand() {}

    @Override
    public Object retrieve(Context cx, CallFrameV2 frame) {
        // See 9.1.1.3.5 GetSuperBase

        // If we are referring to "super", then we always have an activation (this is done in
        // IrFactory). The home object is stored as part of the activation frame to propagate it
        // correctly for nested functions.
        var homeObject = frame.fnOrScript.getHomeObject();
        if (homeObject == null) {
            // This if is specified in the spec, but I cannot imagine
            // how the home object will ever be null since `super` is
            // legal _only_ in method definitions, where we do have a
            // home object!
            return Undefined.instance;
        } else {
            return homeObject.getPrototype();
        }
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
        sb.append("super");
    }
}
