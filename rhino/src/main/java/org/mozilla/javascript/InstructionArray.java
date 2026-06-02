package org.mozilla.javascript;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import org.mozilla.javascript.interpreterv2.instruction.Instruction;

public class InstructionArray implements Serializable {
    private final Instruction[] instructions;
    private final int fHashcode;

    public InstructionArray(Instruction[] instructions) {
        this.instructions = instructions;
        this.fHashcode = Arrays.hashCode(instructions);
    }

    public Instruction[] getInstructions() {
        return instructions;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof InstructionArray
                && Arrays.equals(instructions, ((InstructionArray) obj).instructions);
    }

    @Override
    public int hashCode() {
        return fHashcode;
    }

    @Override
    public String toString() {
        return Collections.singletonList(instructions).toString();
    }
}
