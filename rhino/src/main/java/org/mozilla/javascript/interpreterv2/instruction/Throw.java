package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class Throw extends Instruction {
    private final Operand value;
    private final short lineNumber;

    public Throw(Operand value, short lineNumber) {
        this.value = value;
        this.lineNumber = lineNumber;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        Object value = this.value.retrieveAndWrap(cx, frame);

        frame.throwable =
                new JavaScriptException(
                        value, frame.fnOrScript.getDescriptor().getSourceName(), lineNumber);
    }

    @Override
    public int stackChange() {
        return value.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "value", value, "line", lineNumber);
    }
}
