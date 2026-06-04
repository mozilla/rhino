package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeGenerator;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.interpreterv2.GeneratorState;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class ThawFrame extends Instruction {
    private final boolean isYield;
    private final short baseLineNumber;

    public ThawFrame(boolean isYield, short baseLineNumber) {
        this.isYield = isYield;
        this.baseLineNumber = baseLineNumber;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        Object obj = thawGenerator(frame, frame.generatorState);
        if (obj != Scriptable.NOT_FOUND) {
            frame.throwable = obj;
        }
    }

    private Object thawGenerator(CallFrameV2 frame, GeneratorState generatorState) {
        // we are resuming execution
        frame.frozen = false;
        int sourceLine = baseLineNumber;
        if (generatorState.operation == NativeGenerator.GENERATOR_THROW) {
            // processing a call to <generator>.throw(exception): must
            // act as if exception was thrown from resumption point.
            return new JavaScriptException(
                    generatorState.value,
                    frame.fnOrScript.getDescriptor().getSourceName(),
                    sourceLine);
        }
        if (generatorState.operation == NativeGenerator.GENERATOR_CLOSE) {
            return generatorState.value;
        }
        if (generatorState.operation != NativeGenerator.GENERATOR_SEND) throw Kit.codeBug();
        if (isYield) {
            frame.push(generatorState.value);
        }
        return Scriptable.NOT_FOUND;
    }

    @Override
    public int stackChange() {
        if (isYield) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "isYield", isYield, "baseLineNumber", baseLineNumber);
    }
}
