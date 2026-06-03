package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import java.util.Collections;
import java.util.Set;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class IfNullUndefined extends JumpInstruction {

    private final Operand object;

    public IfNullUndefined(Operand object) {
        this.object = object;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        var val = object.retrieve(cx, frame);
        if (val != null && !Undefined.isUndefined(val)) {
            frame.pc += 1;
        } else {
            if (cx.getInstructionObserverThreshold() != 0) {
                addInstructionCount(cx, frame, 2);
            }
            frame.pc += offset;
            frame.pcPrevBranch = frame.pc;
        }
    }

    @Override
    public int stackChange() {
        return object.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "object", object, "offset", InstructionFormatter.formatOffset(offset));
    }

    @Override
    public Set<Integer> getTargets(int fromPC) {
        return Collections.singleton(fromPC + offset);
    }
}
