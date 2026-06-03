package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.VarScope;

public class EnterDotQuery extends Instruction {
    public static final EnterDotQuery instance = new EnterDotQuery();

    private EnterDotQuery() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        Object lhs;
        if (frame.peek(0) == DOUBLE_MARK) {
            double d = frame.popDouble();
            lhs = ScriptRuntime.wrapNumber(d);
        } else {
            lhs = frame.pop();
        }
        VarScope newScope = ScriptRuntime.enterDotQuery(lhs, frame.scope);
        frame.scope = newScope;
    }

    @Override
    public int stackChange() {
        return -1;
    }
}
