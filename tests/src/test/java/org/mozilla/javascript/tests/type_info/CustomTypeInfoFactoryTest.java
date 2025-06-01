package org.mozilla.javascript.tests.type_info;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

/**
 * @author ZZZank
 */
public class CustomTypeInfoFactoryTest {

    @Test
    public void init() {
        var contextFactory = new ContextFactory();
        contextFactory.setTypeFactoryProvider(cx -> new NoGenericFactory());

        try (var cx = contextFactory.enterContext()) {
            var scope = cx.initStandardObjects();

            var typeFactory = getTypeFactory(ClassCache.get(scope));

            Assertions.assertInstanceOf(NoGenericFactory.class, typeFactory);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void serde() {
        var contextFactory = new ContextFactory();
        contextFactory.setTypeFactoryProvider(cx -> new NoGenericFactory());

        try (var cx = contextFactory.enterContext()) {
            var scope = cx.initStandardObjects();

            var transferred = simulateSerde(scope);

            var typeFactory = getTypeFactory(ClassCache.get(transferred));

            Assertions.assertInstanceOf(NoGenericFactory.class, typeFactory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TypeInfoFactory getTypeFactory(ClassCache classCache)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        var method = ClassCache.class.getDeclaredMethod("getTypeFactory");
        method.setAccessible(true);
        return (TypeInfoFactory) method.invoke(classCache);
    }

    private static ScriptableObject simulateSerde(ScriptableObject o)
            throws IOException, ClassNotFoundException {
        var output = new ByteArrayOutputStream();
        var objectOut = new ObjectOutputStream(output);
        objectOut.writeObject(o);

        var data = output.toByteArray();

        var input = new ByteArrayInputStream(data);
        var objectIn = new ObjectInputStream(input);
        return (ScriptableObject) objectIn.readObject();
    }
}
