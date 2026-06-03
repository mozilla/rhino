package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Arrays;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NewLiteralStorage;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Creates an object literal with no spread. Pushes the result object followed by a freshly
 * allocated {@link NewLiteralStorage} pre-populated with the static keys and with all literal-token
 * property values written directly to their slot indices.
 *
 * <p>Literals are side-effect-free and stack-neutral, so out-of-order evaluation is observationally
 * equivalent. Non-literal slots are filled later by {@link LitSetAt} in source order.
 */
public abstract class NewObjectLiteral extends Instruction {
    public static NewObjectLiteral create(
            Object[] keys, Operand[] literalValues, boolean copyKeys) {
        if (keys.length == 0) {
            return NewEmptyObjectLiteral.instance;
        } else {
            return new NewNonEmptyObjectLiteral(keys, literalValues, copyKeys);
        }
    }

    private static final class NewEmptyObjectLiteral extends NewObjectLiteral {
        static NewEmptyObjectLiteral instance = new NewEmptyObjectLiteral();

        @Override
        public void interpret(Context cx, CallFrameV2 frame) {
            frame.pc += 1;

            var obj = cx.newObject(frame.scope);
            var storage = NewLiteralStorage.create(cx, new Object[0]);

            frame.push(obj);
            frame.push(storage);
        }

        @Override
        public int stackChange() {
            return 2;
        }
    }

    private static final class NewNonEmptyObjectLiteral extends NewObjectLiteral {
        /** Full-length keys array, {@code null} at computed-key slots. */
        private final Object[] keys;

        /**
         * Literal-token operands parallel to {@link #keys}, with {@code null} at non-literal slots
         * (computed keys, getter/setter/method, or any expression that emits instructions).
         */
        private final Operand[] literalValues;

        /**
         * Whether {@link #keys} must be cloned per-invocation (set when any computed key exists).
         */
        private final boolean copyKeys;

        NewNonEmptyObjectLiteral(Object[] keys, Operand[] literalValues, boolean copyKeys) {
            this.keys = keys;
            this.literalValues = literalValues;
            this.copyKeys = copyKeys;
        }

        @Override
        public void interpret(Context cx, CallFrameV2 frame) {
            frame.pc += 1;

            var obj = cx.newObject(frame.scope);
            var storage = NewLiteralStorage.create(cx, copyKeys ? keys.clone() : keys);
            for (int i = 0; i < literalValues.length; i++) {
                Operand op = literalValues[i];
                if (op != null) {
                    storage.setValueAt(i, op.retrieveAndWrap(cx, frame));
                }
            }

            frame.push(obj);
            frame.push(storage);
        }

        @Override
        public int stackChange() {
            return 2;
        }

        @Override
        public String toDebugString() {
            Object[] cleanedKeys = Arrays.stream(keys).map(p -> p == null ? "#" : p).toArray();
            return InstructionFormatter.formatInstruction(
                    this,
                    "keys",
                    cleanedKeys,
                    "literalValues",
                    literalValues,
                    "copyKeys",
                    copyKeys);
        }
    }
}
