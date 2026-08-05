/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.classfile.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mozilla.classfile.ClassFileWriter.ACC_PUBLIC;
import static org.mozilla.classfile.ClassFileWriter.ACC_STATIC;
import static org.mozilla.javascript.Symbol.Kind.REGULAR;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.classfile.DynamicConstant;
import org.mozilla.classfile.DynamicConstantDescriber;
import org.mozilla.javascript.DefiningClassLoader;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.optimizer.SymbolKeyDescriber;

public class DynamicConstantTest {

    private static final ClassDesc CD_SELF = ClassDesc.of(DynamicConstantTest.class.getName());

    // ---------------------------------------------------------------- constants under test

    /** A constant resolved by {@link #stringBootstrap}, carrying its value in the constant name. */
    public static class StringConstant implements DynamicConstant {
        final String value;

        StringConstant(String value) {
            this.value = value;
        }
    }

    /** A two word constant, so that the "ldc2_w" path is exercised. */
    public static class LongConstant implements DynamicConstant {
        final long value;

        LongConstant(long value) {
            this.value = value;
        }
    }

    /** A constant whose describer takes a MethodType static argument. */
    public static class MethodTypeConstant implements DynamicConstant {}

    /** A constant that no describer can describe. */
    public static class UndescribableConstant implements DynamicConstant {}

    // ---------------------------------------------------------------- bootstrap methods

    public static String stringBootstrap(MethodHandles.Lookup lookup, String name, Class<?> type) {
        return "resolved:" + name;
    }

    public static long longBootstrap(MethodHandles.Lookup lookup, String name, Class<?> type) {
        return Long.parseLong(name);
    }

    public static String methodTypeBootstrap(
            MethodHandles.Lookup lookup, String name, Class<?> type, MethodType methodType) {
        return methodType.toString();
    }

