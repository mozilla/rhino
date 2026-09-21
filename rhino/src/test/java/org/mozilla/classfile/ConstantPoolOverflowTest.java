/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.classfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * The constant pool is indexed by an unsigned 16 bit value, and {@code constant_pool_count} (which
 * is one more than the highest index in use) is itself an unsigned 16 bit value. That leaves 0xfffe
 * as the highest usable index, because index zero is reserved.
 *
 * <p>These tests check two separate things for every constant type: that an index in the upper half
 * of the unsigned range is returned faithfully rather than sign-extended, and that running off the
 * end of the pool is reported as a {@link ClassFileWriter.ClassSizeException} instead of silently
 * producing a truncated index.
 */
class ConstantPoolOverflowTest {

    /** Highest index a constant may occupy. */
    private static final int MAX_INDEX = 0xfffe;

    /** First index whose value does not fit in a signed 16 bit quantity. */
    private static final int FIRST_NEGATIVE_SHORT_INDEX = 0x8000;

    private ClassFileWriter cfw() {
        return new ClassFileWriter("Test", "java/lang/Object", "ConstantPoolOverflowTest.java");
    }

    /**
     * Adds single-slot integer constants until the entry at {@code target} has been written, and
     * returns the pool. The next constant added therefore lands at {@code target + 1}.
     */
    private ConstantPool poolFilledTo(int target) {
        ConstantPool pool = new ConstantPool(cfw());
        for (int i = 1; i <= target; i++) {
            int index = pool.addConstant(i);
            assertEquals(i, index, "filler constant landed at an unexpected index");
        }
        return pool;
    }

    /**
     * Checks that a constant added at the top of the unsigned index range is reported with its true
     * index, and that the pool refuses to add another one once the range is exhausted.
     *
     * @param slots the number of pool slots the constant occupies
     * @param add adds one constant of the type under test and returns its index
     */
    private void assertIndexRangeRespected(int slots, ToIntFunction<ConstantPool> add) {
        // An index above Short.MAX_VALUE must survive the trip back to the caller.
        ConstantPool pool = poolFilledTo(FIRST_NEGATIVE_SHORT_INDEX - 1);
        assertEquals(FIRST_NEGATIVE_SHORT_INDEX, add.applyAsInt(pool));

        // The last constant that fits must be accepted...
        pool = poolFilledTo(MAX_INDEX - slots);
        assertEquals(MAX_INDEX - slots + 1, add.applyAsInt(pool));

        // ...and the first one that does not must be rejected.
        ConstantPool full = poolFilledTo(MAX_INDEX - slots + 1);
        assertOverflow(() -> add.applyAsInt(full));
    }

    /**
     * Variant of {@link #assertIndexRangeRespected} for constants that are built out of several
     * pool entries, where the index the caller sees depends on how many of the dependent entries
     * were already interned. Only the boundary behaviour is checked.
     *
     * @param slots the number of pool slots the constant and its dependents occupy
     */
    private void assertCompositeIndexRangeRespected(int slots, ToIntFunction<ConstantPool> add) {
        ConstantPool pool = poolFilledTo(FIRST_NEGATIVE_SHORT_INDEX - 1);
        int index = add.applyAsInt(pool);
        assertTrue(
                index >= FIRST_NEGATIVE_SHORT_INDEX && index <= MAX_INDEX,
                "index " + index + " is outside the usable range");

        pool = poolFilledTo(MAX_INDEX - slots);
        index = add.applyAsInt(pool);
        assertTrue(index > 0 && index <= MAX_INDEX, "index " + index + " is outside the range");

        ConstantPool full = poolFilledTo(MAX_INDEX - slots + 1);
        assertOverflow(() -> add.applyAsInt(full));
    }

    private void assertOverflow(Executable e) {
        assertThrows(ClassFileWriter.ClassSizeException.class, e);
    }

    @Test
    void integerConstant() {
        assertIndexRangeRespected(1, pool -> pool.addConstant(0x0dedbeef));
    }

    @Test
    void floatConstant() {
        assertIndexRangeRespected(1, pool -> pool.addConstant(1.5f));
    }

    @Test
    void longConstant() {
        // A long occupies two pool slots
        assertIndexRangeRespected(2, pool -> pool.addConstant(0x0dedbeefcafebabeL));
    }

    @Test
    void doubleConstant() {
        // A double occupies two pool slots
        assertIndexRangeRespected(2, pool -> pool.addConstant(1.5d));
    }

    @Test
    void utf8Constant() {
        assertIndexRangeRespected(1, pool -> pool.addUtf8("a string that is not yet interned"));
    }

