package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class MethodExpression extends Instruction {
    private final int fnIndex;
    private final Operand homeObjectOperand;

    public MethodExpression(int fnIndex, Operand homeObjectOperand) {
        this.fnIndex = fnIndex;
        this.homeObjectOperand = homeObjectOperand;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var desc = frame.fnOrScript.getDescriptor();
        var fdesc = desc.getFunction(fnIndex);
        boolean isArrow = fdesc.getFunctionType() == FunctionNode.ARROW_FUNCTION;
        Scriptable lexicalThis = isArrow ? frame.thisObj : null;
        Scriptable homeObject = (Scriptable) homeObjectOperand.retrieve(cx, frame);

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
