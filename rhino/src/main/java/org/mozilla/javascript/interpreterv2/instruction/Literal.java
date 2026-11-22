package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class Literal extends Instruction {
    private final Object literal;

    public Literal(Object literal) {
        this.literal = literal;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.push(literal);
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "literal", literal);
    }
}
