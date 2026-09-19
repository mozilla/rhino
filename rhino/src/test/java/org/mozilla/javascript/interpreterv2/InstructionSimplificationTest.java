package org.mozilla.javascript.interpreterv2;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.interpreterv2.instruction.Add;
import org.mozilla.javascript.interpreterv2.instruction.AnyStringAdd;
import org.mozilla.javascript.interpreterv2.instruction.Equal;
import org.mozilla.javascript.interpreterv2.instruction.Goto;
import org.mozilla.javascript.interpreterv2.instruction.IfNe;
import org.mozilla.javascript.interpreterv2.instruction.Instruction;
import org.mozilla.javascript.interpreterv2.instruction.Name;
import org.mozilla.javascript.interpreterv2.instruction.PushConstant;
import org.mozilla.javascript.interpreterv2.instruction.SimpleSwitch;
import org.mozilla.javascript.interpreterv2.instruction.StringAnyAdd;
import org.mozilla.javascript.interpreterv2.instruction.StringStringAdd;
import org.mozilla.javascript.interpreterv2.operand.IntOperand;
import org.mozilla.javascript.interpreterv2.operand.PopOperand;
import org.mozilla.javascript.interpreterv2.operand.StringOperand;

/**
 * Unit tests for InstructionSimplification to ensure it correctly handles jump targets and doesn't
 * optimize across control flow boundaries where it shouldn't.
 */
public class InstructionSimplificationTest {

    @Test
    public void testSimplificationDoesNotCrossJumpTargets() {
        // Create a sequence like:
        // 0: PushConstant("hello")
        // 1: Goto -> 3
        // 2: PushConstant("world")
        // 3: [jump target] Add (string concat)

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new PushConstant("hello")); // 0

        Goto gotoInst = new Goto();
        gotoInst.setOffset(2); // Jump forward by 2
        instructions.add(gotoInst); // 1

        instructions.add(new PushConstant("world")); // 2
        instructions.add(new Add(PopOperand.instance, new StringOperand(", "))); // 3

        Set<Integer> jumpTargets = new HashSet<>();
        jumpTargets.add(3); // Instruction 3 is a jump target

        InstructionSimplification simplifier =
                new InstructionSimplification(instructions, jumpTargets);
        simplifier.simplify();

