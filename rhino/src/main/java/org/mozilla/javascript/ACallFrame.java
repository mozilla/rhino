/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;
import java.util.Arrays;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;

/**
 * Interface representing a call frame in the interpreter. This abstraction allows both the original
 * interpreter's CallFrame and InterpreterV2's CallFrameV2 to be used interchangeably for stack
 * traces and debugging purposes.
 */
public abstract class ACallFrame<T extends ACallFrame<T, U>, U extends ACompilerData<?, U>>
        implements Serializable {
    private static final long serialVersionUID = 6100049821156242226L;

    public final T parentFrame;
    public final short frameIndex;
    public final ACallFrame<?, ?> previousInterpreterFrame;
    public final int parentPC;
    public boolean frozen;

    public ScriptOrFn<?> fnOrScript;
    public U compilerData;

    public final Object[] stack;
    public final byte[] stackAttributes;
    public final double[] doubleStack;

    public Object result;
    public double resultDbl;
    public int pc;
    public int stackTop = -1;
    public VarScope scope;

    public final Scriptable thisObj;

    public final DebugFrame debuggerFrame;
    public final boolean useActivation;

    public ACallFrame<T, U> varSource;
    public final int localShift;
    public final short emptyStackTop;
    public Object throwable;
    final boolean parentStrictness;

    ACallFrame(
            Context cx,
            Scriptable thisObj,
            ScriptOrFn<?> fnOrScript,
            U code,
            T parentFrame,
            ACallFrame<?, ?> previousInterpreterFrame) {
        compilerData = code;
        debuggerFrame =
                cx.debugger != null ? cx.debugger.getFrame(cx, fnOrScript.getDescriptor()) : null;
        useActivation = fnOrScript.getDescriptor().requiresActivationFrame();

        emptyStackTop = (short) (compilerData.maxVars + compilerData.maxLocals - 1);
        int maxFrameArray = compilerData.maxFrameSize;
        if (maxFrameArray != emptyStackTop + compilerData.maxStack + 1) Kit.codeBug();

        stack = new Object[maxFrameArray];
        stackAttributes = new byte[maxFrameArray];
        doubleStack = new double[maxFrameArray];

        this.fnOrScript = fnOrScript;
        localShift = compilerData.maxVars;
        varSource = this;
        this.thisObj = thisObj;

        this.parentFrame = parentFrame;
        if (parentFrame == null) {
            this.parentPC =
                    previousInterpreterFrame == null
                            ? -1
                            : previousInterpreterFrame.getPcSourceLineStart();
        } else {
            this.parentPC = parentFrame.getPcSourceLineStart();
        }
        this.previousInterpreterFrame = previousInterpreterFrame;
        frameIndex = (short) ((parentFrame == null) ? 0 : parentFrame.frameIndex + 1);
        if (frameIndex > cx.getMaximumInterpreterStackDepth()) {
            throw Context.reportRuntimeError("Exceeded maximum stack depth");
        }

        var desc = fnOrScript.getDescriptor();
        if (desc.getFunctionType() != 0) {
            this.parentStrictness = ScriptRuntime.enterFunctionStrictness(
                    cx, fnOrScript.getDescriptor().isStrict());
        } else {
            this.parentStrictness = false;
        }
        // Initialize initial values of variables that change during
        // interpretation.
        result = Undefined.instance;
    }

    protected ACallFrame(T original, T parentFrame, ACallFrame<?, ?> previousInterpreterFrame) {
        this(original, parentFrame, previousInterpreterFrame, true, false);
    }

    protected ACallFrame(
            T original,
            T parentFrame,
            ACallFrame<?, ?> previousInterpreterFrame,
            boolean copyStack,
            boolean keepFrozen) {

        if (!original.frozen) Kit.codeBug();

        if (copyStack) {
            stack = Arrays.copyOf(original.stack, original.stack.length);
            stackAttributes =
                    Arrays.copyOf(original.stackAttributes, original.stackAttributes.length);
            doubleStack = Arrays.copyOf(original.doubleStack, original.doubleStack.length);
        } else {
            stack = original.stack;
            stackAttributes = original.stackAttributes;
            doubleStack = original.doubleStack;
        }

        localShift = original.localShift;

        frozen = keepFrozen;
        this.parentFrame = parentFrame;
        this.previousInterpreterFrame = previousInterpreterFrame;
        if (parentFrame == null) {
            frameIndex = 0;
            parentPC =
                    previousInterpreterFrame == null
                            ? -1
                            : previousInterpreterFrame.getPcSourceLineStart();
        } else {
            frameIndex = original.frameIndex;
            parentPC = parentFrame.parentPC;
        }

        fnOrScript = original.fnOrScript;
        compilerData = original.compilerData;

        if (original.varSource == original) {
            varSource = this;
        } else {
            varSource = original.varSource;
        }
        emptyStackTop = original.emptyStackTop;

        debuggerFrame = original.debuggerFrame;
        useActivation = original.useActivation;

        thisObj = original.thisObj;

        result = original.result;
        resultDbl = original.resultDbl;
        pc = original.pc;
        scope = original.scope;
        parentStrictness = original.parentStrictness;
        stackTop = original.stackTop;
    }

    /** Returns the index of this frame in the call stack (0 for the top-level frame). */
    public abstract int getFrameIndex();

    /** Returns the parent frame, or null if this is the top-level frame. */
    public abstract ACallFrame<T, U> getParentFrame();

    /**
     * Returns the program counter position for source line information. For the original
     * interpreter, this is the PC at the start of the current source line. For InterpreterV2, this
     * may be the current PC.
     */
    public abstract int getPcSourceLineStart();

    /** Returns the debuggable script data associated with this frame. */
    public abstract DebuggableScript getData();

    public int getParentPC() {
        return -1;
    }

    public ACallFrame<?, ?> getPreviousInterpreterFrame() {
        return null;
    }

    public ScriptOrFn<?> getFnOrScript() {
        return null;
    }

    Object getFromVars(int offset) {
        Object value = stack[offset];
        if (value == UniqueTag.DOUBLE_MARK) {
            return doubleStack[offset];
        } else {
            return value;
        }
    }

    void setInVars(int offset, Object value) {
        if (value instanceof Double && Double.isFinite((Double) value)) {
            stack[offset] = UniqueTag.DOUBLE_MARK;
            doubleStack[offset] = ((Double) value);
        } else {
            stack[offset] = value;
        }
    }

    abstract T cloneFrozen();
}
