package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Set;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public abstract class JumpInstruction extends Instruction {
    private int offset;

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public final int getOffset() {
        return offset;
    }

    /**
     * Returns all possible jump targets from this instruction.
     *
     * @param fromPC the PC of this instruction
     * @return set of all possible target PCs
     */
    public abstract Set<Integer> getTargets(int fromPC);

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "offset", InstructionFormatter.formatOffset(offset));
    }
}
