package org.mozilla.javascript.interpreterv2;

import java.io.PrintStream;
import java.util.List;
import org.mozilla.javascript.ACompilerData;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.InterpreterV2;
import org.mozilla.javascript.JSCode;
import org.mozilla.javascript.JSDescriptor;
import org.mozilla.javascript.ScriptOrFn;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.config.RhinoConfig;
import org.mozilla.javascript.interpreterv2.instruction.Instruction;

public class CompilerData<T extends ScriptOrFn<T>> extends ACompilerData<T, CompilerData<?>> {

    static boolean shouldDumpInstructions = RhinoConfig.get("rhino.printICodeV2", false);

    public static final int EXCEPTION_TRY_START_SLOT = 0;
    public static final int EXCEPTION_TRY_END_SLOT = 1;
    public static final int EXCEPTION_HANDLER_SLOT = 2;
    public static final int EXCEPTION_TYPE_SLOT = 3;
    public static final int EXCEPTION_LOCAL_SLOT = 4;
    public static final int EXCEPTION_SCOPE_SLOT = 5;
    // SLOT_SIZE: space for try start/end, handler, start, handler type,
    //            exception local and scope local
    public static final int EXCEPTION_SLOT_SIZE = 6;

    public final Instruction[] instructions;

    /**
     * @see LineNumberTable
     */
    private final LineNumberTable lineNumberTable;

    private CompilerData(Builder<T> b) {
        super(b.maxVars, b.maxLocals, b.maxStack, b.maxFrameSize, b.exceptionTable);
        this.instructions = b.instructions;
        this.lineNumberTable = b.builtLineNumberTable;
    }

    public Instruction[] getInstructions() {
        if (instructions == null) {
            throw new IllegalStateException("Instructions not set");
        }
        return instructions;
    }

    public int[] getLineNumbers() {
        return LineNumberTable.getLineNumbers(lineNumberTable);
    }

    public int getInstructionCount() {
        if (instructions == null) {
            return 0;
        }
        return instructions.length;
    }

    public void dumpInstructions(PrintStream out, JSDescriptor.Builder<?> desc) {
        if (!shouldDumpInstructions) {
            return;
        }

        // Function header with metadata
        String functionName = desc.name != null ? desc.name : "<anonymous>";
        out.printf(
                "function %s [maxStack=%d, locals=%d, instructions=%d]:%n",
                functionName, maxStack, maxLocals, getInstructionCount());
        out.println("  line number table: " + getLineNumberTableForDebug());

        // Instructions with virtual register numbering
        if (instructions != null) {
            for (int pc = 0; pc < instructions.length; pc++) {
                out.printf("%%%-3d: %s%n", pc, instructions[pc].toDebugString());
            }
        }

        // Exception handlers (if any)
        if (hasExceptionHandlers()) {
            out.println();
            out.println("exception handlers:");
            dumpExceptionHandlers(out);
        }

        out.println();
        out.flush();
    }

    private boolean hasExceptionHandlers() {
        return exceptionTable != null && exceptionTable.length > 0;
    }

    private void dumpExceptionHandlers(PrintStream out) {
        if (exceptionTable == null) {
            return;
        }

        for (int i = 0; i < exceptionTable.length; i += EXCEPTION_SLOT_SIZE) {
            int tryStart = exceptionTable[i + EXCEPTION_TRY_START_SLOT];
            int tryEnd = exceptionTable[i + EXCEPTION_TRY_END_SLOT];
            int handler = exceptionTable[i + EXCEPTION_HANDLER_SLOT];
            int type = exceptionTable[i + EXCEPTION_TYPE_SLOT];
            int local = exceptionTable[i + EXCEPTION_LOCAL_SLOT];
            int scope = exceptionTable[i + EXCEPTION_SCOPE_SLOT];

            out.printf(
                    "  try %%%-3d..%%%-3d -> %%%-3d (type=%d, local=%d, scope=%d)%n",
                    tryStart, tryEnd, handler, type, local, scope);
        }
    }

    public int getFirstLineNumber() {
        return LineNumberTable.getFirstLineNumber(lineNumberTable);
    }

    public int getPcFirstLineNumber() {
        return LineNumberTable.getPcFirstLineNumber(lineNumberTable);
    }

    /**
     * Get the set of line numbers associated with a given PC.
     *
     * @param pc The program counter
     * @return List of line numbers, or null if PC has no associated lines
     */
    public List<Integer> getLineSetFromPc(int pc) {
        return LineNumberTable.getLineSetFromPc(lineNumberTable, pc);
    }

    @Override
    public int getLineNumberFromPc(int pc, int pcLineStart) {
        return LineNumberTable.getLineNumberFromPc(lineNumberTable, pc);
    }

    public String getLineNumberTableForDebug() {
        return LineNumberTable.getDebugString(lineNumberTable);
    }

    @Override
    public Object execute(
            Context cx,
            T executableObject,
            Object newTarget,
            VarScope scope,
            Object thisObj,
            Object[] args) {
        return InterpreterV2.interpret(
                executableObject, this, cx, scope, (Scriptable) thisObj, args);
    }

    @Override
    public Object resume(
            Context cx,
            T executableObject,
            Object state,
            VarScope scope,
            int operation,
            Object value) {
        return InterpreterV2.resumeGenerator(cx, scope, operation, state, value);
    }

    @Override
    public String toString() {
        return "Compiler data"; // sourceFile + ':' + name;
    }

    /**
     * Mutable working state for building an immutable {@link CompilerData}. Mirrors the builder
     * pattern used by {@link org.mozilla.javascript.InterpreterData.Builder}.
     */
    public static class Builder<T extends ScriptOrFn<T>> extends JSCode.Builder<T> {

        final Builder<?> parentBuilder;

        public CompilerData<org.mozilla.javascript.JSFunction>[] nestedFunctions;

        public Instruction[] instructions;

        public int[] exceptionTable;

        public int maxVars;
        public int maxLocals;
        public int maxStack = 0;
        public int maxFrameSize;

        LineNumberTable builtLineNumberTable;

        private CompilerData<T> built;

        public Builder() {
            this.parentBuilder = null;
        }

        public Builder(Builder<?> parent) {
            this.parentBuilder = parent;
        }

        public void setLineNumberTable(LineNumberTable.Builder lineNumberTableBuilder) {
            this.builtLineNumberTable = lineNumberTableBuilder.buildFinalTable();
        }

        @Override
        public CompilerData<T> build() {
            if (built == null) {
                built = new CompilerData<>(this);
            }
            return built;
        }
    }
}
