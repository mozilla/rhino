package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import java.util.Collections;
import java.util.Set;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;

public class LeaveDotQuery extends JumpInstruction {

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        boolean booleanValue;
        Object value = frame.peek(0);
        if (value == DOUBLE_MARK) {
            double d = frame.popDouble();
            booleanValue = !Double.isNaN(d) && d != 0.0;
        } else {
            frame.pop();
            booleanValue = ScriptRuntime.toBoolean(value);
        }

        Object result = ScriptRuntime.updateDotQuery(booleanValue, frame.scope);

        if (result != null) {
            frame.push(result);
            frame.scope = ScriptRuntime.leaveDotQuery(frame.scope);
            frame.pc += 1;
        } else {
            frame.pc += offset;
        }
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public Set<Integer> getTargets(int fromPC) {
        return Collections.singleton(fromPC + offset);
    }
}
