package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class Comparison extends Instruction {
    private final Operand lhs;
    private final int op;
    private final Operand rhs;

    public Comparison(Operand lhs, int op, Operand rhs) {
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        Object rhs;
        double rDouble = 0.0;
        if (this.rhs.isDouble(frame)) {
            rDouble = this.rhs.retrieveDouble(frame);
            rhs = DOUBLE_MARK;
        } else {
            rhs = this.rhs.retrieve(cx, frame);
        }
        Object lhs;
        double lDouble = 0.0;
        if (this.lhs.isDouble(frame)) {
            lDouble = this.lhs.retrieveDouble(frame);
            lhs = DOUBLE_MARK;
        } else {
            lhs = this.lhs.retrieve(cx, frame);
        }

        boolean valBln;
        object_compare:
        {
            number_compare:
            {
                Number rNum, lNum;
                if (rhs == DOUBLE_MARK) {
                    rNum = rDouble;
                    if (lhs == DOUBLE_MARK) {
                        lNum = lDouble;
                    } else {
                        lNum = ScriptRuntime.toNumeric(lhs);
                    }
                } else if (lhs == DOUBLE_MARK) {
                    rNum = ScriptRuntime.toNumeric(rhs);
                    lNum = lDouble;
                } else {
                    break number_compare;
                }
                valBln = ScriptRuntime.compare(lNum, rNum, op);
                break object_compare;
            }

            switch (op) {
                case Token.GE:
                    valBln = ScriptRuntime.compare(lhs, rhs, op);
                    break;
                case Token.GT:
                    valBln = ScriptRuntime.compare(lhs, rhs, op);
                    break;
                case Token.LT:
                case Token.LE:
                    valBln = ScriptRuntime.compare(lhs, rhs, op);
                    break;
                default:
                    throw Kit.codeBug();
            }
        }
        frame.push(ScriptRuntime.wrapBoolean(valBln));
    }

    @Override
    public int stackChange() {
        return 1 + lhs.stackChange() + rhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "op", Token.typeToName(op), "lhs", lhs, "rhs", rhs);
    }
}
