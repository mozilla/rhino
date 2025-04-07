package org.mozilla.javascript.tests.type_info;

import org.junit.jupiter.api.Assertions;
import org.mozilla.javascript.FunctionObject;

/**
 * @author ZZZank
 */
public class TypeInfoTestActions {

    public static void defaultTestAction(TypePack pack) {
        var clazz = pack.clazz();
        var info = pack.resolved();
        Assertions.assertSame(clazz, info.asClass());
        Assertions.assertSame(clazz.isArray(), info.isArray());
        Assertions.assertSame(clazz.isPrimitive(), info.isPrimitive());
        Assertions.assertSame(FunctionObject.getTypeTag(clazz), info.getTypeTag());
        Assertions.assertTrue(info.is(clazz));
    }
}
