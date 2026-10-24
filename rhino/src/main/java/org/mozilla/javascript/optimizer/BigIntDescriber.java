package org.mozilla.javascript.optimizer;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import org.mozilla.classfile.DynamicConstantDescriber;
import org.mozilla.classfile.DynamicDescriberRegistry;

public class BigIntDescriber extends DynamicConstantDescriber<BigInteger> {

    private static final ClassDesc CD_BIG_INT_DESCRIBER =
            ClassDesc.of(BigIntDescriber.class.getName());

    private static final ClassDesc CD_BIG_INT_CLASS = ClassDesc.of(BigInteger.class.getName());

    private static final DirectMethodHandleDesc CD_BIG_INT_CONSTANT =
            ConstantDescs.ofConstantBootstrap(
                    CD_BIG_INT_DESCRIBER, "bigIntConstant", CD_BIG_INT_CLASS);

    public BigIntDescriber(DynamicDescriberRegistry registry) {
        super(registry);
    }

    @Override
    public Class<BigInteger> describedType() {
        return BigInteger.class;
    }

    @Override
    public DynamicConstantDesc<BigInteger> describe(BigInteger value) {
        return DynamicConstantDesc.ofNamed(CD_BIG_INT_CONSTANT, value.toString(), CD_BIG_INT_CLASS);
    }

    public static BigInteger bigIntConstant(
            MethodHandles.Lookup lookup, String name, Class<?> type) {
        return new BigInteger(name);
    }
}
