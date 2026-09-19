package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ES6Generator;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.NativeGenerator;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class Generator extends Instruction {
    private final short baseLineno;

    public Generator(short baseLineno) {
        this.baseLineno = baseLineno;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        if (!frame.frozen) {
            // This differs from the old interpreter.
            // Instead of making pc go back and running this again,
            // (but not this code because the frame is frozen)
            // I've made a separate instruction that will thaw the frame
            // which is all that calling it twice did.
            // I am tempted to change this to assert !frame.frozen
            // but unsure if that is true yet.
            frame.pc += 1;
            CallFrameV2 generatorFrame = captureFrameForGenerator(frame);
            generatorFrame.frozen = true;
            if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
                frame.result =
                        new ES6Generator(
                                frame.scope,
                                (JSFunction) generatorFrame.fnOrScript,
                                generatorFrame);
            } else {
                frame.result =
                        new NativeGenerator(
                                frame.scope,
                                (JSFunction) generatorFrame.fnOrScript,
                                generatorFrame);
            }
            frame.shouldYieldToParent = true;
        }
    }

    private static CallFrameV2 captureFrameForGenerator(CallFrameV2 frame) {
        frame.frozen = true;
        CallFrameV2 result = frame.captureForGenerator();
        frame.frozen = false;

        return result;
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "baseLineno", baseLineno);
    }
}
