package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class RefIncDec extends Instruction {
    private final Operand ref;
    private final int incrDecrMask;

    public RefIncDec(Operand ref, int incrDecrMask) {
        this.ref = ref;
        this.incrDecrMask = incrDecrMask;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        Ref ref = (Ref) this.ref.retrieve(cx, frame);
        frame.push(ScriptRuntime.refIncrDecr(ref, cx, frame.scope, incrDecrMask));
    }

    @Override
    public int stackChange() {
        return 1 + ref.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "ref", ref, "mask", incrDecrMask);
    }
}
