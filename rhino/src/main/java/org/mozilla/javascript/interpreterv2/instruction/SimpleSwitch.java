package org.mozilla.javascript.interpreterv2.instruction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class SimpleSwitch extends JumpInstruction {
    private final Operand valueOperand;
    private final Map<Object, Integer> jumpTable;
    private Object[] testValues;
    private int index = 0;

    public SimpleSwitch(Operand valueOperand, List<Operand> testOperands) {
        this.valueOperand = valueOperand;

        testValues = new Object[testOperands.size()];
        jumpTable = new HashMap<>(testOperands.size());

        for (int i = 0; i < testValues.length; i++) {
            Operand operand = testOperands.get(i);
            if (operand.isDouble(null)) {
                Double d = operand.retrieveDouble(null);
                // Normalize negative zero to positive zero for switch comparison
                testValues[i] = normalizeZero(d);
            } else {
                Object value = operand.retrieve(null, null);
                if (value instanceof Number) {
                    // We are storing all numbers as doubles to make
                    // sure they always match
                    Double d = ((Number) value).doubleValue();
                    // Normalize negative zero to positive zero for switch comparison
                    testValues[i] = normalizeZero(d);
                } else {
                    assert value instanceof Boolean || value instanceof String;
                    testValues[i] = value;
                }
            }
        }
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        Object value;
        if (valueOperand.isDouble(frame)) {
            Double d = valueOperand.retrieveDouble(frame);
            // Normalize negative zero to positive zero for switch comparison
            value = normalizeZero(d);
        } else {
            Object rawValue = valueOperand.retrieve(cx, frame);
            if (rawValue instanceof Number) {
                // We store the number as double, so retrieve it as a double
                Double d = ((Number) rawValue).doubleValue();
                // Normalize negative zero to positive zero for switch comparison
                value = normalizeZero(d);
            } else if (rawValue instanceof ConsString) {
                // In our table we'll have actual String, which wouldn't match!
                value = rawValue.toString();
            } else {
                value = rawValue;
            }
        }
        valueOperand.cleanup(frame);

        frame.pc += jumpTable.getOrDefault(value, offset);
    }

    @Override
    public int stackChange() {
        return valueOperand.stackChange();
    }

    @Override
    public void setOffset(int offset) {
        if (index == testValues.length) {
            testValues = null;
            this.offset = offset;
        } else {
            // If there is a duplicate label, javascript
            // prefers the first one, so we won't insert it
            jumpTable.putIfAbsent(testValues[index], offset);
            index += 1;
        }
    }

    private static Double normalizeZero(Double d) {
        // JavaScript treats -0.0 and 0.0 as equal in switch statements
        // Convert -0.0 to 0.0 for consistent HashMap lookup
        if (d == -0.0) {
            return 0.0;
        }
        return d;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "jumpTable", jumpTable, "offset", InstructionFormatter.formatOffset(offset));
    }

    @Override
    public Set<Integer> getTargets(int fromPC) {
        Set<Integer> targets = new HashSet<>();
        // Add all case jump targets
        for (int caseOffset : jumpTable.values()) {
            targets.add(fromPC + caseOffset);
        }
        // Add the default jump target
        targets.add(fromPC + offset);
        return targets;
    }
}
