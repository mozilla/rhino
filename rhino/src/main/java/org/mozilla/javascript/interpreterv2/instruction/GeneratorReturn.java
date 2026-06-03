package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeIterator;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class GeneratorReturn extends Instruction {
    private final short lineNumber;
    private final Operand value;

    public GeneratorReturn(short lineNumber, Operand value) {
        this.lineNumber = lineNumber;
        this.value = value;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        frame.frozen = true;
        value.setResult(cx, frame);

        NativeIterator.StopIteration si =
                new NativeIterator.StopIteration(
                        (frame.result == DOUBLE_MARK)
                                ? Double.valueOf(frame.resultDbl)
                                : frame.result);

        frame.generatorState.returnedException =
                new JavaScriptException(
                        si, frame.fnOrScript.getDescriptor().getSourceName(), lineNumber);
        frame.shouldYieldToParent = true;
    }

    @Override
    public int stackChange() {
        return value.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "lineNumber", lineNumber, "value", value);
    }
}
