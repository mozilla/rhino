package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.INVOCATION_COST;
import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.interpreterv2.CompilerData;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class New extends Instruction {
    private final Operand fun;
    private final Operand[] arguments;

    public New(Operand fun, Operand[] arguments) {
        this.fun = fun;
        this.arguments = arguments;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, INVOCATION_COST);
        }

        // TODO(Cam):
        //  Need to implement all the edge cases. Mostly with continuations

        Object[] args = frame.getArguments(cx, arguments);
        var lhs = fun.retrieveAndWrap(cx, frame);

        if (lhs instanceof JSFunction
                && ((JSFunction) lhs).getDescriptor().getCode() instanceof CompilerData) {
            JSFunction f = (JSFunction) lhs;
            if (frame.fnOrScript.getDescriptor().getSecurityDomain()
                    == f.getDescriptor().getSecurityDomain()) {
                if (cx.getLanguageVersion() >= Context.VERSION_ES6 && f.getHomeObject() != null) {
                    // Only methods have home objects associated with them
                    throw ScriptRuntime.typeErrorById("msg.not.ctor", f.getFunctionName());
                }
                if (f.getDescriptor().getConstructor() == null) {
                    // Arrow functions and generators are not constructors
                    throw ScriptRuntime.typeErrorById("msg.not.ctor", f.getFunctionName());
                }

                Object result = f.construct(cx, frame.scope, args);
                frame.push(result);
                return;
            }
        }

        VarScope frameScope = frame.scope;
        if (!(lhs instanceof Constructable)) {
            throw ScriptRuntime.notFunctionError(lhs);
        }
        Constructable ctor = (Constructable) lhs;

        Scriptable newInstance = ctor.construct(cx, frameScope, args);

        frame.push(newInstance);
    }

    @Override
    public int stackChange() {
        int count = 0;
        for (var argument : arguments) {
            count += argument.stackChange();
        }
        return 1 + fun.stackChange() + count;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "fun", fun, "args", arguments);
    }
}
