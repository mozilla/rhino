package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import java.util.Collections;
import java.util.Set;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class IfNe extends JumpInstruction {
    private final Operand lhs;

    public IfNe(Operand lhs) {
        this.lhs = lhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        if (lhs.coerceToBoolean(cx, frame)) {
            frame.pc += 1;
        } else {
            if (cx.getInstructionObserverThreshold() != 0) {
                addInstructionCount(cx, frame, 2);
            }
            frame.pc += getOffset();
            frame.pcPrevBranch = frame.pc;
        }
    }

    @Override
    public int stackChange() {
        return lhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "lhs", lhs, "offset", InstructionFormatter.formatOffset(getOffset()));
    }

    @Override
    public Set<Integer> getTargets(int fromPC) {
        return Collections.singleton(fromPC + getOffset());
    }
}
