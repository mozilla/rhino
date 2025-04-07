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
    TypeInfo CONTEXT = TypeInfoFactory.GLOBAL.create(Context.class);
    TypeInfo SCRIPTABLE = TypeInfoFactory.GLOBAL.create(Scriptable.class);
    TypeInfo FUNCTION = TypeInfoFactory.GLOBAL.create(Function.class);
    TypeInfo WRAPPED_JAVA_ITERATOR =
            TypeInfoFactory.GLOBAL.create(NativeIterator.WrappedJavaIterator.class);
    TypeInfo BIG_INT = TypeInfoFactory.GLOBAL.create(BigInteger.class);
}
