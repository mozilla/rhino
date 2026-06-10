package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.addInstructionCount;
import static org.mozilla.javascript.InterpreterV2.doShallowEquals;

import java.util.Collections;
import java.util.Set;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class IfEqPop extends JumpInstruction {
    private final Operand value;
    private final Operand test;

    public IfEqPop(Operand value, Operand test) {
        this.value = value;
        this.test = test;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        var condition = doShallowEquals(cx, frame, value, test);
        if (!condition) {
            frame.pc += 1;
        } else {
            if (cx.getInstructionObserverThreshold() != 0) {
                addInstructionCount(cx, frame, 2);
            }
            frame.pc += offset;
            frame.pcPrevBranch = frame.pc;
            value.cleanup(frame);
        }
    }

    @Override
    public int stackChange() {
        return value.stackChange() + test.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this,
                "value",
                value,
                "test",
                test,
                "offset",
                InstructionFormatter.formatOffset(offset));
    }

    @Override
    public Set<Integer> getTargets(int fromPC) {
        return Collections.singleton(fromPC + offset);
    }
}