        // The Add at position 3 is simplified to AnyStringAdd (not StringStringAdd)
        // because it's a jump target and we don't know what's on the stack
        Instruction simplified = instructions.get(3);
        String actualType = simplified.getClass().getSimpleName();
        assertTrue(simplified instanceof AnyStringAdd, "Expected AnyStringAdd got " + actualType);
    }

    @Test
    public void testSimplificationWorksWithoutJumpTargets() {
        // Create a sequence without jumps:
        // 0: PushConstant("hello")
        // 1: Add with string literal

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new PushConstant("hello")); // 0
        instructions.add(new Add(PopOperand.instance, new StringOperand(", "))); // 1

        Set<Integer> jumpTargets = new HashSet<>();
        // No jump targets

        InstructionSimplification simplifier =
                new InstructionSimplification(instructions, jumpTargets);
        simplifier.simplify();

        // The Add should be simplified to StringStringAdd
        // because we know the stack has a string and we're adding another string
        Instruction simplified = instructions.get(1);
        String actualType = simplified.getClass().getSimpleName();
        assertTrue(
                simplified instanceof StringStringAdd,
                "Expected StringStringAdd got " + actualType);
    }

    @Test
    public void testSimplificationStopsAtJumpTarget() {
        // Create a sequence like:
        // 0: PushConstant("start")
        // 1: [jump target] PushConstant(" middle")
        // 2: Add

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new PushConstant("start")); // 0
        instructions.add(new PushConstant(" middle")); // 1
        instructions.add(new Add(PopOperand.instance, PopOperand.instance)); // 2

        Set<Integer> jumpTargets = new HashSet<>();
        jumpTargets.add(1); // Instruction 1 is a jump target

        InstructionSimplification simplifier =
                new InstructionSimplification(instructions, jumpTargets);
        simplifier.simplify();

        // The Add at position 2 is simplified to AnyStringAdd
        // because instruction 1 is a jump target, so we don't know what's on
        // the stack before instruction 1 (only that instruction 1 itself produces a string)
        Instruction simplified = instructions.get(2);
        String actualType = simplified.getClass().getSimpleName();
        assertTrue(simplified instanceof AnyStringAdd, "Expected AnyStringAdd got " + actualType);
    }

    @Test
    public void testSimplificationWithConditionalJump() {
        // Create a pattern like a ternary operator:
        // 0: PushConstant(true)
        // 1: IfNe -> 4
        // 2: PushConstant("yes")
        // 3: Goto -> 5
        // 4: [jump target] PushConstant("no")
        // 5: [jump target] Add (with string suffix)

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new PushConstant(true)); // 0

        IfNe ifNe = new IfNe(PopOperand.instance);
        ifNe.setOffset(3); // Jump to instruction 4
        instructions.add(ifNe); // 1

        instructions.add(new PushConstant("yes")); // 2

        Goto gotoEnd = new Goto();
        gotoEnd.setOffset(2); // Jump to instruction 5
        instructions.add(gotoEnd); // 3

        instructions.add(new PushConstant("no")); // 4
        instructions.add(new Add(PopOperand.instance, new StringOperand(" suffix"))); // 5

        Set<Integer> jumpTargets = new HashSet<>();
        jumpTargets.add(4); // Instruction 4 is a jump target
        jumpTargets.add(5); // Instruction 5 is a jump target

        InstructionSimplification simplifier =
                new InstructionSimplification(instructions, jumpTargets);
        simplifier.simplify();

        // The Add at position 5 is simplified to AnyStringAdd (not StringStringAdd)
        // because it's a jump target from multiple places with potentially different types
        Instruction simplified = instructions.get(5);
        String actualType = simplified.getClass().getSimpleName();
        // Since position 5 is a jump target, we know one operand is a string literal
        // but can't be sure about the stack state, so it becomes AnyStringAdd
        assertTrue(simplified instanceof AnyStringAdd, "Expected AnyStringAdd got " + actualType);
    }

    @Test
    public void testSimplificationWithLoop() {
        // Create a loop-like pattern:
        // 0: [jump target] PushConstant("")
        // 1: PushConstant("x")
        // 2: Add
        // 3: Goto -> 0

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new PushConstant("")); // 0
        instructions.add(new PushConstant("x")); // 1
        instructions.add(new Add(PopOperand.instance, PopOperand.instance)); // 2

        Goto gotoLoop = new Goto();
        gotoLoop.setOffset(-3); // Jump back to instruction 0
        instructions.add(gotoLoop); // 3

        Set<Integer> jumpTargets = new HashSet<>();
        jumpTargets.add(0); // Loop start

        InstructionSimplification simplifier =
                new InstructionSimplification(instructions, jumpTargets);
        simplifier.simplify();

        // The Add at position 2 should be simplified to StringStringAdd
        // because both previous instructions produce strings
        Instruction simplified = instructions.get(2);
        String actualType = simplified.getClass().getSimpleName();
        assertTrue(
                simplified instanceof StringStringAdd,
                "Expected StringStringAdd got " + actualType);
    }

    @Test
    public void testSimplificationAcrossMultipleInstructions() {
        // Test that simplification can look back multiple instructions
        // 0: PushConstant("a")
        // 1: PushConstant("b")
        // 2: StringStringAdd
        // 3: Add with another string

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new PushConstant("a")); // 0
        instructions.add(new PushConstant("b")); // 1
        instructions.add(new Add(PopOperand.instance, PopOperand.instance)); // 2
        instructions.add(new Add(PopOperand.instance, new StringOperand("c"))); // 3

        Set<Integer> jumpTargets = new HashSet<>();
        // No jump targets

        InstructionSimplification simplifier =
                new InstructionSimplification(instructions, jumpTargets);
        simplifier.simplify();

        // The Add should be simplified to StringStringAdd because StringStringAdd produces a string
        // and we're adding another string constant
        Instruction simplified = instructions.get(3);
        String actualType = simplified.getClass().getSimpleName();
        assertTrue(
                simplified instanceof StringStringAdd,
                "Expected StringStringAdd got " + actualType);
    }

    @Test
    public void testComplexConditionalStringAddPattern() {
        // Test the specific pattern that was failing:
        // Conditional that produces either a number or a string concatenation
        // args += (i == 2) ? i : i + ', '
        //
        // This becomes something like:
        // 0: Name("args")
        // 1: Name("i")
        // 2: Equal(2)
        // 3: IfNe -> 6
        // 4: Name("i")
        // 5: Goto -> 8
        // 6: [jump target] Name("i")
        // 7: Add(", ")
        // 8: [jump target] Add (final concat)

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new Name("args")); // 0
        instructions.add(new Name("i")); // 1
        instructions.add(new Equal(new IntOperand(2), PopOperand.instance)); // 2

        IfNe ifNe = new IfNe(PopOperand.instance);
        ifNe.setOffset(3); // Jump to 6
        instructions.add(ifNe); // 3

        instructions.add(new Name("i")); // 4

        Goto gotoEnd = new Goto();
        gotoEnd.setOffset(3); // Jump to 8
        instructions.add(gotoEnd); // 5

        instructions.add(new Name("i")); // 6
        instructions.add(new Add(PopOperand.instance, new StringOperand(", "))); // 7
        instructions.add(new Add(PopOperand.instance, PopOperand.instance)); // 8

        Set<Integer> jumpTargets = new HashSet<>();
        jumpTargets.add(6); // else branch
        jumpTargets.add(8); // merge point after conditional

        InstructionSimplification simplifier =
                new InstructionSimplification(instructions, jumpTargets);
        simplifier.simplify();

        // The Add at position 8 remains as generic Add (not simplified)
        // because it's a jump target that could receive different types from different branches
        Instruction simplified = instructions.get(8);
        String actualType = simplified.getClass().getSimpleName();
        assertTrue(simplified instanceof Add, "Expected Add got " + actualType);
    }

    @Test
    public void testStringAnyAddSimplification() {
        // Create a sequence where we have a string literal operand and unknown stack due to jump:
        // 0: PushConstant(123) - number value
        // 1: Goto -> 3
        // 2: PushConstant("world") - string value
        // 3: [jump target] Add(StringOperand("prefix"), PopOperand) - StringAnyAdd

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new PushConstant(123)); // 0 - number

        Goto gotoInst = new Goto();
        gotoInst.setOffset(2); // Jump to instruction 3
        instructions.add(gotoInst); // 1

        instructions.add(new PushConstant("world")); // 2 - string
        instructions.add(new Add(new StringOperand("prefix"), PopOperand.instance)); // 3

        Set<Integer> jumpTargets = new HashSet<>();
        jumpTargets.add(3); // Instruction 3 is a jump target

        InstructionSimplification simplifier =
                new InstructionSimplification(instructions, jumpTargets);
        simplifier.simplify();

        // The Add should be simplified to StringAnyAdd
        // because lhs is StringOperand (known string) and rhs is PopOperand (unknown due to jump
        // target)
        Instruction simplified = instructions.get(3);
        String actualType = simplified.getClass().getSimpleName();
        assertTrue(simplified instanceof StringAnyAdd, "Expected StringAnyAdd got " + actualType);
    }

    @Test
    public void testJumpBackwardDoesNotConfuseSimplification() {
        // Test that backward jumps (like in loops) don't break simplification
        // 0: PushConstant("")
        // 1: [jump target] PushConstant("x")
        // 2: Add
        // 3: IfNe -> 1  // Jump back to 1

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new PushConstant("")); // 0
        instructions.add(new PushConstant("x")); // 1
        instructions.add(new Add(PopOperand.instance, PopOperand.instance)); // 2

        IfNe ifLoop = new IfNe(PopOperand.instance);
        ifLoop.setOffset(-2); // Jump back to 1
        instructions.add(ifLoop); // 3

        Set<Integer> jumpTargets = new HashSet<>();
        jumpTargets.add(1); // Loop target

        InstructionSimplification simplifier =
                new InstructionSimplification(instructions, jumpTargets);
        simplifier.simplify();

        // The Add is simplified to AnyStringAdd because instruction 1 is a jump target,
        // so we can't know the full stack state (what was there before the jump)
        Instruction simplified = instructions.get(2);
        String actualType = simplified.getClass().getSimpleName();
        assertTrue(simplified instanceof AnyStringAdd, "Expected AnyStringAdd got " + actualType);
    }

    @Test
    public void testSimpleSwitchJumpTargetsAreTracked() {
        // Test that SimpleSwitch properly tracks all its jump targets for simplification
        // Create a switch-like pattern:
        // 0: Name("x")
        // 1: SimpleSwitch with cases jumping to 2, 4, 6, and default to 8
        // 2: [jump target] PushConstant("case1")
        // 3: Goto -> 9
        // 4: [jump target] PushConstant("case2")
        // 5: Goto -> 9
        // 6: [jump target] PushConstant("case3")
        // 7: Goto -> 9
        // 8: [jump target] PushConstant("default")
        // 9: [jump target] Add with string suffix

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new Name("x")); // 0

        // Create SimpleSwitch with test values
        List<org.mozilla.javascript.interpreterv2.operand.Operand> testOperands = new ArrayList<>();
        testOperands.add(new IntOperand(1));
        testOperands.add(new IntOperand(2));
        testOperands.add(new IntOperand(3));
        SimpleSwitch switchInst = new SimpleSwitch(PopOperand.instance, testOperands);

        // Set offsets for each case
        switchInst.setOffset(1); // case 1 -> jump to 2
        switchInst.setOffset(3); // case 2 -> jump to 4
        switchInst.setOffset(5); // case 3 -> jump to 6
        switchInst.setOffset(7); // default -> jump to 8

        instructions.add(switchInst); // 1

        instructions.add(new PushConstant("case1")); // 2
        Goto goto1 = new Goto();
        goto1.setOffset(6);
        instructions.add(goto1); // 3

        instructions.add(new PushConstant("case2")); // 4
        Goto goto2 = new Goto();
        goto2.setOffset(4);
        instructions.add(goto2); // 5

        instructions.add(new PushConstant("case3")); // 6
        Goto goto3 = new Goto();
        goto3.setOffset(2);
        instructions.add(goto3); // 7

        instructions.add(new PushConstant("default")); // 8
        instructions.add(new Add(PopOperand.instance, new StringOperand(" result"))); // 9

        // All jump targets from SimpleSwitch should be tracked
        Set<Integer> jumpTargets = switchInst.getTargets(1);
        assertTrue(jumpTargets.contains(2), "Should have case jump target at 2");
        assertTrue(jumpTargets.contains(4), "Should have case jump target at 4");
        assertTrue(jumpTargets.contains(6), "Should have case jump target at 6");
        assertTrue(jumpTargets.contains(8), "Should have default jump target at 8");
        jumpTargets.add(9); // Also add the merge point

        InstructionSimplification simplifier =
                new InstructionSimplification(instructions, jumpTargets);
        simplifier.simplify();

        // The Add at position 9 should be simplified to AnyStringAdd
        // because it's a jump target from multiple places
        Instruction simplified = instructions.get(9);
        String actualType = simplified.getClass().getSimpleName();
        assertTrue(simplified instanceof AnyStringAdd, "Expected AnyStringAdd got " + actualType);
    }
}
