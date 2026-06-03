package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import java.util.Collections;
import java.util.Set;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class GoSubroutine extends JumpInstruction {
    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.push((double) frame.pc + 1);
        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, 2);
        }
        frame.pc += getOffset();
        frame.pcPrevBranch = frame.pc;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "target", InstructionFormatter.formatTarget(getOffset()));
    }

    @Override
    public Set<Integer> getTargets(int fromPC) {
        return Collections.singleton(fromPC + getOffset());
    }
}
