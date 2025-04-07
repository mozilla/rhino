package org.mozilla.javascript.tests.type_info;

import java.util.*;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.nat.type.TypeInfo;

/**
 * @author ZZZank
 */
public class TypeInfoTest {

    @Test
    public void common() {
        TypesToTest.ALL
            .values()
            .stream()
            .flatMap(Collection::stream)
            .forEach(TypeInfoTest::commonTestAction);
    }

    private static void commonTestAction(TypePack pack) {
        var clazz = pack.clazz();
        var info = pack.resolved();
        Assertions.assertSame(clazz, info.asClass());
        Assertions.assertSame(clazz.isArray(), info.isArray());
        Assertions.assertSame(clazz.isPrimitive(), info.isPrimitive());
        Assertions.assertSame(clazz.isInterface(), info.isInterface());
        Assertions.assertSame(FunctionObject.getTypeTag(clazz), info.getTypeTag());
        Assertions.assertTrue(info.is(clazz));
        Assertions.assertSame(Number.class.isAssignableFrom(clazz), info.isNumber());
        Assertions.assertSame(clazz == Object.class, info.isObjectExact());
        if (!clazz.isArray()) {
            Assertions.assertSame(TypeInfo.NONE, info.getComponentType());
        }
    }

    /// additional tests for primitives
    /// @see TypesToTest#primitives(float, double, byte, short, int, long, char, boolean)
    @Test
    public void primitives() {
        var action = (Consumer<TypePack>) pack -> {
            var clazz = pack.clazz();
            var info = pack.resolved();
            Assertions.assertSame(clazz == int.class, info.isInt());
            Assertions.assertSame(clazz == short.class, info.isShort());
            Assertions.assertSame(clazz == long.class, info.isLong());
            Assertions.assertSame(clazz == byte.class, info.isByte());
            Assertions.assertSame(clazz == char.class, info.isCharacter());
            Assertions.assertSame(clazz == float.class, info.isFloat());
            Assertions.assertSame(clazz == double.class, info.isDouble());
            Assertions.assertSame(clazz == boolean.class, info.isBoolean());
            Assertions.assertSame(clazz == void.class, info.isVoid());
        };
        test("primitives", action);
    }

    /// @see TypesToTest#primitiveObjects(Float, Double, Byte, Short, Integer, Long, Character, Boolean)
    @Test
    public void primitiveObjects() {
        var action = (Consumer<TypePack>) pack -> {
            var clazz = pack.clazz();
            var info = pack.resolved();
            Assertions.assertSame(clazz == Integer.class, info.isInt());
            Assertions.assertSame(clazz == Short.class, info.isShort());
            Assertions.assertSame(clazz == Long.class, info.isLong());
            Assertions.assertSame(clazz == Byte.class, info.isByte());
            Assertions.assertSame(clazz == Character.class, info.isCharacter());
            Assertions.assertSame(clazz == Float.class, info.isFloat());
            Assertions.assertSame(clazz == Double.class, info.isDouble());
            Assertions.assertSame(clazz == Boolean.class, info.isBoolean());
            Assertions.assertSame(clazz == Void.class, info.isVoid());
        };
        test("primitiveObjects", action);
    }

    /// @see TypesToTest#commonObjects(String, Object, Enum, Class, CharSequence, Character.UnicodeScript)
    @Test
    public void commonObjects() {
        var action = (Consumer<TypePack>) pack -> {
            var clazz = pack.clazz();
            var info = pack.resolved();
            for (int i = -2; i < 2; i++) {
                // TypeInfo should return NONE when input is invalid or TypeInfo itself not
                // a parameterized type
                Assertions.assertSame(TypeInfo.NONE, info.param(1));
            }
            if (!clazz.isEnum()) {
                Assertions.assertTrue(info.enumConstants().isEmpty());
            } else {
                Assertions.assertEquals(new HashSet<>(Arrays.asList(clazz.getEnumConstants())), new HashSet<>(info.enumConstants()));
            }
        };
        test("commonObjects", action);
    }

    /// @see TypesToTest#objectArrays(float[], double[], String[], Object[], CharSequence[], float[][][], String[][][])
    @Test
    public void objectArrays() {
        var action = new Consumer<TypePack>() {
            @Override
            public void accept(TypePack pack) {
                commonTestAction(pack);

                var clazz = pack.clazz();
                var info = pack.resolved();
                if (clazz.isArray()) {
                    Assertions.assertTrue(info.isArray());
                    // recursively test array components
                    var component = pack.map(c -> ((Class<?>) c).getComponentType(), Class::getComponentType);
                    this.accept(component);
                }
            }
        };
        test("objectArrays", action);
    }

    @SafeVarargs
    public static void test(String name, Consumer<TypePack>... testActions) {
        for (var pack : get(name)) {
            for (var testAction : testActions) {
                testAction.accept(pack);
            }
        }
    }

    private static List<TypePack> get(String name) {
        return Objects.requireNonNull(TypesToTest.ALL.get(name));
    }
}
