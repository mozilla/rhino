package org.mozilla.javascript.nat.type.definition;

import org.mozilla.javascript.nat.type.TypeInfo;

import java.lang.reflect.WildcardType;
import java.util.List;

/**
 * @see WildcardType
 * @author ZZZank
 */
public interface WildcardTypeInfo extends TypeInfo {

    /**
     * @see WildcardType#getUpperBounds()
     */
    List<TypeInfo> upperBounds(TypeInfoFactory factory);

    /**
     * @see WildcardType#getLowerBounds()
     */
    List<TypeInfo> lowerBounds(TypeInfoFactory factory);

    TypeInfo mainBound();
}
