package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NewLiteralStorage;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Appends one entry to the {@link NewLiteralStorage} currently on top of the stack. The storage's
 * sequential {@code index} is advanced by the value push.
 *
 * <p>When {@code keyOp == null}, the key is assumed to already be present at {@code
 * storage.keys[index]} (pre-populated by {@link NewObjectLiteral}).
 */
public class LitPush extends Instruction {
    private final Operand keyOp;
    private final Operand valueOp;
    private final int kind;

    public LitPush(Operand keyOp, Operand valueOp, int kind) {
        this.keyOp = keyOp;
        this.valueOp = valueOp;
        this.kind = kind;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        // Pop value (top of stack), then key
        Object value = valueOp.retrieveAndWrap(cx, frame);
        Object key = keyOp != null ? keyOp.retrieveAndWrap(cx, frame) : null;
        var storage = (NewLiteralStorage) frame.peek(0);
        if (keyOp != null) {
            storage.pushKey(key);
        }
        switch (kind) {
            case -1:
                storage.pushGetter(value);
                break;
            case 1:
                storage.pushSetter(value);
                break;
            default:
                storage.pushValue(value);
                break;
        }
    }

    @Override
    public int stackChange() {
        int sc = valueOp.stackChange();
        if (keyOp != null) {
            sc += keyOp.stackChange();
        }
        return sc;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "key", keyOp, "value", valueOp, "kind", kind);
    }
}
