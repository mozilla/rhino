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

/**
 * {@code new}-expression instruction.
 *
 * <p>Like {@link Call}, the common case is a small, fixed number of arguments. For those we use
 * arity-specialized subclasses ({@link New0}..{@link New3}) that store their argument operands in
 * fields instead of a heap-allocated {@code Operand[]}, saving the array object and its header. 4+
 * argument constructors go through the generic {@link NewN}, which keeps the argument array.
 */
public abstract class New extends Instruction {
    final Operand fun;

    New(Operand fun) {
        this.fun = fun;
    }

    public static New create(Operand fun, Operand[] arguments) {
        switch (arguments.length) {
            case 0:
                return new New0(fun);
            case 1:
                return new New1(fun, arguments[0]);
            case 2:
                return new New2(fun, arguments[0], arguments[1]);
            case 3:
                return new New3(fun, arguments[0], arguments[1], arguments[2]);
            default:
                return new NewN(fun, arguments);
        }
    }

    void countInvocation(Context cx, CallFrameV2 frame) {
        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, INVOCATION_COST);
        }
    }

    /**
     * Shared {@code new} dispatch. {@code args} must already be fully evaluated (the arguments sit
     * above {@link #fun} on the stack, so they are retrieved before the constructor is popped).
     */
    final void doNew(Context cx, CallFrameV2 frame, Object[] args) {
        // TODO(Cam):
        //  Need to implement all the edge cases. Mostly with continuations

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

    /** Shared {@link #toDebugString()} rendering for the subclasses. */
    String formatDebug(Operand[] args) {
        return InstructionFormatter.formatInstruction("New", "fun", fun, "args", args);
    }
}
