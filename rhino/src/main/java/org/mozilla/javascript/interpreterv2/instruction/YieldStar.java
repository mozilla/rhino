package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ES6Generator;
import org.mozilla.javascript.NativeGenerator;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.GeneratorState;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class YieldStar extends Instruction {
    private final Operand valueOperand;
    private final short lineNumber;

    public YieldStar(Operand valueOperand, short lineNumber) {
        this.valueOperand = valueOperand;
        this.lineNumber = lineNumber;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        if (!frame.frozen) {
            freezeGenerator(cx, frame, frame.generatorState);
            frame.shouldYieldToParent = true;
        }
    }

    private void freezeGenerator(Context cx, CallFrameV2 frame, GeneratorState generatorState) {
        if (generatorState.operation == NativeGenerator.GENERATOR_CLOSE) {
            // Error: no yields when generator is closing
            throw ScriptRuntime.typeErrorById("msg.yield.closing");
        }
        // return to our caller (which should be a method of NativeGenerator)
        frame.frozen = true;

        Object result = valueOperand.retrieve(cx, frame);
        // This isn't what the old interpreter does. But trying it
        frame.setResult(new ES6Generator.YieldStarResult(result));
    }

    @Override
    public int stackChange() {
        return valueOperand.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "value", valueOperand, "lineNumber", lineNumber);
    }
}
