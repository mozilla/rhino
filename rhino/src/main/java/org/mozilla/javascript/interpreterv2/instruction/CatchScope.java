package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class CatchScope extends Instruction {
    private final Operand exception;
    private final String name;
    private final int localIndex;
    private final int scopeIndex;

    public CatchScope(Operand exception, String name, int localIndex, int scopeIndex) {
        this.exception = exception;
        this.name = name;
        this.localIndex = localIndex;
        this.scopeIndex = scopeIndex;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        // stack top: exception object
        // stringReg: name of exception variable
        // indexReg: local for exception scope

        boolean afterFirstScope = scopeIndex > 0;
        Throwable caughtException = (Throwable) exception.retrieve(cx, frame);
        Scriptable lastCatchScope;
        if (afterFirstScope) {
            lastCatchScope = (Scriptable) frame.stack[frame.localShift + localIndex];
        } else {
            lastCatchScope = null;
        }
        var newCatchScope = ScriptRuntime.newCatchScope(
            caughtException, lastCatchScope, name, cx, frame.scope);

        frame.stack[frame.localShift + localIndex] = newCatchScope;
    }

    @Override
    public int stackChange() {
        return exception.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this,
                "exception",
                exception,
                "name",
                name,
                "localIndex",
                localIndex,
                "scopeIndex",
                scopeIndex);
    }
}
