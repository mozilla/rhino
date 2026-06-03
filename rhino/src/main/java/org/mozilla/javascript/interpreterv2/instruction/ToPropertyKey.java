package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.PopOperand;

/**
 * Converts TOS to a property key per ECMA ToPropertyKey: Symbols are left unchanged; everything
 * else is converted to a String via {@link ScriptRuntime#toString}. Emitted after evaluating a
 * computed property-name expression and before evaluating its value, so that any {@code toString}
 * side-effects (e.g. setting a variable read by the value expression) happen in spec order (ECMA
 * 13.2.5.5 step 1 before step 6).
 */
public class ToPropertyKey extends Instruction {
    public static final ToPropertyKey instance = new ToPropertyKey();

    private ToPropertyKey() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        Object key = PopOperand.instance.retrieveAndWrap(cx, frame);
        frame.push(key instanceof Symbol ? key : ScriptRuntime.toString(key));
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this);
    }
}
