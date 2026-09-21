package org.mozilla.classfile.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mozilla.classfile.ClassFileWriter.ACC_PUBLIC;
import static org.mozilla.classfile.ClassFileWriter.ACC_STATIC;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.DefiningClassLoader;

public class ClassFileWriterTest {
    @Test
    public void stackMapTable() throws Exception {
        final String CLASS_NAME = "TestStackMapTable";
        final String METHOD_NAME = "returnObject";

        ClassFileWriter cfw =
                new ClassFileWriter(CLASS_NAME, "java/lang/Object", "ClassFileWriterTest.java");

        // public static Object returnObject()
        cfw.startMethod(METHOD_NAME, "()Ljava/lang/Object;", (short) (ACC_PUBLIC | ACC_STATIC));

        cfw.add(ByteCode.NEW, "java/math/BigInteger");
        cfw.add(ByteCode.DUP);

        // new byte[]{ 123 }
        cfw.addPush(1);
        cfw.add(ByteCode.NEWARRAY, ByteCode.T_BYTE);
        cfw.add(ByteCode.DUP);
        cfw.addPush(0);
        cfw.add(ByteCode.BIPUSH, 123);
        cfw.add(ByteCode.BASTORE);

        // new java.math.BigInteger(bytes)
        cfw.addInvoke(ByteCode.INVOKESPECIAL, "java/math/BigInteger", "<init>", "([B)V");

        // generate StackMapTable
        cfw.add(ByteCode.DUP);
        int target = cfw.acquireLabel();
        cfw.add(ByteCode.IFNULL, target);
        cfw.markLabel(target);

        cfw.add(ByteCode.ARETURN);
        cfw.stopMethod((short) 0);

        byte[] bytecode = cfw.toByteArray();
        DefiningClassLoader loader = new DefiningClassLoader();
        Class<?> cl = loader.defineClass(CLASS_NAME, bytecode);

        Method method = cl.getMethod(METHOD_NAME);
        Object ret = method.invoke(cl);

        assertEquals(ret, new BigInteger(new byte[] {123}));
    }

    @Test
    public void constantPoolIndexAboveShortMaxValue() throws Exception {
        final String CLASS_NAME = "TestLargeConstantPool";
        final String METHOD_NAME = "constant";
        final String CONSTANT = "a constant near the top of the pool";

        ClassFileWriter cfw =
                new ClassFileWriter(CLASS_NAME, "java/lang/Object", "ClassFileWriterTest.java");

        // Push the pool past 0x8000 entries. Each field contributes a name, and they all share the
        // one type descriptor, so the field count is roughly the entry count.
        for (int i = 0; i < 34000; i++) {
            cfw.addField("f" + i, "I", ACC_PUBLIC);
        }

        // public static String constant() { return CONSTANT; }
        cfw.startMethod(METHOD_NAME, "()Ljava/lang/String;", (short) (ACC_PUBLIC | ACC_STATIC));
        cfw.addLoadConstant(CONSTANT);
        cfw.add(ByteCode.ARETURN);
        cfw.stopMethod((short) 0);

        byte[] bytecode = cfw.toByteArray();

        int poolCount = poolCount(bytecode);
        assertTrue(
                poolCount > 0x8000,
                "expected a pool large enough to exercise the upper half of the index range, got "
                        + poolCount);

        DefiningClassLoader loader = new DefiningClassLoader();
        Class<?> cl = loader.defineClass(CLASS_NAME, bytecode);

        Method method = cl.getMethod(METHOD_NAME);
        assertEquals(CONSTANT, method.invoke(cl));
    }

    @Test
    public void repeatedInterfaceInvokeDoesNotGrowConstantPool() {
        ClassFileWriter cfw =
                new ClassFileWriter(
                        "TestIfaceIntern", "java/lang/Object", "ClassFileWriterTest.java");

        // int size() on a List, called twice against the same target
        cfw.startMethod("sizes", "(Ljava/util/List;)I", (short) (ACC_PUBLIC | ACC_STATIC));
        cfw.add(ByteCode.ALOAD_0);
        cfw.addInvoke(ByteCode.INVOKEINTERFACE, "java/util/List", "size", "()I");
        cfw.add(ByteCode.ALOAD_0);
        cfw.addInvoke(ByteCode.INVOKEINTERFACE, "java/util/List", "size", "()I");
        cfw.add(ByteCode.IADD);
        cfw.add(ByteCode.IRETURN);
        cfw.stopMethod((short) 1);

        byte[] first = cfw.toByteArray();

        cfw =
                new ClassFileWriter(
                        "TestIfaceIntern", "java/lang/Object", "ClassFileWriterTest.java");
        cfw.startMethod("sizes", "(Ljava/util/List;)I", (short) (ACC_PUBLIC | ACC_STATIC));
        cfw.add(ByteCode.ALOAD_0);
        cfw.addInvoke(ByteCode.INVOKEINTERFACE, "java/util/List", "size", "()I");
        cfw.add(ByteCode.ICONST_0);
        cfw.add(ByteCode.IADD);
        cfw.add(ByteCode.IRETURN);
        cfw.stopMethod((short) 1);

        byte[] second = cfw.toByteArray();

        // The second call to the same interface method must reuse the interned entry rather than
        // adding a fresh NameAndType and InterfaceMethodref pair
        assertEquals(poolCount(second), poolCount(first));
    }

    /** Reads constant_pool_count, which follows the magic number and the two version numbers. */
    private static int poolCount(byte[] bytecode) {
        return ((bytecode[8] & 0xff) << 8) | (bytecode[9] & 0xff);
    }

    @Test
    public void lineNumberAboveShortMaxValue() throws Exception {
        final String CLASS_NAME = "TestHighLineNumber";
        final String METHOD_NAME = "boom";
        // Above Short.MAX_VALUE, but within the unsigned range a line_number holds
        final int LINE_NUMBER = 40000;

        ClassFileWriter cfw =
                new ClassFileWriter(CLASS_NAME, "java/lang/Object", "ClassFileWriterTest.java");

        // public static void boom() { throw new RuntimeException(); }
        cfw.startMethod(METHOD_NAME, "()V", (short) (ACC_PUBLIC | ACC_STATIC));
        cfw.addLineNumberEntry(LINE_NUMBER);
        cfw.add(ByteCode.NEW, "java/lang/RuntimeException");
        cfw.add(ByteCode.DUP);
        cfw.addInvoke(ByteCode.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V");
        cfw.add(ByteCode.ATHROW);
        cfw.stopMethod((short) 0);

        byte[] bytecode = cfw.toByteArray();
        DefiningClassLoader loader = new DefiningClassLoader();
        Class<?> cl = loader.defineClass(CLASS_NAME, bytecode);

        Method method = cl.getMethod(METHOD_NAME);
        InvocationTargetException thrown =
                assertThrows(InvocationTargetException.class, () -> method.invoke(cl));

        // A sign-extended line number used to borrow into the start_pc half of the packed entry,
        // leaving the frame reporting a line it was never given
        StackTraceElement frame = thrown.getCause().getStackTrace()[0];
        assertEquals(LINE_NUMBER, frame.getLineNumber());
    }
}