    private static DirectMethodHandleDesc bootstrap(String name, MethodTypeDesc type) {
        return MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC, CD_SELF, name, type);
    }

    // ---------------------------------------------------------------- describers

    private static final DynamicConstantDescriber<StringConstant> STRING_DESCRIBER =
            new DynamicConstantDescriber<>() {
                @Override
                public Class<StringConstant> describedType() {
                    return StringConstant.class;
                }

                @Override
                public Optional<DynamicConstantDesc<StringConstant>> describe(
                        StringConstant value) {
                    return Optional.of(
                            DynamicConstantDesc.ofNamed(
                                    bootstrap(
                                            "stringBootstrap",
                                            MethodTypeDesc.of(
                                                    ConstantDescs.CD_String,
                                                    ConstantDescs.CD_MethodHandles_Lookup,
                                                    ConstantDescs.CD_String,
                                                    ConstantDescs.CD_Class)),
                                    value.value,
                                    ConstantDescs.CD_String));
                }
            };

    private static final DynamicConstantDescriber<LongConstant> LONG_DESCRIBER =
            new DynamicConstantDescriber<>() {
                @Override
                public Class<LongConstant> describedType() {
                    return LongConstant.class;
                }

                @Override
                public Optional<DynamicConstantDesc<LongConstant>> describe(LongConstant value) {
                    return Optional.of(
                            DynamicConstantDesc.ofNamed(
                                    bootstrap(
                                            "longBootstrap",
                                            MethodTypeDesc.of(
                                                    ConstantDescs.CD_long,
                                                    ConstantDescs.CD_MethodHandles_Lookup,
                                                    ConstantDescs.CD_String,
                                                    ConstantDescs.CD_Class)),
                                    Long.toString(value.value),
                                    ConstantDescs.CD_long));
                }
            };

    private static final MethodTypeDesc DESCRIBED_METHOD_TYPE =
            MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_String, ConstantDescs.CD_long);

    private static final DynamicConstantDescriber<MethodTypeConstant> METHOD_TYPE_DESCRIBER =
            new DynamicConstantDescriber<>() {
                @Override
                public Class<MethodTypeConstant> describedType() {
                    return MethodTypeConstant.class;
                }

                @Override
                public Optional<DynamicConstantDesc<MethodTypeConstant>> describe(
                        MethodTypeConstant value) {
                    return Optional.of(
                            DynamicConstantDesc.ofNamed(
                                    bootstrap(
                                            "methodTypeBootstrap",
                                            MethodTypeDesc.of(
                                                    ConstantDescs.CD_String,
                                                    ConstantDescs.CD_MethodHandles_Lookup,
                                                    ConstantDescs.CD_String,
                                                    ConstantDescs.CD_Class,
                                                    ConstantDescs.CD_MethodType)),
                                    "unused",
                                    ConstantDescs.CD_String,
                                    DESCRIBED_METHOD_TYPE));
                }
            };

    private static final DynamicConstantDescriber<UndescribableConstant> EMPTY_DESCRIBER =
            new DynamicConstantDescriber<>() {
                @Override
                public Class<UndescribableConstant> describedType() {
                    return UndescribableConstant.class;
                }

                @Override
                public Optional<DynamicConstantDesc<UndescribableConstant>> describe(
                        UndescribableConstant value) {
                    return Optional.empty();
                }
            };

    // ---------------------------------------------------------------- tests

    @Test
    public void wellKnownSymbolResolvesToTheCanonicalInstance() throws Exception {
        // SymbolKey compares by identity, so the constant has to be the very same object the
        // runtime uses, not merely an equal one.
        ClassFileWriter cfw = writer("TestSymbolConstant");
        cfw.registerDynamicConstantDescriber(new SymbolKeyDescriber());
        cfw.startMethod("get", "()Ljava/lang/Object;", (short) (ACC_PUBLIC | ACC_STATIC));
        cfw.addLoadDynamicConstant(SymbolKey.ITERATOR);
        cfw.add(ByteCode.ARETURN);
        cfw.stopMethod((short) 0);

        assertSame(SymbolKey.ITERATOR, invoke(cfw, "TestSymbolConstant", "get"));
    }

    @Test
    public void regularSymbolResolvesToSymbolWithSameName() throws Exception {
        // SymbolKey compares by identity, so the constant has to be the very same object the
        // runtime uses, not merely an equal one.
        ClassFileWriter cfw = writer("TestSymbolConstant");
        cfw.registerDynamicConstantDescriber(new SymbolKeyDescriber());
        cfw.startMethod("get", "()Ljava/lang/Object;", (short) (ACC_PUBLIC | ACC_STATIC));
        cfw.addLoadDynamicConstant(new SymbolKey("foo", REGULAR));
        cfw.add(ByteCode.ARETURN);
        cfw.stopMethod((short) 0);

        assertEquals("foo", ((SymbolKey) invoke(cfw, "TestSymbolConstant", "get")).getName());
    }

    @Test
    public void constantTypeFlowsIntoTheStackMapTable() throws Exception {
        // The branch forces a stack map frame while the dynamic constant is still on the stack,
        // which only works if the writer can derive its type from the constant pool entry.
        ClassFileWriter cfw = writer("TestConstantStackMap");
        cfw.registerDynamicConstantDescriber(STRING_DESCRIBER);
        cfw.startMethod("get", "()Ljava/lang/String;", (short) (ACC_PUBLIC | ACC_STATIC));
        cfw.addLoadDynamicConstant(new StringConstant("branched"));
        cfw.add(ByteCode.DUP);
        int target = cfw.acquireLabel();
        cfw.add(ByteCode.IFNULL, target);
        cfw.markLabel(target);
        cfw.add(ByteCode.ARETURN);
        cfw.stopMethod((short) 0);

        assertEquals("resolved:branched", invoke(cfw, "TestConstantStackMap", "get"));
    }

    @Test
    public void twoWordConstantUsesLdc2w() throws Exception {
        // A long constant must be loaded with ldc2_w and must occupy two stack words in the
        // frame generated at the branch target.
        ClassFileWriter cfw = writer("TestLongConstant");
        cfw.registerDynamicConstantDescriber(LONG_DESCRIBER);
        cfw.startMethod("get", "()J", (short) (ACC_PUBLIC | ACC_STATIC));
        cfw.addLoadDynamicConstant(new LongConstant(1234567890123L));
        cfw.add(ByteCode.DUP2);
        cfw.add(ByteCode.LCONST_0);
        cfw.add(ByteCode.LCMP);
        int target = cfw.acquireLabel();
        cfw.add(ByteCode.IFEQ, target);
        cfw.markLabel(target);
        cfw.add(ByteCode.LRETURN);
        cfw.stopMethod((short) 0);

        byte[] bytecode = cfw.toByteArray();
        assertEquals(1234567890123L, invoke(bytecode, "TestLongConstant", "get"));
        assertEquals(1, ClassFileInfo.parse(bytecode).count(TAG_DYNAMIC));
    }

    @Test
    public void methodTypeStaticArgument() throws Exception {
        ClassFileWriter cfw = writer("TestMethodTypeArg");
        cfw.registerDynamicConstantDescriber(METHOD_TYPE_DESCRIBER);
        cfw.startMethod("get", "()Ljava/lang/String;", (short) (ACC_PUBLIC | ACC_STATIC));
        cfw.addLoadDynamicConstant(new MethodTypeConstant());
        cfw.add(ByteCode.ARETURN);
        cfw.stopMethod((short) 0);

        byte[] bytecode = cfw.toByteArray();
        assertEquals(
                MethodType.fromMethodDescriptorString(
                                DESCRIBED_METHOD_TYPE.descriptorString(),
                                getClass().getClassLoader())
                        .toString(),
                invoke(bytecode, "TestMethodTypeArg", "get"));
        assertEquals(1, ClassFileInfo.parse(bytecode).count(TAG_METHOD_TYPE));
    }

    @Test
    public void repeatedConstantsAreShared() throws Exception {
        ClassFileWriter cfw = writer("TestSharedConstants");
        cfw.registerDynamicConstantDescriber(STRING_DESCRIBER);
        cfw.startMethod("get", "()Ljava/lang/String;", (short) (ACC_PUBLIC | ACC_STATIC));
        cfw.addLoadDynamicConstant(new StringConstant("same"));
        cfw.add(ByteCode.POP);
        cfw.addLoadDynamicConstant(new StringConstant("same"));
        cfw.add(ByteCode.POP);
        // A distinct constant sharing the same bootstrap method.
        cfw.addLoadDynamicConstant(new StringConstant("other"));
        cfw.add(ByteCode.ARETURN);
        cfw.stopMethod((short) 0);

        byte[] bytecode = cfw.toByteArray();
        ClassFileInfo info = ClassFileInfo.parse(bytecode);
        assertEquals(2, info.count(TAG_DYNAMIC), "equal constants should share a pool entry");
        assertEquals(1, info.bootstrapMethodCount, "one bootstrap method should be shared");
        assertEquals("resolved:other", invoke(bytecode, "TestSharedConstants", "get"));
    }

    @Test
    public void unregisteredTypeIsRejected() {
        ClassFileWriter cfw = writer("TestUnregistered");
        cfw.startMethod("get", "()Ljava/lang/String;", (short) (ACC_PUBLIC | ACC_STATIC));

        IllegalArgumentException e =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> cfw.addLoadDynamicConstant(new StringConstant("x")));
        assertTrue(e.getMessage().contains("no dynamic constant describer"), e.getMessage());
    }

    @Test
    public void undescribableValueIsRejected() {
        ClassFileWriter cfw = writer("TestUndescribable");
        cfw.registerDynamicConstantDescriber(EMPTY_DESCRIBER);
        cfw.startMethod("get", "()Ljava/lang/String;", (short) (ACC_PUBLIC | ACC_STATIC));

        IllegalArgumentException e =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> cfw.addLoadDynamicConstant(new UndescribableConstant()));
        assertTrue(e.getMessage().contains("cannot be described"), e.getMessage());
    }

    @Test
    public void regularSymbolsAreDescribable() {
        SymbolKeyDescriber describer = new SymbolKeyDescriber();

        assertTrue(describer.describe(SymbolKey.ITERATOR).isPresent());
        assertTrue(describer.describe(SymbolKey.TO_PRIMITIVE).isPresent());
        // A symbol from "Symbol()" has an identity that cannot be reconstructed, and one from
        // "Symbol.for" belongs to the global registry.
        assertTrue(describer.describe(new SymbolKey("regular", Symbol.Kind.REGULAR)).isPresent());
        assertTrue(
                describer.describe(new SymbolKey("registered", Symbol.Kind.REGISTERED)).isEmpty());
    }

    // ---------------------------------------------------------------- helpers

    private static ClassFileWriter writer(String className) {
        return new ClassFileWriter(className, "java/lang/Object", "DynamicConstantTest.java");
    }

    private static Object invoke(ClassFileWriter cfw, String className, String methodName)
            throws Exception {
        return invoke(cfw.toByteArray(), className, methodName);
    }

    private static Object invoke(byte[] bytecode, String className, String methodName)
            throws Exception {
        // Defining the class runs the verifier, which is what checks the StackMapTable we wrote.
        DefiningClassLoader loader = new DefiningClassLoader();
        Class<?> cl = loader.defineClass(className, bytecode);
        Method method = cl.getMethod(methodName);
        return method.invoke(null);
    }

    private static final int TAG_UTF8 = 1;
    private static final int TAG_LONG = 5;
    private static final int TAG_DOUBLE = 6;
    private static final int TAG_METHOD_HANDLE = 15;
    private static final int TAG_METHOD_TYPE = 16;
    private static final int TAG_DYNAMIC = 17;

    /**
     * Just enough of a class file reader to count constant pool entries and bootstrap methods. The
     * JVM checks that what we wrote is loadable; these counts check that we did not write more than
     * we needed to.
     */
    private static final class ClassFileInfo {
        private final int[] tagCounts = new int[21];
        private int bootstrapMethodCount;

        int count(int tag) {
            return tagCounts[tag];
        }

        static ClassFileInfo parse(byte[] data) {
            ClassFileInfo info = new ClassFileInfo();
            int pos = 8; // magic, minor, major
            int poolCount = u2(data, pos);
            pos += 2;
            // Constant pool indices start at 1, and long and double entries take two of them.
            for (int index = 1; index < poolCount; index++) {
                int tag = data[pos++] & 0xFF;
                info.tagCounts[tag]++;
                switch (tag) {
                    case TAG_UTF8:
                        pos += 2 + u2(data, pos);
                        break;
                    case TAG_LONG:
                    case TAG_DOUBLE:
                        pos += 8;
                        index++;
                        break;
                    case TAG_METHOD_HANDLE:
                        pos += 3;
                        break;
                    case TAG_METHOD_TYPE:
                        pos += 2;
                        break;
                    default:
                        pos += entrySize(tag);
                        break;
                }
            }
            pos += 6; // access flags, this class, super class
            pos += 2 + u2(data, pos) * 2; // interfaces
            pos = skipMembers(data, pos); // fields
            pos = skipMembers(data, pos); // methods
            int attributeCount = u2(data, pos);
            pos += 2;
            for (int i = 0; i < attributeCount; i++) {
                int length = (int) u4(data, pos + 2);
                if ("BootstrapMethods".equals(utf8At(data, u2(data, pos)))) {
                    // The attribute payload begins with num_bootstrap_methods.
                    info.bootstrapMethodCount = u2(data, pos + 6);
                }
                pos += 6 + length;
            }
            return info;
        }

        /** Resolve a Utf8 pool entry by index, used to identify attributes by name. */
        private static String utf8At(byte[] data, int wantedIndex) {
            int pos = 10;
            int poolCount = u2(data, 8);
            for (int index = 1; index < poolCount; index++) {
                int tag = data[pos++] & 0xFF;
                if (tag == TAG_UTF8) {
                    int length = u2(data, pos);
                    if (index == wantedIndex) {
                        return new String(
                                data, pos + 2, length, java.nio.charset.StandardCharsets.UTF_8);
                    }
                    pos += 2 + length;
                } else if (tag == TAG_LONG || tag == TAG_DOUBLE) {
                    pos += 8;
                    index++;
                } else if (tag == TAG_METHOD_HANDLE) {
                    pos += 3;
                } else if (tag == TAG_METHOD_TYPE) {
                    pos += 2;
                } else {
                    pos += entrySize(tag);
                }
            }
            return null;
        }

        private static int skipMembers(byte[] data, int pos) {
            int count = u2(data, pos);
            pos += 2;
            for (int i = 0; i < count; i++) {
                pos += 6; // access flags, name, descriptor
                int attributeCount = u2(data, pos);
                pos += 2;
                for (int a = 0; a < attributeCount; a++) {
                    pos += 6 + (int) u4(data, pos + 2);
                }
            }
            return pos;
        }

        private static int entrySize(int tag) {
            switch (tag) {
                case 7: // Class
                case 8: // String
                case 19: // Module
                case 20: // Package
                    return 2;
                case 3: // Integer
                case 4: // Float
                case 9: // Fieldref
                case 10: // Methodref
                case 11: // InterfaceMethodref
                case 12: // NameAndType
                case 17: // Dynamic
                case 18: // InvokeDynamic
                    return 4;
                default:
                    throw new IllegalStateException("unexpected constant pool tag " + tag);
            }
        }

        private static int u2(byte[] data, int pos) {
            return ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
        }

        private static long u4(byte[] data, int pos) {
            return ((long) u2(data, pos) << 16) | u2(data, pos + 2);
        }
    }
}