    @Test
    void stringConstant() {
        // One Utf8 entry plus the String entry that points at it
        assertCompositeIndexRangeRespected(2, pool -> pool.addConstant("an uninterned string"));
    }

    @Test
    void classConstant() {
        // One Utf8 entry plus the Class entry that points at it
        assertCompositeIndexRangeRespected(2, pool -> pool.addClass("some/uninterned/Class"));
    }

    @Test
    void fieldRefConstant() {
        // Name and type Utf8 pair, a NameAndType, a class name Utf8, a Class and the Fieldref
        assertCompositeIndexRangeRespected(
                6, pool -> pool.addFieldRef("some/Class", "field", "Ljava/lang/Object;"));
    }

    @Test
    void methodRefConstant() {
        assertCompositeIndexRangeRespected(
                6, pool -> pool.addMethodRef("some/Class", "method", "()V"));
    }

    @Test
    void interfaceMethodRefConstant() {
        assertCompositeIndexRangeRespected(
                6, pool -> pool.addInterfaceMethodRef("some/Iface", "method", "()V"));
    }

    @Test
    void invokeDynamicConstant() {
        // Name and type Utf8 pair, a NameAndType and the InvokeDynamic entry
        assertCompositeIndexRangeRespected(4, pool -> pool.addInvokeDynamic("method", "()V", 0));
    }

    @Test
    void methodHandleConstant() {
        // A Methodref and its dependents, plus the MethodHandle entry
        ClassFileWriter.MHandle mh =
                new ClassFileWriter.MHandle(
                        ByteCode.MH_INVOKESTATIC, "some/Class", "method", "()V");
        assertCompositeIndexRangeRespected(7, pool -> pool.addMethodHandle(mh));
    }

    @Test
    void fieldMethodHandleConstant() {
        // Exercises the Fieldref branch of addMethodHandle
        ClassFileWriter.MHandle mh =
                new ClassFileWriter.MHandle(
                        ByteCode.MH_GETSTATIC, "some/Class", "field", "Ljava/lang/Object;");
        assertCompositeIndexRangeRespected(7, pool -> pool.addMethodHandle(mh));
    }

    @Test
    void interfaceMethodHandleConstant() {
        // Exercises the InterfaceMethodref branch of addMethodHandle
        ClassFileWriter.MHandle mh =
                new ClassFileWriter.MHandle(
                        ByteCode.MH_INVOKEINTERFACE, "some/Iface", "method", "()V");
        assertCompositeIndexRangeRespected(7, pool -> pool.addMethodHandle(mh));
    }

    /**
     * Interned constants must be recognised as such, otherwise a pool that would comfortably fit
     * can be driven into overflow by repeating the same reference.
     */
    @Test
    void repeatedConstantsAreInterned() {
        ConstantPool pool = new ConstantPool(cfw());
        int utf8 = pool.addUtf8("name");
        int string = pool.addConstant("a string");
        int clazz = pool.addClass("some/Class");
        int field = pool.addFieldRef("some/Class", "field", "Ljava/lang/Object;");
        int method = pool.addMethodRef("some/Class", "method", "()V");
        int iface = pool.addInterfaceMethodRef("some/Iface", "method", "()V");
        int indy = pool.addInvokeDynamic("method", "()V", 0);
        ClassFileWriter.MHandle mh =
                new ClassFileWriter.MHandle(
                        ByteCode.MH_INVOKESTATIC, "some/Class", "method", "()V");
        int handle = pool.addMethodHandle(mh);

        assertEquals(utf8, pool.addUtf8("name"));
        assertEquals(string, pool.addConstant("a string"));
        assertEquals(clazz, pool.addClass("some/Class"));
        assertEquals(field, pool.addFieldRef("some/Class", "field", "Ljava/lang/Object;"));
        assertEquals(method, pool.addMethodRef("some/Class", "method", "()V"));
        assertEquals(iface, pool.addInterfaceMethodRef("some/Iface", "method", "()V"));
        assertEquals(indy, pool.addInvokeDynamic("method", "()V", 0));
        assertEquals(handle, pool.addMethodHandle(mh));
    }

    /** The count written into the class file must match the number of entries in the pool. */
    @Test
    void writtenCountMatchesTopIndex() {
        ConstantPool pool = poolFilledTo(FIRST_NEGATIVE_SHORT_INDEX + 1);
        byte[] data = new byte[pool.getWriteSize()];
        pool.write(data, 0);
        int count = ((data[0] & 0xff) << 8) | (data[1] & 0xff);
        assertEquals(FIRST_NEGATIVE_SHORT_INDEX + 2, count);
    }
}
