package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import java.math.BigInteger;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.InstructionSimplification;
import org.mozilla.javascript.interpreterv2.KnownType;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class Add extends Instruction {
    private final Operand lhs;
    private final Operand rhs;

    public Add(Operand lhs, Operand rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        doAdd(cx, frame);
    }

    @Override
    public int stackChange() {
        return 1 + lhs.stackChange() + rhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "lhs", lhs, "rhs", rhs);
    }

    private void doAdd(Context cx, CallFrameV2 frame) {
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

        double resultDouble;
        boolean leftRightOrder;
        if (rhs == DOUBLE_MARK) {
            resultDouble = rDouble;
            if (lhs == DOUBLE_MARK) {
                frame.push(lDouble + resultDouble);
                return;
            }
            leftRightOrder = true;
            // fallthrough to object + number code
        } else if (lhs == DOUBLE_MARK) {
            resultDouble = lDouble;
            lhs = rhs;
            leftRightOrder = false;
            // fallthrough to object + number code
        } else {
            if (lhs instanceof Scriptable || rhs instanceof Scriptable) {
                frame.push(ScriptRuntime.add(lhs, rhs, cx));

                // the next two else if branches are a bit more tricky
                // to reduce method calls
            } else if (lhs instanceof CharSequence) {
                if (rhs instanceof CharSequence) {
                    frame.push(new ConsString((CharSequence) lhs, (CharSequence) rhs));
                } else {
                    frame.push(
                            new ConsString((CharSequence) lhs, ScriptRuntime.toCharSequence(rhs)));
                }
            } else if (rhs instanceof CharSequence) {
                frame.push(new ConsString(ScriptRuntime.toCharSequence(lhs), (CharSequence) rhs));
            } else {
                Number lNum = (lhs instanceof Number) ? (Number) lhs : ScriptRuntime.toNumeric(lhs);
                Number rNum = (rhs instanceof Number) ? (Number) rhs : ScriptRuntime.toNumeric(rhs);

                if (lNum instanceof BigInteger && rNum instanceof BigInteger) {
                    frame.push(((BigInteger) lNum).add((BigInteger) rNum));
                } else if (lNum instanceof BigInteger || rNum instanceof BigInteger) {
                    throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
                } else {
                    frame.push(lNum.doubleValue() + rNum.doubleValue());
                }
            }
            return;
        }

        // handle object(lhs) + number(d) code
        if (lhs instanceof Scriptable) {
            rhs = ScriptRuntime.wrapNumber(resultDouble);
            if (!leftRightOrder) {
                Object tmp = lhs;
                lhs = rhs;
                rhs = tmp;
            }
            frame.push(ScriptRuntime.add(lhs, rhs, cx));
        } else if (lhs instanceof CharSequence) {
            CharSequence rstr = ScriptRuntime.numberToString(resultDouble, 10);
            if (leftRightOrder) {
                frame.push(new ConsString((CharSequence) lhs, rstr));
            } else {
                frame.push(new ConsString(rstr, (CharSequence) lhs));
            }
        } else {
            Number lNum = (lhs instanceof Number) ? (Number) lhs : ScriptRuntime.toNumeric(lhs);
            if (lNum instanceof BigInteger) {
                throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
            } else {
                frame.push(lNum.doubleValue() + resultDouble);
            }
        }
    }

    @Override
    public Instruction simplify(InstructionSimplification simplifier) {
        KnownType rhsType = rhs.getKnownType(simplifier);
        KnownType lhsType = lhs.getKnownType(simplifier);

        if (lhsType == KnownType.STRING || rhsType == KnownType.STRING) {
            if (lhsType == KnownType.STRING && rhsType == KnownType.STRING) {
                return new StringStringAdd(lhs, rhs);
            } else if (lhsType == KnownType.STRING) {
                return new StringAnyAdd(lhs, rhs);
            } else {
                return new AnyStringAdd(lhs, rhs);
            }
        }

        return this;
    }

    @Override
    public KnownType getKnownType(InstructionSimplification simplifier) {
        KnownType rhsType = rhs.getKnownType(simplifier);
        KnownType lhsType = lhs.getKnownType(simplifier);

        if (lhsType == KnownType.STRING || rhsType == KnownType.STRING) {
            return KnownType.STRING;
        }
        if (lhsType == KnownType.NUMBER && rhsType == KnownType.NUMBER) {
            return KnownType.NUMBER;
        }
        return KnownType.UNKNOWN;
    }
}
