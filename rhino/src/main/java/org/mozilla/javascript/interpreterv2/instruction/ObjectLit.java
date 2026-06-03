package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NewLiteralStorage;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;
import org.mozilla.javascript.interpreterv2.operand.PeekOperand;

/**
 * Finalizes an object literal: pops the {@link NewLiteralStorage} from the stack and applies its
 * accumulated keys / values / getter-setter markers onto the object produced by {@link
 * NewObjectLiteral} via {@link ScriptRuntime#fillObjectLiteral}.
 */
public class ObjectLit extends Instruction {
    public static final ObjectLit ofPeekOperand = new ObjectLit();

    private ObjectLit() {}

    private static final Operand operand = PeekOperand.instance;

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        var storage = (NewLiteralStorage) frame.pop();
        Object[] keys = storage.getKeys();
        Object[] values = storage.getValues();
        int[] getterSetters = storage.getGetterSetters();
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && keys != null) {
            for (int i = 0; i < keys.length; i++) {
                int gs = getterSetters == null ? 0 : getterSetters[i];
                NewLiteralStorage.inferFunctionName(keys[i], values[i], gs);
            }
        }
        var object = (Scriptable) operand.retrieve(cx, frame);
        ScriptRuntime.fillObjectLiteral(object, keys, values, getterSetters, cx, frame.scope);
    }

    @Override
    public int stackChange() {
        return -1 + operand.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "object", operand);
    }
}
