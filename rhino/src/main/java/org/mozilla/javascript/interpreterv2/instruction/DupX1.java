package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class DupX1 extends Instruction {
    public static final DupX1 instance = new DupX1();

    private DupX1() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        var obj = frame.peek();
        var dbl = frame.peekDouble();
        frame.stack[frame.stackTop + 1] = obj;
        frame.doubleStack[frame.stackTop + 1] = dbl;
        frame.stack[frame.stackTop] = frame.stack[frame.stackTop - 1];
        frame.doubleStack[frame.stackTop] = frame.doubleStack[frame.stackTop - 1];
        frame.stack[frame.stackTop - 1] = obj;
        frame.doubleStack[frame.stackTop - 1] = dbl;
        frame.stackTop++;
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
