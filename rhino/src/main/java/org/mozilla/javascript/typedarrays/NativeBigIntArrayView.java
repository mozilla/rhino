package org.mozilla.javascript.typedarrays;

import java.math.BigInteger;
import org.mozilla.javascript.ScriptRuntime;

public abstract class NativeBigIntArrayView extends NativeTypedArrayView<BigInteger> {
    private static final long serialVersionUID = -3349222145964894609L;

    protected NativeBigIntArrayView() {}

    protected NativeBigIntArrayView(NativeArrayBuffer ab, int off, int len, int byteLen) {
        super(ab, off, len, byteLen);
    }

    @Override
    protected Object toNumeric(Object num) {
        return ScriptRuntime.toBigInt(num);
    }
}
