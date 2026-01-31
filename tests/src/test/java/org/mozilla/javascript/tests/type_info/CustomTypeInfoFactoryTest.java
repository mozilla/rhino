package org.mozilla.javascript.tests.type_info;

import java.io.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.*;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.impl.factory.ClassValueCacheFactory;

/**
 * @author ZZZank
 */
public class CustomTypeInfoFactoryTest {

    @Test
    public void associateAfterInit() throws Exception {
        var contextFactory = new ContextFactory();

        try (var cx = contextFactory.enterContext()) {
            var scope = cx.initStandardObjects();

            var toAssociate = AlwaysFailFactory.INSTANCE;
            var associated = toAssociate.associate(scope);

            Assertions.assertNotSame(toAssociate, associated);
        }
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

            Assertions.assertInstanceOf(ClassValueCacheFactory.Concurrent.class, typeFactory);
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
