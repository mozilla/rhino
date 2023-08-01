/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.classfile;

/**
 * A super block is defined as a contiguous chunk of code with a single entry point and multiple
 * exit points (therefore ending in an unconditional jump or the end of the method). This is used to
 * emulate OpenJDK's compiler, which outputs stack map frames at the start of every super block
 * except the method start.
 */
final class SuperBlock {
    SuperBlock(int index, int start, int end, int[] initialLocals) {
        this.index = index;
        this.start = start;
        this.end = end;
        locals = new int[initialLocals.length];
        System.arraycopy(initialLocals, 0, locals, 0, initialLocals.length);
        stack = new int[0];
        isInitialized = false;
        isInQueue = false;
    }

    int getIndex() {
        return index;
    }

    int[] getLocals() {
        int[] copy = new int[locals.length];
        System.arraycopy(locals, 0, copy, 0, locals.length);
        return copy;
    }

    /**
     * Get a copy of the super block's locals without any trailing TOP types.
     *
     * <p>This is useful for actual writing stack maps; during the computation of stack map types,
     * all local arrays have the same size; the max locals for the method. In addition, DOUBLE and
     * LONG types have trailing TOP types because they occupy two words. For writing purposes, these
     * are not useful.
     */
    int[] getTrimmedLocals() {
        int last = locals.length - 1;
        // Exclude all of the trailing TOPs not bound to a DOUBLE/LONG
        while (last >= 0
                && locals[last] == TypeInfo.TOP
                && !TypeInfo.isTwoWords(locals[last - 1])) {
            last--;
        }
        last++;
        // Exclude trailing TOPs following a DOUBLE/LONG
        int size = last;
        for (int i = 0; i < last; i++) {
            if (TypeInfo.isTwoWords(locals[i])) {
                size--;
            }
        }
        int[] copy = new int[size];
        for (int i = 0, j = 0; i < size; i++, j++) {
            copy[i] = locals[j];
            if (TypeInfo.isTwoWords(locals[j])) {
                j++;
            }
        }
        return copy;
    }

    int[] getStack() {
        int[] copy = new int[stack.length];
        System.arraycopy(stack, 0, copy, 0, stack.length);
        return copy;
    }

    boolean merge(int[] locals, int localsTop, int[] stack, int stackTop, ConstantPool pool) {
        if (!isInitialized) {
            System.arraycopy(locals, 0, this.locals, 0, localsTop);
            this.stack = new int[stackTop];
            System.arraycopy(stack, 0, this.stack, 0, stackTop);
            isInitialized = true;
            return true;
        } else if (this.locals.length == localsTop && this.stack.length == stackTop) {
            boolean localsChanged = mergeState(this.locals, locals, localsTop, pool);
            boolean stackChanged = mergeState(this.stack, stack, stackTop, pool);
            return localsChanged || stackChanged;
        } else {
            if (ClassFileWriter.StackMapTable.DEBUGSTACKMAP) {
                System.out.println("bad merge");
                System.out.println("current type state:");
                TypeInfo.print(this.locals, this.stack, pool);
                System.out.println("incoming type state:");
                TypeInfo.print(locals, localsTop, stack, stackTop, pool);
            }
            throw new IllegalArgumentException("bad merge attempt");
        }
    }

    /**
     * Merge an operand stack or local variable array with incoming state.
     *
     * <p>They are treated the same way; by this point, it should already be ensured that the array
     * sizes are the same, which is the only additional constraint that is imposed on merging
     * operand stacks (the local variable array is always the same size).
     */
    private static boolean mergeState(int[] current, int[] incoming, int size, ConstantPool pool) {
        boolean changed = false;
        for (int i = 0; i < size; i++) {
            int currentType = current[i];

            current[i] = TypeInfo.merge(current[i], incoming[i], pool);
            if (currentType != current[i]) {
                changed = true;
            }
        }
        return changed;
    }

    int getStart() {
        return start;
    }

    int getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "sb " + index;
    }

    boolean isInitialized() {
        return isInitialized;
    }

    void setInitialized(boolean b) {
        isInitialized = b;
    }

    boolean isInQueue() {
        return isInQueue;
    }

    void setInQueue(boolean b) {
        isInQueue = b;
    }

    private int index;
    private int start;
    private int end;
    private int[] locals;
    private int[] stack;
    private boolean isInitialized;
    private boolean isInQueue;
}
