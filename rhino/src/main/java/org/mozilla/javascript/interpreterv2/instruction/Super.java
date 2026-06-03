package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class Super extends Instruction {
    public static final Super instance = new Super();

    private Super() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
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
            frame.push(Undefined.instance);
        } else {
            frame.push(homeObject.getPrototype());
        }

        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this);
    }
}
