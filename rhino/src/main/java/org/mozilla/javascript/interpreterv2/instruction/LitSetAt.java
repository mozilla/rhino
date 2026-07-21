package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NewLiteralStorage;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;
import org.mozilla.javascript.interpreterv2.operand.PopOperand;

/**
 * Writes a key and/or value (or getter/setter) to a known slot of the {@link NewLiteralStorage} on
 * top of the stack. Used in the no-spread path where every slot index is known at compile time.
 *
 * <p>{@code keyOp == null} means the static key was already pre-populated by {@link
 * NewObjectLiteral}; otherwise {@code keyOp} produces the runtime computed-property key that must
 * overwrite the {@code null} placeholder at {@code keys[slotIdx]}.
 *
 * <p>{@code valueOp == null} means the literal value was already pre-populated by {@link
 * NewObjectLiteral} (only possible together with {@code keyOp != null}, i.e. computed key with a
 * literal-token value); otherwise {@code valueOp} produces the runtime value.
 *
 * @see LitPush
 */
public class LitSetAt extends Instruction {
    private static final LitSetAt[] CACHE = new LitSetAt[16];

    static {
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new LitSetAt(i, null, PopOperand.instance, 0);
        }
    }

    public static LitSetAt create(int slotIdx, Operand keyOp, Operand valueOp, int kind) {
        if ((slotIdx >= 0 && slotIdx < CACHE.length)
                && keyOp == null
                && valueOp == PopOperand.instance
                && kind == 0) {
            return CACHE[slotIdx];
        }
        return new LitSetAt(slotIdx, keyOp, valueOp, kind);
    }

    private final int slotIdx;
    private final Operand keyOp;
    private final Operand valueOp;
    private final int kind;

    private LitSetAt(int slotIdx, Operand keyOp, Operand valueOp, int kind) {
        assert keyOp != null || valueOp != null : "LitSetAt with both ops null is a no-op";
        this.slotIdx = slotIdx;
        this.keyOp = keyOp;
        this.valueOp = valueOp;
        this.kind = kind;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        Object value = valueOp != null ? valueOp.retrieveAndWrap(cx, frame) : null;
        Object key = keyOp != null ? keyOp.retrieveAndWrap(cx, frame) : null;
        var storage = (NewLiteralStorage) frame.peek(0);
        if (keyOp != null) {
            storage.setKeyAt(slotIdx, key);
        }
        if (valueOp != null) {
            switch (kind) {
                case -1:
                    storage.setGetterAt(slotIdx, value);
                    break;
                case 1:
                    storage.setSetterAt(slotIdx, value);
                    break;
                default:
                    storage.setValueAt(slotIdx, value);
                    break;
            }
        }
    }

    @Override
    public int stackChange() {
        int sc = 0;
        if (valueOp != null) {
            sc += valueOp.stackChange();
        }
        if (keyOp != null) {
            sc += keyOp.stackChange();
        }
        return sc;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "slot", slotIdx, "key", keyOp, "value", valueOp, "kind", kind);
    }
}
