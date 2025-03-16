package org.mozilla.javascript.nat.type;

import java.math.BigInteger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeIterator;
import org.mozilla.javascript.Scriptable;

/**
 * @see TypeInfo
 * @author ZZZank
 */
public interface TypeInfoExt {
    TypeInfo CONTEXT = TypeInfo.of(Context.class);
    TypeInfo SCRIPTABLE = TypeInfo.of(Scriptable.class);
    TypeInfo FUNCTION = TypeInfo.of(Function.class);
    TypeInfo WRAPPED_JAVA_ITERATOR = TypeInfo.of(NativeIterator.WrappedJavaIterator.class);
    TypeInfo BIG_INT = TypeInfo.of(BigInteger.class);
}
