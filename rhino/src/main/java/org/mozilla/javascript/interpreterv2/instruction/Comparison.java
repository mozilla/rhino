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
        if (lhs == DOUBLE_MARK && rhs == DOUBLE_MARK) {
            valBln = ScriptRuntime.compareTo(lDouble, rDouble, op);
        } else {
            valBln = compareObjs(rhs, rDouble, lhs, lDouble);
        }
        frame.push(ScriptRuntime.wrapBoolean(valBln));
        frame.pc += 1;
    }

    private boolean compareObjs(Object rhs, double rDouble, Object lhs, double lDouble) {
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

            valBln =
                    switch (op) {
                        case Token.GE -> ScriptRuntime.compare(lhs, rhs, op);
                        case Token.GT -> ScriptRuntime.compare(lhs, rhs, op);
                        case Token.LT, Token.LE -> ScriptRuntime.compare(lhs, rhs, op);
                        default -> {
                            throw Kit.codeBug();
                        }
                    };
        }
        return valBln;
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
