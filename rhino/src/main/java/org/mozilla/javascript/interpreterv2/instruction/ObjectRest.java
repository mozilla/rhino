package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Implements the {@code {...rest}} object rest operation in destructuring. Pops the source object
 * and any computed key operands, builds a new object containing all own enumerable properties of
 * the source not in the excluded key set, and pushes the result.
 */
public class ObjectRest extends Instruction {
    private final Operand source;
    private final Object[] staticKeys;
    private final Operand[] computedKeys;

    public ObjectRest(Operand source, Object[] staticKeys, Operand[] computedKeys) {
        this.source = source;
        this.staticKeys = staticKeys;
        this.computedKeys = computedKeys;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        int totalPops = source.stackChange();
        for (var k : computedKeys) {
            totalPops += k.stackChange();
        }

        // Stack order (bottom -> top): source, computedKeys[0], ..., computedKeys[N-1].
        // Following the ObjectLitWithSpread pattern: read deepest-first by starting at
        // totalPops and stepping closer to 0.
        int pops = totalPops;
        Object src = source.viewValue(cx, frame, pops);
        pops -= source.stackChange();

        Object[] excludeKeys = new Object[staticKeys.length + computedKeys.length];
        for (int i = 0; i < staticKeys.length; i++) {
            excludeKeys[i] = staticKeys[i];
        }
        for (int i = 0; i < computedKeys.length; i++) {
            Object key = computedKeys[i].viewValue(cx, frame, pops);
            pops -= computedKeys[i].stackChange();
            excludeKeys[staticKeys.length + i] =
                    key instanceof Symbol ? key : ScriptRuntime.toString(key);
        }

        frame.popN(-totalPops);

        Scriptable result = ScriptRuntime.doObjectRest(cx, frame.scope, src, excludeKeys);
        frame.push(result);
    }

    @Override
    public int stackChange() {
        int change = 1 + source.stackChange();
        for (var k : computedKeys) {
            change += k.stackChange();
        }
        return change;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "source", source, "staticKeys", staticKeys, "computedKeys", computedKeys);
    }
}
