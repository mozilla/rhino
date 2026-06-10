package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeIterator;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class GeneratorEnd extends Instruction {
    private final short lineNumber;

    public GeneratorEnd(short lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        // throw StopIteration
        frame.frozen = true;
        frame.generatorState.returnedException =
                new JavaScriptException(
                        NativeIterator.getStopIterationObject(frame.scope),
                        frame.compilerData.sourceFile,
                        lineNumber);
        frame.shouldYieldToParent = true;
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "lineNumber", lineNumber);
    }
}
