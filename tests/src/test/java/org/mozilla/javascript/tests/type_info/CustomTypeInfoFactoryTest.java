package org.mozilla.javascript.tests.type_info;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.*;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.impl.factory.ConcurrentFactory;

/**
 * @author ZZZank
 */
public class CustomTypeInfoFactoryTest {

    /**
     * @see #exampleFunctionObjectMethod(Context, Scriptable, Object[], Function)
     * @see AlwaysFailFactory
     */
    @Test
    public void functionObject() {
        var contextFactory = new ContextFactory();

        try (var cx = contextFactory.enterContext()) {
            var scope = new NativeObject();
            AlwaysFailFactory.INSTANCE.associate(scope);
            cx.initStandardObjects(scope);

            var method =
                    Arrays.stream(CustomTypeInfoFactoryTest.class.getDeclaredMethods())
                            .filter(
                                    m -> {
                                        var mod = m.getModifiers();
                                        return Modifier.isPublic(mod) && Modifier.isStatic(mod);
                                    })
                            .filter(m -> m.getName().equals("exampleFunctionObjectMethod"))
                            .findFirst()
                            .orElseThrow();
            Assertions.assertThrowsExactly(
                    AssertionError.class,
                    () -> new FunctionObject("test", method, scope),
                    AlwaysFailFactory.MESSAGE);
        }
    }

    public static void exampleFunctionObjectMethod(
            Context cx, Scriptable scope, Object[] args, Function fn) {
        throw new AssertionError("method for test purpose only, do not invoke");
    }

    @Test
    public void associate() throws Exception {
        var contextFactory = new ContextFactory();

        try (var cx = contextFactory.enterContext()) {
            var scope = new NativeObject();
            new NoGenericNoCacheFactory().associate(scope);
            cx.initStandardObjects(scope);

            var typeFactory = TypeInfoFactory.get(scope);

            Assertions.assertInstanceOf(NoGenericNoCacheFactory.class, typeFactory);
        }
    }

    @Test
    public void defaultFactory() throws Exception {
        var contextFactory = new ContextFactory();

        try (var cx = contextFactory.enterContext()) {
            var scope = cx.initStandardObjects();

            var typeFactory = TypeInfoFactory.get(scope);

            Assertions.assertInstanceOf(ConcurrentFactory.class, typeFactory);
        }
    }

    @Test
    public void serdeCustom() throws Exception {
        var contextFactory = new ContextFactory();
        byte[] data;
        try (var cx = contextFactory.enterContext()) {
            var scope = new NativeObject();
            new NoGenericNoCacheFactory().associate(scope);
            cx.initStandardObjects(scope);

            data = simulateSer(scope);
        }

        try (var cx = contextFactory.enterContext()) {
            var transferred = simulateDeser(data);

            var typeFactory = TypeInfoFactory.get(transferred);
            Assertions.assertInstanceOf(NoGenericNoCacheFactory.class, typeFactory);
        }
    }

    @Test
    public void serdeGlobal() throws Exception {
        var contextFactory = new ContextFactory();
        byte[] data;
        try (var cx = contextFactory.enterContext()) {
            var scope = new NativeObject();
            TypeInfoFactory.GLOBAL.associate(scope);
            cx.initStandardObjects(scope);

            data = simulateSer(scope);
        }

        try (var cx = contextFactory.enterContext()) {
            var transferred = simulateDeser(data);

            var typeFactory = TypeInfoFactory.get(transferred);
            Assertions.assertSame(TypeInfoFactory.GLOBAL, typeFactory);
        }
    }

    private static byte[] simulateSer(ScriptableObject o) throws IOException {
        var output = new ByteArrayOutputStream();
        var objectOut = new ObjectOutputStream(output);
        objectOut.writeObject(o);

        return output.toByteArray();
    }

    private static ScriptableObject simulateDeser(byte[] data)
            throws IOException, ClassNotFoundException {
        var input = new ByteArrayInputStream(data);
        var objectIn = new ObjectInputStream(input);
        return (ScriptableObject) objectIn.readObject();
    }
}
