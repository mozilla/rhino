package org.mozilla.javascript.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.TypeInfoFactory;
import org.mozilla.javascript.nat.type.VariableTypeInfo;

/**
 * @author ZZZank
 */
public class TypeInfoTest {

    /** method name -> method return type */
    private static final Map<String, TypeInfo> TYPES = new HashMap<>();

    static {
        for (var method : Typing.class.getMethods()) {
            var old =
                    TYPES.put(
                            method.getName(),
                            TypeInfoFactory.GLOBAL.create(method.getGenericReturnType()));
            Assert.assertNull(
                    String.format("duplicated method name '%s' in Typing.class", method.getName()),
                    old);
        }
    }

    @Test
    public void rawMap() {
        var typeInfo = TYPES.get("rawMap");
        Assert.assertSame(TypeInfo.NONE, typeInfo.param(0));
        Assert.assertSame(TypeInfo.NONE, typeInfo.param(1));
    }

    @Test
    public void paramMap() {
        var typeInfo = TYPES.get("paramMap");
        Assert.assertEquals(TypeInfo.STRING, typeInfo.param(0));
        Assert.assertEquals(TypeInfo.INT, typeInfo.param(1));
    }

    @Test
    public void wildcard() {
        var typeInfo = TYPES.get("wildcard");
        // wildcard type are converted to its bound
        Assert.assertSame(Number.class, typeInfo.param(0).asClass());
    }

    @Test
    public void variable() {
        var typeInfo = TYPES.get("variable");
        var param = typeInfo.param(0);
        Assert.assertTrue(param instanceof VariableTypeInfo);
        Assert.assertEquals("E", param.toString());
        Assert.assertEquals(Number.class, param.asClass());
    }

    @Test
    public void primitive() {
        var typeInfo = TYPES.get("primitive");
        Assert.assertEquals(TypeInfo.PRIMITIVE_INT, typeInfo);
    }

    @Test
    public void primitiveArray() {
        var typeInfo = TYPES.get("primitiveArray");
        Assert.assertTrue(typeInfo.isArray());
        Assert.assertTrue(typeInfo.getComponentType().isPrimitive());
    }

    @Test
    public void genericArray() {
        var typeInfo = TYPES.get("genericArray");
        Assert.assertTrue(typeInfo.isArray());
        Assert.assertEquals("T", typeInfo.getComponentType().toString());
        Assert.assertEquals(Object.class, typeInfo.getComponentType().asClass());
    }

    @Test
    public void integerArray() {
        var typeInfo = TYPES.get("integerArray");
        Assert.assertTrue(typeInfo.isArray());
        Assert.assertTrue(typeInfo.getComponentType().isNumber());
        Assert.assertTrue(typeInfo.getComponentType().isInt());
        Assert.assertEquals(Integer.class, typeInfo.getComponentType().asClass());
    }

    @Test
    public void clazz() {
        var typeInfo = TYPES.get("clazz");
        Assert.assertEquals(String.class, typeInfo.asClass());
        Assert.assertSame(TypeInfo.NONE, typeInfo.getComponentType());
        Assert.assertEquals(TypeInfo.NONE, typeInfo.param(0));
    }

    @Test
    public void functionalInterface() {
        var typeInfo = TYPES.get("functionalInterface");
        Assert.assertTrue(typeInfo.isInterface());
        Assert.assertTrue(typeInfo.isFunctionalInterface());
    }

    @Test
    public void typeTag() {
        for (var typeInfo : TYPES.values()) {
            Assert.assertEquals(
                    FunctionObject.getTypeTag(typeInfo.asClass()), typeInfo.getTypeTag());
        }
    }

    interface Typing {
        HashMap rawMap();

        HashMap<String, Integer> paramMap();

        Map<? extends Number, String> wildcard();

        <E extends Number> List<E> variable();

        int primitive();

        float[] primitiveArray();

        <T> T[] genericArray(T input);

        Integer[] integerArray();

        String clazz();

        Function functionalInterface();
    }
}
