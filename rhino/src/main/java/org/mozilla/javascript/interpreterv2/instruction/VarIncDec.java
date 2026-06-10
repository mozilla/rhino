package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import java.math.BigInteger;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.InstructionSimplification;
import org.mozilla.javascript.interpreterv2.KnownType;

public class VarIncDec extends Instruction {
    private final int index;
    private final int incrDecrMask;

    public VarIncDec(int index, int incrDecrMask) {
        this.index = index;
        this.incrDecrMask = incrDecrMask;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {

        if (!frame.useActivation) {
            Object varValue = frame.getVar(index);
            double d = 0.0;
            BigInteger bi = null;
            if (varValue == DOUBLE_MARK) {
                d = frame.getVarDouble(index);
            } else {
                Number num = ScriptRuntime.toNumeric(varValue);
                if (num instanceof BigInteger) {
                    bi = (BigInteger) num;
                } else {
                    d = num.doubleValue();
                }
            }
            if (bi == null) {
                // double
                double d2 = ((incrDecrMask & Node.DECR_FLAG) == 0) ? d + 1.0 : d - 1.0;
                boolean post = ((incrDecrMask & Node.POST_FLAG) != 0);
                if ((frame.getVarAttribute(index) & ScriptableObject.READONLY) == 0) {
                    frame.setVar(index, d2);
                    frame.push(post ? d : d2);
                } else {
                    if (post && varValue != DOUBLE_MARK) {
                        frame.push(varValue);
                    } else {
                        frame.push(post ? d : d2);
                    }
                }
            } else {
                // BigInt
                BigInteger result;
                if ((incrDecrMask & Node.DECR_FLAG) == 0) {
                    result = bi.add(BigInteger.ONE);
                } else {
                    result = bi.subtract(BigInteger.ONE);
                }

                boolean post = ((incrDecrMask & Node.POST_FLAG) != 0);
                if ((frame.getVarAttribute(index) & ScriptableObject.READONLY) == 0) {
                    frame.setVar(index, result);
                    frame.push(post ? bi : result);
                } else {
                    if (post && varValue != DOUBLE_MARK) {
                        frame.push(varValue);
                    } else {
                        frame.push(post ? bi : result);
                    }
                }
            }
        } else {
            String varName = frame.compilerData.argNames[index];
            frame.push(ScriptRuntime.nameIncrDecr(frame.scope, varName, cx, incrDecrMask));
        }

        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public KnownType getKnownType(InstructionSimplification simplifier) {
        return KnownType.NUMBER;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "index", index, "mask", incrDecrMask);
    }
}
