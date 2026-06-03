package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class ArrayLit extends Instruction {
    private final Operand[] elements;
    private final int[] skipIndices;

    public ArrayLit(Operand[] elements, int[] skipIndices) {
        this.elements = elements;
        this.skipIndices = skipIndices;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        Object[] data = frame.getArguments(cx, elements);
        Object val = ScriptRuntime.newArrayLiteral(data, skipIndices, cx, frame.scope);
        frame.push(val);
    }

    @Override
    public int stackChange() {
        int count = 0;
        for (var element : elements) {
            count += element.stackChange();
        }
        return 1 + count;
    }

    @Override
    public String toDebugString() {
        var length = skipIndices == null ? 0 : skipIndices.length;
        return InstructionFormatter.formatInstruction(
                this, "elements", elements, "skipIndices", length);
    }
}
