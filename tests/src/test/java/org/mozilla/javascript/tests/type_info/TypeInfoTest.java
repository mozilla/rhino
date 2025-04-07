package org.mozilla.javascript.tests.type_info;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.nat.type.ParameterizedTypeInfo;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.TypeInfoFactory;
import org.mozilla.javascript.nat.type.VariableTypeInfo;

/**
 * @author ZZZank
 */
public class TypeInfoTest {

    @Test
    public void common() {
        TypesToTest.ALL.values().stream()
                .flatMap(Collection::stream)
                .forEach(TypeInfoTest::commonTestAction);
    }

    private static void commonTestAction(TypePack pack) {
        var clazz = pack.clazz();
        var info = pack.resolved();
        // info should represent the exact class
        Assertions.assertSame(clazz, info.asClass());
        Assertions.assertTrue(info.is(clazz));
        // TypeInfo should behave the same as the original class
        Assertions.assertSame(clazz.isArray(), info.isArray());
        Assertions.assertSame(clazz.isPrimitive(), info.isPrimitive());
        Assertions.assertSame(clazz.isInterface(), info.isInterface());
        Assertions.assertSame(clazz.isEnum(), info.isEnum());
        Assertions.assertSame(FunctionObject.getTypeTag(clazz), info.getTypeTag());
        Assertions.assertSame(Number.class.isAssignableFrom(clazz), info.isNumber());
        Assertions.assertSame(clazz == Object.class, info.isObjectExact());
        if (!clazz.isArray()) {
            Assertions.assertSame(TypeInfo.NONE, info.getComponentType());
        }
    }

    /**
     * additional tests for primitives
     *
     * @see TypesToTest#primitives(float, double, byte, short, int, long, char, boolean)
     */
    @Test
    public void primitives() {
        var action = (Consumer<TypePack>) TypeInfoTest::primitiveTestAction;
        test("primitives", action);
    }

    private static void primitiveTestAction(TypePack pack) {
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
    }

    /**
     * @see TypesToTest#primitiveObjects(Float, Double, Byte, Short, Integer, Long, Character,
     *     Boolean)
     */
    @Test
    public void primitiveObjects() {
        var action =
                (Consumer<TypePack>)
                        pack -> {
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

    /**
     * @see TypesToTest#commonObjects(String, Object, Enum, Class, CharSequence, Comparator,
     *     Character.UnicodeScript, Map, List)
     */
    @Test
    public void commonObjects() {
        var action =
                (Consumer<TypePack>)
                        pack -> {
                            var clazz = pack.clazz();
                            var info = pack.resolved();
                            for (int i = -2; i < 2; i++) {
                                // TypeInfo should return NONE when input is invalid or TypeInfo
                                // itself not
                                // a parameterized type
                                Assertions.assertSame(TypeInfo.NONE, info.param(1));
                            }
                            if (!clazz.isEnum()) {
                                Assertions.assertTrue(info.enumConstants().isEmpty());
                            } else {
                                Assertions.assertEquals(
                                        new HashSet<>(Arrays.asList(clazz.getEnumConstants())),
                                        new HashSet<>(info.enumConstants()));
                            }
                        };
        test("commonObjects", action);
    }

    /**
     * @see TypesToTest#objectArrays(float[], double[], String[], Object[], CharSequence[],
     *     float[][][], String[][][])
     */
    @Test
    public void objectArrays() {
        var action =
                new Consumer<TypePack>() {
                    @Override
                    public void accept(TypePack pack) {
                        commonTestAction(pack);

                        var clazz = pack.clazz();
                        var info = pack.resolved();
                        if (clazz.isArray()) {
                            Assertions.assertTrue(info.isArray());
                            // recursively test array components
                            var component =
                                    pack.map(
                                            c -> ((Class<?>) c).getComponentType(),
                                            Class::getComponentType);
                            this.accept(component);
                        }
                    }
                };
        test("objectArrays", action);
    }

    /**
     * @see TypesToTest#generics(Object, CharSequence, CharSequence, Character.UnicodeScript)
     */
    @Test
    public void generics() {
        var action =
                (Consumer<TypePack>)
                        (pack) -> {
                            if (!(pack.resolved() instanceof VariableTypeInfo)) {
                                return;
                            }
                            var variableInfo = (VariableTypeInfo) pack.resolved();
                            // only TypeVariable can be resolved to VariableTypeInfo
                            var variable = (TypeVariable<?>) pack.raw();

                            // validate name
                            Assertions.assertEquals(variable.getName(), variableInfo.name());

                            // validate bounds
                            testMulti(
                                    variable.getBounds(),
                                    variableInfo.bounds(TypeInfoFactory.GLOBAL));
                        };
        test("generics", action);
    }

    private static void testMulti(Type[] rawTypes, List<TypeInfo> resolved) {
        Assertions.assertEquals(rawTypes.length, resolved.size());
        var rawClasses =
                Arrays.stream(rawTypes).map(TypeInfoTestUtil::getRawType).toArray(Class[]::new);
        for (int i = 0; i < rawTypes.length; i++) {
            var packForBound = new TypePack(rawTypes[i], rawClasses[i], resolved.get(i));
            commonTestAction(packForBound);
        }
    }

    /**
     * probably no additional test required, since array component extracting is tested in {@link
     * #objectArrays()}, and type variable tested in {@link #generics()}
     *
     * @see TypesToTest#genericArrays(Object[], CharSequence[], Object[][][], CharSequence[][][])
     */
    @Test
    public void genericArray() {}

    /**
     * @see TypesToTest#typeParam(Map, List, Function, Map, List, Function)
     */
    public void typeParam() {
        var action =
                (Consumer<TypePack>)
                        pack -> {
                            if (!(pack.resolved() instanceof ParameterizedTypeInfo)) {
                                return;
                            }
                            var parameterizedInfo = (ParameterizedTypeInfo) pack.resolved();
                            // only ParameterizedType can be resolved to ParameterizedTypeInfo
                            var parameterized = (ParameterizedType) pack.raw();

                            // validate raw type
                            commonTestAction(
                                    new TypePack(
                                            parameterized.getRawType(),
                                            pack.clazz(),
                                            parameterizedInfo.rawType()));

                            // validate type args
                            testMulti(
                                    parameterized.getActualTypeArguments(),
                                    parameterizedInfo.params());
                        };
        test("typeParam", action);
    }

    /**
     * well...no
     *
     * <p>The default implementation of {@link TypeInfoFactory} in Rhino will always resolve
     * wildcard type to its main bound, so basic tests in {@link #common()} should be enough
     */
    @Test
    public void wildcard() {}

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
