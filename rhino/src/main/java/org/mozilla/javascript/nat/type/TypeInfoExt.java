package org.mozilla.javascript.nat.type;

import org.mozilla.javascript.NativeIterator;
import org.mozilla.javascript.Scriptable;

/**
 * @author ZZZank
 */
public interface TypeInfoExt {
    TypeInfo SCRIPTABLE = TypeInfo.of(Scriptable.class);
    TypeInfo WRAPPED_JAVA_ITERATOR = TypeInfo.of(NativeIterator.WrappedJavaIterator.class);
}
