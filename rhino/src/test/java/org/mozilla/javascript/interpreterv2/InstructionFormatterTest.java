package org.mozilla.javascript.interpreterv2;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.instruction.Instruction;
import org.mozilla.javascript.interpreterv2.operand.Operand;

class InstructionFormatterTest {

    private static class FakeInstruction extends Instruction {
        @Override
        public void interpret(Context cx, CallFrameV2 frame) {}

        @Override
        public int stackChange() {
            return 0;
        }
    }

    private static class AnotherFakeInstruction extends Instruction {
        @Override
        public void interpret(Context cx, CallFrameV2 frame) {}

        @Override
        public int stackChange() {
            return 0;
        }
    }

    private static class FakeOperand extends Operand {
        private final String debugString;

        FakeOperand(String debugString) {
            this.debugString = debugString;
        }

        @Override
        public Object retrieve(Context cx, CallFrameV2 frame) {
            return null;
        }

        @Override
        public double retrieveDouble(CallFrameV2 frame) {
            return 0;
        }

        @Override
        public boolean isDouble(CallFrameV2 frame) {
            return false;
        }

        @Override
        public void appendDebugString(StringBuilder sb) {
            sb.append(debugString);
        }
    }

    @Test
    void formatTargetPositive() {
        assertEquals("%7", InstructionFormatter.formatTarget(7));
    }

    @Test
    void formatTargetZero() {
        assertEquals("%0", InstructionFormatter.formatTarget(0));
    }

    @Test
    void formatTargetNegative() {
        assertEquals("%-3", InstructionFormatter.formatTarget(-3));
    }

    @Test
    void formatOffsetPositive() {
        assertEquals("+5", InstructionFormatter.formatOffset(5));
    }

    @Test
    void formatOffsetZeroIsPositive() {
        assertEquals("+0", InstructionFormatter.formatOffset(0));
    }

    @Test
    void formatOffsetNegative() {
        assertEquals("-3", InstructionFormatter.formatOffset(-3));
    }

    @Test
    void formatInstructionUsesConcreteClassSimpleName() {
        assertEquals(
                "FakeInstruction", InstructionFormatter.formatInstruction(new FakeInstruction()));
        assertEquals(
                "AnotherFakeInstruction",
                InstructionFormatter.formatInstruction(new AnotherFakeInstruction()));
    }

    @Test
    void formatInstructionInstructionVariantWithOperands() {
        assertEquals(
                "FakeInstruction(count=1)",
                InstructionFormatter.formatInstruction(new FakeInstruction(), "count", 1));
    }

    @Test
    void formatInstructionWithNoOperands() {
        assertEquals("Op", InstructionFormatter.formatInstruction("Op"));
    }

    @Test
    void formatInstructionWithOperands() {
        assertEquals(
                "Call(args=2, receiver=true)",
                InstructionFormatter.formatInstruction("Call", "args", 2, "receiver", true));
    }

    @Test
    void formatInstructionWithMultipleKeyValuePairs() {
        assertEquals(
                "Call(args=2, receiver=true, name=\"foo\")",
                InstructionFormatter.formatInstruction(
                        "Call", "args", 2, "receiver", true, "name", "foo"));
    }

    @Test
    void formatInstructionOddOperandsThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> InstructionFormatter.formatInstruction("Op", "key"));
    }

    @Test
    void formatInstructionWithNullOperandValue() {
        assertEquals("Op(value=null)", InstructionFormatter.formatInstruction("Op", "value", null));
    }

    @Test
    void formatInstructionWithStringOperandEscapesAndQuotes() {
        assertEquals(
                "Op(name=\"line1\\nline2\")",
                InstructionFormatter.formatInstruction("Op", "name", "line1\nline2"));
    }

    @Test
    void formatInstructionWithBigIntegerOperandAppendsNSuffix() {
        assertEquals(
                "Op(value=42n)",
                InstructionFormatter.formatInstruction("Op", "value", BigInteger.valueOf(42)));
    }

    @Test
    void formatInstructionWithBooleanArrayOperand() {
        assertEquals(
                "Op(flags=[true, false, true])",
                InstructionFormatter.formatInstruction(
                        "Op", "flags", new boolean[] {true, false, true}));
    }

    @Test
    void formatInstructionWithObjectArrayOperand() {
        assertEquals(
                "Op(values=[1, 2, 3])",
                InstructionFormatter.formatInstruction("Op", "values", new Object[] {1, 2, 3}));
    }

    @Test
    void formatInstructionWithNestedObjectArrayOperand() {
        assertEquals(
                "Op(values=[1, [2, 3]])",
                InstructionFormatter.formatInstruction(
                        "Op", "values", new Object[] {1, new Object[] {2, 3}}));
    }

    @Test
    void formatInstructionWithOperandValueDelegatesToAppendDebugString() {
        assertEquals(
                "Op(reg=%3)",
                InstructionFormatter.formatInstruction("Op", "reg", new FakeOperand("%3")));
    }

    @Test
    void appendStringQuotesPlainString() {
        StringBuilder sb = new StringBuilder();
        InstructionFormatter.appendString(sb, "hello");
        assertEquals("\"hello\"", sb.toString());
    }

    @Test
    void appendStringEscapesBackslash() {
        StringBuilder sb = new StringBuilder();
        InstructionFormatter.appendString(sb, "a\\b");
        assertEquals("\"a\\\\b\"", sb.toString());
    }

    @Test
    void appendStringEscapesQuote() {
        StringBuilder sb = new StringBuilder();
        InstructionFormatter.appendString(sb, "a\"b");
        assertEquals("\"a\\\"b\"", sb.toString());
    }

    @Test
    void appendStringEscapesNewlineTabAndCarriageReturn() {
        StringBuilder sb = new StringBuilder();
        InstructionFormatter.appendString(sb, "a\nb\tc\rd");
        assertEquals("\"a\\nb\\tc\\rd\"", sb.toString());
    }
}
