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
