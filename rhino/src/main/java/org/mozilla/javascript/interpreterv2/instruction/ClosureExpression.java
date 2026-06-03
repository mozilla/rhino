package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class ClosureExpression extends Instruction {
    private final int fnIndex;

    public ClosureExpression(int fnIndex) {
        this.fnIndex = fnIndex;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var desc = frame.fnOrScript.getDescriptor();
        var fdesc = desc.getFunction(fnIndex);
        boolean isArrow = fdesc.getFunctionType() == FunctionNode.ARROW_FUNCTION;
        Scriptable lexicalThis = isArrow ? frame.thisObj : null;
        var homeObject = isArrow ? frame.fnOrScript.getHomeObject() : null;

        JSFunction fn = new JSFunction(cx, frame.scope, fdesc, lexicalThis, homeObject);
        frame.push(fn);
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "fnIndex", fnIndex);
    }
}
