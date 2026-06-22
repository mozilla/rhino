package org.mozilla.javascript.interpreterv2;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.mozilla.javascript.interpreterv2.instruction.Instruction;

public class InstructionSimplification {
    private final List<Instruction> instructions;
    private final Set<Integer> jumpTargets;
    private int currentStackOffset = 0;
    private int currentPc = 0;

    public InstructionSimplification(List<Instruction> instructions, Set<Integer> jumpTargets) {
        this.instructions = instructions;
        this.jumpTargets = jumpTargets;
    }

    public void simplify() {
        for (int pc = 0; pc < instructions.size(); pc++) {
            this.currentPc = pc;
            resetForInstruction();

            Instruction instruction = instructions.get(pc);
            Instruction simplified = instruction.simplify(this);

            if (instruction != simplified) {
                instructions.set(pc, simplified);
            }
        }
    }

    public KnownType getStackValueType(int stackOffset) {
        var producerPc = findStackProducer(stackOffset);
        if (producerPc.isEmpty()) {
            return KnownType.UNKNOWN;
        }

        int pc = producerPc.get();
        int savedPc = this.currentPc;
        this.currentPc = pc;
        KnownType type = instructions.get(pc).getKnownType(this);
        this.currentPc = savedPc;
        return type;
    }

    public boolean isJumpTarget(int pc) {
        return jumpTargets.contains(pc);
    }

    public void consumeStack(int count) {
        currentStackOffset += count;
    }

    private Optional<Integer> findStackProducer(int stackOffset) {
        int totalOffset = currentStackOffset + stackOffset;

        if (isJumpTarget(currentPc)) {
            return Optional.empty();
        }

        int remainingDepth = totalOffset;

        for (int i = currentPc - 1; i >= 0; i--) {

            Instruction inst = instructions.get(i);
            int change = inst.stackChange();

            remainingDepth -= change;

            if (change > 0 && remainingDepth < 0) {
                return Optional.of(i);
            }

            if (isJumpTarget(i)) {
                return Optional.empty();
            }
        }

        throw new IllegalStateException("We didn't find the producer of this stack value");
    }

    private void resetForInstruction() {
        currentStackOffset = 0;
    }
}
